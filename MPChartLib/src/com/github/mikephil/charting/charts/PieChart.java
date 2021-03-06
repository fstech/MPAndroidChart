package com.github.mikephil.charting.charts;

import com.github.mikephil.charting.data.DataSet;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.listener.PieChartTouchListener;
import com.github.mikephil.charting.utils.Legend;
import com.github.mikephil.charting.utils.Legend.LegendPosition;
import com.github.mikephil.charting.utils.MulticolorDrawingSpec;
import com.github.mikephil.charting.utils.PieChartAnimator;
import com.github.mikephil.charting.utils.Utils;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;

import java.text.DecimalFormat;
import java.util.ArrayList;

/**
 * View that represents a pie chart. Draws cake like slices.
 *
 * @author Philipp Jahoda
 */
public class PieChart extends Chart<PieDataSet> {

  /**
   * rect object that represents the bounds of the piechart, needed for
   * drawing the circle
   */
  private RectF mCircleBox = new RectF();

  /**
   * holds the current rotation angle of the chart
   */
  private float mChartAngle = 0f;

  /**
   * array that holds the width of each pie-slice in degrees
   */
  private float[] mSlicesAngleWidth;

  /**
   * array that holds the absolute angle in degrees of each slice
   */
  private float[] mSlicesAnglePosition;

  /**
   * if true, the white hole inside the chart will be drawn
   */
  private boolean mDrawHole = true;

  /**
   * variable for the text that is drawn in the center of the pie-chart. If
   * this value is null, the default is "Total Value\n + getYValueSum()"
   */
  private String mCenterText = null;

  /**
   * indicates the selection distance of a pie slice
   */
  private float mShift = 20f;

  /**
   * the space in degrees between the chart-slices, default 0f
   */
  private float mSliceSpace = 0f;

  /**
   * indicates the size of the hole in the center of the piechart, default:
   * radius / 2
   */
  private float mHoleRadiusPercent = 50f;

  /**
   * the radius of the transparent circle next to the chart-hole in the center
   */
  private float mTransparentCircleRadius = 55f;

  /**
   * if enabled, centertext is drawn
   */
  private boolean mDrawCenterText = true;

  /**
   * set this to true to draw the x-values next to the values in the pie
   * slices
   */
  private boolean mDrawXVals = true;

  /**
   * if set to true, all values show up in percent instead of their real value
   */
  private boolean mUsePercentValues = false;

  /**
   * paint for the hole in the center of the pie chart and the transparent
   * circle
   */
  private Paint mHolePaint;

  private GestureDetector mGestureDetector;

  private PieChartAnimator mAnimator;

  /**
   * paint object for the text that can be displayed in the center of the
   * chart
   */
  private Paint mCenterTextPaint;

  private RotationListener mRotationListener;

  public PieChart(Context context) {
    super(context);
  }

  public PieChart(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public PieChart(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
  }

  @Override
  protected void onDetachedFromWindow() {
    stopRotationAnimation();
    super.onDetachedFromWindow();
  }

  @Override
  protected void init() {
    super.init();

    mShift = Utils.convertDpToPixel(mShift);

    mHolePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    mCenterTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    mCenterTextPaint.setColor(mColorDarkBlue);
    mCenterTextPaint.setTextSize(Utils.convertDpToPixel(12f));
    mCenterTextPaint.setTextAlign(Align.CENTER);

    mValuePaint.setTextSize(Utils.convertDpToPixel(13f));
    mValuePaint.setColor(Color.WHITE);
    mValuePaint.setTextAlign(Align.CENTER);

    mGestureDetector = new GestureDetector(this.getContext(), new PieChartTouchListener(this));
    mAnimator = new PieChartAnimator(this);

    // for the piechart, drawing values is enabled
    mDrawYValues = true;
  }

  public PieChartAnimator getAnimator() {
    return mAnimator;
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);

    if (mDataNotSet)
      return;

    long starttime = System.currentTimeMillis();

    drawHighlights();

    drawData();

    drawAdditional();

    drawValues();

    drawLegend();

    drawDescription();

    drawCenterText();

    canvas.drawBitmap(mDrawBitmap, 0, 0, mDrawPaint);

  }

  /**
   * does all necessary preparations, needed when data is changed or flags
   * that effect the data are changed
   */
  @Override
  public void prepare() {
    if (mDataNotSet)
      return;

    super.prepare();

    calcMinMax(false);

    if (mCenterText == null)
      mCenterText = "Total Value\n" + (int) getYValueSum();

    // calculate how many digits are needed
    calcFormats();

    prepareLegend();
  }

  @Override
  public void notifyDataSetChanged() {
    // TODO
  }

  @Override
  protected void calculateOffsets() {

    if (mDrawLegend) {
      if (mLegend.getPosition() == LegendPosition.RIGHT_OF_CHART) {

        mLegendLabelPaint.setTextAlign(Align.LEFT);
        mOffsetTop = (int) (mLegendLabelPaint.getTextSize() * 3.5f);
      } else if (mLegend.getPosition() == LegendPosition.BELOW_CHART_LEFT
          || mLegend.getPosition() == LegendPosition.BELOW_CHART_RIGHT) {
        mOffsetBottom = (int) (mLegendLabelPaint.getTextSize() * 3.5f);
      }
    }

    prepareContentRect();

    float scaleX = (float) ((getWidth() - mOffsetLeft - mOffsetRight) / mDeltaX);
    float scaleY = (float) ((getHeight() - mOffsetBottom - mOffsetTop) / mDeltaY);

    Matrix val = new Matrix();
    val.postTranslate(0, -mYChartMin);
    val.postScale(scaleX, -scaleY);

    mMatrixValueToPx.set(val);

    Matrix offset = new Matrix();
    offset.postTranslate(mOffsetLeft, getHeight() - mOffsetBottom);

    mMatrixOffset.set(offset);
  }

  /**
   * the decimalformat responsible for formatting the values in the chart
   */
  protected DecimalFormat mFormatValue = null;

  /**
   * calculates the required number of digits for the y-legend and for the
   * values that might be drawn in the chart (if enabled)
   */
  protected void calcFormats() {

    // -1 means calculate digits
    if (mValueDigitsToUse == -1)
      mValueFormatDigits = Utils.getPieFormatDigits(mDeltaY);
    else
      mValueFormatDigits = mValueDigitsToUse;

    StringBuffer b = new StringBuffer();
    for (int i = 0; i < mValueFormatDigits; i++) {
      if (i == 0)
        b.append(".");
      b.append("0");
    }

    mFormatValue = new DecimalFormat("###,###,###,##0" + b.toString());
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    // Let the GestureDetector interpret this event
    boolean result = mGestureDetector.onTouchEvent(event);

    if (!result) {
      if (event.getAction() == MotionEvent.ACTION_UP && getSlicesAnglePosition() != null) {
        centerOnHighlighted();
        result = true;
      }
    }
    return result;
  }

  /**
   * the angle where the dragging started
   */
  private float mStartAngle = 0f;

  /**
   * sets the starting angle of the rotation, this is only used by the touch
   * listener, x and y is the touch position
   *
   * @param x
   * @param y
   */
  public void setStartAngle(float x, float y) {
    mStartAngle = getAngleForPoint(x, y);

    // take the current angle into consideration when starting a new drag
    mStartAngle -= mChartAngle;
  }

  /**
   * updates the view rotation depending on the given touch position, also
   * takes the starting angle into consideration
   *
   * @param x
   * @param y
   */
  public void updateRotation(float x, float y) {
    mChartAngle = getAngleForPoint(x, y);
    // take the offset into consideration
    mChartAngle -= mStartAngle;
    doUpdateRotation();
  }

  public void updateRotation(float angle) {
    mChartAngle = angle;
    doUpdateRotation();
  }

  private void doUpdateRotation() {
    // keep the angle >= 0 and <= 360
    mChartAngle = (mChartAngle + 360f) % 360f;
    postInvalidate();
    if (mRotationListener != null) {
      mRotationListener.onRotate();
    }
  }

  @Override
  protected void prepareContentRect() {
    super.prepareContentRect();

    float width = mContentRect.width() + mOffsetLeft + mOffsetRight;
    float height = mContentRect.height() + mOffsetTop + mOffsetBottom;

    float diameter = getDiameter();

    // create the circle box that will contain the pie-chart (the bounds of
    // the pie-chart)
    mCircleBox.set(width / 2 - diameter / 2 + mShift, height / 2 - diameter / 2
            + mShift,
        width / 2 + diameter / 2 - mShift, height / 2 + diameter / 2
            - mShift);
  }

  @Override
  protected void calcMinMax(boolean fixedValues) {
    super.calcMinMax(fixedValues);

    calcAngles();
  }

  /**
   * calculates the needed angles for the chart slices
   */
  private void calcAngles() {

    mSlicesAngleWidth = new float[mCurrentData.getYValCount()];
    mSlicesAnglePosition = new float[mCurrentData.getYValCount()];

    ArrayList<PieDataSet> dataSets = mCurrentData.getDataSets();

    int cnt = 0;

    for (int i = 0; i < mCurrentData.getDataSetCount(); i++) {

      DataSet set = dataSets.get(i);
      ArrayList<Entry> entries = set.getYVals();

      for (int j = 0; j < entries.size(); j++) {

        mSlicesAngleWidth[cnt] = calcAngle(entries.get(j).getVal());

        if (cnt == 0) {
          mSlicesAnglePosition[cnt] = mSlicesAngleWidth[cnt];
        } else {
          mSlicesAnglePosition[cnt] = mSlicesAnglePosition[cnt - 1] + mSlicesAngleWidth[cnt];
        }

        cnt++;
      }
    }
  }

  @Override
  protected void drawHighlights() {

    // if there are values to highlight and highlighnting is enabled, do it
    if (mHighlightEnabled && valuesToHighlight()) {

      float angle = 0f;

      for (int i = 0; i < mIndicesToHightlight.length; i++) {

        // get the index to highlight
        int xIndex = mIndicesToHightlight[i].getXIndex();
        if (xIndex >= mSlicesAngleWidth.length || xIndex > mDeltaX)
          continue;

        if (xIndex == 0)
          angle = mChartAngle;
        else
          angle = mChartAngle + mSlicesAnglePosition[xIndex - 1];

        float sliceDegrees = mSlicesAngleWidth[xIndex];

        float shiftangle = (float) Math.toRadians(angle + sliceDegrees / 2f);

        float xShift = mShift * (float) Math.cos(shiftangle);
        float yShift = mShift * (float) Math.sin(shiftangle);

        RectF highlighted = new RectF(mCircleBox.left + xShift, mCircleBox.top + yShift,
            mCircleBox.right
                + xShift, mCircleBox.bottom + yShift);

        // redefine the rect that contains the arc so that the
        // highlighted pie is not cut off
        mDrawCanvas.drawArc(highlighted, angle + mSliceSpace / 2f, sliceDegrees
            - mSliceSpace / 2f, true, mCurrentData.getDataSetByIndex(i).getDrawingSpec().getBasicPaint());
      }
    }
  }

  @Override
  protected void drawData() {

    float angle = mChartAngle;

    ArrayList<PieDataSet> dataSets = mCurrentData.getDataSets();

    int cnt = 0;

    for (int i = 0; i < mCurrentData.getDataSetCount(); i++) {
      PieDataSet dataSet = dataSets.get(i);
      MulticolorDrawingSpec spec = dataSet.getDrawingSpec();
      ArrayList<Entry> entries = dataSet.getYVals();

      Paint paint = spec.getBasicPaint();

      for (int j = 0; j < entries.size(); j++) {

        float newAngle = mSlicesAngleWidth[cnt];

        int originalColor = paint.getColor();

        if (spec.hasMultipleColors()) {
          paint.setColor(spec.getColor(j));
        }

        if (!needsHighlight(entries.get(j).getXIndex(), i)) {
          mDrawCanvas.drawArc(mCircleBox, angle + mSliceSpace / 2f, newAngle
              - mSliceSpace / 2f, true, paint);
        }

        paint.setColor(originalColor);

        angle += newAngle;
        cnt++;
      }
    }
  }

  /**
   * draws the hole in the center of the chart
   */
  private void drawHole() {

    if (mDrawHole) {

      float radius = getRadius();

      PointF c = getCenterCircleBox();

      int color = mBackgroundColor;

      mHolePaint.setColor(color);

      // draw the hole-circle
      mDrawCanvas.drawCircle(c.x, c.y,
          radius / 100 * mHoleRadiusPercent, mHolePaint);

      // make transparent
      mHolePaint.setColor(color & 0x60FFFFFF);

      // draw the transparent-circle
      mDrawCanvas.drawCircle(c.x, c.y,
          radius / 100 * mTransparentCircleRadius, mHolePaint);

      mHolePaint.setColor(color);
    }
  }

  /**
   * draws the description text in the center of the pie chart makes most
   * sense when center-hole is enabled
   */
  private void drawCenterText() {

    if (mDrawCenterText) {

      PointF c = getCenterCircleBox();

      // get all lines from the text
      String[] lines = mCenterText.split("\n");

      // calculate the height for each line
      float lineHeight = Utils.calcTextHeight(mCenterTextPaint, lines[0]);
      float linespacing = lineHeight * 0.2f;

      float totalheight = lineHeight * lines.length - linespacing * (lines.length - 1);

      int cnt = lines.length;

      float y = c.y;

      for (int i = 0; i < lines.length; i++) {

        String line = lines[lines.length - i - 1];

        mDrawCanvas.drawText(line, c.x, y
                + lineHeight * cnt - totalheight / 2f,
            mCenterTextPaint);
        cnt--;
        y -= linespacing;
      }
    }
  }

  @Override
  protected void drawValues() {

    // if neither xvals nor yvals are drawn, return
    if (!mDrawXVals && !mDrawYValues)
      return;

    PointF center = getCenterCircleBox();

    // get whole the radius
    float r = getRadius();

    float off = r / 2f;

    if (mDrawHole) {
      off = (r - (r / 100f * mHoleRadiusPercent)) / 2f;
    }

    r -= off; // offset to keep things inside the chart

    ArrayList<PieDataSet> dataSets = mCurrentData.getDataSets();

    int cnt = 0;

    for (int i = 0; i < mCurrentData.getDataSetCount(); i++) {

      DataSet dataSet = dataSets.get(i);
      ArrayList<Entry> entries = dataSet.getYVals();

      for (int j = 0; j < entries.size(); j++) {

        // offset needed to center the drawn text in the slice
        float offset = mSlicesAngleWidth[cnt] / 2;

        // calculate the text position
        float x = (float) (r * Math.cos(Math.toRadians(mChartAngle + mSlicesAnglePosition[cnt] - offset)) + center.x);
        float y = (float) (r * Math.sin(Math.toRadians(mChartAngle + mSlicesAnglePosition[cnt] - offset)) + center.y);

        String val = "";
        float value = entries.get(j).getVal();

        if (mUsePercentValues) {
          val = mFormatValue.format(getPercentOfTotal(value)) + " %";
        } else {
          val = mFormatValue.format(value);
        }

        // draw everything, depending on settings
        if (mDrawXVals && mDrawYValues) {

          // use ascent and descent to calculate the new line
          // position,
          // 1.6f is the line spacing
          float lineHeight = (mValuePaint.ascent() + mValuePaint.descent()) * 1.6f;
          y -= lineHeight / 2;

          mDrawCanvas.drawText(val, x, y, mValuePaint);
          mDrawCanvas.drawText(mCurrentData.getXLabels().get(j), x, y + lineHeight,
              mValuePaint);
        } else if (mDrawXVals && !mDrawYValues) {
          mDrawCanvas.drawText(mCurrentData.getXLabels().get(j), x, y, mValuePaint);
        } else if (!mDrawXVals && mDrawYValues) {

          mDrawCanvas.drawText(val, x, y, mValuePaint);
        }

        cnt++;
      }
    }
  }

  @Override
  protected void drawAdditional() {
    drawHole();
  }

  /**
   * calculates the needed angle for a given value
   *
   * @param value
   * @return
   */
  private float calcAngle(float value) {
    float yValueSum = mCurrentData.getYValueSum();
    if (yValueSum == 0.0f) {
      return 360f / mCurrentData.getYValCount();
    }
    return value / yValueSum * 360f;
  }

  /**
   * returns the pie index for the pie at the given angle
   *
   * @param angle
   * @return
   */
  public int getIndexForAngle(float angle) {

    // take the current angle of the chart into consideration
    float a = (angle - mChartAngle + 720) % 360f;

    for (int i = 0; i < mSlicesAnglePosition.length; i++) {
      if (mSlicesAnglePosition[i] > a)
        return i;
    }

    return -1; // return -1 if no index found
  }

  /**
   * returns the index of the DataSet this x-index belongs to.
   *
   * @param xIndex
   * @return
   */
  public int getDataSetIndexForIndex(int xIndex) {

    ArrayList<PieDataSet> sets = mCurrentData.getDataSets();

    for (int i = 0; i < sets.size(); i++) {
      if (sets.get(i).getEntryForXIndex(xIndex) != null)
        return i;
    }

    return -1;
  }

  /**
   * returns an integer array of all the different angles the chart slices
   * have the angles in the returned array determine how much space (of 360°)
   * each slice takes
   *
   * @return
   */
  public float[] getSlicesAngleWidth() {
    return mSlicesAngleWidth;
  }

  /**
   * returns the absolute angles of the different chart slices (where the
   * slices end)
   *
   * @return
   */
  public float[] getSlicesAnglePosition() {
    return mSlicesAnglePosition;
  }

  /**
   * set a new starting angle for the pie chart (0-360) default is 0° -->
   * right side (EAST)
   *
   * @param angle
   */
  public void setChartAngle(float angle) {
    mChartAngle = angle;
  }

  /**
   * gets the current rotation angle of the pie chart
   *
   * @return
   */
  public float getChartAngle() {
    return mChartAngle;
  }

  /**
   * sets the distance the highlighted piechart-slice is "shifted" away from
   * the center of the chart, default 20f
   *
   * @param shift
   */
  public void setSelectionShift(float shift) {
    mShift = Utils.convertDpToPixel(shift);
  }

  /**
   * returns the distance a highlighted piechart slice is "shifted" away from
   * the chart-center
   *
   * @return
   */
  public float getSelectionShift() {
    return mShift;
  }

  /**
   * set this to true to draw the pie center empty
   *
   * @param enabled
   */
  public void setDrawHoleEnabled(boolean enabled) {
    this.mDrawHole = enabled;
  }

  /**
   * returns true if the hole in the center of the pie-chart is set to be
   * visible, false if not
   *
   * @return
   */
  public boolean isDrawHoleEnabled() {
    return mDrawHole;
  }

  /**
   * sets the text that is displayed in the center of the pie-chart. By
   * default, the text is "Total Value + sumofallvalues"
   *
   * @param text
   */
  public void setCenterText(String text) {
    mCenterText = text;
  }

  /**
   * returns the text that is drawn in the center of the pie-chart
   *
   * @return
   */
  public String getCenterText() {
    return mCenterText;
  }

  /**
   * set this to true to draw the text that is displayed in the center of the
   * pie chart
   *
   * @param enabled
   */
  public void setDrawCenterText(boolean enabled) {
    this.mDrawCenterText = enabled;
  }

  /**
   * returns true if drawing the center text is enabled
   *
   * @return
   */
  public boolean isDrawCenterTextEnabled() {
    return mDrawCenterText;
  }

  /**
   * set this to true to draw percent values instead of the actual values
   *
   * @param enabled
   */
  public void setUsePercentValues(boolean enabled) {
    mUsePercentValues = enabled;
  }

  /**
   * returns true if drawing percent values is enabled
   *
   * @return
   */
  public boolean isUsePercentValuesEnabled() {
    return mUsePercentValues;
  }

  /**
   * set this to true to draw the x-value text into the pie slices
   *
   * @param enabled
   */
  public void setDrawXValues(boolean enabled) {
    mDrawXVals = enabled;
  }

  /**
   * returns true if drawing x-values is enabled, false if not
   *
   * @return
   */
  public boolean isDrawXValuesEnabled() {
    return mDrawXVals;
  }

  /**
   * returns the radius of the pie-chart
   *
   * @return
   */
  public float getRadius() {
    if (mCircleBox == null)
      return 0;
    else
      return Math.min(mCircleBox.width() / 2f, mCircleBox.height() / 2f);
  }

  /**
   * returns the diameter of the pie-chart
   *
   * @return
   */
  public float getDiameter() {
    if (mContentRect == null)
      return 0;
    else
      return Math.min(mContentRect.width(), mContentRect.height());
  }

  /**
   * returns the circlebox, the boundingbox of the pie-chart slices
   *
   * @return
   */
  public RectF getCircleBox() {
    return mCircleBox;
  }

  /**
   * returns the center of the circlebox
   *
   * @return
   */
  public PointF getCenterCircleBox() {
    return new PointF(mCircleBox.centerX(), mCircleBox.centerY());
  }

  /**
   * returns the angle relative to the chart center for the given point on the
   * chart in degrees. The angle is always between 0 and 360°, 0° is EAST, 90°
   * is SOUTH, ...
   *
   * @param x
   * @param y
   * @return
   */
  public float getAngleForPoint(float x, float y) {

    PointF c = getCenterCircleBox();

    double tx = x - c.x, ty = y - c.y;

    float angle = (float) Math.toDegrees(Math.atan2(ty, tx));

    return angle;
  }

  /**
   * returns the distance of a certain point on the chart to the center of the
   * piechart
   *
   * @param x
   * @param y
   * @return
   */
  public float distanceToCenter(float x, float y) {
    PointF c = getCenterCircleBox();

    float xDist = x - c.x;
    float yDist = y - c.y;

    return (float) Math.sqrt(xDist * xDist + yDist * yDist);
  }

  /**
   * sets the typeface for the center-text paint
   *
   * @param t
   */
  public void setCenterTextTypeface(Typeface t) {
    mCenterTextPaint.setTypeface(t);
  }

  /**
   * Sets the size of the center text of the piechart.
   *
   * @param size
   */
  public void setCenterTextSize(float size) {
    mCenterTextPaint.setTextSize(Utils.convertDpToPixel(size));
  }

  /**
   * sets the space that is left out between the piechart-slices, default: 0°
   * --> no space, maximum 45, minimum 0 (no space)
   *
   * @param degrees
   */
  public void setSliceSpace(float degrees) {

    if (degrees > 45)
      degrees = 45f;
    if (degrees < 0)
      degrees = 0f;

    mSliceSpace = degrees;
  }

  /**
   * returns the space that is set to be between the piechart-slices, in
   * degrees
   *
   * @return
   */
  public float getSliceSpace() {
    return mSliceSpace;
  }

  /**
   * sets the radius of the hole in the center of the piechart in percent of
   * the maximum radius (max = the radius of the whole chart), default 50%
   *
   * @param percent
   */
  public void setHoleRadius(final float percent) {

    Handler h = new Handler();
    h.post(new Runnable() {

      @Override
      public void run() {
        mHoleRadiusPercent = percent;
      }
    });
  }

  /**
   * sets the radius of the transparent circle that is drawn next to the hole
   * in the piechart in percent of the maximum radius (max = the radius of the
   * whole chart), default 55% -> means 5% larger than the center-hole by
   * default
   *
   * @param percent
   */
  public void setTransparentCircleRadius(final float percent) {
    Handler h = new Handler();
    h.post(new Runnable() {

      @Override
      public void run() {
        mTransparentCircleRadius = percent;
      }
    });
  }

  @Override
  public void setPaint(Paint p, int which) {
    super.setPaint(p, which);

    switch (which) {
    case PAINT_HOLE:
      mHolePaint = p;
      break;
    case PAINT_CENTER_TEXT:
      mCenterTextPaint = p;
      break;
    }
  }

  @Override
  public Paint getPaint(int which) {
    super.getPaint(which);

    switch (which) {
    case PAINT_HOLE:
      return mHolePaint;
    case PAINT_CENTER_TEXT:
      return mCenterTextPaint;
    }

    return null;
  }

  @Override
  protected PieDataSet createDataSet(ArrayList<Entry> approximated, String label) {
    return new PieDataSet(approximated, label);
  }

  public void prepareLegend() {
    ArrayList<String> labels = new ArrayList<String>();
    ArrayList<Integer> colors = new ArrayList<Integer>();

    for (int i = 0; i < mOriginalData.getDataSetCount(); i++) {
      PieDataSet dataSet = mOriginalData.getDataSetByIndex(i);
      MulticolorDrawingSpec spec = dataSet.getDrawingSpec();
      if (spec.hasMultipleColors()) {
        int entriesCount = mOriginalData.getDataSetByIndex(i).getEntryCount();

        for (int j = 0; j < spec.getColorsCount() && j < entriesCount; j++) {
          if (j < spec.getColorsCount() - 1 && j < entriesCount - 1) {
            // if multiple colors are set for a DataSet, group them
            labels.add(null);
          } else {
            // add label to the last entry
            String label = mOriginalData.getDataSetByIndex(i).getLabel();
            labels.add(label);
          }

          colors.add(spec.getColor(j));
        }
      } else {
        labels.add(dataSet.getLabel());
        colors.add(spec.getBasicPaint().getColor());
      }
    }

    Legend l = new Legend(colors, labels);

    if (mLegend != null) {
      l.apply(mLegend);
    }

    mLegend = l;
  }

  public void setRotationListener(RotationListener rotationListener) {
    mRotationListener = rotationListener;
  }

  public void stopRotationAnimation() {
    mAnimator.stop();
  }

  public void startRotationAnimation(float angle, long duration) {
    stopRotationAnimation();
    float startAngle = mChartAngle;
    float endAngle = angle;
    float distance = startAngle - endAngle;
    if (distance > 180.0f) {
      distance -= 360.0f;
    } else if (distance < -180.0f) {
      distance += 360.0f;
    }
    endAngle = startAngle - distance;
    mAnimator.smoothScroll(startAngle, endAngle, duration);
  }

  public interface RotationListener {
    public void onRotate();
  }

  public void centerOnHighlighted() {
    int mChartSelectionAngle = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT ? 90 : 0;
    int index = getIndexForAngle(mChartSelectionAngle);
    float angle = mChartSelectionAngle - getSlicesAnglePosition()[index] + getSlicesAngleWidth()[index] / 2;
    startRotationAnimation(angle, 500);
  }

}
