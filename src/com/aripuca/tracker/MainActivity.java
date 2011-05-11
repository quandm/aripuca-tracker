package com.aripuca.tracker;

import com.aripuca.tracker.util.ContainerCarousel;
import com.aripuca.tracker.view.CompassImage;
import com.aripuca.tracker.R;

import java.io.IOException;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.*;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;

import android.location.*;

import android.widget.*;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.View.OnClickListener;

/**
 * main application activity
 */
public class MainActivity extends Activity {

	private final String waypointsFile = "waypoints.txt";

	/**
	 * 
	 */
	private WakeLock wakeLock;

	/**
	 * Reference to Application object
	 */
	private MyApp myApp;

	private ContainerCarousel speedContainerCarousel = new ContainerCarousel() {
		@Override
		protected void initialize() {

			resourceId = R.id.speedOrPaceContainer;

			containers.add(R.layout.container_speed);
			containers.add(R.layout.container_pace);
			containers.add(R.layout.container_speed_pace);
		}
	};
	private ContainerCarousel timeContainerCarousel = new ContainerCarousel() {
		@Override
		protected void initialize() {

			resourceId = R.id.timeContainer;

			containers.add(R.layout.container_time_1);
			containers.add(R.layout.container_time_2);
			containers.add(R.layout.container_time_3);
		}
	};
	private ContainerCarousel distanceContainerCarousel = new ContainerCarousel() {
		@Override
		protected void initialize() {

			resourceId = R.id.distanceContainer;

			containers.add(R.layout.container_distance_1);
			containers.add(R.layout.container_distance_2);
		}
	};
	private ContainerCarousel elevationContainerCarousel = new ContainerCarousel() {
		@Override
		protected void initialize() {

			resourceId = R.id.elevationContainer;

			containers.add(R.layout.container_elevation_1);
			containers.add(R.layout.container_elevation_2);
			containers.add(R.layout.container_elevation_3);
		}
	};
	private ContainerCarousel coordinatesContainerCarousel = new ContainerCarousel() {
		@Override
		protected void initialize() {

			resourceId = R.id.coordinatesContainer;

			containers.add(R.layout.container_coordinates_1);
			containers.add(R.layout.container_coordinates_2);
		}
	};

	/**
	 * 
	 */
	private OnLongClickListener trackRecordingButtonLongClick = new OnLongClickListener() {

		public boolean onLongClick(View v) {

			// disable pause/resume button when tracking started or stopped
			((Button) findViewById(R.id.pauseResumeTrackButton)).setText(getString(R.string.pause));

			if (myApp.getTrack() != null) {
				stopTracking();
			} else {
				startTracking();
			}

			return true;
		}

	};

	private OnClickListener trackRecordingButtonClick = new OnClickListener() {
		public void onClick(View v) {
			if (myApp.getTrack() == null) {
				Toast.makeText(MainActivity.this, "Press and hold to start track recording", Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(MainActivity.this, "Press and hold to stop track recording", Toast.LENGTH_SHORT).show();
			}
		}

	};

	/**
	 * 
	 */
	private OnClickListener pauseResumeTrackListener = new OnClickListener() {
		public void onClick(View v) {

			if (myApp.getTrack().isTrackingPaused()) {
				resumeTracking();
			} else {
				pauseTracking();
			}

		}
	};

	/**
	 * Adding hidden data to application preferences
	 */
	private void initializeHiddenPreferences() {

		SharedPreferences.Editor editor = myApp.getPreferences().edit();

		if (!myApp.getPreferences().contains("speed_container_id")) {
			editor.putInt("speed_container_id", 0);
			editor.commit();
		}

		if (!myApp.getPreferences().contains("time_container_id")) {
			editor.putInt("time_container_id", 0);
			editor.commit();
		}

		if (!myApp.getPreferences().contains("distance_container_id")) {
			editor.putInt("distance_container_id", 0);
			editor.commit();
		}

		if (!myApp.getPreferences().contains("elevation_container_id")) {
			editor.putInt("elevation_container_id", 0);
			editor.commit();
		}

		if (!myApp.getPreferences().contains("coordinates_container_id")) {
			editor.putInt("coordinates_container_id", 0);
			editor.commit();
		}

		speedContainerCarousel.setCurrentContainerId(myApp.getPreferences().getInt("speed_container_id", 0));
		timeContainerCarousel.setCurrentContainerId(myApp.getPreferences().getInt("time_container_id", 0));
		distanceContainerCarousel.setCurrentContainerId(myApp.getPreferences().getInt("distance_container_id", 0));
		elevationContainerCarousel.setCurrentContainerId(myApp.getPreferences().getInt("elavation_container_id", 0));
		coordinatesContainerCarousel
				.setCurrentContainerId(myApp.getPreferences().getInt("coordinates_container_id", 0));

	}

	private void saveHiddenPreferences() {

		// update preferences
		SharedPreferences.Editor editor = myApp.getPreferences().edit();

		editor.putInt("speed_container_id", speedContainerCarousel.getCurrentContainerId());
		editor.putInt("time_container_id", timeContainerCarousel.getCurrentContainerId());
		editor.putInt("distance_container_id", distanceContainerCarousel.getCurrentContainerId());
		editor.putInt("elevation_container_id", elevationContainerCarousel.getCurrentContainerId());
		editor.putInt("coordinates_container_id", coordinatesContainerCarousel.getCurrentContainerId());

		editor.commit();

	}

	/**
	 * Initialize the activity
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		// reference to application object
		myApp = ((MyApp) getApplicationContext());
		myApp.setMainActivity(this);

		initializeHiddenPreferences();

		// preparing UI -------------------------------------
		// setting layout for main activity
		setContentView(R.layout.main);

		// ---------------------------------------------------

		// restoring previous application state
		if (savedInstanceState != null) {
			restoreInstanceState(savedInstanceState);
		}

		// attaching default middle layout
		if (myApp.getTrack() == null) {
			this.replaceDynamicView(R.layout.main_idle);
		} else {
			this.replaceDynamicView(R.layout.main_tracking);
		}

		// once activity is started set restarting flag to false
		// it will be processed in onRetainNonConfigurationInstance
		myApp.setActivityRestarting(false);

		((Button) findViewById(R.id.addWaypointButton)).setEnabled(false);
		((Button) findViewById(R.id.trackRecordingButton)).setEnabled(false);
		((Button) findViewById(R.id.pauseResumeTrackButton)).setEnabled(false);

		// start gps only if stopped
		// if gps was stopped before screen rotation - do not start
		if (!myApp.isGpsOn() && myApp.getGpsStateBeforeRotation()) {

			Log.v(Constants.TAG, "DEBUG: START GPS");

			startGPSService();

		} else {

			// gps service can already be running if activity is recreated due
			// to orientation change

			// if track recording mode is ON
			if (myApp.getTrack() != null) {

				((Button) findViewById(R.id.trackRecordingButton)).setText(getString(R.string.stop));

				// enabling pause/resume button
				((Button) findViewById(R.id.addWaypointButton)).setEnabled(true);
				((Button) findViewById(R.id.trackRecordingButton)).setEnabled(true);
				((Button) findViewById(R.id.pauseResumeTrackButton)).setEnabled(true);

			} else {

			}

		}

		initializeControlButtons();

		// create all folders required by the application on external storage
		createFolderStructure();

		// TODO: ???
		//		createStorageFiles();

	}

	@Override
	public Object onRetainNonConfigurationInstance() {

		// setting a flag that activity is restarting and we will not
		// stop gps service in onDestroy when in track recording mode
		myApp.setActivityRestarting(true);

		// save gps state before rotation
		// if gps stopped, let's not start it after rotation
		myApp.setGpsStateBeforeRotation();

		return null;

	}

	/**
	 * onResume event handler
	 */
	@Override
	protected void onResume() {

		Log.v(Constants.TAG, "onResume");

		// preventing phone from sleeping
		if (findViewById(R.id.dynamicView) != null) {
			findViewById(R.id.dynamicView).setKeepScreenOn(myApp.getPreferences().getBoolean("wake_lock", true));
		}

		super.onResume();
	}

	/**
	 * onPause event handler
	 */
	@Override
	protected void onPause() {

		Log.v(Constants.TAG, "onPause");

		super.onPause();
	}

	/**
	 * onDestroy event handler
	 */
	@Override
	protected void onDestroy() {

		Log.v(Constants.TAG, "DEBUG: onDestroy");

		if (!myApp.isActivityRestarting()) {

			// stop gps service if application is going to be destroyed
			if (myApp.isGpsOn()) {
				stopGPSService();
			}

			this.saveHiddenPreferences();
		}

		myApp.setMainActivity(null);

		super.onDestroy();
	}

	/**
	 * Restoring data saved in onSaveInstanceState
	 */
	private void restoreInstanceState(Bundle savedInstanceState) {

		speedContainerCarousel.setCurrentContainerId(savedInstanceState.getInt("speedContainerId"));
		timeContainerCarousel.setCurrentContainerId(savedInstanceState.getInt("timeContainerId"));
		distanceContainerCarousel.setCurrentContainerId(savedInstanceState.getInt("distanceContainerId"));
		elevationContainerCarousel.setCurrentContainerId(savedInstanceState.getInt("elevationContainerId"));
		coordinatesContainerCarousel.setCurrentContainerId(savedInstanceState.getInt("coordinatesContainerId"));

		if (findViewById(R.id.pauseResumeTrackButton) != null) {
			((Button) findViewById(R.id.pauseResumeTrackButton)).setText(savedInstanceState
					.getString("pauseButtonText"));
		}

		if (findViewById(R.id.pauseResumeTrackButton) != null) {
			((Button) findViewById(R.id.pauseResumeTrackButton)).setEnabled(savedInstanceState
					.getBoolean("pauseButtonState"));
		}

	}

	/**
	 * onSaveInstanceState event handler
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState) {

		super.onSaveInstanceState(outState);

		outState.putInt("speedContainerId", speedContainerCarousel.getCurrentContainerId());
		outState.putInt("timeContainerId", timeContainerCarousel.getCurrentContainerId());
		outState.putInt("distanceContainerId", distanceContainerCarousel.getCurrentContainerId());
		outState.putInt("elevationContainerId", elevationContainerCarousel.getCurrentContainerId());
		outState.putInt("coordinatesContainerId", coordinatesContainerCarousel.getCurrentContainerId());
		
		outState.putInt("coordinatesContainerId", coordinatesContainerCarousel.getCurrentContainerId());
		
		

		outState.putString("pauseButtonText", ((Button) findViewById(R.id.pauseResumeTrackButton)).getText().toString());
		outState.putBoolean("pauseButtonState", ((Button) findViewById(R.id.pauseResumeTrackButton)).isEnabled());

	}

	/**
	 * 
	 */
	private void initializeControlButtons() {

		// setting up listeners for application buttons
		// ((Button)
		// findViewById(R.id.gpsServiceButton)).setOnClickListener(gpsServiceButtonClick);
		// ((Button)
		// findViewById(R.id.gpsServiceButton)).setOnLongClickListener(gpsServiceButtonLongClick);

		((Button) findViewById(R.id.addWaypointButton)).setOnClickListener(addWaypointListener);

		((Button) findViewById(R.id.trackRecordingButton)).setOnClickListener(trackRecordingButtonClick);
		((Button) findViewById(R.id.trackRecordingButton)).setOnLongClickListener(trackRecordingButtonLongClick);

		((Button) findViewById(R.id.pauseResumeTrackButton)).setOnClickListener(pauseResumeTrackListener);

	}

	protected void setContainer(ContainerCarousel carousel) {

		// assigning click event listener to speed or pace container
		if (findViewById(carousel.getResourceId()) != null) {
			ViewGroup containerView = (ViewGroup) findViewById(carousel.getResourceId());

			final int resourceId = carousel.getResourceId();
			final ContainerCarousel car = carousel;

			containerView.setOnClickListener(
					new OnClickListener() {
						public void onClick(View v) {
							ViewGroup containerView = (ViewGroup) findViewById(resourceId);

							// attaching default layout
							View tmpView1 = getLayoutInflater().inflate(car.getNextContainer(), containerView, false);
							containerView.removeAllViews();
							containerView.addView(tmpView1, 0);
						}
					}
					);

			// attaching default layout
			View tmpView1 = getLayoutInflater().inflate(carousel.getCurrentContainer(), containerView, false);
			containerView.removeAllViews();
			containerView.addView(tmpView1, 0);
		}

	}

	/**
	 * @param resourceId
	 */
	private void replaceDynamicView(int resourceId) {

		ViewGroup dynamicView = (ViewGroup) findViewById(R.id.dynamicView);

		// attaching default layout
		View tmpView = getLayoutInflater().inflate(resourceId, dynamicView, false);
		dynamicView.removeAllViews();
		dynamicView.addView(tmpView, 0);

		if (resourceId == R.layout.main_idle) {
			// show/hide compass
			if (myApp.getPreferences().getBoolean("show_compass", true)) {
				showCompass();
			} else {
				hideCompass();
			}
		} else {

			setContainer(speedContainerCarousel);
			setContainer(timeContainerCarousel);
			setContainer(distanceContainerCarousel);
			setContainer(elevationContainerCarousel);
			setContainer(coordinatesContainerCarousel);

		}

	}

	/**
	 * start GPS listener service
	 */
	private void startGPSService() {

		// starting GPS listener service
		Intent i = new Intent(this, GpsService.class);
		startService(i);

		// ((Button)
		// findViewById(R.id.gpsServiceButton)).setText(getString(R.string.gps_on));

		myApp.setGpsOn(true);

	}

	/**
	 * stop GPS listener service
	 */
	private void stopGPSService() {

		((Button) findViewById(R.id.addWaypointButton)).setEnabled(false);
		((Button) findViewById(R.id.trackRecordingButton)).setEnabled(false);
		((Button) findViewById(R.id.pauseResumeTrackButton)).setEnabled(false);

		// ((Button)
		// findViewById(R.id.gpsServiceButton)).setText(getString(R.string.gps_off));

		// stop tracking if active
		if (myApp.getTrack() != null) {
			stopTracking();
		}

		Intent i = new Intent(this, GpsService.class);
		stopService(i);

		myApp.setGpsOn(false);

	}

	/**
	 * 
	 */
	private void startTracking() {

		// Change button label from Record to Stop
		((Button) findViewById(R.id.trackRecordingButton)).setText(getString(R.string.stop));

		// enabling pause/resume button
		((Button) findViewById(R.id.pauseResumeTrackButton)).setEnabled(true);

		this.replaceDynamicView(R.layout.main_tracking);

		// new track recording started
		myApp.startTrackRecording();

		Toast.makeText(this, R.string.recording_started, Toast.LENGTH_SHORT).show();

	}

	/**
	 * 
	 */
	private void stopTracking() {

		// disabling pause/resume button
		((Button) findViewById(R.id.pauseResumeTrackButton)).setEnabled(false);
		((Button) findViewById(R.id.pauseResumeTrackButton)).setText(getString(R.string.pause));

		// Change button label from Stop to Record
		((Button) findViewById(R.id.trackRecordingButton)).setText(getString(R.string.record));

		myApp.stopTrackRecording();

		// switching to initial layout
		this.replaceDynamicView(R.layout.main_idle);

		Toast.makeText(this, R.string.recording_finished, Toast.LENGTH_SHORT).show();

	}

	/**
	 * Resume track recording
	 */
	private void resumeTracking() {

		((Button) findViewById(R.id.pauseResumeTrackButton)).setText(getString(R.string.pause));

		myApp.getTrack().resume();

		Toast.makeText(this, R.string.recording_resumed, Toast.LENGTH_SHORT).show();

	}

	/**
	 * Pause track recording
	 */
	private void pauseTracking() {

		((Button) findViewById(R.id.pauseResumeTrackButton)).setText(getString(R.string.resume));

		myApp.getTrack().pause();

		Toast.makeText(this, R.string.recording_paused, Toast.LENGTH_SHORT).show();

	}

	/**
	 * add waypoint button listener
	 */
	private OnClickListener addWaypointListener = new OnClickListener() {

		public void onClick(View v) {

			/*
			 * if (myApp.getCurrentLocation() != null) { if
			 * (myApp.getCurrentLocation().getAccuracy() != 0 &&
			 * myApp.getCurrentLocation().getAccuracy() >
			 * Integer.parseInt(myApp.getPreferences().getString(
			 * "min_accuracy", "10"))) { Toast.makeText(MainActivity.this,
			 * "Please wait for a better fix", Toast.LENGTH_SHORT).show(); } }
			 */

			Context mContext = v.getContext();

			LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(LAYOUT_INFLATER_SERVICE);
			View layout = inflater.inflate(R.layout.add_waypoint_dialog,
					(ViewGroup) findViewById(R.id.add_waypoint_dialog_layout_root));

			AlertDialog.Builder builder = new AlertDialog.Builder(mContext);

			builder.setTitle(R.string.add_waypoint);
			builder.setView(layout);

			// creating references to input fields in order to use them in
			// onClick handler
			final EditText wpTitle = (EditText) layout.findViewById(R.id.waypointTitleInputText);
			wpTitle.setText(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format((new Date()).getTime()));

			final EditText wpDescr = (EditText) layout.findViewById(R.id.waypointDescriptionInputText);

			final EditText wpLat = (EditText) layout.findViewById(R.id.waypointLatInputText);
			wpLat.setText(Location.convert(myApp.getCurrentLocation().getLatitude(), 0));

			final EditText wpLng = (EditText) layout.findViewById(R.id.waypointLngInputText);
			wpLng.setText(Location.convert(myApp.getCurrentLocation().getLongitude(), 0));

			builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

				public void onClick(DialogInterface dialog, int id) {

					// waypoint title from input dialog
					String titleStr = wpTitle.getText().toString().trim();
					String descrStr = wpDescr.getText().toString().trim();

					if (titleStr.equals("")) {
						Toast.makeText(MainActivity.this, R.string.waypoint_title_required, Toast.LENGTH_SHORT).show();
						dialog.dismiss();
					}

					String latStr = wpLat.getText().toString().trim();
					String lngStr = wpLng.getText().toString().trim();

					ContentValues values = new ContentValues();
					values.put("title", titleStr);
					values.put("descr", descrStr);
					values.put("lat", latStr);
					values.put("lng", lngStr);
					values.put("elevation", myApp.getCurrentLocation().getAltitude());
					values.put("time", myApp.getCurrentLocation().getTime());

					// if track recording started save track_id as
					if (myApp.getTrack() != null) {
						values.put("track_id", myApp.getTrack().getTrackId());
					}

					try {
						myApp.getDatabase().insertOrThrow("waypoints", null, values);
					} catch (SQLiteException e) {
						Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
						Log.w(Constants.TAG, "SQLiteException: " + e.getMessage(), e);
					}

					Toast.makeText(MainActivity.this, R.string.waypoint_saved, Toast.LENGTH_SHORT).show();

					dialog.dismiss();

				}
			});

			builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					// dialog.dismiss();
				}
			});

			AlertDialog dialog = builder.create();
			dialog.show();

		}

	};

	/**
	 * Show compass image
	 */
	public void showCompass() {

		if (findViewById(R.id.compassImage) != null) {
			((CompassImage) findViewById(R.id.compassImage)).setVisibility(View.VISIBLE);
		}

	}

	/**
	 * Hide compass image
	 */
	public void hideCompass() {

		if (findViewById(R.id.compassImage) != null) {
			((CompassImage) findViewById(R.id.compassImage)).setVisibility(View.GONE);
		}

	}

	/**
	 * Set external storage for data import/export
	 */
	private void createStorageFiles() {

		if (!myApp.externalStorageAvailable) {
			Toast.makeText(MainActivity.this, R.string.memory_card_not_found, Toast.LENGTH_SHORT).show();
			return;
		}

		if (!myApp.externalStorageWriteable) {
			Toast.makeText(MainActivity.this, R.string.memory_card_not_writable, Toast.LENGTH_SHORT).show();
			return;
		}

		File file = this.createStorageFile(this.waypointsFile);
		myApp.setWaypointsFile(file);

		// myApp.setTracksFile(this.createStorageFile(this.tracksFile));

	}

	/**
	 * Create file on external storage
	 * 
	 * @param fileName Name of external storage file
	 * @return File
	 */
	private File createStorageFile(String fileName) {

		File file = new File(myApp.getAppDir(), fileName);

		// creating storage files if needed
		try {

			if (!file.exists()) {
				file.createNewFile();
			}

			return file;

		} catch (IOException e) {
			Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
			return null;
		}

	}

	/**
	 * Create application folders
	 */
	private void createFolderStructure() {
		createFolder(myApp.getAppDir());
		createFolder(myApp.getAppDir() + "/tracks");
		createFolder(myApp.getAppDir() + "/waypoints");
	}

	/**
	 * Create folder if not exists
	 * 
	 * @param folderName
	 */
	private void createFolder(String folderName) {

		File folder = new File(myApp.getAppDir() + "/" + folderName);

		// create output folder
		if (!folder.exists()) {
			folder.mkdir();
		}

	}

	/**
	 * onCreateOptionsMenu handler
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		return true;
	}

	/**
	 * Make all changes to the menu before it loads
	 */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {

		MenuItem mi = menu.findItem(R.id.gpsOnOff);

		if (myApp.isGpsOn()) {
			mi.setTitle(R.string.stop_gps);
		} else {
			mi.setTitle(R.string.start_gps);
		}

		return true;
	}

	/**
	 * Process main activity menu
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		// Handle item selection
		switch (item.getItemId()) {

			case R.id.waypointsMenuItem:

				startActivity(new Intent(this, WaypointsListActivity.class));

				return true;

			case R.id.tracksMenuItem:

				startActivity(new Intent(this, TracksListActivity.class));

				return true;

			case R.id.aboutMenuItem:

				this.showAboutDialog();

				return true;

			case R.id.settingsMenuItem:

				startActivity(new Intent(this, SettingsActivity.class));

				return true;

			case R.id.gpsOnOff:

				if (myApp.isGpsOn()) {

					// if in track recording mode do not stop GPS, display warning message instead 
					if (myApp.getTrack() == null) {
						stopGPSService();
					} else {
						Toast.makeText(MainActivity.this, R.string.stop_track_recording, Toast.LENGTH_SHORT).show();
					}

				} else {
					startGPSService();
				}

				return true;

			default:

				return super.onOptionsItemSelected(item);

		}

	}

	private void showAboutDialog() {

		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		builder.setMessage(getString(R.string.main_app_title) + " " + MyApp.getVersionName(this) + "\n"
						+ getString(R.string.app_url))
				.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.dismiss();
					}
				})
				.setInverseBackgroundForced(true)
				.setTitle(getString(R.string.about))
				.setCancelable(true);

		AlertDialog alert = builder.create();

		alert.show();

	}

	/**
	 * Update main activity view
	 */
	public void updateMainActivity() {

		((Button) findViewById(R.id.addWaypointButton)).setEnabled(true);
		((Button) findViewById(R.id.trackRecordingButton)).setEnabled(true);

		// measuring units
		String speedUnit = myApp.getPreferences().getString("speed_units", "kph");
		String distanceUnit = myApp.getPreferences().getString("distance_units", "km");
		String elevationUnit = myApp.getPreferences().getString("elevation_units", "m");
		int coordUnit = Integer.parseInt(myApp.getPreferences().getString("coord_units", "0"));

		if (findViewById(R.id.lat) != null) {
			((TextView) findViewById(R.id.lat)).setText(Utils.formatLat(myApp.getCurrentLocation().getLatitude(),
					coordUnit));
		}

		if (findViewById(R.id.lng) != null) {
			((TextView) findViewById(R.id.lng)).setText(Utils.formatLng(myApp.getCurrentLocation().getLongitude(),
					coordUnit));
		}

		if (myApp.getCurrentLocation().hasAccuracy()) {
			if (findViewById(R.id.accuracy) != null) {
				((TextView) findViewById(R.id.accuracy)).setText(Utils.formatDistance(myApp.getCurrentLocation()
						.getAccuracy(), distanceUnit));
			}
		}

		if (findViewById(R.id.lastFix) != null) {
			String lastFix = (new SimpleDateFormat("H:mm:ss")).format(myApp.getCurrentLocation().getTime());
			((TextView) findViewById(R.id.lastFix)).setText(lastFix);
		}

		if (myApp.getCurrentLocation().hasAltitude()) {
			if (findViewById(R.id.elevation) != null) {
				((TextView) findViewById(R.id.elevation)).setText(Utils.formatElevation((float) myApp
						.getCurrentLocation().getAltitude(), elevationUnit));
			}
		}

		// current speed and pace
		float speed = 0;
		if (myApp.getCurrentLocation().hasSpeed()) {

			// speed is in meters per second
			speed = myApp.getCurrentLocation().getSpeed();

			// current speed (cycling, driving)
			if (findViewById(R.id.speed) != null) {
				((TextView) findViewById(R.id.speed)).setText(Utils.formatSpeed(speed, speedUnit));
			}

			// current pace (running, hiking, walking)
			if (findViewById(R.id.pace) != null) {
				((TextView) findViewById(R.id.pace)).setText(Utils.formatPace(speed, speedUnit));
			}

		}

		// updating track recording info
		if (myApp.getTrack() != null) {

			// elevation gain
			if (findViewById(R.id.elevationGain) != null) {
				((TextView) findViewById(R.id.elevationGain)).setText(Utils.formatElevation(myApp.getTrack()
						.getElevationGain(), elevationUnit));
			}

			// elevation loss
			if (findViewById(R.id.elevationLoss) != null) {
				((TextView) findViewById(R.id.elevationLoss)).setText(Utils.formatElevation(myApp.getTrack()
						.getElevationLoss(), elevationUnit));
			}

			// average speed
			if (findViewById(R.id.averageSpeed) != null) {
				((TextView) findViewById(R.id.averageSpeed)).setText(Utils.formatSpeed(myApp.getTrack()
						.getAverageSpeed(), speedUnit));
			}

			// average moving speed
			if (findViewById(R.id.averageMovingSpeed) != null) {
				((TextView) findViewById(R.id.averageMovingSpeed)).setText(Utils.formatSpeed(myApp.getTrack()
						.getAverageMovingSpeed(), speedUnit));
			}

			// max speed
			if (findViewById(R.id.maxSpeed) != null) {
				((TextView) findViewById(R.id.maxSpeed)).setText(Utils.formatSpeed(myApp.getTrack().getMaxSpeed(),
						speedUnit));
			}

			// average pace
			if (findViewById(R.id.averagePace) != null) {
				((TextView) findViewById(R.id.averagePace)).setText(Utils.formatPace(
						myApp.getTrack().getAverageSpeed(),
						speedUnit));
			}

			// average moving pace
			if (findViewById(R.id.averageMovingPace) != null) {
				((TextView) findViewById(R.id.averageMovingPace)).setText(Utils.formatPace(myApp.getTrack()
						.getAverageMovingSpeed(), speedUnit));
			}

			// max pace
			if (findViewById(R.id.maxPace) != null) {
				((TextView) findViewById(R.id.maxPace)).setText(Utils.formatPace(myApp.getTrack().getMaxSpeed(),
						speedUnit));
			}

			// total distance
			if (findViewById(R.id.distance) != null) {
				((TextView) findViewById(R.id.distance)).setText(Utils.formatDistance(myApp.getTrack().getDistance(),
						distanceUnit));
			}

		}

	}

	private int getOrientationAdjustment() {

		switch (this.getResources().getConfiguration().orientation) {
			case Configuration.ORIENTATION_PORTRAIT:
				return 0;
			case Configuration.ORIENTATION_LANDSCAPE:
				return 90;
		}

		return 0;
	}

	/**
	 * Update compass image and azimuth text
	 */
	public void updateCompass(float[] values) {

		float azimuth = values[0];

		if (findViewById(R.id.azimuth) != null) {
			((TextView) findViewById(R.id.azimuth)).setText(Utils.formatNumber(azimuth, 0) + Utils.degreeChar + " "
					+ Utils.getDirectionCode(azimuth));
		}

		// updating compass image
		if (findViewById(R.id.compassImage) != null) {

			CompassImage compassImage = (CompassImage) findViewById(R.id.compassImage);

			if (compassImage.getVisibility() == View.VISIBLE) {

				// Bitmap arrowBitmap =
				// BitmapFactory.decodeResource(getResources(),
				// R.drawable.windrose);
				// BitmapDrawable bmd = new BitmapDrawable(arrowBitmap);
				compassImage.setAngle(360 - azimuth - getOrientationAdjustment());
				// compassImage.setAlpha(230);
				compassImage.invalidate();
				// compassImage.setImageDrawable(bmd);
			}
		}

	}

	// --------------------------------------------------------------------------------------------
	// UPDATE TIME IN TRACK RECORDING MODE
	// --------------------------------------------------------------------------------------------

	/**
	 * Update total and moving time in track recording mode
	 */
	public void updateTime() {

		if (myApp.getTrack() != null) {

			if (findViewById(R.id.totalTime) != null) {
				((TextView) findViewById(R.id.totalTime)).setText(Utils.formatInterval(
						myApp.getTrack().getTotalTime(), false));
			}

			if (findViewById(R.id.movingTime) != null) {
				((TextView) findViewById(R.id.movingTime)).setText(Utils.formatInterval(
						myApp.getTrack().getMovingTime(), false));
			}

		}

	}

	// ---------------------------------------------------------------------------
	// HANDLING BACK BUTTON CLICKS
	// ---------------------------------------------------------------------------
	/**
	 * Back button click count
	 */
	private int backClickCount = 0;

	/**
	 * Intercepting Back button click to prevent accidental exit in track
	 * recording mode
	 */
	@Override
	public void onBackPressed() {
		backClickCount++;
		if (myApp.getTrack() != null && backClickCount < 2) {
			Toast.makeText(MainActivity.this, R.string.click_again_to_exit, Toast.LENGTH_SHORT).show();
			// click count is cleared after 3 seconds
			clearClickCountHandler.postDelayed(clearClickCountTask, 3000);
		} else {
			this.finish();
		}
	}

	/**
	 * Clear click count handler
	 */
	private Handler clearClickCountHandler = new Handler();
	private Runnable clearClickCountTask = new Runnable() {
		public void run() {
			backClickCount = 0;
		}
	};

	// --------------------------------------------------------------------------------------------

	// -------------------------------------------------------------------------
	// WAKE LOCK
	// -------------------------------------------------------------------------

	/**
	 * 
	 */
	public void aquireWakeLock() {
		wakeLock = ((PowerManager) getSystemService(Context.POWER_SERVICE))
				.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK
						| PowerManager.ON_AFTER_RELEASE, Constants.TAG);
		wakeLock.acquire();
	}

	/**
	 * 
	 */
	public void releaseWakeLock() {
		wakeLock.release();
	}

}
