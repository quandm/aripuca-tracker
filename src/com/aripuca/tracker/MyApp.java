package com.aripuca.tracker;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * 
 */
public class MyApp extends Application {

	/**
	 * gps on/off flag
	 */
	private boolean gpsOn = false;

	public void setGpsOn(boolean flag) {
		this.gpsOn = flag;
	}

	public boolean isGpsOn() {
		return this.gpsOn;
	}

	/**
	 * 
	 */
	private boolean gpsStateBeforeRotation = true;

	public void setGpsStateBeforeRotation() {
		gpsStateBeforeRotation = this.gpsOn;
	}

	public boolean getGpsStateBeforeRotation() {
		return gpsStateBeforeRotation;
	}

	/**
	 * activity is being restarted flag
	 */
	private boolean activityRestarting = false;

	public boolean isActivityRestarting() {
		return activityRestarting;
	}

	public void setActivityRestarting(boolean activityRestarting) {
		this.activityRestarting = activityRestarting;
	}

	/**
	 * 
	 */
	private Location currentLocation = null;

	public void setCurrentLocation(Location cl) {
		currentLocation = cl;
	}

	public Location getCurrentLocation() {
		return currentLocation;
	}

	private SQLiteDatabase db;

	public SQLiteDatabase getDatabase() {
		return db;
	}

	public void setDatabase() {
		
		OpenHelper openHelper = new OpenHelper(this);
		
		db = openHelper.getWritableDatabase();
		
	}
	
	/**
	 * is external storage available, ex: SD card
	 */
	private boolean externalStorageAvailable = false;

	public boolean getExternalStorageAvailable() {
		return externalStorageAvailable;
	}

	/**
	 * is external storage writable
	 */
	private boolean externalStorageWriteable = false;

	public boolean getExternalStorageWriteable() {
		return externalStorageWriteable;
	}

	/**
	 * MainActivity object reference
	 */
	private static MainActivity mainActivity;

	public void setMainActivity(MainActivity ma) {
		mainActivity = ma;
	}

	public MainActivity getMainActivity() {
		return mainActivity;
	}

	/**
	 * WaypointsListActivity object reference
	 */
	private static WaypointsListActivity waypointsListActivity;

	public void setWaypointsListActivity(WaypointsListActivity wa) {
		waypointsListActivity = wa;
	}

	public WaypointsListActivity getWaypointsListActivity() {
		return waypointsListActivity;
	}

	/**
	 * CompassActivity object reference
	 */
	private static CompassActivity compassActivity;

	public void setCompassActivity(CompassActivity ca) {
		compassActivity = ca;
	}

	public CompassActivity getCompassActivity() {
		return compassActivity;
	}
	
	/**
	 * Android shared preferences
	 */
	private SharedPreferences preferences;

	public SharedPreferences getPreferences() {
		return preferences;
	}

	/**
	 * 
	 */
	private String appDir;

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
		private static final String DATABASE_NAME = "AripucaTracker.db";

		private static final int DATABASE_VERSION = 1;

		// private static final String NOTES_TABLE_NAME = "notes";

		/**
		 * tracks table name
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
		 * track points table title
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
		 * waypoints sql table name
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

			db.execSQL("DROP TABLE IF EXISTS " + WAYPOINTS_TABLE);
			db.execSQL("DROP TABLE IF EXISTS " + TRACKS_TABLE);
			db.execSQL("DROP TABLE IF EXISTS " + TRACKPOINTS_TABLE);
			db.execSQL("DROP TABLE IF EXISTS " + SEGMENTS_TABLE);
			
			onCreate(db);

		}

		// upgrading db example
		/*public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

			Log.w(Constants.TAG, "Upgrading database from version " + oldVersion + " to "
					+ newVersion);

			if (oldVersion < 1) {

				db.execSQL("DROP TABLE IF EXISTS " + WAYPOINTS_TABLE);
				db.execSQL("DROP TABLE IF EXISTS " + TRACKS_TABLE);
				db.execSQL("DROP TABLE IF EXISTS " + TRACKPOINTS_TABLE);
				db.execSQL("DROP TABLE IF EXISTS " + SEGMENTS_TABLE);
				onCreate(db);

			} else {

				// adding "distance" field to track points table
				if (oldVersion <= 1) {
					Log.i(Constants.TAG, "distance field added to track_points table");
					db.execSQL("ALTER TABLE " + TRACKPOINTS_TABLE + " ADD distance REAL");
				}
	
				// adding segment stats table
				if (oldVersion <= 1) {
					Log.i(Constants.TAG, "Segments table added");
					db.execSQL(SEGMENTS_TABLE_CREATE);
				}

			}

		} */ 

	}

	@Override
	public void onCreate() {

		// accessing preferences
		preferences = PreferenceManager.getDefaultSharedPreferences(this);

		//		waypointList = new ArrayList<Waypoint>();

		OpenHelper openHelper = new OpenHelper(this);

		// SQLiteDatabase
		db = openHelper.getWritableDatabase();
		
		setExternalStorageState();

		appDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/"
				+ getString(R.string.main_app_title_code);

		super.onCreate();

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

}
