package com.example.filip.chemeq.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.TextureView;

/**
 * From the TFLite demo project
 */
public class AutoFitTV extends TextureView {
    private int ratioWidth = 0;
    private int ratioHeight = 0;

    public AutoFitTV(Context context) {
        this(context, null);
    }

    public AutoFitTV(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AutoFitTV(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setAspectRatio(final int width, final int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Size cannot be negative.");
        }
        ratioWidth = width;
        ratioHeight = height;
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        final int width = MeasureSpec.getSize(widthMeasureSpec);
        final int height = MeasureSpec.getSize(heightMeasureSpec);
        if (0 == ratioWidth || 0 == ratioHeight) {
            setMeasuredDimension(width, height);
        } else {
            if (width < height * ratioWidth / ratioHeight) {
                setMeasuredDimension(width, width * ratioHeight / ratioWidth);
            } else {
                setMeasuredDimension(height * ratioWidth / ratioHeight, height);
            }
        }
    }
}
