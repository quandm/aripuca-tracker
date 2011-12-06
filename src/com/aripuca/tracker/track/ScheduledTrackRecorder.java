package com.aripuca.tracker.track;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.aripuca.tracker.MyApp;
import com.aripuca.tracker.app.Constants;
import com.aripuca.tracker.util.Utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteException;
import android.location.Location;
import android.util.Log;
import android.widget.Toast;

public class ScheduledTrackRecorder {

	private static ScheduledTrackRecorder instance = null;

	protected MyApp myApp;

	private int trackPointsCount;

	protected long trackTimeStart;

	private long gpsFixWaitTime;

	private Location lastRecordedLocation = null;

	/**
	 * @return the gpsFixWaitTime
	 */
	public long getGpsFixWaitTime() {
		return gpsFixWaitTime;
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
	 * @return the trackTimeStart
	 */
	public long getTrackTimeStart() {
		return trackTimeStart;
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
	 * returns stopRecordingAfter
	 */
	public long getStopRecordingAfter() {
		return stopRecordingAfter;
	}

	/**
	 * Id of the track being recorded
	 */
	private long trackId;

	public long getTrackId() {
		return this.trackId;
	}

	/**
	 * Singleton pattern
	 */
	public static ScheduledTrackRecorder getInstance(MyApp app) {

		if (instance == null) {
			instance = new ScheduledTrackRecorder(app);
		}

		return instance;

	}

	public ScheduledTrackRecorder(MyApp app) {

		this.myApp = app;

	}

	public void initialize() {

		// waypoint track settings
		requestInterval = Integer.parseInt(myApp.getPreferences().getString("wpt_request_interval", "10")) * 60 * 1000;
//		requestInterval = 1 * 60 * 1000;

		minAccuracy = Integer.parseInt(myApp.getPreferences().getString("wpt_min_accuracy", "30"));

		minDistance = Integer.parseInt(myApp.getPreferences().getString("wpt_min_distance", "200"));

		stopRecordingAfter = Integer.parseInt(myApp.getPreferences().getString("wpt_stop_recording_after", "1")) * 60 * 60 * 1000;
//		stopRecordingAfter = 3 * 60 * 1000;

		gpsFixWaitTime = Integer.parseInt(myApp.getPreferences().getString("wpt_gps_fix_wait_time", "2")) * 60 * 1000;

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

		try {
			// setting track id
			trackId = myApp.getDatabase().insertOrThrow("tracks", null, values);
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
		// for scheduled tracks distance is always 0
		values.put("distance", 0);
		values.put("recording", 0);

		try {

			myApp.getDatabase().update("tracks", values, "_id=?", new String[] { String.valueOf(this.getTrackId()) });

		} catch (SQLiteException e) {

			Log.e(Constants.TAG, "SQLiteException: " + e.getMessage(), e);
		}

	}

	/**
	 * records one track point on schedule
	 */
	public void recordTrackPoint(Location location) {

		// minimum distance check
		if (lastRecordedLocation != null && location.distanceTo(lastRecordedLocation) < minDistance) {
			// wait for next location
			return;
		}

		float distance = 0;
		if (lastRecordedLocation != null) {
			distance = location.distanceTo(lastRecordedLocation);
		}

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

			myApp.getDatabase().insertOrThrow("track_points", null, values);

			this.trackPointsCount++;

		} catch (SQLiteException e) {
			Log.e(Constants.TAG, "SQLiteException: " + e.getMessage(), e);
		}

		// save last location for distance calculation
		lastRecordedLocation = location;

	}

	private boolean recording = false;

	public boolean isRecording() {
		return recording;
	}

	public void start() {

		this.initialize();

		this.trackTimeStart = (new Date()).getTime();

		this.recording = true;

		this.trackPointsCount = 0;

		this.insertNewTrack();

	}

	public void stop() {

		recording = false;

		this.updateNewTrack();

	}

}
