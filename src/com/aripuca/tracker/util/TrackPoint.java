package com.aripuca.tracker.util;

import com.google.android.maps.GeoPoint;

public class TrackPoint extends Object {

	private GeoPoint geoPoint;

	private int segmentIndex;
	
	private float speed; 
	private float distance; 
	private float elevation; 

	public TrackPoint(GeoPoint gp, int si) {

		this.geoPoint = gp;
		this.segmentIndex = si;

	}
	
	public GeoPoint getGeoPoint() {
		return this.geoPoint;
	}

	public int getSegmentIndex() {
		return this.segmentIndex;
	}
	
	public void setSpeed(float s) {
		this.speed = s;
	}
	
	public float getSpeed() {
		return this.speed;
	}

	public void setDistance(float d) {
		this.distance = d;
	}
	
	public float getDistance() {
		return this.distance;
	}

	public void setElevation(float e) {
		this.elevation = e;
	}
	
	public float getElevation() {
		return this.elevation;
	}

}
