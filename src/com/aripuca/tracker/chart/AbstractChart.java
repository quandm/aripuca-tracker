package com.aripuca.tracker.chart;

import java.util.ArrayList;

import android.graphics.Canvas;

public class AbstractChart {
	
	private ArrayList<Series> series;
	
	private Canvas canvas;

	public AbstractChart() {
		
		this.series = new ArrayList<Series>();
		
	}
	
	public void setCanvas(Canvas c) {
		this.canvas = c;
	}
	
	public void addSeries(Series s) {
		series.add(s);
	}
	
	public void drawChart() {
		
	}

	private void drawAxes() {
		
		
	}
	
	public void setDataSource() {
		
		
	}
	
	
	
}
