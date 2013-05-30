package com.aripuca.tracker.db;

import java.util.ArrayList;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.aripuca.tracker.utils.MapSpan;
import com.aripuca.tracker.utils.TrackStatsBundle;
import com.aripuca.tracker.utils.Utils;

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
	 * Generating track statistics from recorded points
	 * 
	 * @param db
	 * @param trackId
	 * @param segmentIndex
	 * @return
	 */
	public static TrackStatsBundle getStats(SQLiteDatabase db, long trackId, int segmentIndex) {

		String segmentIndexCondition = "";
		if (segmentIndex == -1) {
			segmentIndexCondition = " AND segment_index=" + segmentIndex;
		}

		String sql = "SELECT MAX(distance)-MIN(distance) AS distance, " +
				"MAX(elevation) AS max_elevation, " +
				"MIN(elevation) AS min_elevation, " +
				"MAX(time) - MIN(time) AS total_time, " +
				"MIN(time) AS start_time, MAX(time) AS finish_time" +
				"MAX(speed) AS max_speed FROM track_points " +
				"WHERE track_id=" + trackId + segmentIndexCondition;

		Cursor cursor = db.rawQuery(sql, null);
		cursor.moveToFirst();

		TrackStatsBundle tsb = new TrackStatsBundle();
		
		tsb.setDistance(cursor.getFloat(cursor.getColumnIndex("distance")));
		tsb.setMaxElevation(cursor.getDouble(cursor.getColumnIndex("max_elevation")));
		tsb.setMinElevation(cursor.getDouble(cursor.getColumnIndex("min_elevation")));
		tsb.setMaxSpeed(cursor.getFloat(cursor.getColumnIndex("max_speed")));
		tsb.setTotalTime(cursor.getLong(cursor.getColumnIndex("total_time")));
		tsb.setStartTime(cursor.getLong(cursor.getColumnIndex("start_time")));
		tsb.setFinishTime(cursor.getLong(cursor.getColumnIndex("finish_time")));

		cursor.close();

		return tsb;
	}

	//

	/**
	 * Get all track points for required track
	 * 
	 * @param db
	 * @param trackId
	 * @param points
	 */
	public static ArrayList<TrackPoint> getAll(SQLiteDatabase db, long trackId, MapSpan mapSpan) {

		ArrayList<TrackPoint> points = new ArrayList<TrackPoint>();

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

		return points;

	}

	/**
	 * Get all track points for required track
	 * 
	 * @param db
	 * @param trackId
	 * @param points
	 */
	public static TrackPoint getLast(SQLiteDatabase db, long trackId) {

		String sql = "SELECT * FROM track_points WHERE track_id=" + trackId + " ORDER BY _id DESC LIMIT 1;";
		Cursor cursor = db.rawQuery(sql, null);

		if (cursor.getCount() != 1) {
			return null;
		}

		cursor.moveToFirst();

		// track point object
		TrackPoint tp = new TrackPoint(cursor);

		cursor.close();

		return tp;

	}

	public static long insert(SQLiteDatabase db, TrackPoint trackPoint) {

		ContentValues values = new ContentValues();

		values.put("track_id", trackPoint.getTrackId());
		values.put("segment_index", trackPoint.getSegmentIndex());

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
