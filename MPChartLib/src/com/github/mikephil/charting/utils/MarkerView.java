package com.github.mikephil.charting.utils;

import com.github.mikephil.charting.data.ChartData;
import com.github.mikephil.charting.data.DataSet;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.FrameLayout;

/**
 * View that can be displayed when selecting values in the chart. Extend this
 * class to provide custom layouts for your markers.
 *
 * @author Philipp Jahoda
 */
public abstract class MarkerView<T extends DataSet> extends FrameLayout {

  /**
   * draw offset on the x-axis
   */
  private float mAnchorX, mAnchorY;
  private float mPositionX, mPositionY;
  private float mOffsetX, mOffsetY;
  private Animation mInAnimation;
  private Animation mOutAnimation;

  /**
   * Constructor. Sets up the MarkerView with a custom layout resource.
   *
   * @param context
   * @param layoutResource the layout resource to use for the MarkerView
   */
  public MarkerView(Context context, int layoutResource) {
    super(context);
    setupLayoutResource(layoutResource);
    super.setVisibility(View.GONE);
  }

  public void setInAnimation(Animation inAnimation) {
    mInAnimation = inAnimation;
  }

  public Animation getInAnimation() {
    return mInAnimation;
  }

  public void setOutAnimation(Animation outAnimation) {
    mOutAnimation = outAnimation;
  }

  public Animation getOutAnimation() {
    return mOutAnimation;
  }

  /**
   * Sets the layout resource for a custom MarkerView.
   *
   * @param layoutResource
   */
  private void setupLayoutResource(int layoutResource) {
    View child = LayoutInflater.from(getContext()).inflate(layoutResource, this, true);
    child.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    child.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
    setMeasuredDimension(child.getMeasuredWidth(), child.getMeasuredHeight());
  }

  public void setVisibility(int visibility) {
    if (getVisibility() != visibility) {
      super.setVisibility(visibility);
      View child = getChildAt(0);
      if (visibility == View.VISIBLE) {
        if (mInAnimation != null) {
          child.startAnimation(mInAnimation);
        }
      } else if (visibility == View.GONE) {
        if (mOutAnimation != null) {
          child.startAnimation(mOutAnimation);
        }
      }
    }
  }

  /**
   * Method called every time when data point gets selected.
   * @param xIndex
   * @param value
   * @param dataSetIndex
   * @param data
   */
  public abstract void onContentUpdate(int xIndex, float value, int dataSetIndex, ChartData<T> data);

  public abstract void onFreeSpaceChanged(int left, int top, int right, int bottom);

  public void setAnchor(float anchorX, float anchorY) {
    mAnchorX = anchorX;
    mAnchorY = anchorY;
  }

  public void setOffset(float offsetX, float offsetY) {
    mOffsetX = offsetX;
    mOffsetY = offsetY;
  }

  public void setPosition(float x, float y) {
    mPositionX = x;
    mPositionY = y;
  }

  public void updatePosition() {
    int l = (int) (mPositionX - mAnchorX * getMeasuredWidth() + mOffsetX);
    int t = (int) (mPositionY - mAnchorY * getMeasuredHeight() + mOffsetY);
    int r = l + getMeasuredWidth();
    int b = t + getMeasuredHeight();
    layout(l, t, r, b);
  }
}
