package com.github.mikephil.charting.charts;

import com.github.mikephil.charting.data.DataSet;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.ScatterDataSet;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;

import java.util.ArrayList;

/**
 * The ScatterChart. Draws dots, triangles, squares and custom shapes into the
 * chartview.
 *
 * @author Philipp Jahoda
 */
public class ScatterChart extends BarLineChartBase<ScatterDataSet> {

  /**
   * enum that defines the shape that is drawn where the values are
   */
  public enum ScatterShape {
    CROSS, TRIANGLE, CIRCLE, SQUARE, CUSTOM
  }

  /**
   * Custom path object the user can provide that is drawn where the values
   * are at. This is used when ScatterShape.CUSTOM is set for a DataSet.
   */
  private Path mCustomScatterPath = null;

  /**
   * array that holds all the scattershapes that this chart uses, each shape
   * represents one dataset in the chart
   */
  private ScatterShape[] mScatterShapes = new ScatterShape[] {
      ScatterShape.SQUARE, ScatterShape.TRIANGLE
  };

  /**
   * the size the scattershape will have, in screen pixels
   */
  private float mShapeSize = 12f;

  public ScatterChart(Context context) {
    super(context);
  }

  public ScatterChart(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public ScatterChart(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }

  @Override
  protected void drawData() {

    ArrayList<ScatterDataSet> dataSets = mCurrentData.getDataSets();

    float shapeHalf = mShapeSize / 2f;

    for (int i = 0; i < mCurrentData.getDataSetCount(); i++) {

      DataSet dataSet = dataSets.get(i);
      ArrayList<Entry> entries = dataSet.getYVals();

      float[] pos = generateTransformedValues(entries, 0f);

      ScatterShape shape = mScatterShapes[i % mScatterShapes.length];

      for (int j = 0; j < pos.length; j += 2) {

        // Set the color for the currently drawn value. If the index is
        // out of bounds, reuse colors.
        Paint renderPaint = mCurrentData.getDataSetByIndex(i).getDrawingSpec().getBasicPaint();

        if (isOffContentRight(pos[j]))
          break;

        // make sure the lines don't do shitty things outside bounds
        if (j != 0 && isOffContentLeft(pos[j - 1])
            && isOffContentTop(pos[j + 1])
            && isOffContentBottom(pos[j + 1]))
          continue;

        if (shape == ScatterShape.SQUARE) {

          mDrawCanvas.drawRect(pos[j] - shapeHalf, pos[j + 1] - shapeHalf, pos[j]
              + shapeHalf, pos[j + 1]
              + shapeHalf, renderPaint);
        } else if (shape == ScatterShape.CIRCLE) {

          mDrawCanvas.drawCircle(pos[j], pos[j + 1], mShapeSize / 2f, renderPaint);
        } else if (shape == ScatterShape.CROSS) {

          mDrawCanvas.drawLine(pos[j] - shapeHalf, pos[j + 1], pos[j] + shapeHalf,
              pos[j + 1], renderPaint);
          mDrawCanvas.drawLine(pos[j], pos[j + 1] - shapeHalf, pos[j], pos[j + 1]
              + shapeHalf, renderPaint);
        } else if (shape == ScatterShape.TRIANGLE) {

          // create a triangle path
          Path tri = new Path();
          tri.moveTo(pos[j], pos[j + 1] - shapeHalf);
          tri.lineTo(pos[j] + shapeHalf, pos[j + 1] + shapeHalf);
          tri.lineTo(pos[j] - shapeHalf, pos[j + 1] + shapeHalf);
          tri.close();

          mDrawCanvas.drawPath(tri, renderPaint);
        } else if (shape == ScatterShape.CUSTOM) {

          if (mCustomScatterPath == null)
            return;

          // transform the provided custom path
          transformPath(mCustomScatterPath);
          mDrawCanvas.drawPath(mCustomScatterPath, renderPaint);
        }
      }
    }
  }

  @Override
  protected void drawValues() {
    // if values are drawn
    if (mDrawYValues && mCurrentData.getYValCount() < mMaxVisibleCount * mScaleX) {

      ArrayList<ScatterDataSet> dataSets = mCurrentData.getDataSets();

      for (int i = 0; i < mCurrentData.getDataSetCount(); i++) {

        DataSet dataSet = dataSets.get(i);
        ArrayList<Entry> entries = dataSet.getYVals();

        float[] positions = generateTransformedValues(entries, 0f);

        for (int j = 0; j < positions.length; j += 2) {

          if (isOffContentRight(positions[j]))
            break;

          if (isOffContentLeft(positions[j]) || isOffContentTop(positions[j + 1])
              || isOffContentBottom(positions[j + 1]))
            continue;

          float val = entries.get(j / 2).getVal();

          if (mDrawUnitInChart) {

            mDrawCanvas.drawText(mValueFormat.format(val) + mUnit, positions[j],
                positions[j + 1] - mShapeSize, mValuePaint);
          } else {

            mDrawCanvas.drawText(mValueFormat.format(val), positions[j],
                positions[j + 1] - mShapeSize,
                mValuePaint);
          }
        }
      }
    }
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

  @Override
  protected ScatterDataSet createDataSet(ArrayList<Entry> approximated, String label) {
    return new ScatterDataSet(approximated, label);
  }

  @Override
  protected void drawAdditional() {

  }

  /**
   * Sets the size the drawn scattershape will have. This only applies for non
   * custom shapes. Default 12f
   *
   * @param size
   */
  public void setScatterShapeSize(float size) {
    mShapeSize = size;
  }

  /**
   * returns the currently set scatter shape size
   *
   * @return
   */
  public float getScatterShapeSize() {
    return mShapeSize;
  }

  /**
   * Sets the shapes that are drawn on the position where the values are at.
   * One shape per DataSet. If "CUSTOM" is chosen for a DataSet, you need to
   * call setCustomScatterShape(...) and provide a path object that is drawn
   * as the custom scattershape. If more DataSets are drawn than ScatterShapes
   * exist, shapes are reused.
   *
   * @param shapes
   */
  public void setScatterShapes(ScatterShape[] shapes) {
    mScatterShapes = shapes;
  }

  /**
   * returns all the different scattershapes the chart uses
   *
   * @return
   */
  public ScatterShape[] getScatterShapes() {
    return mScatterShapes;
  }

  /**
   * Sets a path object as the shape to be drawn where the values are at. Do
   * not forget to call setScatterShapes(...) and set the shape for one
   * DataSet to ScatterShape.CUSTOM.
   *
   * @param shape
   */
  public void setCustomScatterShape(Path shape) {
    mCustomScatterPath = shape;
  }
}
