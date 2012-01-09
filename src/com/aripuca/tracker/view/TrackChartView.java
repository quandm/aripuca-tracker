package com.aripuca.tracker.view;
import java.util.ArrayList;

import com.aripuca.tracker.app.Constants;
import com.aripuca.tracker.chart.Series;
import com.aripuca.tracker.chart.TrackChart;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

public class TrackChartView extends View {

	private Series elevationSeries;
	private Series speedSeries;

	public TrackChartView(Context context) {
		super(context);
	}
	
	public TrackChartView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	@Override
	protected void onDraw(Canvas canvas) {

		TrackChart trackChart = new TrackChart(this);
		trackChart.addSeries(elevationSeries);
		trackChart.addSeries(speedSeries);
		trackChart.draw(canvas);

	}
	
	public void setElevationSeries(Series es) {
		this.elevationSeries = es;
	}

	public void setSpeedSeries(Series ss) {
		this.speedSeries = ss;
	}
	
}
