package com.aripuca.tracker.db;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

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
	
	
}
