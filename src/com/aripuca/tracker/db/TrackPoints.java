package com.aripuca.tracker.db;

import java.util.ArrayList;
import java.util.Date;

import com.aripuca.tracker.Constants;
import com.aripuca.tracker.utils.MapSpan;
import com.aripuca.tracker.utils.Utils;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.location.Location;
import android.util.Log;
import android.widget.Toast;

public class TrackPoints {

	public static final String TABLE_NAME = "track_points";

	public static final String TABLE_CREATE =
			"CREATE TABLE " + TABLE_NAME +
					" (_id INTEGER PRIMARY KEY AUTOINCREMENT," +
					"track_id INTEGER NOT NULL," +
					"segment_index INTEGER," +
					"distance REAL," +
					"lat INTEGER NOT NULL," +
					"lng INTEGER NOT NULL," +
					"accuracy REAL," +
					"elevation REAL," +
					"speed REAL," +
					"time INTEGER NOT NULL)";

	/**
	 * Get total number of track points in track
	 * 
	 * @param db
	 * @param trackId
	 * @return
	 */
	public static int getCount(SQLiteDatabase db, long trackId) {

		String sql = "SELECT COUNT(*) AS count FROM track_points WHERE track_id=" + trackId + ";";
		Cursor cursor = db.rawQuery(sql, null);
		cursor.moveToFirst();

		int count = cursor.getInt(cursor.getColumnIndex("count"));

		cursor.close();

		return count;
	}

	/**
	 * Get all track points for required track
	 * 
	 * @param db
	 * @param trackId
	 * @param points
	 */
	public static void getAll(SQLiteDatabase db, long trackId, ArrayList<TrackPoint> points, MapSpan mapSpan) {

		String sql = "SELECT * FROM track_points WHERE track_id=" + trackId + ";";
		Cursor cursor = db.rawQuery(sql, null);
		cursor.moveToFirst();

		while (cursor.isAfterLast() == false) {

			// track point object
			TrackPoint tp = new TrackPoint(cursor);

			// update map span
			mapSpan.updateMapSpan(tp.getLatE6(), tp.getLngE6());

			points.add(tp);

			cursor.moveToNext();
		}

		cursor.close();

	}

	
	public static long insert(SQLiteDatabase db, TrackPoint trackPoint) {
		
		ContentValues values = new ContentValues();
		values.put("track_id", trackPoint.getTrackId());
		values.put("lat", trackPoint.getLatE6());
		values.put("lng", trackPoint.getLngE6());
		values.put("elevation", Utils.formatNumber(trackPoint.getElevation(), 1));
		values.put("speed", Utils.formatNumber(trackPoint.getSpeed(), 2));

		values.put("time", trackPoint.getTime());
		
		values.put("distance", Utils.formatNumber(trackPoint.getDistance(), 1));
		values.put("accuracy", trackPoint.getAccuracy());

		long trackPointId = db.insertOrThrow("track_points", null, values);
		
		return trackPointId;
		
	}

}
