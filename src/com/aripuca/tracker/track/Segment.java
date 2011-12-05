package com.aripuca.tracker.track;

import java.util.Date;

import com.aripuca.tracker.app.Constants;
import com.aripuca.tracker.util.Utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteException;
import android.util.Log;
import android.widget.Toast;

public class Segment extends AbstractTrack {

	public Segment(Context context) {

		super(context);

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
		values.put("distance", Utils.formatNumber(this.getDistance(),1));
		values.put("total_time", this.getTotalTime());
		values.put("moving_time", this.getMovingTime());
		values.put("max_speed", Utils.formatNumber(this.getMaxSpeed(), 2));
		values.put("max_elevation", Utils.formatNumber(this.getMaxElevation(),1));
		values.put("min_elevation", Utils.formatNumber(this.getMinElevation(), 1));
		values.put("elevation_gain", this.getElevationGain());
		values.put("elevation_loss", this.getElevationLoss());
		values.put("start_time", this.trackTimeStart);
		values.put("finish_time", finishTime);

		Log.w(Constants.TAG, "insertSegment: Total: " + this.getTotalTime() + " Moving: " + this.getMovingTime());

		long newSegmentId = -1;
		try {

			newSegmentId = myApp.getDatabase().insertOrThrow("segments", null, values);

		} catch (SQLiteException e) {

			Toast.makeText(context, "SQLiteException: " + e.getMessage(), Toast.LENGTH_SHORT).show();
			Log.w(Constants.TAG, "SQLiteException: " + e.getMessage(), e);

		}
		
		return newSegmentId;

	}

}