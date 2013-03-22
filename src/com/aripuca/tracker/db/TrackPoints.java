package com.aripuca.tracker.db;

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

}
