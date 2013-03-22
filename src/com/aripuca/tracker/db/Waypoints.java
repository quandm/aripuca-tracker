package com.aripuca.tracker.db;

import java.util.ArrayList;
import java.util.Iterator;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import com.aripuca.tracker.track.Waypoint;

public class Waypoints extends Table {

	{
		TABLE_NAME = "waypoints";
	}

	public static final String TABLE_CREATE =
			"CREATE TABLE " + TABLE_NAME +
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
	 * Create a list of famous waypoints and insert to db when application first installed
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

	public static void insert(SQLiteDatabase db, Waypoint wp) {

		ContentValues values = new ContentValues();

		values.put("title", wp.getTitle());
		values.put("lat", (int) (wp.getLatitude() * 1E6));
		values.put("lng", (int) (wp.getLongitude() * 1E6));
		values.put("time", wp.getTime());

		db.insert(Waypoints.TABLE_NAME, null, values);

	}

	/**
	 * 
	 * 
	 * @param db
	 * @param waypointId
	 */
	public static void delete(SQLiteDatabase db, long waypointId) {

		String sql = "DELETE FROM waypoints WHERE _id=" + waypointId + ";";
		db.execSQL(sql);

	}

	public static Waypoint get(SQLiteDatabase db, long waypointId) {
		
		
		
		return null;
		
	}
	

}
