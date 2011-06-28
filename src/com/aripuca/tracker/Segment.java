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
	 * Index of the segment being recorded
	 */
	private long segmentIndex;

	public void setSegmentIndex(long sid) {
		this.segmentIndex = sid;
	}

	public long getSegmentIndex() {
		return this.segmentIndex;
	}

	/**
	 * Add new track to application db after recording started
	 */
	public long insertSegment(long trackId, long segmentIndex) {

		long finishTime = (new Date()).getTime();

		ContentValues values = new ContentValues();
		values.put("track_id", trackId);
		values.put("segment_index", segmentIndex);
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

		long newSegmentId = -1;
		try {

			newSegmentId = myApp.getDatabase().insertOrThrow("segments", null, values);

		} catch (SQLiteException e) {

			Toast.makeText(myApp.getMainActivity(), "SQLiteException: " + e.getMessage(), Toast.LENGTH_SHORT).show();
			Log.w(Constants.TAG, "SQLiteException: " + e.getMessage(), e);

		}
		
		return newSegmentId;

	}

}