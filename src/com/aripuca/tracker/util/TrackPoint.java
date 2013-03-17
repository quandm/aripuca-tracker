package com.aripuca.tracker.util;

import com.google.android.maps.GeoPoint;

public class TrackPoint extends Object {

	private GeoPoint geoPoint;

	private int segmentIndex;

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

}
