package com.aripuca.tracker.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.aripuca.tracker.Constants;
import com.aripuca.tracker.utils.Utils;

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
	 * @param activity normal (0) or scheduled (1) track recording
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

	/**
	 * Insert new track record
	 * 
	 * @param db
	 * @param track
	 * @return new track id
	 */
	public static long insert(SQLiteDatabase db, Track track) {

		ContentValues values = new ContentValues();

		values.put("title", track.getTitle());
		values.put("activity", track.getActivity());
		values.put("recording", track.getRecording());
		values.put("start_time", track.getStartTime());
		values.put("finish_time", track.getFinishTime());

		// setting track id
		long trackId = db.insertOrThrow("tracks", null, values);

		return trackId;

	}

	/**
	 * 
	 * @param db
	 * @param track
	 */
	public static int update(SQLiteDatabase db, Track track) {

		ContentValues values = new ContentValues();

		values.put("title", track.getTitle());

		values.put("activity", track.getActivity());
		values.put("recording", track.getRecording());

		values.put("start_time", track.getStartTime());
		values.put("finish_time", track.getFinishTime());

		values.put("total_time", track.getTotalTime());
		values.put("moving_time", track.getMovingTime());

		values.put("max_speed", track.getMaxSpeed());

		values.put("max_elevation", track.getMaxElevation());
		values.put("min_elevation", track.getMinElevation());

		values.put("elevation_gain", track.getElevationGain());
		values.put("elevation_loss", track.getElevationLoss());

		values.put("max_speed", track.getMaxSpeed());

		values.put("distance", Utils.formatNumber(track.getDistance(), 2));

		// return number of affected rows
		return db.update("tracks", values, "_id=?", new String[] { String.valueOf(track.getId()) });

	}

	public static int update(SQLiteDatabase db, long trackId, String title, String descr) {

		ContentValues values = new ContentValues();

		values.put("title", title);
		values.put("descr", descr);

		// return number of affected rows
		return db.update("tracks", values, "_id=?", new String[] { String.valueOf(trackId) });

	}

	/**
	 * Get track by id
	 * 
	 * @param db
	 * @param waypointId
	 * @return
	 */
	public static Track get(SQLiteDatabase db, long trackId) {

		String sql = "SELECT tracks.*, COUNT(track_points._id) AS points_count " +
				"FROM tracks " +
				"LEFT JOIN track_points ON tracks._id = track_points.track_id " +
				"WHERE tracks._id=" + trackId;

		// String sql = "SELECT * FROM " + TABLE_NAME + " WHERE _id=" + trackId + ";";

		Cursor cursor = db.rawQuery(sql, null);
		cursor.moveToFirst();

		Track track = new Track(cursor);

		cursor.close();

		return track;

	}

	/**
	 * Get track being recorded
	 * 
	 * @param db
	 * @return
	 */
	public static Track getLastRecordingTrack(SQLiteDatabase db) {

		String sql = "SELECT tracks.* " +
				"FROM tracks " +
				"WHERE recording=1 ORDER BY tracks._id DESC LIMIT 1";

		Cursor cursor = db.rawQuery(sql, null);

		if (cursor.getCount() != 1) {
			return null;
		}

		cursor.moveToFirst();

		Track track = new Track(cursor);

		cursor.close();

		return track;

	}

}
