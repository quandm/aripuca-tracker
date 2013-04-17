package com.aripuca.tracker.db;

import android.database.Cursor;

public class Track extends AbstractTrack {
	
	private String descr;
	
	private String title;
	
	private int activity;
	
	private int recording;
	
	public Track(Cursor cursor) {
		
		super(cursor);

		this.title = cursor.getString(cursor.getColumnIndex("title"));

		this.descr = cursor.getString(cursor.getColumnIndex("descr"));

		this.activity = cursor.getInt(cursor.getColumnIndex("activity"));

		this.recording = cursor.getInt(cursor.getColumnIndex("recording"));
		
	}
	
	public Track() {
		super();
	}

	public String getDescr() {
		return descr;
	}

	public void setDescr(String descr) {
		this.descr = descr;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public int getActivity() {
		return activity;
	}

	public void setActivity(int activity) {
		this.activity = activity;
	}

	public int getRecording() {
		return recording;
	}

	public void setRecording(int recording) {
		this.recording = recording;
	}

	
	
	
}
