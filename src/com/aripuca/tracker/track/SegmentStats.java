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
	private long segmentIndex;
	
	public SegmentStats(Context context) {

		super(context);
		
	}
	
	public void setSegmentIndex(long sid) {
		this.segmentIndex = sid;
	}

	public long getSegmentIndex() {
		return this.segmentIndex;
	}

	/**
	 * Add new track to application db after recording started
	 */
	public long insertSegment(long trackId, int segmentIndex) {

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