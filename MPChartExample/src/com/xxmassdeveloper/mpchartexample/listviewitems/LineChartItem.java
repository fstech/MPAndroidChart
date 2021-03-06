package com.xxmassdeveloper.mpchartexample.listviewitems;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.ChartData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.utils.XLabels;
import com.github.mikephil.charting.utils.XLabels.XLabelPosition;
import com.xxmassdeveloper.mpchartexample.R;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;

public class LineChartItem extends ChartItem<LineDataSet> {

  private Typeface mTf;

  public LineChartItem(ChartData cd, Context c) {
    super(cd);

    mTf = Typeface.createFromAsset(c.getAssets(), "OpenSans-Regular.ttf");
  }

  @Override
  public int getItemType() {
    return TYPE_LINECHART;
  }

  @Override
  public View getView(int position, View convertView, Context c) {

    ViewHolder holder = null;

    if (convertView == null) {

      holder = new ViewHolder();

      convertView = LayoutInflater.from(c).inflate(
          R.layout.list_item_linechart, null);
      holder.chart = (LineChart) convertView.findViewById(R.id.chart);

      convertView.setTag(holder);
    } else {
      holder = (ViewHolder) convertView.getTag();
    }

    // apply styling
    holder.chart.setYLabelCount(5);
    holder.chart.setCircleSize(5f);
    holder.chart.setYLabelTypeface(mTf);
    holder.chart.setXLabelTypeface(mTf);
    holder.chart.setValueTypeface(mTf);
    holder.chart.setDescription("");
    holder.chart.setDrawVerticalGrid(false);
    holder.chart.setDrawGridBackground(false);

    XLabels xl = holder.chart.getXLabels();
    xl.setCenterXLabelText(true);
    xl.setPosition(XLabelPosition.BOTTOM);

    // set data
    holder.chart.setData(mChartData);

    // do not forget to refresh the chart
    holder.chart.invalidate();

    return convertView;
  }

  private static class ViewHolder {
    LineChart chart;
  }
}
