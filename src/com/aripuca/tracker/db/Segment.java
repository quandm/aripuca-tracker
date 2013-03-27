package com.aripuca.tracker.db;

import android.database.Cursor;

public class Segment extends AbstractTrack {
	
	private long trackId;

	private int segmentIndex;
	
	public Segment(Cursor cursor) {
		
		super(cursor);

		this.trackId = cursor.getLong(cursor.getColumnIndex("track_id"));

		this.segmentIndex = cursor.getInt(cursor.getColumnIndex("segment_index"));
		
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
