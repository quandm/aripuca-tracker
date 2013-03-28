package com.aripuca.tracker.db;

import java.util.ArrayList;
import java.util.Iterator;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.aripuca.tracker.db.Waypoint;

public class Waypoints {

	public static final String TABLE_NAME = "waypoints";

	public static final String TABLE_CREATE = "CREATE TABLE " + TABLE_NAME +
			" (_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
			"track_id INTEGER," +
			"title TEXT NOT NULL, " +
			"descr TEXT, " +
			"lat INTEGER NOT NULL, " +
			"lng INTEGER NOT NULL, " +
			"accuracy REAL," +
			"elevation REAL," +
			"time INTEGER NOT NULL)";

	/**
	 * Create a list of famous waypoints and insert to db when application first
	 * installed
	 */
	public static void insertFamousWaypoints(SQLiteDatabase db) {

		// create array of waypoints
		ArrayList<Waypoint> famousWaypoints = new ArrayList<Waypoint>();

		famousWaypoints.add(new Waypoint("Aripuca", 50.12457, 8.6555));
		famousWaypoints.add(new Waypoint("Eiffel Tower", 48.8583, 2.2945));
		famousWaypoints.add(new Waypoint("Niagara Falls", 43.08, -79.071));
		famousWaypoints.add(new Waypoint("Golden Gate Bridge", 37.819722, -122.478611));
		famousWaypoints.add(new Waypoint("Stonehenge", 51.178844, -1.826189));
		famousWaypoints.add(new Waypoint("Mount Everest", 27.988056, 86.925278));
		famousWaypoints.add(new Waypoint("Colosseum", 41.890169, 12.492269));
		famousWaypoints.add(new Waypoint("Red Square", 55.754167, 37.62));
		famousWaypoints.add(new Waypoint("Charles Bridge", 50.086447, 14.411856));

		// insert waypoints to db
		Iterator<Waypoint> itr = famousWaypoints.iterator();
		while (itr.hasNext()) {

			Waypoint wp = itr.next();

			Waypoints.insert(db, wp);
		}

	}

	public static long insert(SQLiteDatabase db, Waypoint wp) {

		ContentValues values = new ContentValues();

		values.put("title", wp.getTitle());

		values.put("descr", wp.getDescr());

		values.put("lat", wp.getLatE6());

		values.put("lng", wp.getLngE6());

		if (!Float.isNaN(wp.getAccuracy())) {
			values.put("accuracy", wp.getAccuracy());
		}

		if (!Double.isNaN(wp.getElevation())) {
			values.put("elevation", wp.getElevation());
		}

		values.put("time", wp.getTime());

		return db.insertOrThrow(Waypoints.TABLE_NAME, null, values);

	}

	public static void update(SQLiteDatabase db, Waypoint wp) {
		
		ContentValues values = new ContentValues();

		values.put("title", wp.getTitle());
		values.put("descr", wp.getDescr());
		values.put("lat", wp.getLatE6());
		values.put("lng", wp.getLngE6());

		db.update("waypoints", values, "_id=" + wp.getId(), null);
		
	}
	
	
	/**
	 * Delete all waypoints
	 * 
	 * @param db
	 */
	public static void deleteAll(SQLiteDatabase db) {

		String sql = "DELETE FROM " + TABLE_NAME;
		db.execSQL(sql);

	}

	/**
	 * 
	 * 
	 * @param db
	 * @param waypointId
	 */
	public static void delete(SQLiteDatabase db, long waypointId) {

		String sql = "DELETE FROM " + TABLE_NAME + " WHERE _id=" + waypointId + ";";
		db.execSQL(sql);

	}

	/**
	 * Get waypoint from by id
	 * 
	 * @param db
	 * @param waypointId
	 * @return
	 */
	public static Waypoint get(SQLiteDatabase db, long waypointId) {

		String sql = "SELECT * FROM " + TABLE_NAME + " WHERE _id=" + waypointId + ";";
		Cursor cursor = db.rawQuery(sql, null);
		cursor.moveToFirst();

		Waypoint wp = new Waypoint(cursor);

		cursor.close();

		return wp;

	}

	public static void getAll(SQLiteDatabase db, ArrayList<Waypoint> waypoints) {

		String sql = "SELECT * FROM waypoints";

		Cursor cursor = db.rawQuery(sql, null);
		cursor.moveToFirst();
		
		while (cursor.isAfterLast() == false) {

			Waypoint wp = new Waypoint(cursor);

			waypoints.add(wp);

			cursor.moveToNext();
		}

		cursor.close();

	}

	/**
	 * 
	 * 
	 * @param db
	 * @param waypoints
	 * @param trackId
	 */
	public static void getAllTrackPoints(SQLiteDatabase db, ArrayList<Waypoint> waypoints, long trackId) {

		String sql = "SELECT * FROM track_points WHERE track_id=" + trackId + ";";
		
		Cursor cursor = db.rawQuery(sql, null);
		cursor.moveToFirst();
		
		int i = 1;
		
		while (cursor.isAfterLast() == false) {

			Waypoint wp = new Waypoint(cursor);

			// track_points table does not have "title" and "descr" fields
			// setting title as order number
			wp.setTitle(Integer.toString(i));
			
			waypoints.add(wp);

			cursor.moveToNext();
			i++;
		}

		cursor.close();

	}
	
}
