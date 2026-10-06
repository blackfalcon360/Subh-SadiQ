package com.blackfalcon.subhsadiq;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

/** Night -> Subh Kazib -> dark gap -> Subh Sadiq -> sunrise, with a marker for "now". */
public class TimelineView extends View {

    private static final int[] COLS = {
            Color.parseColor("#0B0B2E"), Color.parseColor("#FFB300"), Color.parseColor("#2A2A44"),
            Color.parseColor("#2ECC71"), Color.parseColor("#FFD54F")};
    private static final String[] LEGEND = {"Night", "Subh Kazib", "Dark gap", "Subh Sadiq"};

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private long[] ev;       // 4 boundaries
    private String[] labels; // 4 time strings
    private long now = -1;

    public TimelineView(Context c) {
        super(c);
    }

    public void set(long[] events, String[] timeLabels, long nowMs) {
        ev = events; labels = timeLabels; now = nowMs;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);
        if (ev == null) return;
        for (long e : ev) if (e == Dawn.NONE) return;

        float w = getWidth(), h = getHeight();
        float size = Math.min(w * 0.032f, h * 0.11f);
        long t0 = ev[0] - 20 * 60000L, t1 = ev[3] + 10 * 60000L;
        long[] b = {t0, ev[0], ev[1], ev[2], ev[3], t1};
        float pad = w * 0.03f;
        float barTop = h * 0.22f, barBottom = h * 0.42f;
        float span = w - 2 * pad;

        // coloured segments
        for (int i = 0; i < 5; i++) {
            float x0 = pad + span * (b[i] - t0) / (float) (t1 - t0);
            float x1 = pad + span * (b[i + 1] - t0) / (float) (t1 - t0);
            p.setStyle(Paint.Style.FILL);
            p.setColor(COLS[i]);
            c.drawRect(x0, barTop, x1, barBottom, p);
        }

        // boundary ticks + times (alternate rows so close ones do not overlap)
        p.setTextAlign(Paint.Align.CENTER);
        p.setTextSize(size);
        for (int i = 0; i < 4; i++) {
            float x = pad + span * (ev[i] - t0) / (float) (t1 - t0);
            p.setColor(Color.WHITE);
            p.setStrokeWidth(2f);
            float row = (i % 2 == 0) ? barBottom + size * 1.2f : barBottom + size * 2.5f;
            c.drawLine(x, barBottom, x, row - size * 0.9f, p);
            c.drawText(labels[i], x, row, p);
        }

        // "now" marker
        if (now >= t0 && now <= t1) {
            float x = pad + span * (now - t0) / (float) (t1 - t0);
            p.setColor(Color.WHITE);
            p.setStrokeWidth(4f);
            c.drawLine(x, barTop - size * 0.3f, x, barBottom, p);
            Path tri = new Path();
            tri.moveTo(x, barTop - size * 0.2f);
            tri.lineTo(x - size * 0.5f, barTop - size * 1.2f);
            tri.lineTo(x + size * 0.5f, barTop - size * 1.2f);
            tri.close();
            p.setStyle(Paint.Style.FILL);
            c.drawPath(tri, p);
            p.setTextSize(size * 0.9f);
            c.drawText("now", x, barTop - size * 1.45f, p);
            p.setTextSize(size);
        }

        // legend
        float ly = h - size * 0.4f;
        float step = span / 4f;
        p.setTextAlign(Paint.Align.LEFT);
        p.setTextSize(size * 0.95f);
        for (int i = 0; i < 4; i++) {
            float x = pad + step * i;
            p.setColor(COLS[i]);
            c.drawRoundRect(new RectF(x, ly - size * 0.8f, x + size * 0.8f, ly), 4, 4, p);
            p.setColor(Color.parseColor("#C0C0D8"));
            c.drawText(LEGEND[i], x + size * 1.0f, ly - size * 0.05f, p);
        }
        p.setTextAlign(Paint.Align.CENTER);
    }
}
