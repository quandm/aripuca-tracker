package com.aripuca.tracker.db;

import java.util.Date;

import android.database.Cursor;
import android.location.Location;

import com.google.android.maps.GeoPoint;

/**
 * 
 */
public class TrackPoint {

	private long id;
	
	private long trackId;
	
	private int segmentIndex = 0;
	
	private float distance = 0;
	
	private double lat = 0;
	
	private double lng = 0;
	
	private float accuracy = 0;

	private double elevation = 0;

	private float speed = 0;
	
	private long time = 0;
	

	public TrackPoint(Cursor cursor) {

		this.id = cursor.getLong(cursor.getColumnIndex("_id"));

		this.trackId = cursor.getLong(cursor.getColumnIndex("track_id"));

		this.segmentIndex = cursor.getInt(cursor.getColumnIndex("segment_index"));

		this.distance = cursor.getFloat(cursor.getColumnIndex("distance"));
		
		this.lat = cursor.getInt(cursor.getColumnIndex("lat")) / 1E6;

		this.lng = cursor.getInt(cursor.getColumnIndex("lng")) / 1E6;
		
		this.accuracy = cursor.getFloat(cursor.getColumnIndex("accuracy"));

		this.elevation = cursor.getDouble(cursor.getColumnIndex("elevation"));

		this.speed = cursor.getFloat(cursor.getColumnIndex("speed"));
		
		this.time = cursor.getLong(cursor.getColumnIndex("time"));

	}	
	
	public TrackPoint(double lat, double lng) {

		this.lat = lat;
		
		this.lng = lng;

	}

	public TrackPoint(long trackId, Location location) {

		this.trackId = trackId;
		
		this.lat = location.getLatitude();
		
		this.lng = location.getLongitude();

		this.elevation = location.getAltitude();
		
		this.speed = location.getSpeed();
		
		this.time = (new Date()).getTime();
		
		this.accuracy = location.getAccuracy();
		
	}
	
	/**
	 * 
	 * @return
	 */
	public long getId() {
		return id;
	}

	/**
	 * 
	 * @param id
	 */
	public void setId(long id) {
		this.id = id;
	}


	public long getTrackId() {
		return trackId;
	}

	public void setTrackId(long trackId) {
		this.trackId = trackId;
	}


	public int getSegmentIndex() {
		return segmentIndex;
	}


	public void setSegmentIndex(int segmentIndex) {
		this.segmentIndex = segmentIndex;
	}
	
	public float getDistance() {
		return distance;
	}


	public void setDistance(float distance) {
		this.distance = distance;
	}

	/**
	 * @return the lat
	 */
	public double getLat() {
		return lat;
	}

	/**
	 * @return the lat in 1E6 format
	 */
	public int getLatE6() {
		return (int) (this.lat * 1E6);
	}

	/**
	 * @param lat the lat to set
	 */
	public void setLat(double lat) {
		this.lat = lat;
	}

	/**
	 * @return the lng
	 */
	public double getLng() {
		return lng;
	}

	/**
	 * @return the lng in 1E6 format
	 */
	public int getLngE6() {
		return (int) (this.lng * 1E6);
	}

	/**
	 * @param lng the lng to set
	 */
	public void setLng(double lng) {
		this.lng = lng;
	}
	
	
	public float getAccuracy() {
		return accuracy;
	}


	public void setAccuracy(float accuracy) {
		this.accuracy = accuracy;
	}


	public double getElevation() {
		return elevation;
	}


	public void setElevation(double elevation) {
		this.elevation = elevation;
	}


	public float getSpeed() {
		return speed;
	}


	public void setSpeed(float speed) {
		this.speed = speed;
	}


	public long getTime() {
		return time;
	}


	public void setTime(long time) {
		this.time = time;
	}


	public GeoPoint getGeoPoint() {
		
		return new GeoPoint(this.getLatE6(), this.getLngE6());
		
	}
	
	public Location getLocation() {
		
		Location loc = new Location("gps");

		loc.setLatitude(lat);
		loc.setLongitude(lng);
		loc.setAltitude(elevation);
		loc.setTime(time);

		return loc;
		
	}
	

}
