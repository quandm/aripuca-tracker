package com.aripuca.tracker.track;

import java.util.Date;

import android.location.Location;

public class Waypoint extends Object {

	private double latitude;
	private double longitude;
	private double elevation = 0;
	private float accuracy = 0;
	private long time;

	private String title;

	public Waypoint(String title, long t, double lat, double lng, double ele, float acc) {

		this.title = title;

		this.latitude = lat;
		this.longitude = lng;

		this.time = t;

		this.elevation = ele;

		this.accuracy = acc;

	}

	public Waypoint(String title, double lat, double lng) {

		this.title = title;

		this.latitude = lat;
		this.longitude = lng;

		this.time = (new Date()).getTime();

	}

	public Waypoint(String title, int latE6, int lngE6) {

		this.title = title;
		this.latitude = latE6 / 1E6;
		this.longitude = lngE6 / 1E6;

		this.time = (new Date()).getTime();

	}

	/**
	 * returns simple Location object for calculating bearing and distance to
	 * this waypoint
	 * 
	 * @return Location
	 */
	public Location getLocation() {

		Location loc = new Location("MyAPP");

		loc.setLatitude(latitude);
		loc.setLongitude(longitude);
		loc.setAltitude(elevation);
		// loc.setTime();

		return loc;

	}

	public double getLatitude() {
		return latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public double getElevation() {
		return elevation;
	}

	public float getAccuracy() {
		return this.accuracy;
	}

	public long getTime() {
		return time;
	}

	/**
	 * returns string ready for storing
	 */
	public String getFormattedString() {

		return title + "|" + Long.toString(time) + "|" + Double.toString(latitude) + "|" + Double.toString(longitude)
				+ "|" + Double.toString(elevation);

	}

	public void setTitle(String t) {
		title = t;
	}

	public String getTitle() {
		return title;
	}

	private float distanceTo = 0;

	public void setDistanceTo(float d) {
		this.distanceTo = d;
	}

	public float getDistanceTo() {
		return this.distanceTo;
	}

	/**
	 * db waypoint record id
	 */
	private long id;

	public void setId(long id) {
		this.id = id;
	}

	public long getId() {
		return this.id;
	}

}
