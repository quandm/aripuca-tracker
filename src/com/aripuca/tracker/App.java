package com.aripuca.tracker;

import java.lang.Thread.UncaughtExceptionHandler;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.ConnectivityManager;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.aripuca.tracker.db.Segments;
import com.aripuca.tracker.db.TrackPoints;
import com.aripuca.tracker.db.Tracks;
import com.aripuca.tracker.db.Waypoints;
import com.aripuca.tracker.utils.AppLog;
import com.aripuca.tracker.utils.Utils;

/**
 * 
 */
public class App extends Application {

	// uncaught exception handler variable
	private UncaughtExceptionHandler defaultUncaughtExceptionHandler;

	// handler listener
	private Thread.UncaughtExceptionHandler uncaughtExceptionHandler =
			new Thread.UncaughtExceptionHandler() {
				@Override
				public void uncaughtException(Thread thread, Throwable ex) {

					AppLog.e(getApplicationContext(), "uncought exception");

					// re-throw critical exception further to the os (important)
					defaultUncaughtExceptionHandler.uncaughtException(thread, ex);
				}
			};

	/**
	 * Android shared preferences
	 */
	private SharedPreferences preferences;

	/**
	 * application directory
	 */
	private String appDir;

	private String dataDir;

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

	public String getDataDir() {
		return dataDir;
	}

	/**
	 * application database create/open helper class
	 */
	public class OpenHelper extends SQLiteOpenHelper {

		/**
		 * Database version for
		 */
		private static final int DATABASE_VERSION = 1;

		OpenHelper(Context context) {
			super(context, Constants.DATABASE_FILE, null, DATABASE_VERSION);
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

		// setup handler for uncaught exception
		this.defaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
		Thread.setDefaultUncaughtExceptionHandler(uncaughtExceptionHandler);

		// accessing preferences
		preferences = PreferenceManager.getDefaultSharedPreferences(this);

		// set application external storage folder
		appDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Constants.APP_NAME;

		// database helper
		OpenHelper openHelper = new OpenHelper(this);

		// SQLiteDatabase
		try {
			db = openHelper.getWritableDatabase();
		} catch (SQLiteException e) {
			Toast.makeText(this, R.string.memory_card_not_available,
					Toast.LENGTH_SHORT).show();
			android.os.Process.killProcess(android.os.Process.myPid());
		}

		setExternalStorageState();

		dataDir = Environment.getDataDirectory().getAbsolutePath() + "/com.aripuca.tracker/databases";
		// TODO: NEW FEATURE: app database file in external memory
		// dataDir = appDir + "/" + Constants.PATH_DATABASE;

		// create all folders required by the application on external storage
		if (getExternalStorageAvailable() && getExternalStorageWriteable()) {
			createFolderStructure();
		} else {
			Toast.makeText(this, R.string.memory_card_not_available,
					Toast.LENGTH_SHORT).show();
		}

		// display density
		// density = getContext().getResources().getDisplayMetrics().density;

		// adding famous waypoints only once
		if (!getPreferences().contains("famous_waypoints")) {

			Waypoints.insertFamousWaypoints(db);

			// switch flag of famous locations added to true
			SharedPreferences.Editor editor = getPreferences().edit();
			editor.putInt("famous_waypoints", 1);
			editor.commit();

		}

		// AppLog.d(this, "=================== app: onCreate ===================");

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

	/**
	 * Create application folders
	 */
	private void createFolderStructure() {
		Utils.createFolder(getAppDir());
		Utils.createFolder(getAppDir() + "/" + Constants.PATH_DATABASE);
		Utils.createFolder(getAppDir() + "/" + Constants.PATH_TRACKS);
		Utils.createFolder(getAppDir() + "/" + Constants.PATH_WAYPOINTS);
		Utils.createFolder(getAppDir() + "/" + Constants.PATH_BACKUP);
		Utils.createFolder(getAppDir() + "/" + Constants.PATH_DEBUG);
		Utils.createFolder(getAppDir() + "/" + Constants.PATH_LOGS);
	}

}
