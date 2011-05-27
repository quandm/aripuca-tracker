package com.aripuca.tracker;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.content.ContentValues;
import android.database.sqlite.SQLiteException;
import android.location.Location;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

public class Segment {

	/**
	 * Reference to application object for accessing db etc.
	 */
	protected MyApp myApp;

	protected float distance = 0;

	protected Location currentLocation;

	protected Location lastRecordedLocation;

	protected float averageSpeed = 0;

	protected float maxSpeed = 0;

	protected double currentElevation = 0;

	protected double minElevation = 8848;

	protected double maxElevation = -10971;

	protected double elevationGain = 0;

	protected double elevationLoss = 0;

	protected boolean trackingPaused = false;

	protected int trackPointsCount = 0;

	protected long currentSystemTime = 0;

	/**
	 * Id of the current track segment
	 */
	private int segmentId = 0;

	/**
	 * recording start time
	 */
	protected long startTime = 0;

	/**
	 * real time of the track start
	 */
	protected long trackTimeStart;

	/**
	 * total time of recording
	 */
	protected long totalTime = 0;
	
	protected long movingTime = 0;
	/**
	 * recording paused start time
	 */
	protected long pauseTimeStart = 0;

	public Segment(MyApp myApp) {
		
		this.myApp = myApp;

		this.trackTimeStart = (new Date()).getTime();

	}

	public void addNewSegment() {
		
		
		
	}
	
	
}