package com.github.mikephil.charting.charts;

import com.github.mikephil.charting.data.DataSet;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.utils.Utils;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.util.AttributeSet;

import java.util.ArrayList;

/**
 * Chart that draws lines, surfaces, circles, ...
 *
 * @author Philipp Jahoda
 */
public class LineChart extends BarLineChartBase<LineDataSet> {

  /**
   * the radius of the circle-shaped value indicators
   */
  protected float mCircleSize = 4f;

  /**
   * the width of the highlighning line
   */
  protected float mHighlightWidth = 3f;

  /**
   * if true, the data will also be drawn filled
   */
  protected boolean mDrawFilled = false;

  /**
   * if true, drawing circles is enabled
   */
  protected boolean mDrawCircles = true;

  /**
   * flag for cubic curves instead of lines
   */
  protected boolean mDrawCubic = false;

  /**
   * Tells how many values should be treated as place holders
   */
  protected int mValuePadding = 0;

  public LineChart(Context context) {
    super(context);
  }

  public LineChart(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public LineChart(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }

  @Override
  protected void init() {
    super.init();

    mCircleSize = Utils.convertDpToPixel(mCircleSize);

    mHighlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    mHighlightPaint.setStyle(Paint.Style.STROKE);
    mHighlightPaint.setStrokeWidth(2f);
    mHighlightPaint.setColor(Color.rgb(255, 187, 115));
  }

  @Override
  protected void drawHighlights() {

    // if there are values to highlight and highlighnting is enabled, do it
    if (mHighlightEnabled && mHighLightIndicatorEnabled && valuesToHighlight()) {

      for (int i = 0; i < mIndicesToHightlight.length; i++) {

        DataSet set = getDataSetByIndex(mIndicesToHightlight[i].getDataSetIndex());

        int xIndex = mIndicesToHightlight[i].getXIndex(); // get the
        // x-position
        float y = set.getYValForXIndex(xIndex); // get the y-position

        float[] pts = new float[] {
            xIndex, mYChartMax, xIndex, mYChartMin, 0, y, mDeltaX, y
        };

        transformValueToPixel(pts);
        // draw the highlight lines
        mDrawCanvas.drawLines(pts, mHighlightPaint);
      }
    }
  }

  /**
   * draws the given y values to the screen
   */
  @Override
  protected void drawData() {

    ArrayList<LineDataSet> dataSets = mCurrentData.getDataSets();

    if (mDrawFilled) {
      float heightOffset = pixelHeightToValue(mOffsetBottom + mOffsetTop);
      for (int i = 0; i < mCurrentData.getDataSetCount(); i++) {

        LineDataSet dataSet = dataSets.get(i);
        ArrayList<Entry> entries = dataSet.getYVals();

        // if drawing filled is enabled
        if (entries.size() > 0) {
          Path filled = new Path();
          filled.moveTo(entries.get(0).getXIndex(), entries.get(0).getVal());

          // create a new path
          for (int x = 1; x < entries.size(); x++) {

            filled.lineTo(entries.get(x).getXIndex(), entries.get(x).getVal());
          }

          // close up
          float y = mYChartMin - heightOffset;
          filled.lineTo(entries.get(entries.size() - 1).getXIndex(), y);
          filled.lineTo(entries.get(0).getXIndex(), y);
          filled.close();

          transformPath(filled);

          mDrawCanvas.drawPath(filled, dataSet.getDrawingSpec().getFillPaint());
        }
      }
    }

    for (int i = 0; i < mCurrentData.getDataSetCount(); i++) {

      DataSet dataSet = dataSets.get(i);
      ArrayList<Entry> entries = dataSet.getYVals();

      float[] valuePoints = generateTransformedValues(entries, 0f);

      Paint paint = mCurrentData.getDataSetByIndex(i).getDrawingSpec().getBasicPaint();

      if (mDrawCubic) {
        Path spline = new Path();

        spline.moveTo(entries.get(0).getXIndex(), entries.get(0).getVal());

        // create a new path
        for (int x = 1; x < entries.size() - 3; x += 2) {

          // spline.rQuadTo(entries.get(x).getXIndex(),
          // entries.get(x).getVal(), entries.get(x+1).getXIndex(),
          // entries.get(x+1).getVal());

          spline.cubicTo(entries.get(x).getXIndex(), entries.get(x).getVal(), entries
              .get(x + 1).getXIndex(), entries.get(x + 1).getVal(), entries
              .get(x + 2).getXIndex(), entries.get(x + 2).getVal());
        }

        // spline.close();

        transformPath(spline);

        mDrawCanvas.drawPath(spline, paint);
      } else {
        for (int j = 0; j < valuePoints.length - 2; j += 2) {

          if (isOffContentRight(valuePoints[j]))
            break;

          // make sure the lines don't do shitty things outside bounds
          if (j != 0 && isOffContentLeft(valuePoints[j - 1])
              && isOffContentTop(valuePoints[j + 1])
              && isOffContentBottom(valuePoints[j + 1]))
            continue;

          mDrawCanvas.drawLine(valuePoints[j], valuePoints[j + 1], valuePoints[j + 2],
              valuePoints[j + 3], paint);
        }
      }
    }
  }

  /**
   * Calculates the middle point between two points and multiplies its
   * coordinates with the given smoothness _Mulitplier.
   *
   * @param p1 First point
   * @param p2 Second point
   * @param _Result Resulting point
   * @param mult Smoothness multiplier
   */
  private void calculatePointDiff(PointF p1, PointF p2, PointF _Result, float mult) {
    float diffX = p2.x - p1.x;
    float diffY = p2.y - p1.y;
    _Result.x = (p1.x + (diffX * mult));
    _Result.y = (p1.y + (diffY * mult));
  }

  @Override
  protected void drawValues() {

    // if values are drawn
    if (mDrawYValues && mCurrentData.getYValCount() < mMaxVisibleCount * mScaleX) {

      // make sure the values do not interfear with the circles
      int valOffset = (int) (mCircleSize * 1.7f);

      if (!mDrawCircles)
        valOffset = valOffset / 2;

      ArrayList<LineDataSet> dataSets = mCurrentData.getDataSets();

      final int padding = mValuePadding * 2;
      for (int i = 0; i < mCurrentData.getDataSetCount(); i++) {

        DataSet dataSet = dataSets.get(i);
        ArrayList<Entry> entries = dataSet.getYVals();

        float[] positions = generateTransformedValues(entries, 0f);

        for (int j = padding; j < positions.length - padding; j += 2) {

          if (isOffContentRight(positions[j]))
            break;

          if (isOffContentLeft(positions[j]) || isOffContentTop(positions[j + 1])
              || isOffContentBottom(positions[j + 1]))
            continue;

          float val = entries.get(j / 2).getVal();

          String label;
          if (mDrawValueXLabelsInChart) {
            label = mCurrentData.getXLabels().get(j / 2);
          } else {
            label = mDrawUnitInChart ? mValueFormat.format(val) + mUnit : mValueFormat.format(val);
          }

          float yPosition = positions[j + 1];
          if (j - 1 >= 0 && j + 3 < positions.length && positions[j - 1] < yPosition && positions[j + 3] < yPosition) {
            yPosition += valOffset + mValuePaint.getTextSize();
          } else {
            yPosition -= valOffset;
          }
          mDrawCanvas.drawText(label, positions[j],
              yPosition, mValuePaint);
        }
      }
    }
  }

  /**
   * draws the circle value indicators
   */
  @Override
  protected void drawAdditional() {
    // if drawing circles is enabled
    if (mDrawCircles) {

      ArrayList<LineDataSet> dataSets = mCurrentData.getDataSets();

      for (int i = 0; i < mCurrentData.getDataSetCount(); i++) {

        LineDataSet dataSet = dataSets.get(i);
        ArrayList<Entry> entries = dataSet.getYVals();

        float[] positions = generateTransformedValues(entries, 0f);

        final int padding = mValuePadding * 2;
        for (int j = padding; j < positions.length - padding; j += 2) {

          if (isOffContentRight(positions[j]))
            break;

          // make sure the circles don't do shitty things outside
          // bounds
          if (isOffContentLeft(positions[j]) || isOffContentTop(positions[j + 1])
              || isOffContentBottom(positions[j + 1]))
            continue;

          mDrawCanvas.drawCircle(positions[j], positions[j + 1], mCircleSize,
              dataSet.getDrawingSpec().getBasicPaint());
          mDrawCanvas.drawCircle(positions[j], positions[j + 1], mCircleSize / 2,
              dataSet.getDrawingSpec().getDataPointInnerCirclePaint());
        }
      }
    }
  }

  /**
   * set this to true to enable the drawing of circle indicators
   *
   * @param enabled
   */
  public void setDrawCircles(boolean enabled) {
    this.mDrawCircles = enabled;
  }

  /**
   * returns true if drawing circles is enabled, false if not
   *
   * @return
   */
  public boolean isDrawCirclesEnabled() {
    return mDrawCircles;
  }

  /**
   * sets the size (radius) of the circle shpaed value indicators, default
   * size = 4f
   *
   * @param size
   */
  public void setCircleSize(float size) {
    mCircleSize = size;
  }

  /**
   * returns the circlesize
   *
   * @param size
   */
  public float getCircleSize(float size) {
    return size;
  }

  /**
   * set if the chartdata should be drawn as a line or filled default = line /
   * default = false, disabling this will give up to 20% performance boost on
   * large datasets
   *
   * @param filled
   */
  public void setDrawFilled(boolean filled) {
    mDrawFilled = filled;
  }

  /**
   * returns true if filled drawing is enabled, false if not
   *
   * @return
   */
  public boolean isDrawFilledEnabled() {
    return mDrawFilled;
  }

  public void setValuePadding(int valuePadding) {
    mValuePadding = valuePadding;
  }

  public int getValuePadding() {
    return mValuePadding;
  }

  /**
   * set the width of the highlightning lines, default 3f
   *
   * @param width
   */
  public void setHighlightLineWidth(float width) {
    mHighlightWidth = width;
  }

  /**
   * returns the width of the highlightning line, default 3f
   *
   * @return
   */
  public float getHighlightLineWidth() {
    return mHighlightWidth;
  }

  @Override
  public void setPaint(Paint p, int which) {
    switch (which) {
    case PAINT_HIGHLIGHT_LINE:
      mHighlightPaint = p;
      break;
    default:
      super.setPaint(p, which);
      break;
    }
  }

  @Override
  public Paint getPaint(int which) {
    switch (which) {
    case PAINT_HIGHLIGHT_LINE:
      return mHighlightPaint;
    default:
      return super.getPaint(which);
    }
  }

  @Override
  protected LineDataSet createDataSet(ArrayList<Entry> approximated, String label) {
    return new LineDataSet(approximated, label);
  }
}
