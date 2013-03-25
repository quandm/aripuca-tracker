package com.aripuca.tracker.db;

import android.database.sqlite.SQLiteDatabase;

public class Table {

	public static String TABLE_NAME;
	
	public static String TABLE_CREATE;
	
	/**
	 * Delete all waypoints
	 * 
	 * @param db
	 */
	public static void deleteAll(SQLiteDatabase db) {

		String sql = "DELETE FROM " + TABLE_NAME;
		db.execSQL(sql);

	}
	
	
	
	
}
