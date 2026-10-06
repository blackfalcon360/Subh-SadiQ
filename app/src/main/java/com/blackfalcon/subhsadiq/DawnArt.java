package com.blackfalcon.subhsadiq;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;

import java.util.Random;

/** Little picture of what the two dawns look like. */
public class DawnArt extends View {

    private final boolean sadiq;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

    public DawnArt(Context c, boolean sadiq) {
        super(c);
        this.sadiq = sadiq;
    }

    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);
        float w = getWidth(), h = getHeight();
        float horizon = h * 0.78f;

        // sky
        int[] cols = sadiq
                ? new int[]{0xFF04040F, 0xFF101A44, 0xFF2B3F7A, 0xFFE59A4E}
                : new int[]{0xFF02020A, 0xFF050A22, 0xFF0A1236, 0xFF14204A};
        p.setShader(new LinearGradient(0, 0, 0, horizon, cols, new float[]{0f, 0.45f, 0.8f, 1f}, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, horizon, p);
        p.setShader(null);

        // stars
        Random r = new Random(sadiq ? 3 : 5);
        int n = sadiq ? 14 : 40;
        p.setColor(Color.WHITE);
        for (int i = 0; i < n; i++) {
            float x = r.nextFloat() * w, y = r.nextFloat() * horizon * 0.7f;
            p.setAlpha(80 + r.nextInt(175));
            c.drawCircle(x, y, 1.5f + r.nextFloat() * 1.5f, p);
        }
        p.setAlpha(255);

        if (!sadiq) {
            // false dawn: a faint column of light standing straight up
            Path cone = new Path();
            cone.moveTo(w * 0.37f, horizon);
            cone.lineTo(w * 0.5f, h * 0.05f);
            cone.lineTo(w * 0.63f, horizon);
            cone.close();
            p.setShader(new LinearGradient(0, horizon, 0, h * 0.05f, 0xAAFFE9A8, 0x00FFE9A8, Shader.TileMode.CLAMP));
            c.drawPath(cone, p);
            p.setShader(null);
        } else {
            // true dawn: light spreading sideways along the horizon
            p.setShader(new LinearGradient(0, horizon - h * 0.30f, 0, horizon, 0x00FFB060, 0xCCFFC080, Shader.TileMode.CLAMP));
            c.drawRect(0, horizon - h * 0.30f, w, horizon, p);
            p.setShader(new RadialGradient(w * 0.5f, horizon, w * 0.55f, 0xAAFFD090, 0x00FFD090, Shader.TileMode.CLAMP));
            c.save();
            c.scale(1f, 0.35f, w * 0.5f, horizon);
            c.drawCircle(w * 0.5f, horizon, w * 0.55f, p);
            c.restore();
            p.setShader(null);
        }

        // ground + a mosque silhouette
        p.setColor(Color.BLACK);
        Path g = new Path();
        g.moveTo(0, h);
        g.lineTo(0, horizon + h * 0.02f);
        g.quadTo(w * 0.2f, horizon - h * 0.05f, w * 0.4f, horizon + h * 0.01f);
        g.quadTo(w * 0.6f, horizon + h * 0.04f, w * 0.75f, horizon);
        g.lineTo(w, horizon);
        g.lineTo(w, h);
        g.close();
        c.drawPath(g, p);
        c.drawRect(w * 0.74f, horizon - h * 0.07f, w * 0.86f, horizon + 2, p);
        c.drawArc(new RectF(w * 0.745f, horizon - h * 0.17f, w * 0.855f, horizon - h * 0.01f), 180, 180, true, p);
        c.drawRect(w * 0.88f, horizon - h * 0.22f, w * 0.90f, horizon + 2, p);
        Path cap = new Path();
        cap.moveTo(w * 0.875f, horizon - h * 0.22f);
        cap.lineTo(w * 0.89f, horizon - h * 0.30f);
        cap.lineTo(w * 0.905f, horizon - h * 0.22f);
        cap.close();
        c.drawPath(cap, p);
    }
}
