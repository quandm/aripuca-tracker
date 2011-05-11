package com.aripuca.tracker;

import android.location.Location;

public class Waypoint extends Object {
	
	protected double latitude;
	protected double longitude;
	protected double elevation;
	protected long time;
	protected String title;
	
	public Waypoint(String title, long t, double lat, double lng, double ele) {
		
		this.title = title;
		this.latitude = lat;
		this.longitude = lng;
		this.time = t;
		this.elevation = ele;
		
//		description = descr;
		
	}
	/**
	 * returns simple Location object for calculating bearing and distance to this waypoint
	 * @return Location
	 */
	public Location getLocation() {
		
		Location loc = new Location("MyAPP");

		loc.setLatitude(latitude);
		loc.setLongitude(longitude);
		loc.setAltitude(elevation);
//		loc.setTime();
		
		return loc; 
		
	}
	/**
	 * returns string ready for storing
	 */
	public String getFormattedString() {
		
		return title+"|"+Long.toString(time)+"|"+Double.toString(latitude)+"|"+Double.toString(longitude)+
				"|"+Double.toString(elevation); 
		
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
