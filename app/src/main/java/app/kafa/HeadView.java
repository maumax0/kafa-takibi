package app.kafa;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;
import java.util.List;

public class HeadView extends View {
    private volatile double[] target = {1, 0, 0, 0};
    private double[] shown = {1, 0, 0, 0};
    private volatile double smooth = 0.5; // 0.05 (cok yumusak) .. 1 (ham)
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

    public HeadView(Context c) { super(c); }
    public void setTarget(double[] q) { target = q; }
    public void setSmooth(double s) { smooth = s; }

    @Override protected void onDraw(Canvas cv) {
        cv.drawColor(Color.rgb(14, 21, 27));
        shown = ImuCore.nlerp(shown, target, smooth);
        List<double[]> segs = ImuCore.headLines(shown);
        float w = getWidth(), h = getHeight();
        float sc = Math.min(w, h) / 4.3f;
        float cx = w / 2, cy = h / 2;
        for (double[] s : segs) {
            int k = (int) s[5];
            int a = (int) (255 * Math.max(0.15, Math.min(1, 0.6 + s[4] * 0.4)));
            int col;
            float sw = 2f;
            switch (k) {
                case 1: col = Color.rgb(255, 140, 50); sw = 7f; a = 255; break;
                case 2: col = Color.argb(a, 70, 230, 205); sw = 3f; break;
                case 3: col = Color.RED; break;
                case 4: col = Color.GREEN; break;
                case 5: col = Color.CYAN; break;
                default: col = Color.argb(a, 150, 205, 230);
            }
            p.setColor(col);
            p.setStrokeWidth(sw);
            cv.drawLine(cx + (float) s[0] * sc, cy - (float) s[1] * sc, cx + (float) s[2] * sc, cy - (float) s[3] * sc, p);
        }
        postInvalidateOnAnimation();
    }
}
