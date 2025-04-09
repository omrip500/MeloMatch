package com.example.melomatch.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class WaveView extends View {

    private float amplitude = 1f; // ערך ראשוני
    private Paint paint;

    public WaveView(Context context) {
        super(context);
        init();
    }

    public WaveView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public WaveView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(5f);
        paint.setStyle(Paint.Style.FILL);
    }

    public void setAmplitude(float rms) {
        this.amplitude = rms;
        invalidate(); // מצייר מחדש
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float centerY = getHeight() / 2f;
        float centerX = getWidth() / 2f;

        float barHeight = amplitude * 4f; // מכפיל כדי שיהיה בולט

        // קו אופקי עבה/דק בהתאם לקול
        canvas.drawRect(0, centerY - barHeight, getWidth(), centerY + barHeight, paint);
    }
}
