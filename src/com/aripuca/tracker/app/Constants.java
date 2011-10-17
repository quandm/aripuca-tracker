package com.aripuca.tracker.app;

public abstract class Constants {

	public static final String TAG = "AripucaTracker";
	
	public static final String APP_NAME = "AripucaTracker";
	
	public static final int NOTIFICATION_RECORDING_TRACK = 1;
	
	/**
	 * map modes
	 */
	public static final int SHOW_WAYPOINT = 0;
	public static final int SHOW_TRACK = 1;

	/**
	 * map view modes
	 */
	public static final int MAP_STREET = 0;
	public static final int MAP_SATELLITE = 1;
	
	/**
	 * Minimum distance between 2 consecutive track points
	 */
	public static final int MIN_DISTANCE = 5; // meters
	
	public static final float MAX_ACCELERATION = 19.6F;
	
	public static final float MIN_SPEED = 0.224F; // meters per second

	public static final int SEGMENT_NONE = 0;
	public static final int SEGMENT_PAUSE_RESUME = 1;
	public static final int SEGMENT_DISTANCE = 2;
	public static final int SEGMENT_TIME = 3;
	public static final int SEGMENT_CUSTOM_1 = 4;
	public static final int SEGMENT_CUSTOM_2 = 5;
	
	public static final int ORIENTATION_PORTRAIT = 1;
	public static final int ORIENTATION_LANDSCAPE = 2;
	public static final int ORIENTATION_REVERSE_LANDSCAPE = 10;
	public static final int ORIENTATION_REVERSE_PORTRAIT = 20;
	
	/**
	 * location providers
	 */
	public static final int GPS_PROVIDER = 0;
	public static final int GPS_PROVIDER_LAST = 1;
	public static final int NETWORK_PROVIDER = 2;
	public static final int NETWORK_PROVIDER_LAST = 3;
	

	
}