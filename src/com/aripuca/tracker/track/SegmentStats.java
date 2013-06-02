package com.aripuca.tracker.track;

import java.util.Date;

import android.content.Context;
import android.database.sqlite.SQLiteException;
import android.widget.Toast;

import com.aripuca.tracker.db.Segment;
import com.aripuca.tracker.db.Segments;
import com.aripuca.tracker.utils.AppLog;

public class SegmentStats extends AbstractTrackStats {
	
	/**
	 * Index of the segment being recorded
	 */
	private Segment segment;
	
	/**
	 * Object collecting current segment statistics
	 * 
	 * @param context
	 * @param trackId
	 * @param segmentIndex
	 */
	public SegmentStats(Context context, long trackId, int segmentIndex) {

		super(context);
		
		this.insertNewSegment(trackId, segmentIndex);
		
	}
	
	public void insertNewSegment(long trackId, int segmentIndex) {

		this.segment = new Segment(trackId, segmentIndex);

		this.segment.setStartTime(this.trackTimeStart);
		this.segment.setFinishTime(this.trackTimeStart);

		try {

			long segmentId = Segments.insert(app.getDatabase(), segment);
			this.segment.setId(segmentId);
			
		} catch (SQLiteException e) {

			Toast.makeText(context, "SQLiteException: " + e.getMessage(), Toast.LENGTH_SHORT).show();
			AppLog.w(context, "SQLiteException: " + e.getMessage());

		}

	}
	
	/**
	 * Update track data during recording
	 */
	protected void updateNewSegment() {

		segment.setDistance(this.getDistance());
		segment.setTotalTime(this.getTotalTime());
		segment.setMovingTime(this.getMovingTime());
		segment.setMaxSpeed(this.getMaxSpeed());
		segment.setMaxElevation(this.getMaxElevation());
		segment.setMinElevation(this.getMinElevation());
		segment.setElevationGain(this.getElevationGain());
		segment.setElevationLoss(this.getElevationLoss());
		segment.setFinishTime((new Date()).getTime());

		int affectedRows = Segments.update(app.getDatabase(), segment);

		if (affectedRows == 0) {
			AppLog.e(context, "updateNewSegment failed");
		}

	}
	
	/**
	 * Add new track to application db after recording started
	 */
	public long insertSegment_DEPRECATED(long trackId, int segmentIndex) {

		long finishTime = (new Date()).getTime();

		Segment segment = new Segment(trackId, segmentIndex);

		segment.setDistance(this.getDistance());
		segment.setTotalTime(this.getTotalTime());
		segment.setMovingTime(this.getMovingTime());
		segment.setMaxSpeed(this.getMaxSpeed());
		segment.setMaxElevation(this.getMaxElevation());
		segment.setMinElevation(this.getMinElevation());
		segment.setElevationGain(this.getElevationGain());
		segment.setElevationLoss(this.getElevationLoss());
		segment.setStartTime(this.trackTimeStart);
		segment.setFinishTime(finishTime);

		long newSegmentId = -1;
		try {

			newSegmentId = Segments.insert(app.getDatabase(), segment);
			
		} catch (SQLiteException e) {

			Toast.makeText(context, "SQLiteException: " + e.getMessage(), Toast.LENGTH_SHORT).show();
			AppLog.w(context, "SQLiteException: " + e.getMessage());

		}

		return newSegmentId;

	}
	
}