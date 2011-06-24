package com.aripuca.tracker;

public abstract class Constants {

	public static final String TAG = "AripucaTracker";
	
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
	 * Minimum distance between 2 consecutive track points to be recorded
	 */
	public static final int MIN_DISTANCE = 5; // meters
	
	public static final double MIN_SPEED = 0.224; // meters per second

	public static final double MAX_SEGMENT_DISTANCE = 5000; //meters
	
	public static final int SEGMENT_NONE = 0;
	public static final int SEGMENT_PAUSE_RESUME = 1;
	public static final int SEGMENT_EQUAL = 2;
	public static final int SEGMENT_CUSTOM_1 = 3;
	public static final int SEGMENT_CUSTOM_2 = 4;
	
	
}