package com.aripuca.tracker.chart;

import java.util.ArrayList;

public class Series {

	private ArrayList<Point> points;
	
	private float minValueX = Float.POSITIVE_INFINITY;
	private float maxValueX = Float.NEGATIVE_INFINITY;

	private float minValueY = Float.POSITIVE_INFINITY;
	private float maxValueY = Float.NEGATIVE_INFINITY;
	
	private int color;
	
	public Series(int c) {
		
		this.color = c;
		
		this.points = new ArrayList<Point>(); 
		
	}
	
	public int getColor() {
		return color;
	}
	
	public void addPoint(Point p) {
		
		if (p.getValueX() < minValueX) {
			minValueX = p.getValueX(); 
		}

		if (p.getValueX() > maxValueX) {
			maxValueX = p.getValueX(); 
		}

		if (p.getValueY() < minValueY) {
			minValueY = p.getValueY(); 
		}

		if (p.getValueY() > maxValueY) {
			maxValueY = p.getValueY(); 
		}
		
		points.add(p);
		
	}
	
	public Point getPoint(int index) {
		
		return points.get(index);
		
	}
	
	public int getCount() {
		
		return points.size();
		
	}
	
	public float getRangeX() {
		
		return maxValueX - minValueX;
		
	}

	public float getRangeY() {
		
		return maxValueY - minValueY;
		
	}
	
	public float getMinX() {
		return maxValueX;
	}

	public float getMinY() {
		return minValueY;
	}
	
}
