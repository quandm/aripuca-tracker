package com.aripuca.tracker.track;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.aripuca.tracker.Constants;
import com.aripuca.tracker.utils.Utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteException;
import android.location.Location;
import android.util.Log;
import android.widget.Toast;

/**
 * Track statistics class
 */
public class Track extends AbstractTrack {

	public Track(Context context) {

		super(context);

		this.insertNewTrack();

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
	 * Add new track to application db after recording started
	 */
	public void insertNewTrack() {

		ContentValues values = new ContentValues();
		values.put("title", "New track");
		values.put("activity", 0);
		values.put("recording", 1);
		values.put("start_time", this.trackTimeStart);

		try {
			long newTrackId = app.getDatabase().insertOrThrow("tracks", null, values);
			this.setTrackId(newTrackId);
		} catch (SQLiteException e) {
			Toast.makeText(context, "SQLiteException: " + e.getMessage(), Toast.LENGTH_SHORT).show();
			app.loge("Track.insertNewTrack:  SQLiteException: " + e.getMessage());
		}

	}

	/**
	 * Update track data after recording finished
	 */
	protected void finishNewTrack() {

		long finishTime = (new Date()).getTime();

		String trackTitle = (new SimpleDateFormat("yyyy-MM-dd H:mm")).format(this.trackTimeStart) + "-"
				+ (new SimpleDateFormat("H:mm")).format(finishTime);

		ContentValues values = new ContentValues();
		values.put("title", trackTitle);
		values.put("distance", Utils.formatNumber(this.getDistance(), 1));
		values.put("total_time", this.getTotalTime());
		values.put("moving_time", this.getMovingTime());
		values.put("max_speed", Utils.formatNumber(this.getMaxSpeed(), 2));
		values.put("max_elevation", Utils.formatNumber(this.getMaxElevation(), 1));
		values.put("min_elevation", Utils.formatNumber(this.getMinElevation(), 1));
		values.put("elevation_gain", this.getElevationGain());
		values.put("elevation_loss", this.getElevationLoss());
		values.put("finish_time", finishTime);
		values.put("recording", 0);

		try {

			app.getDatabase().update("tracks", values, "_id=?", new String[] { String.valueOf(this.getTrackId()) });

		} catch (SQLiteException e) {

			Toast.makeText(context, "SQLiteException: " + e.getMessage(), Toast.LENGTH_SHORT).show();

			Log.e(Constants.TAG, "SQLiteException: " + e.getMessage(), e);
		}

	}

	/**
	 * Update track data during recording
	 */
	protected void updateNewTrack() {

		ContentValues values = new ContentValues();
		values.put("distance", Utils.formatNumber(this.getDistance(), 1));
		values.put("total_time", this.getTotalTime());
		values.put("moving_time", this.getMovingTime());
		values.put("max_speed", Utils.formatNumber(this.getMaxSpeed(), 2));
		values.put("max_elevation", Utils.formatNumber(this.getMaxElevation(), 1));
		values.put("min_elevation", Utils.formatNumber(this.getMinElevation(), 1));
		values.put("elevation_gain", this.getElevationGain());
		values.put("elevation_loss", this.getElevationLoss());

		try {

			app.getDatabase().update("tracks", values, "_id=?", new String[] { String.valueOf(this.getTrackId()) });

		} catch (SQLiteException e) {

			Toast.makeText(context, "SQLiteException: " + e.getMessage(), Toast.LENGTH_SHORT).show();

			Log.e(Constants.TAG, "SQLiteException: " + e.getMessage(), e);
		}

	}

	/**
	 * Record one track point
	 * 
	 * @param location Current location
	 */
	protected void recordTrackPoint(Location location, int segmentIndex) {

		ContentValues values = new ContentValues();
		values.put("track_id", this.getTrackId());
		values.put("lat", (int) (location.getLatitude() * 1E6));
		values.put("lng", (int) (location.getLongitude() * 1E6));
		values.put("elevation", Utils.formatNumber(location.getAltitude(), 1));
		values.put("speed", Utils.formatNumber(location.getSpeed(), 2));
		values.put("time", (new Date()).getTime());
		values.put("segment_index", segmentIndex);
		values.put("distance", Utils.formatNumber(this.distance, 1));
		values.put("accuracy", location.getAccuracy());

		try {

			app.getDatabase().insertOrThrow("track_points", null, values);

		} catch (SQLiteException e) {

			Toast.makeText(context, "SQLiteException: " + e.getMessage(), Toast.LENGTH_SHORT).show();

			Log.e(Constants.TAG, "SQLiteException: " + e.getMessage(), e);

		}

	}

}
