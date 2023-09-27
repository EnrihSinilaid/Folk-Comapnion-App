package com.enrih.folkbluetoothcompanion.common;

import android.content.Context;
import android.util.AttributeSet;

import com.github.mikephil.charting.charts.BarLineChartBase;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.interfaces.dataprovider.LineDataProvider;

public class LineChartCustom extends BarLineChartBase<LineData> implements LineDataProvider {
    public LineChartCustom(Context context) {
        super(context);
    }

    public LineChartCustom(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LineChartCustom(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void init() {
        super.init();

        mRenderer = new LineChartRenderCustom(this, mAnimator, mViewPortHandler);
    }

    @Override
    public LineData getLineData() {
        return mData;
    }

    @Override
    protected void onDetachedFromWindow() {
        // releases the bitmap in the renderer to avoid oom error
        if (mRenderer != null && mRenderer instanceof LineChartRenderCustom) {
            ((LineChartRenderCustom) mRenderer).releaseBitmap();
        }
        super.onDetachedFromWindow();
    }
}
