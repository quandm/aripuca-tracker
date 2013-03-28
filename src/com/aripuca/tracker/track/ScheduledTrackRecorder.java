package com.aripuca.tracker.track;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.aripuca.tracker.App;
import com.aripuca.tracker.Constants;

import com.aripuca.tracker.utils.AppLog;
import com.aripuca.tracker.utils.Utils;

import android.content.ContentValues;
import android.database.sqlite.SQLiteException;
import android.location.Location;
import android.os.SystemClock;
import android.util.Log;

/**
 * Scheduled track recording class
 */
public class ScheduledTrackRecorder {

	private static ScheduledTrackRecorder instance = null;

	protected App app;

	private boolean recording = false;
	
	private float totalDistance = 0;

	protected long trackTimeStart;

	/**
	 * wait time for GPS fix of acceptable accuracy
	 */
	private long gpsFixWaitTime;

	/**
	 * new scheduler session start time in milliseconds
	 */
	private long startTime;

	/**
	 * new location request start time in milliseconds
	 */
	private long requestStartTime;

	/**
	 * @return the startTime
	 */
	public long getStartTime() {
		return startTime;
	}

	/**
	 * minimum distance between two recorded points
	 */
	private int minDistance;

	/**
	 * minimum accuracy required for recording in meters
	 */
	private int minAccuracy;

	/**
	 * returns minAccuracy
	 */
	public int getMinAccuracy() {
		return minAccuracy;
	}

	/**
	 * time interval between location requests in milliseconds
	 */
	private long requestInterval;

	/**
	 * returns requestInterval
	 */
	public long getRequestInterval() {
		return requestInterval;
	}

	/**
	 * recording time limit in milliseconds
	 */
	private long stopRecordingAfter;

	/**
	 * Id of the track being recorded
	 */
	private long trackId = 0;

	public long getTrackId() {
		return this.trackId;
	}

	/**
	 * Singleton pattern
	 */
	public static ScheduledTrackRecorder getInstance(App app) {

		if (instance == null) {
			instance = new ScheduledTrackRecorder(app);
		}

		return instance;

	}

	public ScheduledTrackRecorder(App app) {

		this.app = app;

	}

	public void initialize() {

		// interval between requests in seconds
		requestInterval = Integer.parseInt(app.getPreferences().getString("wpt_request_interval", "10")) * 60;

		// milliseconds
		gpsFixWaitTime = Integer.parseInt(app.getPreferences().getString("wpt_gps_fix_wait_time", "2")) * 60 * 1000;

		// acceptable accuracy
		minAccuracy = Integer.parseInt(app.getPreferences().getString("wpt_min_accuracy", "30"));

		// minimum distance between two recorded points
		minDistance = Integer.parseInt(app.getPreferences().getString("wpt_min_distance", "200"));

		// stop scheduler after
		stopRecordingAfter = Integer.parseInt(app.getPreferences().getString("wpt_stop_recording_after", "1")) * 60 * 60 * 1000;

	}

	/**
	 * Add new track to application db after recording started
	 */
	private void insertNewTrack() {

		ContentValues values = new ContentValues();
		values.put("title", "New scheduled track");
		values.put("activity", 1);
		values.put("recording", 1);
		values.put("start_time", this.trackTimeStart);
		values.put("finish_time", this.trackTimeStart);

		try {
			// setting track id
			trackId = app.getDatabase().insertOrThrow("tracks", null, values);
		} catch (SQLiteException e) {
			Log.e(Constants.TAG, "SQLiteException: " + e.getMessage(), e);
		}

	}

	/**
	 * Update track data after recording finished
	 */
	private void updateNewTrack() {

		long finishTime = (new Date()).getTime();

		String trackTitle = (new SimpleDateFormat("yyyy-MM-dd H:mm")).format(this.trackTimeStart) + "-"
				+ (new SimpleDateFormat("H:mm")).format(finishTime);

		ContentValues values = new ContentValues();
		values.put("title", trackTitle);
		values.put("finish_time", finishTime);
		values.put("distance", Utils.formatNumber(this.totalDistance, 2));
		values.put("recording", 0);

		try {

			app.getDatabase().update("tracks", values, "_id=?", new String[] { String.valueOf(this.getTrackId()) });

		} catch (SQLiteException e) {

			Log.e(Constants.TAG, "SQLiteException: " + e.getMessage(), e);
		}

	}

	/**
	 * records one track point on schedule
	 */
	public void recordTrackPoint(Location location, float distance) {
		
		totalDistance += distance;

		ContentValues values = new ContentValues();
		values.put("track_id", this.getTrackId());
		values.put("lat", (int) (location.getLatitude() * 1E6));
		values.put("lng", (int) (location.getLongitude() * 1E6));
		values.put("elevation", Utils.formatNumber(location.getAltitude(), 1));
		values.put("speed", Utils.formatNumber(location.getSpeed(), 2));
		values.put("time", (new Date()).getTime());
		values.put("distance", Utils.formatNumber(distance, 1));
		values.put("accuracy", location.getAccuracy());

		try {

			app.getDatabase().insertOrThrow("track_points", null, values);

		} catch (SQLiteException e) {
			Log.e(Constants.TAG, "SQLiteException: " + e.getMessage(), e);
		}

	}

	public boolean isRecording() {
		return recording;
	}

	public void start() {

		this.startTime = SystemClock.elapsedRealtime();

		this.initialize();

		this.trackTimeStart = (new Date()).getTime();

		this.recording = true;

		this.insertNewTrack();

	}

	public void stop() {

		recording = false;

		this.updateNewTrack();

	}

	/**
	 * sets new location request start time that's how wait time for each
	 * location request is measured
	 */
	public void setRequestStartTime() {
		requestStartTime = SystemClock.elapsedRealtime();
	}

	/**
	 * returns current location request start time
	 */
	public long getRequestStartTime() {
		return requestStartTime;
	}

	public long getMinDistance() {
		return minDistance;
	}

	/**
	 * checking stopRecordingAfter time limit
	 */
	public boolean timeLimitReached() {

		// if stopRecordingAfter == 0 scheduler will run indefinitely
		if (stopRecordingAfter != 0 && startTime + stopRecordingAfter < SystemClock.elapsedRealtime()) {
			return true;
		} else {
			return false;
		}

	}

	/**
	 * Checking if acceptable accuracy
	 * 
	 * @return boolean
	 */
	public boolean gpsFixWaitTimeLimitReached() {

		AppLog.d(app.getApplicationContext(), "requestTimeLimitReached: Start: " + Utils.formatInterval(requestStartTime, true) + " Elapsed: "
				+ Utils.formatInterval(SystemClock.elapsedRealtime(), true) + " Wait: "
				+ Utils.formatInterval(this.gpsFixWaitTime, true));

		if (this.requestStartTime + this.gpsFixWaitTime < SystemClock.elapsedRealtime()) {
			return true;
		} else {
			return false;
		}

	}

}
