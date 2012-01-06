package com.aripuca.tracker.chart;

import java.util.ArrayList;

import com.aripuca.tracker.R;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.view.View;

public class AbstractChart {

	protected ArrayList<Series> series;

	protected int sizeX;
	protected int sizeY;

	protected int offsetX = 20;
	protected int offsetY = 20;

	protected View chartView;

	public AbstractChart(View view) {

		chartView = view;

		sizeX = view.getWidth() - offsetX * 2;
		sizeY = view.getHeight() - offsetY * 2;

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

		canvas.drawLine(offsetX, offsetY, offsetX, offsetY + sizeY, paint);
		canvas.drawLine(offsetX, offsetY + sizeY, offsetX + sizeX, offsetY + sizeY, paint);

	}

	protected void drawLabels(Canvas canvas) {

	}

	protected void drawChart(Canvas canvas) {

	}

	protected void drawLegend(Canvas canvas) {
		
	}
	
	public void draw(Canvas canvas) {

		this.drawChart(canvas);
		
		this.drawAxes(canvas);
		
		this.drawLabels(canvas);

		this.drawLegend(canvas);
		
	}
	

}
