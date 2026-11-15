package app.kafa;

import java.util.ArrayList;
import java.util.List;

/** Android'e bagimli olmayan cekirdek: cerceve cozucu, kuaterniyon matematigi, kalibrasyon, 3B cizim. */
public class ImuCore {
    public static final byte[] START = hex("5A0006002B58010101" + "4209");
    public static final byte[] STOP = hex("5A0006002B58010100" + "5228");

    public static byte[] hex(String s) {
        byte[] b = new byte[s.length() / 2];
        for (int i = 0; i < b.length; i++) b[i] = (byte) Integer.parseInt(s.substring(2 * i, 2 * i + 2), 16);
        return b;
    }

    public static int crc16(byte[] d, int off, int len) {
        int c = 0;
        for (int i = off; i < off + len; i++) {
            c ^= (d[i] & 0xff) << 8;
            for (int k = 0; k < 8; k++) c = ((c & 0x8000) != 0) ? ((c << 1) ^ 0x1021) & 0xffff : (c << 1) & 0xffff;
        }
        return c;
    }

    // ---------- cerceve cozucu ----------
    public interface Listener { void onImu(long counter, double[] q); }

    public static class Parser {
        private byte[] buf = new byte[8192];
        private int n = 0;
        private final Listener l;
        public Parser(Listener l) { this.l = l; }

        public void feed(byte[] d, int len) {
            if (n + len > buf.length) { n = 0; } // tasma: sifirla
            System.arraycopy(d, 0, buf, n, len);
            n += len;
            int p = 0;
            while (true) {
                while (p < n && buf[p] != 0x5A) p++;
                if (n - p < 7) break;
                int L = ((buf[p + 1] & 0xff) << 8) | (buf[p + 2] & 0xff);
                if (L < 3 || L > 1024) { p++; continue; }
                int e = p + 3 + L + 2;
                if (e > n) break;
                int crc = ((buf[e - 2] & 0xff) << 8) | (buf[e - 1] & 0xff);
                if (crc16(buf, p, e - p - 2) == crc) {
                    handle(p, e);
                    p = e;
                } else p++;
            }
            if (p > 0) { System.arraycopy(buf, p, buf, 0, n - p); n -= p; }
        }

        private void handle(int p, int e) {
            if ((buf[p + 4] & 0xff) != 0x2B || (buf[p + 5] & 0xff) != 0x59) return;
            int i = p + 6, end = e - 2;
            long cnt = -1;
            double[] q = null;
            while (i + 2 <= end) {
                int tag = buf[i] & 0xff, ln = buf[i + 1] & 0xff;
                int v = i + 2;
                i = v + ln;
                if (i > end) break;
                if (tag == 1 && ln == 4) cnt = u32le(buf, v);
                if (tag == 2 && ln == 16) {
                    q = new double[4];
                    for (int k = 0; k < 4; k++) q[k] = Float.intBitsToFloat((int) u32le(buf, v + 4 * k));
                }
            }
            if (q != null && cnt >= 0) { normalize(q); l.onImu(cnt, q); }
        }
    }

    static long u32le(byte[] b, int o) {
        return (b[o] & 0xffL) | ((b[o + 1] & 0xffL) << 8) | ((b[o + 2] & 0xffL) << 16) | ((b[o + 3] & 0xffL) << 24);
    }

    // ---------- kuaterniyon (w,x,y,z) ----------
    public static double[] mul(double[] a, double[] b) {
        return new double[]{
            a[0] * b[0] - a[1] * b[1] - a[2] * b[2] - a[3] * b[3],
            a[0] * b[1] + a[1] * b[0] + a[2] * b[3] - a[3] * b[2],
            a[0] * b[2] - a[1] * b[3] + a[2] * b[0] + a[3] * b[1],
            a[0] * b[3] + a[1] * b[2] - a[2] * b[1] + a[3] * b[0]};
    }
    public static double[] inv(double[] a) { return new double[]{a[0], -a[1], -a[2], -a[3]}; }
    public static void normalize(double[] q) {
        double l = Math.sqrt(q[0] * q[0] + q[1] * q[1] + q[2] * q[2] + q[3] * q[3]);
        if (l < 1e-9) { q[0] = 1; q[1] = q[2] = q[3] = 0; return; }
        for (int i = 0; i < 4; i++) q[i] /= l;
    }
    public static double[] nlerp(double[] a, double[] b, double t) {
        double d = a[0] * b[0] + a[1] * b[1] + a[2] * b[2] + a[3] * b[3];
        double s = d < 0 ? -1 : 1;
        double[] r = new double[4];
        for (int i = 0; i < 4; i++) r[i] = a[i] * (1 - t) + s * b[i] * t;
        normalize(r);
        return r;
    }
    /** Donme vektoru (derece cinsinden aci * eksen), radyan. */
    public static double[] rotVec(double[] q) {
        double s = q[0] < 0 ? -1 : 1;
        double vn = Math.sqrt(q[1] * q[1] + q[2] * q[2] + q[3] * q[3]);
        if (vn < 1e-12) return new double[]{0, 0, 0};
        double ang = 2 * Math.atan2(vn, Math.abs(q[0]));
        return new double[]{s * q[1] / vn * ang, s * q[2] / vn * ang, s * q[3] / vn * ang};
    }
    /** Roll(x), pitch(y), yaw(z) derece. */
    public static double[] euler(double[] q) {
        double w = q[0], x = q[1], y = q[2], z = q[3];
        double roll = Math.atan2(2 * (w * x + y * z), 1 - 2 * (x * x + y * y));
        double pitch = Math.asin(Math.max(-1, Math.min(1, 2 * (w * y - z * x))));
        double yaw = Math.atan2(2 * (w * z + x * y), 1 - 2 * (y * y + z * z));
        return new double[]{Math.toDegrees(roll), Math.toDegrees(pitch), Math.toDegrees(yaw)};
    }
    public static double[][] matrix(double[] q) {
        double w = q[0], x = q[1], y = q[2], z = q[3];
        return new double[][]{
            {1 - 2 * (y * y + z * z), 2 * (x * y - w * z), 2 * (x * z + w * y)},
            {2 * (x * y + w * z), 1 - 2 * (x * x + z * z), 2 * (y * z - w * x)},
            {2 * (x * z - w * y), 2 * (y * z + w * x), 1 - 2 * (x * x + y * y)}};
    }
    /** Sutunlari x,y,z eksenleri olan ortonormal matristen kuaterniyon. */
    public static double[] fromAxes(double[] ax, double[] ay, double[] az) {
        double m00 = ax[0], m10 = ax[1], m20 = ax[2], m01 = ay[0], m11 = ay[1], m21 = ay[2], m02 = az[0], m12 = az[1], m22 = az[2];
        double tr = m00 + m11 + m22, w, x, y, z;
        if (tr > 0) {
            double s = Math.sqrt(tr + 1) * 2; w = s / 4; x = (m21 - m12) / s; y = (m02 - m20) / s; z = (m10 - m01) / s;
        } else if (m00 > m11 && m00 > m22) {
            double s = Math.sqrt(1 + m00 - m11 - m22) * 2; w = (m21 - m12) / s; x = s / 4; y = (m01 + m10) / s; z = (m02 + m20) / s;
        } else if (m11 > m22) {
            double s = Math.sqrt(1 + m11 - m00 - m22) * 2; w = (m02 - m20) / s; x = (m01 + m10) / s; y = s / 4; z = (m12 + m21) / s;
        } else {
            double s = Math.sqrt(1 + m22 - m00 - m11) * 2; w = (m10 - m01) / s; x = (m02 + m20) / s; y = (m12 + m21) / s; z = s / 4;
        }
        double[] q = {w, x, y, z};
        normalize(q);
        return q;
    }

    // ---------- kalibrasyon ----------
    public static class Calib {
        public double[] q0 = {1, 0, 0, 0};   // durus referansi
        public double[] m = {1, 0, 0, 0};    // kulaklik -> bas ekseni duzeltmesi
        public boolean axisOk = false;

        public double[] delta(double[] q) { return mul(inv(q0), q); }
        public double[] head(double[] q) { return mul(mul(inv(m), delta(q)), m); }
        public void tare(double[] q) { q0 = q.clone(); }
        public void resetAxes() { m = new double[]{1, 0, 0, 0}; axisOk = false; }
    }

    /** Bir hareket sirasinda donme vektorlerini toplar ve ana donme eksenini bulur. */
    public static class AxisCapture {
        private final List<double[]> rv = new ArrayList<>();
        public void add(double[] deltaQ) { rv.add(rotVec(deltaQ)); }
        public int size() { return rv.size(); }

        /** @param sign ilk belirgin (>10 derece) sapmanin eksenle iliskisi: +1 ise eksen o yone bakar, -1 ise ters. */
        public double[] axis(double[] guess, int sign) {
            double[][] c = new double[3][3];
            double maxMag = 0;
            for (double[] v : rv) {
                for (int i = 0; i < 3; i++) for (int j = 0; j < 3; j++) c[i][j] += v[i] * v[j];
                maxMag = Math.max(maxMag, Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]));
            }
            if (Math.toDegrees(maxMag) < 12) return null; // yeterli hareket yok
            double[] a = guess.clone();
            for (int it = 0; it < 60; it++) {
                double[] b = new double[3];
                for (int i = 0; i < 3; i++) b[i] = c[i][0] * a[0] + c[i][1] * a[1] + c[i][2] * a[2];
                double l = Math.sqrt(b[0] * b[0] + b[1] * b[1] + b[2] * b[2]);
                if (l < 1e-12) return null;
                for (int i = 0; i < 3; i++) a[i] = b[i] / l;
            }
            double[] first = null;
            for (double[] v : rv) {
                if (Math.toDegrees(Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2])) > 10) { first = v; break; }
            }
            double d = first == null ? 0 : first[0] * a[0] + first[1] * a[1] + first[2] * a[2];
            if (d * sign < 0) for (int i = 0; i < 3; i++) a[i] = -a[i];
            return a;
        }
    }

    /** Bas sallama (pitch) ve cevirme (yaw) eksenlerinden duzeltme kuaterniyonunu kurar; basarisizsa null. */
    public static double[] buildAxisFix(double[] nodAxis, double[] shakeAxis) {
        if (nodAxis == null || shakeAxis == null) return null;
        double[] y = nodAxis;
        double dot = shakeAxis[0] * y[0] + shakeAxis[1] * y[1] + shakeAxis[2] * y[2];
        double[] z = {shakeAxis[0] - dot * y[0], shakeAxis[1] - dot * y[1], shakeAxis[2] - dot * y[2]};
        double zl = Math.sqrt(z[0] * z[0] + z[1] * z[1] + z[2] * z[2]);
        if (zl < 0.35) return null; // iki hareket ayni eksende
        for (int i = 0; i < 3; i++) z[i] /= zl;
        double[] x = {y[1] * z[2] - y[2] * z[1], y[2] * z[0] - y[0] * z[2], y[0] * z[1] - y[1] * z[0]};
        return fromAxes(x, y, z);
    }

    // ---------- 3B cizim (Android ve test ortak) ----------
    /** Her parca: {x1,y1,x2,y2,derinlik(-1..1), tur}. tur 0 ag, 1 burun, 2 kulaklik, 3 eksen x, 4 y, 5 z */
    public static List<double[]> headLines(double[] q) {
        double[][] R = matrix(q);
        double cy = Math.toRadians(-35), ct = Math.toRadians(14);
        List<double[]> out = new ArrayList<>();
        double rx = 1.05, ry = 0.88, rz = 1.15;
        int segs = 28;
        // enlem halkalari (z sabit)
        for (int r = -3; r <= 3; r++) {
            double lat = r * Math.PI / 8.5, cz = Math.sin(lat) * rz, rr = Math.cos(lat);
            double[] prev = null;
            for (int s = 0; s <= segs; s++) {
                double a = 2 * Math.PI * s / segs;
                double[] p = {rx * rr * Math.cos(a), ry * rr * Math.sin(a), cz};
                if (prev != null) addSeg(out, R, cy, ct, prev, p, 0);
                prev = p;
            }
        }
        // boylam cizgileri
        for (int m = 0; m < 8; m++) {
            double a = Math.PI * m / 4;
            double[] prev = null;
            for (int s = 0; s <= 20; s++) {
                double lat = -Math.PI / 2 + Math.PI * s / 20;
                double[] p = {rx * Math.cos(lat) * Math.cos(a), ry * Math.cos(lat) * Math.sin(a), rz * Math.sin(lat)};
                if (prev != null) addSeg(out, R, cy, ct, prev, p, 0);
                prev = p;
            }
        }
        // burun (ileri +x)
        addSeg(out, R, cy, ct, new double[]{rx * 0.98, 0, -0.05}, new double[]{rx + 0.5, 0, -0.2}, 1);
        addSeg(out, R, cy, ct, new double[]{rx + 0.5, 0, -0.2}, new double[]{rx * 0.95, 0, -0.35}, 1);
        // kulakliklar ve saplari
        for (int s = -1; s <= 1; s += 2) {
            double[] c = {0, s * (ry + 0.05), -0.05};
            double[] prev = null;
            for (int k = 0; k <= 12; k++) {
                double a = 2 * Math.PI * k / 12;
                double[] p = {c[0] + 0.2 * Math.cos(a), c[1], c[2] + 0.2 * Math.sin(a)};
                if (prev != null) addSeg(out, R, cy, ct, prev, p, 2);
                prev = p;
            }
            addSeg(out, R, cy, ct, new double[]{c[0] + 0.03, c[1], c[2] - 0.2}, new double[]{c[0] + 0.08, c[1], c[2] - 0.75}, 2);
        }
        // baslangic eksen oklari (basa bagli degil, sabit)
        double[][] I = {{1, 0, 0}, {0, 1, 0}, {0, 0, 1}};
        for (int k = 0; k < 3; k++) {
            double[] o = {-1.9, 1.9, -1.5};
            double[] e = {o[0] + I[k][0] * 0.8, o[1] + I[k][1] * 0.8, o[2] + I[k][2] * 0.8};
            addSegNoRot(out, cy, ct, o, e, 3 + k);
        }
        return out;
    }

    private static double[] view(double[] p, double cy, double ct) {
        double x = p[0] * Math.cos(cy) - p[1] * Math.sin(cy), y = p[0] * Math.sin(cy) + p[1] * Math.cos(cy), z = p[2];
        double x2 = x * Math.cos(ct) + z * Math.sin(ct), z2 = -x * Math.sin(ct) + z * Math.cos(ct);
        return new double[]{x2, y, z2};
    }
    private static double[] rot(double[][] R, double[] v) {
        return new double[]{R[0][0] * v[0] + R[0][1] * v[1] + R[0][2] * v[2],
            R[1][0] * v[0] + R[1][1] * v[1] + R[1][2] * v[2],
            R[2][0] * v[0] + R[2][1] * v[1] + R[2][2] * v[2]};
    }
    private static void addSeg(List<double[]> out, double[][] R, double cy, double ct, double[] a, double[] b, int kind) {
        addSegNoRot(out, cy, ct, rot(R, a), rot(R, b), kind);
    }
    private static void addSegNoRot(List<double[]> out, double cy, double ct, double[] a, double[] b, int kind) {
        double[] va = view(a, cy, ct), vb = view(b, cy, ct);
        // ekran: sag = y (kisinin solu), yukari = z, derinlik = x (izleyiciye dogru)
        double pa = 1 / (1 - va[0] * 0.1), pb = 1 / (1 - vb[0] * 0.1);
        out.add(new double[]{va[1] * pa, va[2] * pa, vb[1] * pb, vb[2] * pb, (va[0] + vb[0]) / 2, kind});
    }
}
