package com.aripuca.tracker.db;

import android.database.Cursor;

public class Segment extends AbstractTrack {
	
	private long trackId;

	private int segmentIndex = 0;
	
	/**
	 * Create track segment from database
	 * @param cursor
	 */
	public Segment(Cursor cursor) {
		
		super(cursor);

		this.trackId = cursor.getLong(cursor.getColumnIndex("track_id"));

		this.segmentIndex = cursor.getInt(cursor.getColumnIndex("segment_index"));
		
	}

	/**
	 * Create empty track segment 
	 */
	public Segment(long trackId, int segmentIndex) {

		super();

		this.trackId = trackId;

		this.segmentIndex = segmentIndex;
		
	}

	public long getTrackId() {
		return trackId;
	}


	public void setTrackId(long trackId) {
		this.trackId = trackId;
	}


	public int getSegmentIndex() {
		return segmentIndex;
	}


	public void setSegmentIndex(int segmentIndex) {
		this.segmentIndex = segmentIndex;
	}


}
