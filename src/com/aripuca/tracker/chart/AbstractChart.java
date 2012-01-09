package com.aripuca.tracker.chart;

import java.util.ArrayList;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.view.View;

public class AbstractChart {

	protected ArrayList<Series> series;

	protected int sizeX;
	protected int sizeY;
	protected int totalSizeX;
	protected int totalSizeY;

	protected int offsetTop = 40;
	protected int offsetLeft = 40;
	protected int offsetRight = 20;
	protected int offsetBottom = 25;

	protected View chartView;

	protected String title;

	public AbstractChart() {}

	public AbstractChart(View v, String t) {

		chartView = v;

		title = t;

		totalSizeX = chartView.getWidth();
		totalSizeY = chartView.getHeight();

		sizeX = chartView.getWidth() - offsetLeft - offsetRight;
		sizeY = chartView.getHeight() - offsetTop - offsetBottom;

		this.series = new ArrayList<Series>();

	}

	public void addSeries(Series s) {
		series.add(s);
	}

	protected void drawAxes(Canvas canvas) {

		Paint paint = new Paint();

		// axes
		paint.setColor(0xffcccccc);
		paint.setStrokeWidth(1);

		// y axis
		canvas.drawLine(offsetLeft, offsetTop - 10, offsetLeft, offsetTop + sizeY + 5, paint);

		// x axis
		canvas.drawLine(offsetLeft - 5, offsetTop + sizeY, offsetLeft + sizeX + 10, offsetTop + sizeY, paint);

	}

	protected void drawTitle(Canvas canvas) {
		Paint paint = new Paint();
		paint.setColor(Color.WHITE);
		paint.setTextAlign(Align.CENTER);
		paint.setTextSize(20);
		paint.setAntiAlias(true);
		canvas.drawText(title, totalSizeX / 2, offsetTop / 2, paint);
	}

	protected void drawLabels(Canvas canvas) {

	}

	protected void drawChart(Canvas canvas) {

	}

	protected void drawLegend(Canvas canvas) {

	}

	protected void drawAxisXLabels(Canvas canvas) {

	}

	protected void drawAxisYLabels(Canvas canvas) {

	}
	
	public void draw(Canvas canvas) {

		this.drawLabels(canvas);

		this.drawTitle(canvas);

		this.drawAxisXLabels(canvas);

		this.drawAxisYLabels(canvas);

		this.drawChart(canvas);

		this.drawLegend(canvas);

		this.drawAxes(canvas);

	}

}
