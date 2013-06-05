package com.aripuca.tracker.recorder;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.database.sqlite.SQLiteException;
import android.location.Location;
import android.os.SystemClock;

import com.aripuca.tracker.App;
import com.aripuca.tracker.Constants;
import com.aripuca.tracker.db.Track;
import com.aripuca.tracker.db.TrackPoint;
import com.aripuca.tracker.db.TrackPoints;
import com.aripuca.tracker.db.Tracks;
import com.aripuca.tracker.utils.AppLog;
import com.aripuca.tracker.utils.Utils;

/**
 * Scheduled track recording class
 */
public class ScheduledTrackRecorder {

	private static ScheduledTrackRecorder instance = null;

	protected App app;

	private boolean recording = false;

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

	private Track track;

	public Track getTrack() {
		return this.track;
	}

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

		this.track = new Track();

		this.track.setTitle("New scheduled track");
		this.track.setActivity(Constants.ACTIVITY_SCHEDULED_TRACK);
		this.track.setRecording(1);
		this.track.setStartTime(this.trackTimeStart);
		this.track.setFinishTime(this.trackTimeStart);

		try {

			// insert new track record
			long trackId = Tracks.insert(app.getDatabase(), track);

			// set track id
			this.track.setId(trackId);

		} catch (SQLiteException e) {
			AppLog.e(app.getApplicationContext(), "SQLiteException: " + e.getMessage());
		}

	}

	/**
	 * Update track data after recording finished
	 */
	private void updateNewTrack() {

		this.track.setFinishTime((new Date()).getTime());
		this.track.setDistance(track.getDistance());
		this.track.setRecording(0);

		String trackTitle = (new SimpleDateFormat("yyyy-MM-dd H:mm")).format(this.track.getStartTime()) + "-"
				+ (new SimpleDateFormat("H:mm")).format(this.track.getFinishTime());

		track.setTitle(trackTitle);

		try {
			Tracks.update(app.getDatabase(), this.track);
		} catch (SQLiteException e) {
			AppLog.e(app.getApplicationContext(), "SQLiteException: " + e.getMessage());
		}

	}

	/**
	 * records one track point on schedule
	 */
	public void recordTrackPoint(Location location, float distance) {

		// accumulate total track distance
		this.track.setDistance(this.track.getDistance() + distance);

		TrackPoint trackPoint = new TrackPoint(this.track.getId(), location);
		trackPoint.setDistance(distance);

		try {
			TrackPoints.insert(app.getDatabase(), trackPoint);
		} catch (SQLiteException e) {
			AppLog.e(app.getApplicationContext(), "SQLiteException: " + e.getMessage());
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
	 * sets new location request start time that's how wait time for each location request is measured
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

		AppLog.d(app.getApplicationContext(),
				"requestTimeLimitReached: Start: " + Utils.formatInterval(requestStartTime, true) + " Elapsed: "
						+ Utils.formatInterval(SystemClock.elapsedRealtime(), true) + " Wait: "
						+ Utils.formatInterval(this.gpsFixWaitTime, true));

		if (this.requestStartTime + this.gpsFixWaitTime < SystemClock.elapsedRealtime()) {
			return true;
		} else {
			return false;
		}

	}

}
