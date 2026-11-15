package app.kafa;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends Activity {
    private static final UUID[] UUIDS = {
        UUID.fromString("00000000-deca-fade-deca-deafdecacafe"),
        UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")};

    private final Handler ui = new Handler(Looper.getMainLooper());
    private HeadView head;
    private TextView status, readout, hint;
    private Spinner spinner;
    private Button btnConnect, btnTare, btnCal;
    private final List<BluetoothDevice> devices = new ArrayList<>();

    private volatile BluetoothSocket socket;
    private volatile boolean running = false;
    private final ImuCore.Calib calib = new ImuCore.Calib();
    private volatile double[] lastRaw = {1, 0, 0, 0};
    private volatile long packets = 0;
    private long tStart = 0;

    // kalibrasyon durumu: 0 yok, 1 duz bak, 2 sallama, 3 cevirme
    private volatile int calStep = 0;
    private volatile long calEnd = 0;
    private ImuCore.AxisCapture nodCap, shakeCap;
    private double[] nodAxis;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        int pad = dp(12);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(14, 21, 27));
        root.setPadding(pad, dp(32), pad, pad);

        status = text("Kulakliklari tak, telefona esle, sonra sec.", 14, Color.rgb(160, 180, 195));
        root.addView(status);

        spinner = new Spinner(this);
        root.addView(spinner);

        LinearLayout r1 = row();
        btnConnect = button("Baglan");
        r1.addView(btnConnect, lp());
        root.addView(r1);

        head = new HeadView(this);
        root.addView(head, new LinearLayout.LayoutParams(-1, 0, 1f));

        readout = text("roll 0  pitch 0  yaw 0", 20, Color.WHITE);
        readout.setTypeface(Typeface.MONOSPACE);
        readout.setGravity(Gravity.CENTER);
        root.addView(readout);

        hint = text("", 15, Color.rgb(255, 170, 90));
        hint.setGravity(Gravity.CENTER);
        root.addView(hint);

        LinearLayout r2 = row();
        btnTare = button("Sifirla");
        btnCal = button("Kalibre et");
        r2.addView(btnTare, lp());
        r2.addView(btnCal, lp());
        root.addView(r2);

        TextView sl = text("Yumusatma", 12, Color.rgb(160, 180, 195));
        root.addView(sl);
        SeekBar sb = new SeekBar(this);
        sb.setMax(100);
        sb.setProgress(55);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int v, boolean u) { head.setSmooth(1.0 - 0.95 * v / 100.0); }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
        });
        head.setSmooth(1.0 - 0.95 * 0.55);
        root.addView(sb);
        setContentView(root);

        btnConnect.setOnClickListener(v -> { if (running) disconnect(); else connect(); });
        btnTare.setOnClickListener(v -> { calib.tare(lastRaw); hint.setText("Sifirlandi: su anki durus ileri bakis."); });
        btnCal.setOnClickListener(v -> startCalibration());

        ensurePermission();
        ui.post(tickUi);
    }

    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density); }
    private TextView text(String s, int sp, int color) {
        TextView t = new TextView(this); t.setText(s); t.setTextSize(sp); t.setTextColor(color); t.setPadding(0, dp(4), 0, dp(4)); return t;
    }
    private Button button(String s) { Button b = new Button(this); b.setText(s); return b; }
    private LinearLayout row() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); return l; }
    private LinearLayout.LayoutParams lp() { return new LinearLayout.LayoutParams(0, -2, 1f); }

    // ---- izin ve cihaz listesi ----
    private void ensurePermission() {
        if (Build.VERSION.SDK_INT >= 31 && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1);
        } else loadDevices();
    }
    @Override public void onRequestPermissionsResult(int rc, String[] p, int[] r) {
        if (r.length > 0 && r[0] == PackageManager.PERMISSION_GRANTED) loadDevices();
        else status.setText("Bluetooth izni gerekli. Ayarlardan izin ver.");
    }
    private void loadDevices() {
        try {
            BluetoothAdapter ad = ((BluetoothManager) getSystemService(BLUETOOTH_SERVICE)).getAdapter();
            devices.clear();
            List<String> names = new ArrayList<>();
            int pick = 0;
            for (BluetoothDevice d : ad.getBondedDevices()) {
                devices.add(d);
                String n = d.getName() == null ? d.getAddress() : d.getName();
                if (n.toLowerCase().contains("freebuds")) pick = names.size();
                names.add(n);
            }
            ArrayAdapter<String> a = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names);
            a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(a);
            spinner.setSelection(pick);
            if (devices.isEmpty()) status.setText("Eslesmis cihaz yok. Once kulakligi esle.");
        } catch (SecurityException e) { status.setText("Bluetooth izni yok."); }
    }

    // ---- baglanti ----
    private void connect() {
        int i = spinner.getSelectedItemPosition();
        if (i < 0 || i >= devices.size()) return;
        final BluetoothDevice dev = devices.get(i);
        running = true; packets = 0; tStart = System.currentTimeMillis();
        btnConnect.setText("Baglantiyi kes");
        status.setText("Baglaniyor...");
        new Thread(() -> {
            BluetoothSocket s = null;
            Exception last = null;
            try {
                
                for (UUID u : UUIDS) {
                    try {
                        s = dev.createRfcommSocketToServiceRecord(u);
                        s.connect();
                        last = null;
                        break;
                    } catch (Exception e) { last = e; try { s.close(); } catch (Exception x) {} s = null; }
                }
                if (s == null) throw last != null ? last : new Exception("baglanamadi");
                socket = s;
                final BluetoothSocket fs = s;
                ui.post(() -> status.setText("Bagli. IMU akisi bekleniyor (iki kulaklik da kulakta olmali)."));
                OutputStream out = s.getOutputStream();
                InputStream in = s.getInputStream();
                out.write(ImuCore.START); out.flush();
                ImuCore.Parser parser = new ImuCore.Parser((cnt, q) -> onImu(q));
                byte[] buf = new byte[1024];
                long lastKick = System.currentTimeMillis();
                while (running) {
                    int n = in.read(buf);
                    if (n < 0) break;
                    parser.feed(buf, n);
                }
            } catch (Exception e) {
                final String m = e.getMessage();
                ui.post(() -> status.setText("Baglanti hatasi: " + m));
            } finally {
                try { if (socket != null) { socket.getOutputStream().write(ImuCore.STOP); socket.close(); } } catch (Exception e) {}
                socket = null; running = false;
                ui.post(() -> btnConnect.setText("Baglan"));
            }
        }, "bt").start();
    }
    private void disconnect() {
        running = false;
        try { BluetoothSocket s = socket; if (s != null) { s.getOutputStream().write(ImuCore.STOP); s.close(); } } catch (Exception e) {}
        status.setText("Baglanti kesildi.");
    }

    // ---- veri akisi (arka plan evresi) ----
    private void onImu(double[] q) {
        lastRaw = q;
        packets++;
        if (packets == 1) calib.tare(q);
        // kalibrasyon adimlari
        long now = System.currentTimeMillis();
        int st = calStep;
        if (st == 2 && nodCap != null) nodCap.add(calib.delta(q));
        if (st == 3 && shakeCap != null) shakeCap.add(calib.delta(q));
        if (st != 0 && now >= calEnd) advanceCalibration(q);
        head.setTarget(calib.head(q));
    }

    // ---- kalibrasyon ----
    private void startCalibration() {
        if (!running || packets == 0) { hint.setText("Once baglan."); return; }
        calib.resetAxes();
        calStep = 1; calEnd = System.currentTimeMillis() + 3000;
        hint.setText("1/3  Dik otur, duz ileri bak. Kipirdama (3 sn)");
    }
    private void advanceCalibration(double[] q) {
        if (calStep == 1) {
            calib.tare(q);
            nodCap = new ImuCore.AxisCapture();
            calStep = 2; calEnd = System.currentTimeMillis() + 5000;
            ui.post(() -> hint.setText("2/3  Basini ONCE ASAGI egip kaldir, 3-4 kez tekrarla (5 sn)"));
        } else if (calStep == 2) {
            nodAxis = nodCap.axis(new double[]{0, 1, 0}, 1);
            shakeCap = new ImuCaptureHolder().make();
            calStep = 3; calEnd = System.currentTimeMillis() + 5000;
            ui.post(() -> hint.setText(nodAxis == null ? "Sallama algilanmadi, devam: yine de cevir (5 sn)" : "3/3  Basini ONCE SAGA cevirip geri getir, 3-4 kez (5 sn)"));
        } else if (calStep == 3) {
            double[] shakeAxis = shakeCap.axis(new double[]{0, 0, 1}, -1);
            double[] m = ImuCore.buildAxisFix(nodAxis, shakeAxis);
            calStep = 0;
            if (m == null) {
                ui.post(() -> hint.setText("Kalibrasyon basarisiz: hareketler yetersiz veya ayni eksende. Tekrar dene."));
            } else {
                calib.m = m; calib.axisOk = true;
                calib.tare(q);
                ui.post(() -> hint.setText("Kalibrasyon tamam. Eksen kacagi duzeltildi."));
            }
        }
    }
    private static class ImuCaptureHolder { ImuCore.AxisCapture make() { return new ImuCore.AxisCapture(); } }

    // ---- arayuz guncelleme ----
    private final Runnable tickUi = new Runnable() {
        public void run() {
            double[] e = ImuCore.euler(calib.head(lastRaw));
            readout.setText(String.format("roll %4.0f  pitch %4.0f  yaw %4.0f", e[0], e[1], e[2]));
            if (running && packets > 0) {
                double sec = Math.max(1, (System.currentTimeMillis() - tStart) / 1000.0);
                status.setText(String.format("Canli: %d paket, ~%.0f Hz%s", packets, packets / sec, calib.axisOk ? ", eksen kalibreli" : ""));
            }
            ui.postDelayed(this, 100);
        }
    };

    @Override protected void onDestroy() { disconnect(); super.onDestroy(); }
}
