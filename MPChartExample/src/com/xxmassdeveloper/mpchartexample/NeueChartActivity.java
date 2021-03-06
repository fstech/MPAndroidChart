package com.xxmassdeveloper.mpchartexample;

import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.ChartData;
import com.github.mikephil.charting.data.ChartData.LabelFormatter;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.Highlight;
import com.github.mikephil.charting.utils.YLabels.YLabelPosition;
import com.xxmassdeveloper.mpchartexample.notimportant.DemoBase;

import android.content.res.Resources;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Shader.TileMode;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Transformation;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class NeueChartActivity extends DemoBase implements OnChartValueSelectedListener {
  private static final int MAX_VALS_PER_PAGE = 6;
  private LineChart mChart;
  private boolean mListVisible = false;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
        WindowManager.LayoutParams.FLAG_FULLSCREEN);
    setContentView(R.layout.activity_neuechart);

    final Resources r = getResources();

    mChart = (LineChart) findViewById(R.id.chart1);
    mChart.setValuePadding(1);
    mChart.setUnit("$");
    mChart.setOffsets(0, 0, 0, 0);
    mChart.setStartAtZero(false);
    mChart.setHighlightEnabled(false);
    mChart.setHighlightIndicatorEnabled(false);
    mChart.setOnChartValueSelectedListener(this);
    mChart.setValuePaintColor(r.getColor(R.color.neue_text));
    mChart.setValueTypeface(Typeface.DEFAULT_BOLD);
    mChart.setCircleSize(4f);
    mChart.setTouchEnabled(true);
    mChart.setDragEnabled(true);
    mChart.setMaxScaleY(1.0f);
    mChart.setPinchZoom(true);
    mChart.setDrawFilled(true);
    mChart.setDrawXLabels(false);
    mChart.setDrawYLabels(true);
    mChart.setDrawAxisLabelsInChart(true);
    mChart.setDrawGridBackground(false);
    mChart.setBackgroundColor(r.getColor(R.color.neue_fill));
    mChart.setDrawVerticalGrid(false);
    mChart.setGridColor(r.getColor(R.color.neue_grid));
    mChart.setDrawBorder(false);
    mChart.setDrawValueXLabelsInChart(true);
    mChart.getYLabels().setPosition(YLabelPosition.RIGHT);
    mChart.getPaint(Chart.PAINT_YLABEL).setColor(r.getColor(R.color.neue_text));

    MyMarkerView mv = new MyMarkerView(this);
    mChart.setMarkerView(mv);

    mChart.setHighlightIndicatorEnabled(false);
    setData(30, 10000, 100000);
    mChart.setDrawLegend(false);
    mChart.zoom(mChart.getDataCurrent().getXValCount() / MAX_VALS_PER_PAGE, 1.0f, mChart.getWidth() / 2, mChart.getHeight() / 2);
    mChart.invalidate();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.neue, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {

    switch (item.getItemId()) {
    case R.id.actionToggleSize: {
      final int initialHeight = mChart.getMeasuredHeight();
      final int targetHeight = mListVisible ? ((View) mChart.getParent()).getMeasuredHeight() : mChart.getMeasuredHeight() * 5 / 6;
      final int delta = initialHeight - targetHeight;

      Animation anim = new Animation() {
        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
          mChart.getLayoutParams().height = initialHeight - (int) (interpolatedTime * delta);
          mChart.requestLayout();
        }

        @Override
        public boolean willChangeBounds() {
          return true;
        }
      };
      anim.setDuration(500);
      mChart.startAnimation(anim);
      mListVisible = !mListVisible;
      break;
    }
    }
    return true;
  }

  @Override
  public void onValuesSelected(Entry[] values, Highlight[] highlights) {
    Log.i("VALS SELECTED",
        "Value: " + values[0].getVal() + ", xIndex: " + highlights[0].getXIndex()
            + ", DataSet index: " + highlights[0].getDataSetIndex());
  }

  @Override
  public void onNothingSelected() {
    // TODO Auto-generated method stub

  }

  private LineDataSet createSet(String name, int count, float range, float rangeOffset, boolean dashed) {
    Resources r = getResources();
    ArrayList<Entry> yVals = new ArrayList<Entry>();

    for (int i = 0; i < count; i++) {
      float mult = (range + 1);
      float val = rangeOffset + (float) (Math.random() * mult);
      yVals.add(new Entry(val, i));
    }

    // create a dataset and give it a type
    LineDataSet set = new LineDataSet(yVals, name);
    Paint paint = set.getDrawingSpec().getBasicPaint();
    paint.setColor(getResources().getColor(R.color.neue_line));
    if (dashed) {
      paint.setPathEffect(new DashPathEffect(new float[] { 3, 3 }, 0));
    }
    Point size = new Point();
    getWindowManager().getDefaultDisplay().getSize(size);
    set.getDrawingSpec().getFillPaint().setShader(new LinearGradient(0, 0, 0, size.y, r.getColor(R.color.neue_gradient_start), r.getColor(R.color.neue_gradient_end), TileMode.CLAMP));
    set.getDrawingSpec().getDataPointInnerCirclePaint().setColor(r.getColor(R.color.neue_fill));
    return set;
  }

  private void setData(int count, float range, float rangeOffset) {
    ArrayList<Long> xVals = new ArrayList<Long>();
    long ts = System.currentTimeMillis();

    for (int i = 0; i < count; i++) {
      xVals.add(ts);
      ts += TimeUnit.DAYS.toMillis(2);
    }

    ArrayList<LineDataSet> dataSets = new ArrayList<LineDataSet>();
    dataSets.add(createSet("Data 1", count, range, rangeOffset, true));
    dataSets.add(createSet("Data 2", count, range, rangeOffset, false));

    // create a data object with the datasets
    ChartData<LineDataSet> data = new ChartData<LineDataSet>(xVals, dataSets, new LabelFormatter() {
      SimpleDateFormat sdf = new SimpleDateFormat("MMM dd", Locale.US);

      @Override
      public String formatValue(long value) {
        return sdf.format(new Date(value)).toUpperCase();
      }
    }, 1);

    // set data
    mChart.setData(data);
  }
}
