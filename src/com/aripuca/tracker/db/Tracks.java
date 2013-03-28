package com.aripuca.tracker.db;

import com.aripuca.tracker.Constants;

import android.database.sqlite.SQLiteDatabase;

public class Tracks {

	public static final String TABLE_NAME = "tracks";

	public static final String TABLE_CREATE =
			"CREATE TABLE " + TABLE_NAME +
					" (_id INTEGER PRIMARY KEY AUTOINCREMENT," +
					"title TEXT NOT NULL," +
					"descr TEXT," +
					"activity INTEGER," +
					"distance REAL," +
					"total_time INTEGER," +
					"moving_time INTEGER," +
					"max_speed REAL," +
					"max_elevation REAL," +
					"min_elevation REAL," +
					"elevation_gain REAL," +
					"elevation_loss REAL," +
					"recording INTEGER NOT NULL," +
					"start_time INTEGER NOT NULL," +
					"finish_time INTEGER)";

	/**
	 * Delete all tracks and related data from db
	 * 
	 * @param db
	 * @param track_id
	 * @param activity
	 *            normal (0) or scheduled (1) track recording
	 */
	public static void deleteAll(SQLiteDatabase db, int activity) {

		String where;

		if (activity == Constants.ACTIVITY_TRACK) {
			where = " activity = " + activity + " OR activity IS NULL";
		} else {
			if (activity == Constants.ACTIVITY_SCHEDULED_TRACK) {
				where = " activity = " + activity;
			} else {
				return;
			}
		}

		// delete from segments table
		String sql = "DELETE FROM segments WHERE track_id IN " +
				"(SELECT _id FROM tracks WHERE " + where + ");";
		db.execSQL(sql);

		// delete from track_points table
		sql = "DELETE FROM track_points WHERE track_id IN " +
				"(SELECT _id FROM tracks WHERE " + where + ");";
		db.execSQL(sql);

		// clear track_id in waypoints table
		sql = "UPDATE waypoints SET track_id=NULL WHERE track_id IN "
				+ "(SELECT _id FROM tracks WHERE " + where + ");";
		db.execSQL(sql);

		// delete from tracks table
		sql = "DELETE FROM tracks WHERE " + where;
		db.execSQL(sql);

	}

	/**
	 * Delete one track
	 * 
	 * @param db
	 * @param trackId
	 */
	public static void delete(SQLiteDatabase db, long trackId) {

		// delete all track points first
		String sql = "DELETE FROM track_points WHERE track_id=" + trackId + ";";
		db.execSQL(sql);

		// delete track segments
		sql = "DELETE FROM segments WHERE track_id=" + trackId + ";";
		db.execSQL(sql);

		// clear track_id in waypoints table
		sql = "UPDATE waypoints SET track_id=NULL WHERE track_id = " + trackId + ";";
		db.execSQL(sql);

		// delete track
		sql = "DELETE FROM tracks WHERE _id=" + trackId + ";";
		db.execSQL(sql);

	}

}
