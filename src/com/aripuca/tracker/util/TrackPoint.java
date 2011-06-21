package com.aripuca.tracker.util;

import com.google.android.maps.GeoPoint;

public class TrackPoint extends Object {
	
	private GeoPoint geoPoint;
	
	private int segmentId;
	
	public TrackPoint(GeoPoint gp, int sid) {
		
		this.geoPoint = gp;
		this.segmentId = sid;
		
	}
	
	public GeoPoint getGeoPoint() {
		return this.geoPoint;
	}

	public int getSegmentId() {
		return this.segmentId;
	}

}
