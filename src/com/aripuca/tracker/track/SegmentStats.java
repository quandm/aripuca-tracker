package com.aripuca.tracker.track;

import java.util.Date;

import android.content.Context;
import android.database.sqlite.SQLiteException;
import android.widget.Toast;

import com.aripuca.tracker.db.Segment;
import com.aripuca.tracker.db.Segments;
import com.aripuca.tracker.db.Track;
import com.aripuca.tracker.utils.AppLog;

public class SegmentStats extends AbstractTrackStats {
	
	private Segment segment;
	
	/**
	 * Id of the track being recorded
	 */
	private long segmentId;

	/**
	 * Index of the segment being recorded
	 */
	private long segmentIndex;
	
	public SegmentStats(Context context) {

		super(context);
		
	}
	
	public SegmentStats(Context context, Segment segment) {

		super(context);

		this.segment = segment;
		
		this.segmentId = this.segment.getId();

		// loading saved track statistics
		this.distance = this.segment.getDistance();
		this.maxSpeed = this.segment.getMaxSpeed();
		this.minElevation = this.segment.getMinElevation();
		this.maxElevation = this.segment.getMaxElevation();
		this.elevationGain = this.segment.getElevationGain();
		this.elevationLoss = this.segment.getElevationLoss();
		
		this.trackTimeStart = this.segment.getStartTime();
		this.totalIdleTime = this.segment.getTotalTime() - this.segment.getMovingTime();

	}
	

	public void setSegmentIndex(long sid) {
		this.segmentIndex = sid;
	}

	public long getSegmentIndex() {
		return this.segmentIndex;
	}

	/**
	 * Add new segment for current track
	 */
	public void insertNewSegment(long trackId, int segmentIndex) {

		Segment segment = new Segment(trackId, segmentIndex);
		segment.setStartTime(this.trackTimeStart);

		try {

			this.segmentId = Segments.insert(app.getDatabase(), segment);
			
		} catch (SQLiteException e) {

			Toast.makeText(context, "SQLiteException: " + e.getMessage(), Toast.LENGTH_SHORT).show();
			AppLog.w(context, "SQLiteException: " + e.getMessage());

		}

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
	
	public void updateSegment(long segmentId) {


	}
	
}