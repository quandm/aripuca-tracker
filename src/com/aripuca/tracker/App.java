package com.aripuca.tracker;

import com.aripuca.tracker.db.Segments;
import com.aripuca.tracker.db.TrackPoints;
import com.aripuca.tracker.db.Tracks;
import com.aripuca.tracker.db.Waypoints;
import com.aripuca.tracker.util.AppLog;
import com.aripuca.tracker.util.Utils;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.net.ConnectivityManager;

import android.os.Environment;
import android.preference.PreferenceManager;

import android.util.Log;

/**
 * 
 */
public class App extends Application {

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

	private Location currentLocation;

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
	 * @return the currentLocation
	 */
	public Location getCurrentLocation() {
		return currentLocation;
	}

	/**
	 * @param currentLocation
	 *            the currentLocation to set
	 */
	public void setCurrentLocation(Location currentLocation) {
		this.currentLocation = currentLocation;
	}

	/**
	 * application database create/open helper class
	 */
	public class OpenHelper extends SQLiteOpenHelper {

		/**
		 * 
		 */
		private static final String DATABASE_NAME = Constants.APP_NAME + ".db";

		/**
		 * Database version for
		 */
		private static final int DATABASE_VERSION = 1;

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

			db.execSQL(Waypoints.TABLE_CREATE);

			db.execSQL(Tracks.TABLE_CREATE);

			db.execSQL(TrackPoints.TABLE_CREATE);
			db.execSQL(Segments.TABLE_CREATE);
		}

		/**
		 * Upgrading application db
		 */
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

			// recreate database if version changes (DATABASE_VERSION)
			db.execSQL("DROP TABLE IF EXISTS " + Waypoints.TABLE_NAME);

			db.execSQL("DROP TABLE IF EXISTS " + Tracks.TABLE_NAME);
			db.execSQL("DROP TABLE IF EXISTS " + TrackPoints.TABLE_NAME);
			db.execSQL("DROP TABLE IF EXISTS " + Segments.TABLE_NAME);

			onCreate(db);
		}

	}

	@Override
	public void onCreate() {

		super.onCreate();

		// accessing preferences
		preferences = PreferenceManager.getDefaultSharedPreferences(this);

		// database helper
		OpenHelper openHelper = new OpenHelper(this);

		// SQLiteDatabase
		db = openHelper.getWritableDatabase();

		setExternalStorageState();

		// set application external storage folder
		appDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Constants.APP_NAME;

		// create all folders required by the application on external storage
		if (getExternalStorageAvailable() && getExternalStorageWriteable()) {
			createFolderStructure();
		} else {
			// Toast.makeText(this, R.string.memory_card_not_available,
			// Toast.LENGTH_SHORT).show();
		}

		// display density
		// density = getContext().getResources().getDisplayMetrics().density;

		// adding famous waypointsto db if not added yet
		
		// adding famous waypoints only once
		if (!getPreferences().contains("famous_waypoints")) {
			
			Waypoints.insertFamousWaypoints(db);
			
			// switch flag of famous locations added to true
			SharedPreferences.Editor editor = getPreferences().edit();
			editor.putInt("famous_waypoints", 1);
			editor.commit();
			
		}

		this.logd("=================== app: onCreate ===================");

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
			packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
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

	public void loge(String message) {
		AppLog.e(this, message);
	}

	public void logw(String message) {
		AppLog.w(this, message);
	}

	public void logi(String message) {
		AppLog.i(this, message);
	}

	public void logd(String message) {
		AppLog.d(this, message);
	}

	/**
	 * Create application folders
	 */
	private void createFolderStructure() {
		Utils.createFolder(getAppDir());
		Utils.createFolder(getAppDir() + "/" + Constants.PATH_TRACKS);
		Utils.createFolder(getAppDir() + "/" + Constants.PATH_WAYPOINTS);
		Utils.createFolder(getAppDir() + "/" + Constants.PATH_BACKUP);
		Utils.createFolder(getAppDir() + "/" + Constants.PATH_DEBUG);
		Utils.createFolder(getAppDir() + "/" + Constants.PATH_LOGS);
	}

}
