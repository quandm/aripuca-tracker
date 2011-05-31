package com.aripuca.tracker;

import java.util.Date;

import android.content.ContentValues;
import android.database.sqlite.SQLiteException;
import android.util.Log;
import android.widget.Toast;

public class Segment extends AbstractTrack {

	public Segment(MyApp myApp) {

		super(myApp);

	}

	/**
	 * Id of the track being recorded
	 */
	private long segmentId;

	public void setSegmentId(long sid) {
		this.segmentId = sid;
	}

	public long getSegmentId() {
		return this.segmentId;
	}

	/**
	 * Add new track to application db after recording started
	 */
	public void insertSegment(long trackId) {

		long finishTime = (new Date()).getTime();

		ContentValues values = new ContentValues();
		values.put("track_id", trackId);
		values.put("distance", this.getDistance());
		values.put("total_time", this.getTotalTime());
		values.put("moving_time", this.getMovingTime());
		values.put("max_speed", this.getMaxSpeed());
		values.put("max_elevation", this.getMaxElevation());
		values.put("min_elevation", this.getMinElevation());
		values.put("elevation_gain", this.getElevationGain());
		values.put("elevation_loss", this.getElevationLoss());
		values.put("start_time", this.trackTimeStart);
		values.put("finish_time", finishTime);

		Log.w(Constants.TAG, "insertSegment: Total: " + this.getTotalTime() + " Moving: " + this.getMovingTime());

		try {

			myApp.getDatabase().insertOrThrow("segments", null, values);

		} catch (SQLiteException e) {

			Toast.makeText(myApp.getMainActivity(), "SQLiteException: " + e.getMessage(), Toast.LENGTH_SHORT).show();
			Log.w(Constants.TAG, "SQLiteException: " + e.getMessage(), e);

		}

	}

}