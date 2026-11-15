package app.kafa;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;
import java.util.List;

public class HeadView extends View {
    private volatile double[] target = {1, 0, 0, 0};
    private double[] shown = {1, 0, 0, 0};
    private volatile double smooth = 0.5; // 0.05 (cok yumusak) .. 1 (ham)
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();

    public HeadView(Context c) { super(c); }
    public void setTarget(double[] q) { target = q; }
    public void setSmooth(double s) { smooth = s; }

    @Override protected void onDraw(Canvas cv) {
        cv.drawColor(Color.rgb(14, 21, 27));
        shown = ImuCore.nlerp(shown, target, smooth);
        List<double[]> tris = ImuCore.headMesh(shown);
        float w = getWidth(), h = getHeight();
        float sc = Math.min(w, h) / 3.2f;
        float cx = w / 2, cy = h / 2;
        p.setStyle(Paint.Style.FILL_AND_STROKE);
        p.setStrokeWidth(1f);
        for (double[] t : tris) {
            path.rewind();
            path.moveTo(cx + (float) t[0] * sc, cy - (float) t[1] * sc);
            path.lineTo(cx + (float) t[2] * sc, cy - (float) t[3] * sc);
            path.lineTo(cx + (float) t[4] * sc, cy - (float) t[5] * sc);
            path.close();
            p.setColor(Color.rgb((int) t[7], (int) t[8], (int) t[9]));
            cv.drawPath(path, p);
        }
        postInvalidateOnAnimation();
    }
}
