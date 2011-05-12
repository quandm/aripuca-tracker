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

public class Track {

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
	/**
	 * total time recording was paused
	 */
	protected long totalPauseTime = 0;

	protected long totalIdleTime = 0;

	protected long idleTimeStart = 0;

	/**
	 * 
	 */
	protected Activity activity;

	public Track(MyApp myApp) {

		this.myApp = myApp;

		this.trackTimeStart = (new Date()).getTime();

		this.saveNewTrack();

	}

	// --------------------------------------------------------------------------------------------------------

	public long getTotalIdleTime() {
		return this.totalIdleTime;
	}

	/**
	 * Return time track recording started
	 */
	public long getTrackTimeStart() {

		return this.trackTimeStart;

	}

	/**
	 * Get average trip speed in meters per second
	 */
	public float getAverageSpeed() {

		if (this.getTotalTime() < 1000) {
			return 0;
		}

		return this.getDistance() / (this.getTotalTime() / 1000.0f);
	}

	/**
	 * @return Average moving speed in meters per second
	 */
	public float getAverageMovingSpeed() {

		if (this.getMovingTime() < 1000) {
			return 0;
		}

		return this.getDistance() / (this.getMovingTime() / 1000.0f);
	}

	/**
	 * Get total trip distance
	 */
	public float getDistance() {
		return this.distance;
	}

	/**
	 * Get maximum trip speed
	 */
	public float getMaxSpeed() {
		return this.maxSpeed;
	}

	/**
	 * @return Elevation gain during track recording
	 */
	public int getElevationGain() {
		return (int) this.elevationGain;
	}

	/**
	 * @return Elevation loss during track recording
	 */
	public int getElevationLoss() {
		return (int) this.elevationLoss;
	}

	/**
	 * @return Max elevation during track recording
	 */
	public double getMaxElevation() {
		return this.maxElevation;
	}

	/**
	 * @return Min elevation during track recording
	 */
	public double getMinElevation() {
		return this.minElevation;
	}

	/**
	 * Id of the track being recorded
	 */
	private long trackId;

	public void setTrackId(long tid) {
		this.trackId = tid;
	}

	public long getTrackId() {
		return this.trackId;
	}

	/**
	 * Stop track recording
	 */
	public void stopRecording() {

		// updating track statistics in db
		this.updateNewTrack();

	}

	public int getTrackPointsCount() {
		return trackPointsCount;
	}

	/**
	 * Return true if track recording paused
	 */
	public boolean isTrackingPaused() {
		return this.trackingPaused;
	}

	/**
	 * Pause track recording
	 */
	public void pause() {

		this.trackingPaused = true;

	}

	/**
	 * Resume track recording
	 */
	public void resume() {

		this.trackingPaused = false;

	}

	/**
	 * Get total trip time in milliseconds
	 */
	public long getTotalTime() {
		return this.currentSystemTime - this.startTime - this.totalPauseTime;
	}

	/**
	 * Get total moving trip time in milliseconds
	 */
	public long getMovingTime() {
		return this.getTotalTime() - this.totalIdleTime;
	}

	/**
	 * Updates track statistics when recording
	 * 
	 * @param location
	 */
	public void updateStatistics(Location location) {

		this.currentSystemTime = SystemClock.uptimeMillis();

		if (this.startTime == 0) {
			this.startTime = this.currentSystemTime;
		}

		this.processPauseTime();

		if (this.trackingPaused) {
			// after resuming the recording we will start measuring distance from saved location
			this.currentLocation = location;
			return;
		}

		// calculating total distance starting from 2nd update
		if (this.currentLocation != null && this.currentLocation.getSpeed() != 0) {
			this.distance += this.currentLocation.distanceTo(location);
		}

		// save current location once distance is incremented
		this.currentLocation = location;

		this.segmentTrack();

		this.processIdleTime();

		this.processElevation();

		this.processSpeed();

		// add new track point to db
		this.recordTrackPoint(this.currentLocation);

	}
	
	private void segmentTrack() {
		
		// let's segment our track every MAX_SEGMENT_DISTANCE
		if (this.distance/Constants.MAX_SEGMENT_DISTANCE > this.segmentId+1) {
			this.segmentId++;
		}
		
	}

	private void processPauseTime() {

		if (this.trackingPaused) {

			// if idle interval started increment total idle time
			if (this.idleTimeStart != 0) {
				this.totalIdleTime += this.currentSystemTime - this.idleTimeStart;
				this.idleTimeStart = 0;
			}

			if (this.pauseTimeStart != 0) {
				this.totalPauseTime += this.currentSystemTime - this.pauseTimeStart;
			}

			// saving new pause time start
			this.pauseTimeStart = this.currentSystemTime;

		} else {

			if (this.pauseTimeStart != 0) {
				this.totalPauseTime += this.currentSystemTime - this.pauseTimeStart;
			}

			this.pauseTimeStart = 0;
		}

	}

	private void processIdleTime() {

		// updating idle time in track
		if (this.currentLocation.getSpeed() < Constants.MIN_SPEED) {

			// if idle interval started increment total idle time 
			if (this.idleTimeStart != 0) {
				this.totalIdleTime += this.currentSystemTime - this.idleTimeStart;
			}
			// save start idle time
			this.idleTimeStart = this.currentSystemTime;

		} else {

			// increment total idle time with already started interval  
			if (this.idleTimeStart != 0) {
				this.totalIdleTime += this.currentSystemTime - this.idleTimeStart;
				this.idleTimeStart = 0;
			}

		}

	}

	private void processElevation() {

		// processing elevation data
		if (this.currentLocation.hasAltitude()) {
			double e = this.currentLocation.getAltitude();
			// max elevation
			if (this.maxElevation < e) {
				this.maxElevation = e;
			}
			// min elevation
			if (e < this.minElevation) {
				this.minElevation = e;
			}
			// if current elevation not set yet
			if (this.currentElevation == 0) {
				this.currentElevation = e;
			} else {
				// elevation gain/loss
				if (e > this.currentElevation) {
					this.elevationGain += e - this.currentElevation;
				} else {
					this.elevationLoss += this.currentElevation - e;
				}
				this.currentElevation = e;
			}
		}
	}

	private void processSpeed() {

		// calculating max speed
		if (this.currentLocation.hasSpeed()) {

			float s = this.currentLocation.getSpeed();

			if (s == 0) {
				s = this.getAverageSpeed();
			}
			if (s > this.maxSpeed) {
				this.maxSpeed = s;
			}
		}
	}

	/**
	 * Add new track to application db after recording started
	 */
	private void saveNewTrack() {

		ContentValues values = new ContentValues();
		values.put("title", "New track");
		values.put("recording", 1);
		values.put("start_time", this.getTrackTimeStart());

		try {
			long newTrackId = myApp.getDatabase().insertOrThrow("tracks", null, values);
			this.setTrackId(newTrackId);
		} catch (SQLiteException e) {
			Toast.makeText(myApp.getMainActivity(), "SQLiteException: " + e.getMessage(), Toast.LENGTH_SHORT).show();
			Log.w(Constants.TAG, "SQLiteException: " + e.getMessage(), e);
		}

	}

	/**
	 * Update track data after recording finished
	 */
	protected void updateNewTrack() {

		long finishTime = (new Date()).getTime();

		String trackTitle = (new SimpleDateFormat("yyyy-MM-dd H:mm")).format(this.getTrackTimeStart()) + "-" +
								(new SimpleDateFormat("H:mm")).format(finishTime);

		ContentValues values = new ContentValues();
		values.put("title", trackTitle);
		values.put("distance", this.getDistance());
		values.put("total_time", this.getTotalTime());
		values.put("moving_time", this.getMovingTime());
		values.put("max_speed", this.getMaxSpeed());
		values.put("max_elevation", this.getMaxElevation());
		values.put("min_elevation", this.getMinElevation());
		values.put("elevation_gain", this.getElevationGain());
		values.put("elevation_loss", this.getElevationLoss());
		values.put("finish_time", finishTime);
		values.put("recording", 0);

		try {
			myApp.getDatabase().update("tracks", values, "_id=?", new String[] { String.valueOf(this.getTrackId()) });
		} catch (SQLiteException e) {
			Toast.makeText(myApp.getMainActivity(), "SQLiteException: " + e.getMessage(), Toast.LENGTH_SHORT).show();
			Log.e(Constants.TAG, "SQLiteException: " + e.getMessage(), e);
		}

	}

	/**
	 * Record one track point
	 * 
	 * @param location Current location
	 */
	protected void recordTrackPoint(Location location) {

		// record points only if distance between 2 consecutive points is greater than min_distance
		if (this.lastRecordedLocation != null) {
			if (this.lastRecordedLocation.distanceTo(location) < Constants.MIN_DISTANCE) {
				return;
			}
		}

		ContentValues values = new ContentValues();
		values.put("track_id", this.getTrackId());
		values.put("lat", location.getLatitude());
		values.put("lng", location.getLongitude());
		values.put("elevation", location.getAltitude());
		values.put("speed", location.getSpeed());
		values.put("time", (new Date()).getTime());
		values.put("segment_id", segmentId);
		values.put("distance", this.distance);
		values.put("accuracy", location.getAccuracy());

		try {

			myApp.getDatabase().insertOrThrow("track_points", null, values);

			this.lastRecordedLocation = location;

			this.trackPointsCount++;

		} catch (SQLiteException e) {
			Toast.makeText(myApp.getMainActivity(), "SQLiteException: " + e.getMessage(), Toast.LENGTH_SHORT).show();
			Log.e(Constants.TAG, "SQLiteException: " + e.getMessage(), e);
		}

	}

}
