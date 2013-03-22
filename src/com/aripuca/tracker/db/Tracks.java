package com.aripuca.tracker.db;

public class Tracks extends Table {

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
	
	
}
