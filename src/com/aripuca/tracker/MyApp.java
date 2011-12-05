package com.aripuca.tracker;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;

import com.aripuca.tracker.R;
import com.aripuca.tracker.app.Constants;
import com.aripuca.tracker.track.TrackRecorder;
import com.aripuca.tracker.track.Waypoint;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.ConnectivityManager;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

/**
 * 
 */
public class MyApp extends Application {

	private Locale locale = null;

	/**
	 * Android shared preferences
	 */
	private SharedPreferences preferences;
	/**
	 * application directory
	 */
	private String appDir;

	/**
	 * is external storage writable
	 */
	private boolean externalStorageWriteable = false;

	/**
	 * is external storage available, ex: SD card
	 */
	private boolean externalStorageAvailable = false;

	/**
	 * database object
	 */
	private SQLiteDatabase db;
	
	public TrackRecorder trackRecorder;

	/**
	 * location updates broadcast receiver
	 */
	protected BroadcastReceiver languageUpdateBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

			Log.d(Constants.TAG, "languageUpdateBroadcastReceiver");
			updateLocale();

			Toast.makeText(context, getString(R.string.restart_required), Toast.LENGTH_LONG).show();

		}
	};

	public SQLiteDatabase getDatabase() {
		return db;
	}

	/**
	 * 
	 */
	public void setDatabase() {

		OpenHelper openHelper = new OpenHelper(this);

		db = openHelper.getWritableDatabase();

	}

	public boolean getExternalStorageAvailable() {
		return externalStorageAvailable;
	}

	public boolean getExternalStorageWriteable() {
		return externalStorageWriteable;
	}

	public SharedPreferences getPreferences() {
		return preferences;
	}

	public String getAppDir() {
		return appDir;
	}

	/**
	 * application database create/open helper class
	 */
	public class OpenHelper extends SQLiteOpenHelper {

		/**
		 * 
		 */
		private static final String DATABASE_NAME = Constants.APP_NAME + ".db";

		private static final int DATABASE_VERSION = 1;

		// private static final String NOTES_TABLE_NAME = "notes";

		/**
		 * tracks table
		 */
		private static final String TRACKS_TABLE = "tracks";

		/**
		 * tracks table create sql
		 */
		private static final String TRACKS_TABLE_CREATE =
				"CREATE TABLE " + TRACKS_TABLE +
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

		private static final String SEGMENTS_TABLE = "segments";

		private static final String SEGMENTS_TABLE_CREATE =
				"CREATE TABLE " + SEGMENTS_TABLE +
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

		/**
		 * track points table
		 */
		private static final String TRACKPOINTS_TABLE = "track_points";
		/**
		 * track points table create sql
		 */
		private static final String TRACKPOINTS_TABLE_CREATE =
				"CREATE TABLE " + TRACKPOINTS_TABLE +
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
		 * waypoints sql table
		 */
		private static final String WAYPOINTS_TABLE = "waypoints";

		/**
		 * waypointss table create sql
		 */
		private static final String WAYPOINTS_TABLE_CREATE =
				"CREATE TABLE " + WAYPOINTS_TABLE +
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
		 * OpenHelper constructor
		 */
		OpenHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		/**
		 * Creating application db
		 */
		@Override
		public void onCreate(SQLiteDatabase db) {

			db.execSQL(WAYPOINTS_TABLE_CREATE);
			db.execSQL(TRACKS_TABLE_CREATE);
			db.execSQL(TRACKPOINTS_TABLE_CREATE);
			db.execSQL(SEGMENTS_TABLE_CREATE);
		}

		/**
		 * Upgrading application db
		 */
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

			// recreate database if version changes (DATABASE_VERSION)

			db.execSQL("DROP TABLE IF EXISTS " + WAYPOINTS_TABLE);
			db.execSQL("DROP TABLE IF EXISTS " + TRACKS_TABLE);
			db.execSQL("DROP TABLE IF EXISTS " + TRACKPOINTS_TABLE);
			db.execSQL("DROP TABLE IF EXISTS " + SEGMENTS_TABLE);

			onCreate(db);

		}

		// upgrading db example
		/*
		 * public void onUpgrade(SQLiteDatabase db, int oldVersion, int
		 * newVersion) {
		 * 
		 * Log.w(Constants.TAG, "Upgrading database from version "
		 * + oldVersion + " to " + newVersion);
		 * 
		 * if (oldVersion < 1) {
		 * 
		 * db.execSQL("DROP TABLE IF EXISTS " + WAYPOINTS_TABLE);
		 * db.execSQL("DROP TABLE IF EXISTS " + TRACKS_TABLE);
		 * db.execSQL("DROP TABLE IF EXISTS " + TRACKPOINTS_TABLE);
		 * db.execSQL("DROP TABLE IF EXISTS " + SEGMENTS_TABLE);
		 * 
		 * onCreate(db);
		 * }
		 * else {
		 * if (oldVersion <= 1) {
		 * Log.i(Constants.TAG,
		 * "distance field added to track_points table");
		 * 
		 * // adding "distance" field to track points table
		 * db.execSQL("ALTER TABLE " + TRACKPOINTS_TABLE +
		 * " ADD distance REAL");
		 * }
		 * 
		 * if (oldVersion <= 1) {
		 * 
		 * Log.i(Constants.TAG, "Segments table added");
		 * 
		 * // adding segment stats table
		 * db.execSQL(SEGMENTS_TABLE_CREATE);
		 * 
		 * }
		 * }
		 * }
		 */
	}

	@Override
	public void onCreate() {

		super.onCreate();

		// accessing preferences
		preferences = PreferenceManager.getDefaultSharedPreferences(this);

		updateLocale();

		// registering receiver for language updates
		registerReceiver(languageUpdateBroadcastReceiver, new IntentFilter(
				"com.aripuca.tracker.LANGUAGE_UPDATES_ACTION"));

		// database helper
		OpenHelper openHelper = new OpenHelper(this);

		// SQLiteDatabase
		db = openHelper.getWritableDatabase();

		setExternalStorageState();

		// set application external storage folder
		appDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/"
				+ Constants.APP_NAME;

		// create all folders required by the application on external storage
		if (getExternalStorageAvailable() && getExternalStorageWriteable()) {
			createFolderStructure();
		} else {
			//Toast.makeText(this, R.string.memory_card_not_available, Toast.LENGTH_SHORT).show();
		}

		// adding famous waypoints to db if not added yet
		insertFamousWaypoints();

	}

	/**
	 * 
	 */
	protected void updateLocale() {

		// ----------------------------------------------------------------------
		// setting locale
		Configuration config = getBaseContext().getResources().getConfiguration();
		String lang = preferences.getString("language", "en-US");

		if (!config.locale.getLanguage().equals(lang)) {

			locale = new Locale(lang);
			Locale.setDefault(locale);
			config.locale = locale;
			getBaseContext().getResources().updateConfiguration(config,
					getBaseContext().getResources().getDisplayMetrics());

		}

	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {

		if (locale != null) {

			newConfig.locale = locale;
			Locale.setDefault(locale);
			getBaseContext().getResources().updateConfiguration(newConfig,
					getBaseContext().getResources().getDisplayMetrics());
		}

		super.onConfigurationChanged(newConfig);

	}

	/**
	 * Checking if external storage is available and writable
	 */
	private void setExternalStorageState() {

		// checking access to SD card
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state)) {
			// We can read and write the media
			externalStorageAvailable = externalStorageWriteable = true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			// We can only read the media
			externalStorageAvailable = true;
			externalStorageWriteable = false;
		} else {
			// Something else is wrong. It may be one of many other states, but
			// all we need to know is we can neither read nor write
			externalStorageAvailable = externalStorageWriteable = false;
		}

	}

	/**
	 * Get application version name
	 * 
	 * @param context
	 */
	public static String getVersionName(Context context) {

		PackageInfo packageInfo;
		try {
			packageInfo = context.getPackageManager().getPackageInfo(
					context.getPackageName(), 0);
			return packageInfo.versionName;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
			return "";
		}
	}

	/**
	 * Checking if Internet connection exists
	 */
	public boolean checkInternetConnection() {

		ConnectivityManager conMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

		if (conMgr.getActiveNetworkInfo() != null && conMgr.getActiveNetworkInfo().isAvailable()
				&& conMgr.getActiveNetworkInfo().isConnected()) {
			return true;
		} else {
			Log.v(Constants.TAG, "Internet Connection Not Present");
			return false;
		}

	}

	/**
	 * Append application debug log file
	 * 
	 * @param text
	 */
	public void log(String message) {

		String fileName = (new SimpleDateFormat("yyyy-MM-dd")).format(new Date()) + ".log";

		StringBuilder sb = new StringBuilder();
		sb.append((new SimpleDateFormat("yyyy-MM-dd HH-mm-ss")).format(new Date()));
		sb.append(" | ");
		sb.append(message);

		File logFile = new File(this.getAppDir() + "/debug/" + fileName);

		if (!logFile.exists()) {
			try {
				logFile.createNewFile();
			} catch (IOException e) {
				Toast.makeText(this, "IOException: " + e.getMessage(), Toast.LENGTH_SHORT).show();
			}
		}

		try {
			BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
			buf.append(sb.toString());
			buf.newLine();
			buf.close();
		} catch (IOException e) {
			Toast.makeText(this, "IOException: " + e.getMessage(), Toast.LENGTH_SHORT).show();
		}

	}

	/**
	 * Create application folders
	 */
	private void createFolderStructure() {
		createFolder(getAppDir());
		createFolder(getAppDir() + "/tracks");
		createFolder(getAppDir() + "/waypoints");
		createFolder(getAppDir() + "/backup");
		createFolder(getAppDir() + "/debug");
	}

	/**
	 * Create folder if not exists
	 * 
	 * @param folderName
	 */
	private void createFolder(String folderName) {

		File folder = new File(folderName);

		// create output folder
		if (!folder.exists()) {
			folder.mkdir();
		}

	}

	/**
	 * Create a list of famous waypoints and insert to db when application first
	 * installed
	 */
	private void insertFamousWaypoints() {

		// adding famous waypoints only once
		if (getPreferences().contains("famous_waypoints")) {
			return;
		}

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

			ContentValues values = new ContentValues();
			values.put("title", wp.getTitle());
			values.put("lat", (int) (wp.getLatitude() * 1E6));
			values.put("lng", (int) (wp.getLongitude() * 1E6));
			values.put("time", wp.getTime());

			getDatabase().insert("waypoints", null, values);

		}

		// switch flag of famous locations added to true
		SharedPreferences.Editor editor = getPreferences().edit();
		editor.putInt("famous_waypoints", 1);
		editor.commit();

	}

}
