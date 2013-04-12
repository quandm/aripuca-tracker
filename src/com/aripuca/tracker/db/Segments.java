package com.aripuca.tracker.db;

import java.util.ArrayList;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class Segments {

	public static final String TABLE_NAME = "segments";

	public static final String TABLE_CREATE =
			"CREATE TABLE " + TABLE_NAME +
					" (_id INTEGER PRIMARY KEY AUTOINCREMENT," +
					"track_id INTEGER NOT NULL," +
					"segment_index INTEGER," +
					"distance REAL," +
					"total_time INTEGER," +
					"moving_time INTEGER," +
					"max_speed REAL," +
					"max_elevation REAL," +
					"min_elevation REAL," +
					"elevation_gain REAL," +
					"elevation_loss REAL," +
					"start_time INTEGER NOT NULL," +
					"finish_time INTEGER)";

	public static ArrayList<Segment> getAll(SQLiteDatabase db, long trackId) {

		ArrayList<Segment> segments = new ArrayList<Segment>();

		String sql = "SELECT * FROM " + TABLE_NAME + " WHERE track_id = " + trackId;

		Cursor cursor = db.rawQuery(sql, null);
		cursor.moveToFirst();

		while (cursor.isAfterLast() == false) {

			Segment wp = new Segment(cursor);

			segments.add(wp);

			cursor.moveToNext();
		}

		cursor.close();

		return segments;
	}

	
	public static Segment get(SQLiteDatabase db, long trackId, long segmentId, int segmentIndex) {

		String sql = "SELECT segments.*, COUNT(track_points._id) AS count FROM segments, track_points WHERE"
				+ " segments._id=" + segmentId + " AND segments.track_id=" + trackId
				+ " AND track_points.segment_index=" + (segmentIndex - 1)
				+ " AND segments.track_id = track_points.track_id";

		Cursor cursor = db.rawQuery(sql, null);
		cursor.moveToFirst();

		Segment segment = new Segment(cursor);

		cursor.close();

		return segment;
	}
	
}
