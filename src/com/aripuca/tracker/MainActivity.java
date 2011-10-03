package com.aripuca.tracker;

import com.aripuca.tracker.R.id;
import com.aripuca.tracker.R.layout;
import com.aripuca.tracker.R.menu;
import com.aripuca.tracker.R.string;
import com.aripuca.tracker.app.Constants;
import com.aripuca.tracker.service.GpsService;
import com.aripuca.tracker.track.TrackRecorder;
import com.aripuca.tracker.track.Waypoint;
import com.aripuca.tracker.util.ContainerCarousel;
import com.aripuca.tracker.util.SunriseSunset;
import com.aripuca.tracker.util.Utils;
import com.aripuca.tracker.view.CompassImage;
import com.aripuca.tracker.R;

import java.io.File;
import java.io.IOException;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.io.*;
import java.nio.channels.FileChannel;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.location.*;

import android.widget.*;

import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.View.OnClickListener;

/**
 * main application activity
 */
public class MainActivity extends Activity {

	/**
	 * 
	 */
	// private WakeLock wakeLock;

	/**
	 * Reference to Application object
	 */
	private MyApp myApp;

	private TrackRecorder trackRecorder;

	private String importDatabaseFileName;

	private Handler mainHandler = new Handler();

	/**
	 * good fix received flag
	 */
	private boolean fixReceived = false;

	private static final int HELLO_ID = 1;

	/**
	 * location updates broadcast receiver
	 */
	protected BroadcastReceiver locationBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			// Log.d(Constants.TAG,
			// "MainActivity: LOCATION BROADCAST MESSAGE RECEIVED");

			Bundle bundle = intent.getExtras();

			updateMainActivity(bundle.getInt("location_provider"));
		}
	};
	/**
	 * compass updates broadcast receiver
	 */
	protected BroadcastReceiver compassBroadcastReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			// Log.d(Constants.TAG,
			// "MainActivity: COMPASS BROADCAST MESSAGE RECEIVED");
			Bundle bundle = intent.getExtras();
			updateCompass(bundle.getFloat("azimuth"));
		}
	};

	private ContainerCarousel speedContainerCarousel = new ContainerCarousel() {
		@Override
		protected void initialize() {

			resourceId = R.id.speedOrPaceContainer;

			containers.add(R.layout.container_speed);
			containers.add(R.layout.container_pace);
			containers.add(R.layout.container_speed_pace);
			containers.add(R.layout.container_speed_acceleration);
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
			containers.add(R.layout.container_distance_3);
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

		@Override
		public boolean onLongClick(View v) {

			// disable pause/resume button when tracking started or stopped
			((Button) findViewById(R.id.pauseResumeTrackButton)).setText(getString(R.string.pause));

			if (TrackRecorder.getInstance(myApp).isRecording()) {
				stopTracking();
			} else {
				startTracking();
			}

			return true;
		}

	};

	private OnClickListener trackRecordingButtonClick = new OnClickListener() {
		@Override
		public void onClick(View v) {

			TrackRecorder trackRecorder = TrackRecorder.getInstance(myApp);

			if (trackRecorder.isRecording()) {
				Toast.makeText(MainActivity.this, R.string.press_and_hold_to_stop, Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(MainActivity.this, R.string.press_and_hold_to_start, Toast.LENGTH_SHORT).show();
			}

		}

	};

	/**
	 * 
	 */
	private OnClickListener pauseResumeTrackListener = new OnClickListener() {

		@Override
		public void onClick(View v) {

			if (trackRecorder.isRecordingPaused()) {

				((Button) findViewById(R.id.pauseResumeTrackButton)).setText(getString(R.string.pause));

				trackRecorder.resume();

				Toast.makeText(MainActivity.this, R.string.recording_resumed, Toast.LENGTH_SHORT).show();

			} else {

				((Button) findViewById(R.id.pauseResumeTrackButton)).setText(getString(R.string.resume));

				trackRecorder.pause();

				Toast.makeText(MainActivity.this, R.string.recording_paused, Toast.LENGTH_SHORT).show();

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

		Log.v(Constants.TAG, "onCreate");

		// reference to application object
		myApp = ((MyApp) getApplicationContext());
		myApp.setMainActivity(this);

		initializeHiddenPreferences();

		// ----------------------------------------------------------------------
		// preparing UI
		// setting layout for main activity
		setContentView(R.layout.main);

		// restoring previous application state
		if (savedInstanceState != null) {
			restoreInstanceState(savedInstanceState);
		}

		// get instance of TrackRecorder class for fast access from MainActivity
		trackRecorder = TrackRecorder.getInstance(myApp);

		// attaching default middle layout
		if (trackRecorder.isRecording()) {
			this.replaceDynamicView(R.layout.main_tracking);
		} else {
			this.replaceDynamicView(R.layout.main_idle);
		}

		// once activity is started set restarting flag to false
		// it will be processed in onRetainNonConfigurationInstance
		myApp.setActivityRestarting(false);

		// disable control buttons 
		((Button) findViewById(R.id.addWaypointButton)).setEnabled(false);
		((Button) findViewById(R.id.trackRecordingButton)).setEnabled(false);
		((Button) findViewById(R.id.pauseResumeTrackButton)).setEnabled(false);

		// start gps only if stopped
		// if gps was stopped before screen rotation - do not start
		if (!myApp.isGpsOn() && myApp.getGpsStateBeforeRotation()) {

			startGPSService();

		} else {

			// gps service can already be running if activity is recreated due
			// to orientation change

			// if track recording mode is ON
			if (trackRecorder.isRecording()) {

				((Button) findViewById(R.id.trackRecordingButton)).setText(getString(R.string.stop));

				// enabling control buttons
				((Button) findViewById(R.id.addWaypointButton)).setEnabled(true);
				((Button) findViewById(R.id.trackRecordingButton)).setEnabled(true);
				((Button) findViewById(R.id.pauseResumeTrackButton)).setEnabled(true);

			} else {

			}

		}

		setControlButtonListeners();

		// create all folders required by the application on external storage
		if (myApp.getExternalStorageAvailable() && myApp.getExternalStorageWriteable()) {
			createFolderStructure();
		} else {
			Toast.makeText(MainActivity.this, R.string.memory_card_not_available, Toast.LENGTH_SHORT).show();
		}

		// adding famous waypoints
		processFamousWaypoints();

	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {

		super.onConfigurationChanged(newConfig);

	}

	@Override
	public Object onRetainNonConfigurationInstance() {

		Log.d(Constants.TAG, "onRetainNonConfigurationInstance");

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

		// registering receiver for compass updates
		registerReceiver(compassBroadcastReceiver, new IntentFilter("com.aripuca.tracker.COMPASS_UPDATES_ACTION"));

		// registering receiver for location updates
		registerReceiver(locationBroadcastReceiver, new IntentFilter("com.aripuca.tracker.LOCATION_UPDATES_ACTION"));

		super.onResume();
	}

	/**
	 * onPause event handler
	 */
	@Override
	protected void onPause() {

		Log.v(Constants.TAG, "onPause");

		unregisterReceiver(compassBroadcastReceiver);
		unregisterReceiver(locationBroadcastReceiver);

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

		outState.putString("pauseButtonText", ((Button) findViewById(R.id.pauseResumeTrackButton)).getText().toString());
		outState.putBoolean("pauseButtonState", ((Button) findViewById(R.id.pauseResumeTrackButton)).isEnabled());

	}

	/**
	 * Setting up listeners for application buttons
	 */
	private void setControlButtonListeners() {

		((Button) findViewById(R.id.addWaypointButton)).setOnClickListener(addWaypointListener);

		((Button) findViewById(R.id.trackRecordingButton)).setOnClickListener(trackRecordingButtonClick);
		((Button) findViewById(R.id.trackRecordingButton)).setOnLongClickListener(trackRecordingButtonLongClick);

		((Button) findViewById(R.id.pauseResumeTrackButton)).setOnClickListener(pauseResumeTrackListener);

	}

	private void setContainer(ContainerCarousel carousel) {

		// assigning click event listener to speed or pace container
		if (findViewById(carousel.getResourceId()) != null) {
			ViewGroup containerView = (ViewGroup) findViewById(carousel.getResourceId());

			final int resourceId = carousel.getResourceId();
			final ContainerCarousel car = carousel;

			containerView.setOnClickListener(
					new OnClickListener() {
						@Override
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
		startService(new Intent(this, GpsService.class));

		myApp.setGpsOn(true);
	}

	/**
	 * stop GPS listener service
	 */
	private void stopGPSService() {

		fixReceived = false;

		((Button) findViewById(R.id.addWaypointButton)).setEnabled(false);
		((Button) findViewById(R.id.trackRecordingButton)).setEnabled(false);
		((Button) findViewById(R.id.pauseResumeTrackButton)).setEnabled(false);

		// ((Button)
		// findViewById(R.id.gpsServiceButton)).setText(getString(R.string.gps_off));

		// stop tracking if active
		if (trackRecorder.isRecording()) {
			stopTracking();
		}

		stopService(new Intent(this, GpsService.class));

		myApp.setGpsOn(false);

	}

	/**
	 * 
	 */
	private void startTracking() {

		//TODO: add notification icon in track recording mode

		// --------------------------------------------------------------------------------
		/*
		 * String ns = Context.NOTIFICATION_SERVICE;
		 * NotificationManager mNotificationManager = (NotificationManager)
		 * getSystemService(ns);
		 * 
		 * int icon = R.drawable.arrow36;
		 * long when = System.currentTimeMillis();
		 * 
		 * Notification notification = new Notification(icon,
		 * getString(R.string.recording_started), when);
		 * 
		 * CharSequence contentTitle = "Aripuca Tracker";
		 * CharSequence contentText = "Recording track";
		 * 
		 * Intent notificationIntent = new Intent(this, MainActivity.class);
		 * PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
		 * notificationIntent, 0);
		 * 
		 * notification.setLatestEventInfo(myApp, contentTitle, contentText,
		 * contentIntent);
		 * 
		 * mNotificationManager.notify(HELLO_ID, notification);
		 */

		// --------------------------------------------------------------------------------

		// Change button label from Record to Stop
		((Button) findViewById(R.id.trackRecordingButton)).setText(getString(R.string.stop));

		// enabling pause/resume button
		((Button) findViewById(R.id.pauseResumeTrackButton)).setEnabled(true);

		this.replaceDynamicView(R.layout.main_tracking);

		// new track recording started
		// myApp.startTrackRecording();

		trackRecorder.start();

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

		TrackRecorder.getInstance(myApp).stop();

		// switching to initial layout
		this.replaceDynamicView(R.layout.main_idle);

		Toast.makeText(this, R.string.recording_finished, Toast.LENGTH_SHORT).show();

	}

	/**
	 * add waypoint button listener
	 */
	private OnClickListener addWaypointListener = new OnClickListener() {

		@Override
		public void onClick(View v) {

			/*
			 * if (myApp.getCurrentLocation() != null) { if
			 * (myApp.getCurrentLocation().getAccuracy() != 0 &&
			 * myApp.getCurrentLocation().getAccuracy() >
			 * Integer.parseInt(myApp.getPreferences().getString(
			 * "min_accuracy", "10"))) { Toast.makeText(MainActivity.this,
			 * "Please wait for a better fix", Toast.LENGTH_SHORT).show(); } }
			 */

			// let's make reverse geocoder request and then show Add Waypoint
			// dialog
			// address will be inserted as default description for this waypoint
			// processing is done in separate thread

			if (myApp.getPreferences().getBoolean("waypoint_default_description", true)) {
				geocodeLocation(myApp.getCurrentLocation(), MainActivity.this, new GeocoderHandler());
			} else {
				showAddWaypointDialog(null);
			}

		}

	};

	/**
	 * Geocoder handler class. Receives a message from geocoder thread and
	 * displays "Add Waypoint" dialog even if geocoding request failed
	 */
	private class GeocoderHandler extends Handler {

		/**
		 * Processing message from geocoder thread
		 */
		@Override
		public void handleMessage(Message message) {

			String addressStr;
			switch (message.what) {
				case 1:
					Bundle bundle = message.getData();
					addressStr = bundle.getString("address");
				break;
				default:
					addressStr = null;
			}

			showAddWaypointDialog(addressStr);

		}
	};

	/**
	 * Running geocoder request as a separate thread. The thread will send a
	 * message to provided Handler object in order to update UI
	 * 
	 * @param location
	 * @param context
	 * @param handler
	 */
	private void geocodeLocation(final Location location, final Context context, final Handler handler) {

		Thread thread = new Thread() {

			@Override
			public void run() {

				String addressStr = "";

				try {
					Geocoder myLocation = new Geocoder(context, Locale.getDefault());

					List<Address> addressList = myLocation.getFromLocation(location.getLatitude(),
							location.getLongitude(), 1);

					if (addressList != null && addressList.size() > 0) {

						Address address = addressList.get(0);

						int linesCount = address.getMaxAddressLineIndex();
						for (int i = 0; i < linesCount; i++) {
							addressStr += address.getAddressLine(i);
							if (i != linesCount - 1) {
								addressStr += ", ";
							}
						}
					}

				}
				catch (IOException e) {

					Log.e(Constants.TAG, "Impossible to connect to Geocoder", e);

				} finally {

					// sending message to a handler
					Message msg = Message.obtain();
					msg.setTarget(handler);

					if (addressStr != "") {
						msg.what = 1;

						// passing address string in the bundle
						Bundle bundle = new Bundle();
						bundle.putString("address", addressStr);

						msg.setData(bundle);

					} else {
						msg.what = 0;
					}

					msg.sendToTarget();

				}
			}
		};

		thread.start();

	}

	/**
	 * Add Waypoint dialog
	 * 
	 * @param address Address string returned from geocoder thread
	 */
	private void showAddWaypointDialog(String address) {

		Context mContext = this;

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
		if (address != null) {
			wpDescr.setText(address);
		}

		final EditText wpLat = (EditText) layout.findViewById(R.id.waypointLatInputText);
		// wpLat.setText(Location.convert(myApp.getCurrentLocation().getLatitude(),
		// 0));
		wpLat.setText(Utils.formatCoord(myApp.getCurrentLocation().getLatitude()));

		final EditText wpLng = (EditText) layout.findViewById(R.id.waypointLngInputText);
		// wpLng.setText(Location.convert(myApp.getCurrentLocation().getLongitude(),
		// 0));
		wpLng.setText(Utils.formatCoord(myApp.getCurrentLocation().getLongitude()));

		builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int id) {

				// waypoint title from input dialog
				String titleStr = wpTitle.getText().toString().trim();
				String descrStr = wpDescr.getText().toString().trim();

				if (titleStr.equals("")) {
					Toast.makeText(MainActivity.this, R.string.waypoint_title_required, Toast.LENGTH_SHORT).show();
					dialog.dismiss();
				}

				int lat = (int) (Double.parseDouble(wpLat.getText().toString()) * 1E6);
				int lng = (int) (Double.parseDouble(wpLng.getText().toString()) * 1E6);

				ContentValues values = new ContentValues();
				values.put("title", titleStr);
				values.put("descr", descrStr);
				values.put("lat", lat);
				values.put("lng", lng);
				values.put("elevation", Utils.formatNumber(myApp.getCurrentLocation().getAltitude(), 1));
				values.put("time", myApp.getCurrentLocation().getTime());

				// if track recording started save track_id as
				if (trackRecorder.isRecording()) {
					values.put("track_id", trackRecorder.getTrack().getTrackId());
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
			@Override
			public void onClick(DialogInterface dialog, int id) {
				// dialog.dismiss();
			}
		});

		AlertDialog dialog = builder.create();
		dialog.show();

	}

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
	 * Create application folders
	 */
	private void createFolderStructure() {
		createFolder(myApp.getAppDir());
		createFolder(myApp.getAppDir() + "/tracks");
		createFolder(myApp.getAppDir() + "/waypoints");
		createFolder(myApp.getAppDir() + "/backup");
		createFolder(myApp.getAppDir() + "/debug");
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

			case R.id.compassMenuItem:

				startActivity(new Intent(this, CompassActivity.class));

				return true;

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

					// if in track recording mode do not stop GPS, display
					// warning message instead
					if (!trackRecorder.isRecording()) {
						stopGPSService();
					} else {
						Toast.makeText(MainActivity.this, R.string.stop_track_recording, Toast.LENGTH_SHORT).show();
					}

				} else {
					startGPSService();
				}

				return true;

			case R.id.backupMenuItem:

				backupDatabase();
				return true;

			case R.id.restoreMenuItem:

				restoreDatabase();
				return true;

			default:

				return super.onOptionsItemSelected(item);

		}

	}

	/**
	 * About dialog
	 */
	private void showAboutDialog() {

		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.about_dialog, null);

		TextView versionView = (TextView) layout.findViewById(R.id.version);
		versionView.setText(getString(R.string.main_app_title) + " " + getString(R.string.ver)
				+ MyApp.getVersionName(this));

		TextView messageView = (TextView) layout.findViewById(R.id.message);

		String aboutStr = getString(R.string.about_dialog_message);
		// adding links to "about" text
		final SpannableString s = new SpannableString(aboutStr);
		Linkify.addLinks(s, Linkify.ALL);

		messageView.setText(s);

		// textView.setText(getString(R.string.main_app_title) + " " +
		// getString(R.string.ver)
		// + MyApp.getVersionName(this) + "\n\n" + s);
		messageView.setMovementMethod(LinkMovementMethod.getInstance());

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.about);
		builder.setView(layout);

		builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
				dialog.dismiss();
			}
		});

		builder.setCancelable(true);

		AlertDialog alert = builder.create();
		alert.show();

	}

	/**
	 * Update main activity view
	 */
	public void updateMainActivity(int locationProvider) {
		
		Location location = myApp.getCurrentLocation();
		
//		myApp.log("Lat: " + location.getLatitude()+" | Lng: " + location.getLongitude()+
//					" | Accuracy: "+location.getAccuracy() + 
//					" | Speed: "+location.getSpeed());

		TrackRecorder trackRecorder = TrackRecorder.getInstance(myApp);

		// activate buttons if location updates come from GPS_ORI 
		if (locationProvider == Constants.GPS_PROVIDER) {
			
			if (!fixReceived) {
				((Button) findViewById(R.id.addWaypointButton)).setEnabled(true);
				((Button) findViewById(R.id.trackRecordingButton)).setEnabled(true);
				fixReceived = true;
			}
			
		} else {
			Toast.makeText(MainActivity.this, R.string.waiting_for_fix, Toast.LENGTH_SHORT).show();
		}

		// measuring units
		String speedUnit = myApp.getPreferences().getString("speed_units", "kph");
		String distanceUnit = myApp.getPreferences().getString("distance_units", "km");
		String elevationUnit = myApp.getPreferences().getString("elevation_units", "m");
		int coordUnit = Integer.parseInt(myApp.getPreferences().getString("coord_units", "0"));

		if (findViewById(R.id.lat) != null) {
			((TextView) findViewById(R.id.lat)).setText(Utils.formatLat(location.getLatitude(),
					coordUnit));
		}

		if (findViewById(R.id.lng) != null) {
			((TextView) findViewById(R.id.lng)).setText(Utils.formatLng(location.getLongitude(),
					coordUnit));
		}

		if (location.hasAccuracy()) {

			float accuracy = location.getAccuracy();

			if (findViewById(R.id.accuracy) != null) {
				((TextView) findViewById(R.id.accuracy)).setText(Utils.PLUSMINUS_CHAR+" "+Utils.formatDistance(accuracy, distanceUnit));
			}
			if (findViewById(R.id.accuracyUnit) != null) {
				((TextView) findViewById(R.id.accuracyUnit)).setText(Utils.getLocalaziedDistanceUnit(this, accuracy,
						distanceUnit));
			}
		}

		if (findViewById(R.id.lastFix) != null) {
			String lastFix = (new SimpleDateFormat("H:mm:ss")).format(location.getTime());
			((TextView) findViewById(R.id.lastFix)).setText(lastFix);
		}

		if (location.hasAltitude()) {
			if (findViewById(R.id.elevation) != null) {
				((TextView) findViewById(R.id.elevation)).setText(Utils.formatElevation((float) myApp
						.getCurrentLocation().getAltitude(), elevationUnit));
			}
			if (findViewById(R.id.elevationUnit) != null) {
				((TextView) findViewById(R.id.elevationUnit)).setText(Utils.getLocalizedElevationUnit(this, elevationUnit));
			}
		}

		// current speed and pace
		float speed = 0;
		if (location.hasSpeed()) {

			// speed is in meters per second
			speed = location.getSpeed();

			// current speed (cycling, driving)
			if (findViewById(R.id.speed) != null) {
				((TextView) findViewById(R.id.speed)).setText(Utils.formatSpeed(speed, speedUnit));
			}

			if (findViewById(R.id.speedUnit) != null) {
				((TextView) findViewById(R.id.speedUnit)).setText(Utils.getLocalizedSpeedUnit(this, speedUnit));
			}

			// current pace (running, hiking, walking)
			if (findViewById(R.id.pace) != null) {
				((TextView) findViewById(R.id.pace)).setText(Utils.formatPace(speed, speedUnit));
			}

		}

		// updating track recording info
		if (trackRecorder.isRecording()) {

			// number of track points recorded
			if (findViewById(R.id.pointsCount) != null) {
				((TextView) findViewById(R.id.pointsCount)).setText(Integer.toString(trackRecorder.getPointsCount()));
			}

			// number of track points recorded
			if (findViewById(R.id.segmentsCount) != null) {
				((TextView) findViewById(R.id.segmentsCount))
						.setText(Integer.toString(trackRecorder.getSegmentsCount()));
			}

			// elevation gain
			if (findViewById(R.id.elevationGain) != null) {
				((TextView) findViewById(R.id.elevationGain)).setText(Utils.formatElevation(trackRecorder.getTrack()
						.getElevationGain(), elevationUnit));
			}

			// elevation loss
			if (findViewById(R.id.elevationLoss) != null) {
				((TextView) findViewById(R.id.elevationLoss)).setText(Utils.formatElevation(trackRecorder.getTrack()
						.getElevationLoss(), elevationUnit));
			}

			// average speed
			if (findViewById(R.id.averageSpeed) != null) {
				((TextView) findViewById(R.id.averageSpeed)).setText(Utils.formatSpeed(trackRecorder.getTrack()
						.getAverageSpeed(), speedUnit));
			}

			// average moving speed
			if (findViewById(R.id.averageMovingSpeed) != null) {
				((TextView) findViewById(R.id.averageMovingSpeed)).setText(Utils.formatSpeed(trackRecorder.getTrack()
						.getAverageMovingSpeed(), speedUnit));
			}

			// max speed
			if (findViewById(R.id.maxSpeed) != null) {
				((TextView) findViewById(R.id.maxSpeed)).setText(Utils.formatSpeed(trackRecorder.getTrack()
						.getMaxSpeed(),
						speedUnit));
			}

			// acceleration
			if (findViewById(R.id.acceleration) != null) {

				// let's display last non-zero acceleration
				((TextView) findViewById(R.id.acceleration)).setText(
							Utils.formatNumber(trackRecorder.getTrack().getAcceleration(), 2));

			}

			// average pace
			if (findViewById(R.id.averagePace) != null) {
				((TextView) findViewById(R.id.averagePace)).setText(Utils.formatPace(
						trackRecorder.getTrack().getAverageSpeed(),
						speedUnit));
			}

			// average moving pace
			if (findViewById(R.id.averageMovingPace) != null) {
				((TextView) findViewById(R.id.averageMovingPace)).setText(Utils.formatPace(trackRecorder.getTrack()
						.getAverageMovingSpeed(), speedUnit));
			}

			// max pace
			if (findViewById(R.id.maxPace) != null) {
				((TextView) findViewById(R.id.maxPace)).setText(Utils.formatPace(
						trackRecorder.getTrack().getMaxSpeed(),
						speedUnit));
			}

			// total distance
			if (findViewById(R.id.distance) != null) {
				((TextView) findViewById(R.id.distance)).setText(Utils.formatDistance(trackRecorder.getTrack()
						.getDistance(),
						distanceUnit));
			}

			if (findViewById(R.id.distanceUnit) != null) {
				((TextView) findViewById(R.id.distanceUnit)).setText(Utils.getLocalaziedDistanceUnit(this, trackRecorder
						.getTrack()
						.getDistance(),
						distanceUnit));
			}

		}

		// sunrise/sunset times
		Calendar cal = Calendar.getInstance();
		TimeZone tz = TimeZone.getTimeZone(cal.getTimeZone().getID());

		SunriseSunset ss = new SunriseSunset(location.getLatitude(),
				location.getLongitude(), cal.getTime(),
				tz.getOffset(cal.getTimeInMillis()) / 1000 / 60 / 60);

		if (findViewById(R.id.sunrise) != null) {
			String srise = (new SimpleDateFormat("H:mm")).format(ss.getSunrise());
			((TextView) findViewById(R.id.sunrise)).setText(srise);
		}

		if (findViewById(R.id.sunset) != null) {
			String sset = (new SimpleDateFormat("H:mm")).format(ss.getSunset());
			((TextView) findViewById(R.id.sunset)).setText(sset);
		}

	}

	/**
	 * Update compass image and azimuth text
	 */
	public void updateCompass(float azimuth) {

		boolean trueNorth = myApp.getPreferences().getBoolean("true_north", true);

		float rotation = 0;

		// TODO: let's not request declination on every compass update
		float declination = 0;
		if (trueNorth && myApp.getCurrentLocation() != null) {
			long now = System.currentTimeMillis();
			declination = Utils.getDeclination(myApp.getCurrentLocation(), now);
		}

		// magnetic north to true north
		rotation = getAzimuth(azimuth + declination);

		if (findViewById(R.id.azimuth) != null) {
			((TextView) findViewById(R.id.azimuth)).setText(Utils.formatNumber(rotation, 0)
					+ Utils.DEGREE_CHAR + " "
					+ Utils.getDirectionCode(rotation));
		}

	}

	protected float getAzimuth(float az) {

		if (az > 360) {
			return az - 360;
		}

		return az;

	}

	// -------------------------------------------------------------------------
	// UPDATE TIME IN TRACK RECORDING MODE
	// -------------------------------------------------------------------------

	/**
	 * Update total and moving time in track recording mode
	 */
	public void updateTime() {

		if (trackRecorder.isRecording()) {

			if (findViewById(R.id.totalTime) != null) {
				((TextView) findViewById(R.id.totalTime)).setText(Utils.formatInterval(
						trackRecorder.getTrack().getTotalTime(), false));
			}

			if (findViewById(R.id.movingTime) != null) {
				((TextView) findViewById(R.id.movingTime)).setText(Utils.formatInterval(
						trackRecorder.getTrack().getMovingTime(), false));
			}

		}

	}

	// -------------------------------------------------------------------------
	// HANDLING BACK BUTTON CLICKS
	// -------------------------------------------------------------------------
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

		if (trackRecorder.isRecording() && backClickCount < 2) {
			Toast.makeText(MainActivity.this, R.string.press_again_to_exit, Toast.LENGTH_SHORT).show();
			// click count is cleared after 3 seconds
			mainHandler.postDelayed(clearClickCountTask, 3000);
		} else {
			this.finish();
		}
	}

	/**
	 * Clear click count handler
	 */
	private Runnable clearClickCountTask = new Runnable() {
		@Override
		public void run() {
			backClickCount = 0;
		}
	};

	// --------------------------------------------------------------------------------------------

	// -------------------------------------------------------------------------
	// WAKE LOCK
	// -------------------------------------------------------------------------
	/*
	 * public void aquireWakeLock() { wakeLock = ((PowerManager)
	 * getSystemService(Context.POWER_SERVICE))
	 * .newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK |
	 * PowerManager.ON_AFTER_RELEASE, Constants.TAG); wakeLock.acquire(); }
	 * public void releaseWakeLock() { wakeLock.release(); }
	 */

	/**
	 * Create a list of famous waypoints and insert to db when application first
	 * installed
	 */
	public void processFamousWaypoints() {

		// adding famous waypoints only once
		if (myApp.getPreferences().contains("famous_waypoints")) {
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
			values.put("descr", "");
			values.put("lat", (int) (wp.getLatitude() * 1E6));
			values.put("lng", (int) (wp.getLongitude() * 1E6));
			values.put("time", wp.getTime());

			myApp.getDatabase().insert("waypoints", null, values);

		}

		// switch flag of famous locations added to true
		SharedPreferences.Editor editor = myApp.getPreferences().edit();
		editor.putInt("famous_waypoints", 1);
		editor.commit();

	}

	/**
	 * Copy application database to sd card
	 */
	private void backupDatabase() {

		if (trackRecorder.isRecording()) {
			Toast.makeText(MainActivity.this, R.string.stop_track_recording, Toast.LENGTH_SHORT).show();
			return;
		}

		try {

			File data = Environment.getDataDirectory();

			if (myApp.getExternalStorageWriteable()) {

				String currentDBPath = "/data/com.aripuca.tracker/databases/" + Constants.APP_NAME + ".db";

				String dateStr = (new SimpleDateFormat("yyyyMMdd_HHmmss")).format(new Date());

				File currentDB = new File(data, currentDBPath);
				File backupDB = new File(myApp.getAppDir() + "/backup/" + dateStr + ".db");

				if (currentDB.exists()) {
					FileChannel src = new FileInputStream(currentDB).getChannel();
					FileChannel dst = new FileOutputStream(backupDB).getChannel();
					dst.transferFrom(src, 0, src.size());
					src.close();
					dst.close();

					Toast.makeText(MainActivity.this, getString(R.string.backup_completed) + " " + backupDB.getPath(),
							Toast.LENGTH_LONG).show();

				} else {
					Toast.makeText(MainActivity.this, getString(R.string.backup_error) + ": Source db file not found",
							Toast.LENGTH_LONG).show();
				}

			}
		}

		catch (Exception e) {

			Log.e(Constants.TAG, e.getMessage());

			Toast.makeText(MainActivity.this, getString(R.string.backup_error) + " " + e.getMessage(),
					Toast.LENGTH_LONG).show();

		}

	}

	/**
	 * Restoring database from previously saved copy
	 */
	private void restoreDatabase() {

		if (trackRecorder.isRecording()) {
			Toast.makeText(MainActivity.this, R.string.stop_track_recording, Toast.LENGTH_SHORT).show();
			return;
		}

		// show "select a file" dialog
		File importFolder = new File(myApp.getAppDir() + "/backup/");
		final String importFiles[] = importFolder.list();

		if (importFiles == null ||
				importFiles.length == 0) {
			Toast.makeText(MainActivity.this, R.string.source_folder_empty, Toast.LENGTH_SHORT).show();
			return;
		}

		importDatabaseFileName = importFiles[0];

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.select_file);
		builder.setSingleChoiceItems(importFiles, 0, new
				DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {

						importDatabaseFileName = importFiles[whichButton];
						mainHandler.post(restoreDatabaseRunnable);

						dialog.dismiss();

					}
				});

		AlertDialog alert = builder.create();
		alert.show();

	}

	/**
	 * Runnable performing restoration of the application database from external
	 * source
	 */
	private Runnable restoreDatabaseRunnable = new Runnable() {
		@Override
		public void run() {

			try {

				// open database in readonly mode
				SQLiteDatabase db = SQLiteDatabase.openDatabase(
						myApp.getAppDir() + "/backup/" + importDatabaseFileName,
						null, SQLiteDatabase.OPEN_READONLY);

				// check version compatibility
				// only same version of the db can be restored
				if (myApp.getDatabase().getVersion() != db.getVersion()) {
					Toast.makeText(MainActivity.this, getString(R.string.restore_db_version_conflict),
							Toast.LENGTH_LONG).show();
					return;
				}

				db.close();

			} catch (SQLiteException e) {

				Toast.makeText(MainActivity.this, getString(R.string.restore_file_error) + ": " + e.getMessage(),
						Toast.LENGTH_LONG).show();

				return;
			}

			// closing current db
			// track recording mode is always off at this point
			myApp.getDatabase().close();

			try {

				File data = Environment.getDataDirectory();

				if (myApp.getExternalStorageWriteable()) {

					String restoreDBPath = myApp.getAppDir() + "/backup/" + importDatabaseFileName;

					File restoreDB = new File(restoreDBPath);
					File currentDB = new File(data, "/data/com.aripuca.tracker/databases/AripucaTracker.db");

					FileChannel src = new FileInputStream(restoreDB).getChannel();
					FileChannel dst = new FileOutputStream(currentDB).getChannel();

					dst.transferFrom(src, 0, src.size());
					src.close();
					dst.close();

					myApp.setDatabase();

					Toast.makeText(MainActivity.this, getString(R.string.restore_completed),
							Toast.LENGTH_SHORT).show();

				}

			}
			catch (Exception e) {

				Log.e(Constants.TAG, e.getMessage());

				myApp.setDatabase();

				Toast.makeText(MainActivity.this, getString(R.string.restore_error) + ": " + e.getMessage(),
						Toast.LENGTH_LONG).show();

			}

		}
	};

}
