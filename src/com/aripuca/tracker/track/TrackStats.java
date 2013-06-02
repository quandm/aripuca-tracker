package com.aripuca.tracker.track;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.content.Context;
import android.database.sqlite.SQLiteException;
import android.location.Location;
import android.widget.Toast;

import com.aripuca.tracker.Constants;
import com.aripuca.tracker.db.Track;
import com.aripuca.tracker.db.TrackPoint;
import com.aripuca.tracker.db.TrackPoints;
import com.aripuca.tracker.db.Tracks;
import com.aripuca.tracker.utils.AppLog;

/**
 * Track statistics class
 */
public class TrackStats extends AbstractTrackStats {

	private Track track;

	/**
	 * 
	 * @param context
	 */
	public TrackStats(Context context) {

		super(context);

		this.insertNewTrack();

	}

	/**
	 * TrackStats for interrupted track
	 * 
	 * @param context
	 * @param track
	 */
	public TrackStats(Context context, Track track) {

		super(context);

		this.track = track;

		// loading saved track statistics
		this.distance = this.track.getDistance();
		this.maxSpeed = this.track.getMaxSpeed();
		this.minElevation = this.track.getMinElevation();
		this.maxElevation = this.track.getMaxElevation();
		this.elevationGain = this.track.getElevationGain();
		this.elevationLoss = this.track.getElevationLoss();

		this.trackTimeStart = this.track.getStartTime();
		this.totalIdleTime = this.track.getTotalTime() - this.track.getMovingTime();

	}

	public Track getTrack() {
		return this.track;
	}

	/**
	 * Add new track to application db after recording started
	 */
	public void insertNewTrack() {

		this.track = new Track();
		
		track.setTitle("New track");
		track.setActivity(Constants.ACTIVITY_TRACK);
		track.setRecording(Constants.TRACK_RECORDING_IN_PROGRESS);
		track.setStartTime(this.trackTimeStart);
		track.setFinishTime(this.trackTimeStart);

		try {

			long trackId = Tracks.insert(app.getDatabase(), track);

			track.setId(trackId);

		} catch (SQLiteException e) {
			Toast.makeText(context, "SQLiteException: " + e.getMessage(), Toast.LENGTH_SHORT).show();
			AppLog.e(app.getApplicationContext(), "TrackStats.insertNewTrack:  SQLiteException: " + e.getMessage());
		}

	}

	/**
	 * Update track data after recording finished
	 */
	protected void finishNewTrack() {

		long finishTime = (new Date()).getTime();

		String trackTitle = (new SimpleDateFormat("yyyy-MM-dd H:mm", Locale.US)).format(this.trackTimeStart) + "-"
				+ (new SimpleDateFormat("H:mm", Locale.US)).format(finishTime);

		track.setTitle(trackTitle);

		track.setDistance(this.getDistance());
		track.setTotalTime(this.getTotalTime());
		track.setMovingTime(this.getMovingTime());
		track.setMaxSpeed(this.getMaxSpeed());
		track.setMaxElevation(this.getMaxElevation());
		track.setMinElevation(this.getMinElevation());
		track.setElevationGain(this.getElevationGain());
		track.setElevationLoss(this.getElevationLoss());

		track.setRecording(Constants.TRACK_RECORDING_STOPPED);
		track.setFinishTime(finishTime);

		int affectedRows = Tracks.update(app.getDatabase(), track);
		if (affectedRows == 0) {
			Toast.makeText(context, "finishNewTrack updates failed", Toast.LENGTH_SHORT).show();
			AppLog.e(context, "finishNewTrack updates failed");
		}

	}

	/**
	 * Update track data during recording
	 */
	protected void updateNewTrack() {

		track.setDistance(this.getDistance());
		track.setTotalTime(this.getTotalTime());
		track.setMovingTime(this.getMovingTime());
		track.setMaxSpeed(this.getMaxSpeed());
		track.setMaxElevation(this.getMaxElevation());
		track.setMinElevation(this.getMinElevation());
		track.setElevationGain(this.getElevationGain());
		track.setElevationLoss(this.getElevationLoss());
		track.setFinishTime((new Date()).getTime());

		int affectedRows = Tracks.update(app.getDatabase(), track);

		if (affectedRows == 0) {
			Toast.makeText(context, "updateNewTrack failed", Toast.LENGTH_SHORT).show();
			AppLog.e(context, "updateNewTrack failed");
		}

	}

	/**
	 * Record one track point
	 * 
	 * @param location Current location
	 */
	protected void recordTrackPoint(Location location, int segmentIndex) {

		TrackPoint trackPoint = new TrackPoint(this.getTrack().getId(), location);

		trackPoint.setDistance(this.distance);
		trackPoint.setSegmentIndex(segmentIndex);

		try {

			TrackPoints.insert(app.getDatabase(), trackPoint);

		} catch (SQLiteException e) {
			Toast.makeText(context, "SQLiteException: " + e.getMessage(), Toast.LENGTH_SHORT).show();
			AppLog.e(app.getApplicationContext(), "SQLiteException: " + e.getMessage());
		}

	}

}
