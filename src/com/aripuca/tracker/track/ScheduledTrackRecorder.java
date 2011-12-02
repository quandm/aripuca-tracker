package com.aripuca.tracker.track;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.aripuca.tracker.MyApp;
import com.aripuca.tracker.app.Constants;
import com.aripuca.tracker.util.Utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteException;
import android.location.Location;
import android.util.Log;
import android.widget.Toast;

public class ScheduledTrackRecorder {
	
	private Context context;
	
	protected MyApp myApp;	
	
	private int trackPointsCount;
	
	protected long trackTimeStart;
	
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
	

	public ScheduledTrackRecorder(Context context) {
	
		this.context = context;

		myApp = (MyApp)context.getApplicationContext();
		
	}
	
	
	public void startScheduler() {

		trackPointsCount = 0;
		
		this.trackTimeStart = (new Date()).getTime();

		
	}

	public void stopScheduler() {

		
	}
	
	/**
	 * Add new track to application db after recording started
	 */
	public void insertNewTrack() {

		ContentValues values = new ContentValues();
		values.put("title", "New scheduled track");
		values.put("activity", 1);
		values.put("recording", 1);
		values.put("start_time", this.trackTimeStart);

		try {
			
			long newTrackId = myApp.getDatabase().insertOrThrow("tracks", null, values);
			this.setTrackId(newTrackId);
			
		} catch (SQLiteException e) {
			
			Toast.makeText(context, "SQLiteException: " + e.getMessage(), Toast.LENGTH_SHORT).show();
			
			Log.w(Constants.TAG, "SQLiteException: " + e.getMessage(), e);
		}

	}
	

	/**
	 * Update track data after recording finished
	 */
	protected void updateNewTrack() {

		long finishTime = (new Date()).getTime();

		String trackTitle = (new SimpleDateFormat("yyyy-MM-dd H:mm")).format(this.trackTimeStart) + "-" +
								(new SimpleDateFormat("H:mm")).format(finishTime);

		ContentValues values = new ContentValues();
		values.put("title", trackTitle);
		values.put("finish_time", finishTime);
		// for scheduled tracks distance is always 0
		values.put("distance", 0);
		values.put("recording", 0);

		try {
			
			myApp.getDatabase().update("tracks", values, "_id=?", new String[] { String.valueOf(this.getTrackId()) });
			
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
	protected void recordTrackPoint(Location location) {

		ContentValues values = new ContentValues();
		values.put("track_id", this.getTrackId());
		values.put("lat", (int)(location.getLatitude()*1E6));
		values.put("lng", (int)(location.getLongitude()*1E6));
		values.put("elevation", Utils.formatNumber(location.getAltitude(), 1));
		values.put("speed", Utils.formatNumber(location.getSpeed(), 2));
		values.put("time", (new Date()).getTime());
		values.put("accuracy", location.getAccuracy());

		try {

			myApp.getDatabase().insertOrThrow("track_points", null, values);

			this.trackPointsCount++;

		} catch (SQLiteException e) {
			Log.e(Constants.TAG, "SQLiteException: " + e.getMessage(), e);
		}

	}
	
	
	
	

	
}
