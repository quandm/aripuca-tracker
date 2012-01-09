package com.aripuca.tracker;

import com.aripuca.tracker.app.AppLog;
import com.aripuca.tracker.app.Constants;
import com.aripuca.tracker.dialog.QuickHelpDialog;
import com.aripuca.tracker.service.GpsService;

import com.aripuca.tracker.util.ContainerCarousel;
import com.aripuca.tracker.util.OrientationHelper;
import com.aripuca.tracker.util.SunriseSunset;
import com.aripuca.tracker.util.Utils;
import com.aripuca.tracker.view.CompassImage;
import com.aripuca.tracker.R;

import java.io.File;
import java.io.IOException;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.io.*;
import java.nio.channels.FileChannel;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.location.*;

import android.widget.*;

import android.text.SpannableString;
import android.text.format.DateFormat;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.KeyEvent;
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
	 * Reference to Application object
	 */
	private MyApp myApp;

	private String importDatabaseFileName;

	private Handler mainHandler = new Handler();

	private OrientationHelper orientationHelper;

	private long declinationLastUpdate = 0;

	/**
	 * location received from GpsService
	 */
	private Location currentLocation;

	private String speedUnit;
	private String distanceUnit;
	private String elevationUnit;
	private int coordUnit;

	/**
	 * location updates broadcast receiver
	 */
	protected BroadcastReceiver locationBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

			Bundle bundle = intent.getExtras();

			currentLocation = (Location) bundle.getParcelable("location");
			
			myApp.logd("location");
			
			updateActivity();

		}
	};

	private void showWaitForFixMessage() {

		// hide waiting for gps fix message
		if (findViewById(R.id.messageBox) != null) {
			((LinearLayout) findViewById(R.id.messageBox)).setVisibility(View.VISIBLE);
		}

		// calculating gps fix age
		if (findViewById(R.id.fixAge) != null) {

			long fixAge = System.currentTimeMillis() - currentLocation.getTime();
			
			//TODO: for some reason currentLocation.getTime returns date in the future (started in January 2012)
			if (fixAge<0) {
				fixAge = Utils.ONE_DAY - Math.abs(fixAge);
			}
			
			String t = Utils.timeToHumanReadableString(MainActivity.this, fixAge);
			((TextView) findViewById(R.id.fixAge)).setText(String.format(getString(R.string.fix_age), t));
		}

	}

	private void hideWaitForFixMessage() {

		// hide waiting for gps fix message
		if (findViewById(R.id.messageBox) != null) {
			((LinearLayout) findViewById(R.id.messageBox)).setVisibility(View.INVISIBLE);
		}

	}

	protected BroadcastReceiver scheduledLocationBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

			Bundle bundle = intent.getExtras();

			currentLocation = (Location) bundle.getParcelable("location");

			updateActivity();

			Toast.makeText(MainActivity.this, R.string.location_received_on_schedule, Toast.LENGTH_SHORT).show();

		}
	};
	/**
	 * compass updates broadcast receiver
	 */
	protected BroadcastReceiver compassBroadcastReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {

			if (myApp == null) {
				return;
			}

			Bundle bundle = intent.getExtras();

			orientationHelper.setOrientationValues(bundle.getFloat("azimuth"), bundle.getFloat("pitch"),
					bundle.getFloat("roll"));

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

			// show pause/resume button
			((Button) findViewById(R.id.pauseResumeTrackButton)).setVisibility(View.VISIBLE);

			// disable pause/resume button when tracking started or stopped
			((Button) findViewById(R.id.pauseResumeTrackButton)).setText(getString(R.string.pause));

			if (gpsService.getTrackRecorder().isRecording()) {
				stopTracking();
				updateSunriseSunset();
			} else {
				startTracking();
			}

			return true;
		}

	};

	private OnClickListener trackRecordingButtonClick = new OnClickListener() {
		@Override
		public void onClick(View v) {

			if (gpsService.getTrackRecorder().isRecording()) {
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

			if (gpsService.getTrackRecorder().isRecordingPaused()) {

				((Button) findViewById(R.id.pauseResumeTrackButton)).setText(getString(R.string.pause));

				gpsService.getTrackRecorder().resume();

				Toast.makeText(MainActivity.this, R.string.recording_resumed, Toast.LENGTH_SHORT).show();

			} else {

				((Button) findViewById(R.id.pauseResumeTrackButton)).setText(getString(R.string.resume));

				gpsService.getTrackRecorder().pause();

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

		if (!myApp.getPreferences().contains("trackpoints_sort")) {
			editor.putInt("trackpoints_sort", 0);
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

		Log.i(Constants.TAG, "MainActivity: onCreate");

		// reference to application object
		myApp = ((MyApp) getApplicationContext());

		orientationHelper = new OrientationHelper(MainActivity.this);

		initializeHiddenPreferences();

		// ----------------------------------------------------------------------
		// preparing UI
		// setting layout for main activity
		setContentView(R.layout.main);

		// restoring previous application state
		// it should be done between setContentView & replaceDynamicView calls
		if (savedInstanceState != null) {
			restoreInstanceState(savedInstanceState);
		}

		this.disableControlButtons();

		gpsServiceConnection = new GpsServiceConnection();

		Log.d(Constants.TAG, "SERVICE CONNECTION CREATED: " + gpsServiceConnection.toString());

		// start GPS service only if not started
		startGpsService();

		// show quick help only when activity first started
		if (savedInstanceState == null) {
			if (myApp.getPreferences().getBoolean("quick_help", true)) {
				showQuickHelp();
			}
		}

		this.setControlButtonListeners();

	}

	/**
	 * onResume event handler
	 */
	@Override
	protected void onResume() {

		Log.i(Constants.TAG, "MainActivity: onResume");
		super.onResume();

		this.initializeMeasuringUnits();

		this.keepScreenOn();

		// registering receiver for compass updates
		registerReceiver(compassBroadcastReceiver, new IntentFilter(Constants.ACTION_COMPASS_UPDATES));

		// registering receiver for location updates
		registerReceiver(locationBroadcastReceiver, new IntentFilter(Constants.ACTION_LOCATION_UPDATES));

		// registering receiver for time updates
		registerReceiver(scheduledLocationBroadcastReceiver, new IntentFilter(
				Constants.ACTION_SCHEDULED_LOCATION_UPDATES));

		// bind to GPS service
		// once bound gpsServiceBoundCallback will be called
		this.bindGpsService();

		// start updating time of tracking every second
		updateTimeHandler.postDelayed(updateTimeTask, 500);

	}

	protected void tryUnregisterReceiver(BroadcastReceiver receiver) {

		try {
			unregisterReceiver(receiver);
		} catch (IllegalArgumentException e) {
			Log.e(Constants.TAG, e.getMessage());
		}

	}

	/**
	 * onPause event handler
	 */
	@Override
	protected void onPause() {

		Log.i(Constants.TAG, "MainActivity: onPause");

		this.tryUnregisterReceiver(compassBroadcastReceiver);
		this.tryUnregisterReceiver(locationBroadcastReceiver);
		this.tryUnregisterReceiver(scheduledLocationBroadcastReceiver);

		if (!this.isFinishing()) {

			// activity will be recreated
			if (gpsService != null) {

				// stop location updates when not recording track
				if (!gpsService.getTrackRecorder().isRecording()) {
					gpsService.stopLocationUpdates();
				}

				gpsService.stopSensorUpdates();
			}

		} else {

			// activity will be destroyed and not recreated

			// stop tracking if active
			if (gpsService.getTrackRecorder().isRecording()) {
				stopTracking();
			}

		}

		// unbind GpsService
		this.unbindGpsService();

		if (this.isFinishing()) {

			// if activity is not going to be recreated - stop service
			stopGpsService();

			// save layout for next session
			this.saveHiddenPreferences();

		}

		this.updateTimeHandler.removeCallbacks(updateTimeTask);

		super.onPause();
	}

	/**
	 * onDestroy event handler
	 */
	@Override
	protected void onDestroy() {

		Log.i(Constants.TAG, "MainActivity: onDestroy");

		gpsServiceConnection = null;

		myApp = null;

		super.onDestroy();
	}

	/**
	 * Restoring data saved in onSaveInstanceState
	 */
	private void restoreInstanceState(Bundle savedInstanceState) {

		// Log.i(Constants.TAG, "MainActivity: restoreInstanceState");

		speedContainerCarousel.setCurrentContainerId(savedInstanceState.getInt("speedContainerId"));
		timeContainerCarousel.setCurrentContainerId(savedInstanceState.getInt("timeContainerId"));
		distanceContainerCarousel.setCurrentContainerId(savedInstanceState.getInt("distanceContainerId"));
		elevationContainerCarousel.setCurrentContainerId(savedInstanceState.getInt("elevationContainerId"));
		coordinatesContainerCarousel.setCurrentContainerId(savedInstanceState.getInt("coordinatesContainerId"));

		// restore current location
		currentLocation = (Location) savedInstanceState.getParcelable("location");

		// restore pauseResumeTrackButton title and state
		if (findViewById(R.id.pauseResumeTrackButton) != null) {

			((Button) findViewById(R.id.pauseResumeTrackButton)).setText(savedInstanceState
					.getString("pauseButtonText"));

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

		// Log.i(Constants.TAG, "MainActivity: onSaveInstanceState");

		outState.putInt("speedContainerId", speedContainerCarousel.getCurrentContainerId());
		outState.putInt("timeContainerId", timeContainerCarousel.getCurrentContainerId());
		outState.putInt("distanceContainerId", distanceContainerCarousel.getCurrentContainerId());
		outState.putInt("elevationContainerId", elevationContainerCarousel.getCurrentContainerId());
		outState.putInt("coordinatesContainerId", coordinatesContainerCarousel.getCurrentContainerId());

		// saving current location
		outState.putParcelable("location", currentLocation);

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

	/**
	 * preventing phone from sleeping
	 */
	private void keepScreenOn() {

		if (findViewById(R.id.dynamicView) != null) {
			findViewById(R.id.dynamicView).setKeepScreenOn(myApp.getPreferences().getBoolean("wake_lock", true));
		}

	}

	/**
	 * 
	 */
	private void setContainer(ContainerCarousel carousel) {

		// assigning click event listener to container
		if (findViewById(carousel.getResourceId()) != null) {

			ViewGroup containerView = (ViewGroup) findViewById(carousel.getResourceId());

			final int resourceId = carousel.getResourceId();
			final ContainerCarousel car = carousel;

			containerView.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					ViewGroup containerView = (ViewGroup) findViewById(resourceId);

					// attaching default layout
					View tmpView1 = getLayoutInflater().inflate(car.getNextContainer(), containerView, false);
					containerView.removeAllViews();
					containerView.addView(tmpView1, 0);
				}
			});

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

		if (resourceId != R.layout.main_idle2) {
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
	private void startGpsService() {

		if (!GpsService.isRunning()) {

			Log.i(Constants.TAG, "startGPSService");

			// starting GPS listener service
			startService(new Intent(this, GpsService.class));
		}

	}

	/**
	 * stop GPS listener service
	 */
	private void stopGpsService() {

		Log.i(Constants.TAG, "stopGPSService");

		stopService(new Intent(this, GpsService.class));

	}

	/**
	 * Enable control buttons
	 */
	protected void enableControlButtons() {

		((Button) findViewById(R.id.addWaypointButton)).setEnabled(true);
		((Button) findViewById(R.id.trackRecordingButton)).setEnabled(true);
		((Button) findViewById(R.id.pauseResumeTrackButton)).setEnabled(true);

	}

	/**
	 * Disable control buttons
	 */
	protected void disableControlButtons() {

		((Button) findViewById(R.id.addWaypointButton)).setEnabled(false);
		((Button) findViewById(R.id.trackRecordingButton)).setEnabled(false);
		((Button) findViewById(R.id.pauseResumeTrackButton)).setEnabled(false);

	}

	/**
	 * Show ongoing notification
	 */
	private void showNotification() {

		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		long when = System.currentTimeMillis();

		Notification notification = new Notification(R.drawable.aripuca, getString(R.string.recording_started), when);

		// show notification under ongoing title
		notification.flags += Notification.FLAG_ONGOING_EVENT;

		CharSequence contentTitle = getString(R.string.main_app_title);
		CharSequence contentText = getString(R.string.recording_track);

		Intent notificationIntent = new Intent(this, NotificationActivity.class);

		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

		notification.setLatestEventInfo(myApp, contentTitle, contentText, contentIntent);

		mNotificationManager.notify(Constants.NOTIFICATION_TRACK_RECORDING, notification);

	}

	private void clearNotification() {

		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		// remove all notifications
		// mNotificationManager.cancelAll();
		mNotificationManager.cancel(Constants.NOTIFICATION_TRACK_RECORDING);

	}

	/**
	 * start real time track recording
	 */
	private void startTracking() {

		this.initializeMeasuringUnits();

		// Change button label from Record to Stop
		((Button) findViewById(R.id.trackRecordingButton)).setText(getString(R.string.stop));

		// show pause/resume button
		((Button) findViewById(R.id.pauseResumeTrackButton)).setVisibility(View.VISIBLE);

		// enabling pause/resume button
		((Button) findViewById(R.id.pauseResumeTrackButton)).setEnabled(true);

		this.replaceDynamicView(R.layout.main_tracking);

		gpsService.getTrackRecorder().start();

		// add notification icon in track recording mode
		this.showNotification();

		Toast.makeText(this, R.string.recording_started, Toast.LENGTH_SHORT).show();

	}

	/**
	 * stop real time track recording
	 */
	private void stopTracking() {

		// disabling pause/resume button
		((Button) findViewById(R.id.pauseResumeTrackButton)).setEnabled(false);
		((Button) findViewById(R.id.pauseResumeTrackButton)).setText(getString(R.string.pause));

		// hide pause/resume button
		((Button) findViewById(R.id.pauseResumeTrackButton)).setVisibility(View.GONE);

		// change button label from Stop to Record
		((Button) findViewById(R.id.trackRecordingButton)).setText(getString(R.string.record));

		gpsService.getTrackRecorder().stop();

		// switching to initial layout
		this.replaceDynamicView(R.layout.main_idle2);

		this.clearNotification();

		Toast.makeText(this, R.string.recording_finished, Toast.LENGTH_SHORT).show();

	}

	private void initializeMeasuringUnits() {

		speedUnit = myApp.getPreferences().getString("speed_units", "kph");
		distanceUnit = myApp.getPreferences().getString("distance_units", "km");
		elevationUnit = myApp.getPreferences().getString("elevation_units", "m");
		coordUnit = Integer.parseInt(myApp.getPreferences().getString("coord_units", "0"));

	}

	/**
	 * add waypoint button listener
	 */
	private OnClickListener addWaypointListener = new OnClickListener() {

		@Override
		public void onClick(View v) {

			// let's make reverse geocoder request and then show Add Waypoint
			// dialog
			// address will be inserted as default description for this waypoint

			// disable "add waypoint" button for the time of request
			((Button) findViewById(R.id.addWaypointButton)).setEnabled(false);

			if (myApp.checkInternetConnection()
					&& myApp.getPreferences().getBoolean("waypoint_default_description", false)) {

				// processing is done in separate thread
				geocodeLocation(currentLocation, MainActivity.this, new GeocoderHandler());

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

				} catch (IOException e) {

					Log.e(Constants.TAG, "Cannot connect to Geocoder", e);

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
		wpTitle.setText(DateFormat.format("yyyy-MM-dd k:mm:ss", new Date()));

		final EditText wpDescr = (EditText) layout.findViewById(R.id.waypointDescriptionInputText);
		if (address != null) {
			wpDescr.setText(address);
		}

		final EditText wpLat = (EditText) layout.findViewById(R.id.waypointLatInputText);
		// wpLat.setText(Location.convert(location.getLatitude(),
		// 0));
		wpLat.setText(Utils.formatCoord(currentLocation.getLatitude()));

		final EditText wpLng = (EditText) layout.findViewById(R.id.waypointLngInputText);
		// wpLng.setText(Location.convert(location.getLongitude(),
		// 0));
		wpLng.setText(Utils.formatCoord(currentLocation.getLongitude()));

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
				values.put("accuracy", currentLocation.getAccuracy());
				values.put("elevation", Utils.formatNumber(currentLocation.getAltitude(), 1));
				values.put("time", currentLocation.getTime());

				// if track recording started assign track_id
				if (gpsService.getTrackRecorder().isRecording()) {
					values.put("track_id", gpsService.getTrackRecorder().getTrack().getTrackId());
				}

				try {
					myApp.getDatabase().insertOrThrow("waypoints", null, values);
				} catch (SQLiteException e) {
					Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
					Log.e(Constants.TAG, "SQLiteException: " + e.getMessage(), e);
				}

				Toast.makeText(MainActivity.this, R.string.waypoint_saved, Toast.LENGTH_SHORT).show();

				((Button) findViewById(R.id.addWaypointButton)).setEnabled(true);

				dialog.dismiss();

			}
		});

		builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {

				((Button) findViewById(R.id.addWaypointButton)).setEnabled(true);
				dialog.dismiss();
			}
		});

		AlertDialog dialog = builder.create();
		dialog.show();

	}

	protected Dialog onCreateDialog(int id) {

		Context mContext = this;

		Dialog dialog;

		switch (id) {

			case Constants.QUICK_HELP_DIALOG_ID:

				dialog = new QuickHelpDialog(mContext);

			break;

			default:
				dialog = null;
		}

		return dialog;
	}

	/**
	 * Show quick help
	 */
	private void showQuickHelp() {

		showDialog(Constants.QUICK_HELP_DIALOG_ID);

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

		// hide test lab menu
		MenuItem testLabMenuItem = (MenuItem) menu.findItem(R.id.testLabMenuItem);
		testLabMenuItem.setVisible(false);

		// setting icon and title for scheduled track recording menu
		MenuItem scheduledRecordingMenuItem = (MenuItem) menu.findItem(R.id.scheduledRecordingMenuItem);

		if (gpsService != null) {
			if (gpsService.getScheduledTrackRecorder().isRecording()) {
				scheduledRecordingMenuItem.setTitle(R.string.stop_scheduler);
				scheduledRecordingMenuItem.setIcon(R.drawable.ic_media_pause);
			} else {
				scheduledRecordingMenuItem.setTitle(R.string.start_scheduler);
				scheduledRecordingMenuItem.setIcon(R.drawable.ic_media_play);
			}
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

				startActivity(new Intent(this, TracksTabActivity.class));

				return true;

			case R.id.aboutMenuItem:

				this.showAboutDialog();

				return true;

			case R.id.settingsMenuItem:

				startActivity(new Intent(this, SettingsActivity.class));

				return true;

			case R.id.quickHelp:

				showQuickHelp();

				return true;

			case R.id.backupMenuItem:

				backupDatabase();
				return true;

			case R.id.restoreMenuItem:

				restoreDatabase();
				return true;

			case R.id.scheduledRecordingMenuItem:

				this.startStopScheduledTrackRecording();
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

		TextView buildDate = (TextView) layout.findViewById(R.id.build_date);
		buildDate.setText(getString(R.string.build_date) + ": " + getString(R.string.app_build_date));

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
	public void updateActivity() {

		if (currentLocation == null || gpsService == null) {
			return;
		}

		// ///////////////////////////////////////////////////////////////////
		if (gpsService.isListening()) {
			// activate buttons if location updates come from GPS
			((Button) findViewById(R.id.addWaypointButton)).setEnabled(true);
			((Button) findViewById(R.id.trackRecordingButton)).setEnabled(true);
			this.hideWaitForFixMessage();
		} else {
			// disable recording buttons when waiting for new location
			((Button) findViewById(R.id.addWaypointButton)).setEnabled(false);
			((Button) findViewById(R.id.trackRecordingButton)).setEnabled(false);
			this.showWaitForFixMessage();
		}
		// //////////////////////////////////////////////////////////////////

		// update coordinates
		if (findViewById(R.id.lat) != null) {
			((TextView) findViewById(R.id.lat)).setText(Utils.formatLat(currentLocation.getLatitude(), coordUnit));
		}
		if (findViewById(R.id.lng) != null) {
			((TextView) findViewById(R.id.lng)).setText(Utils.formatLng(currentLocation.getLongitude(), coordUnit));
		}

		// update accuracy data
		if (currentLocation.hasAccuracy()) {

			float accuracy = currentLocation.getAccuracy();

			if (findViewById(R.id.accuracy) != null) {
				((TextView) findViewById(R.id.accuracy)).setText(Utils.PLUSMINUS_CHAR
						+ Utils.formatDistance(accuracy, distanceUnit));
			}
			if (findViewById(R.id.accuracyUnit) != null) {
				((TextView) findViewById(R.id.accuracyUnit)).setText(Utils.getLocalizedDistanceUnit(this, accuracy,
						distanceUnit));
			}
		}

		// last fix time
		if (findViewById(R.id.lastFix) != null) {
			String lastFix = (String) DateFormat.format("k:mm:ss", currentLocation.getTime());
			((TextView) findViewById(R.id.lastFix)).setText(lastFix);
		}

		// update elevation data
		if (currentLocation.hasAltitude()) {
			if (findViewById(R.id.elevation) != null) {
				((TextView) findViewById(R.id.elevation)).setText(Utils.formatElevation(
						(float) currentLocation.getAltitude(), elevationUnit));
			}
			if (findViewById(R.id.elevationUnit) != null) {
				((TextView) findViewById(R.id.elevationUnit)).setText(Utils.getLocalizedElevationUnit(this,
						elevationUnit));
			}
		}

		// current speed and pace
		float speed = currentLocation.getSpeed();

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

		// updating track recording info
		this.updateTrackRecording();

	}

	/**
	 * Updates UI in track recording mode
	 */
	private void updateTrackRecording() {

		if (gpsService == null || !gpsService.getTrackRecorder().isRecording()) {
			return;
		}

		// number of track points recorded
		if (findViewById(R.id.pointsCount) != null) {
			((TextView) findViewById(R.id.pointsCount)).setText(Integer.toString(gpsService.getTrackRecorder()
					.getPointsCount()));
		}

		// number of track points recorded
		if (findViewById(R.id.segmentsCount) != null) {
			((TextView) findViewById(R.id.segmentsCount)).setText(Integer.toString(gpsService.getTrackRecorder()
					.getSegmentsCount()));
		}

		// ------------------------------------------------------------------
		// elevation gain
		if (findViewById(R.id.elevationGain) != null) {
			((TextView) findViewById(R.id.elevationGain)).setText(Utils.formatElevation(gpsService.getTrackRecorder()
					.getTrack().getElevationGain(), elevationUnit));
		}

		// elevation loss
		if (findViewById(R.id.elevationLoss) != null) {
			((TextView) findViewById(R.id.elevationLoss)).setText(Utils.formatElevation(gpsService.getTrackRecorder()
					.getTrack().getElevationLoss(), elevationUnit));
		}

		// max elevation
		if (findViewById(R.id.maxElevation) != null) {
			((TextView) findViewById(R.id.maxElevation)).setText(Utils.formatElevation(gpsService.getTrackRecorder()
					.getTrack().getMaxElevation(), elevationUnit));
		}

		// min elevation
		if (findViewById(R.id.minElevation) != null) {
			((TextView) findViewById(R.id.minElevation)).setText(Utils.formatElevation(gpsService.getTrackRecorder()
					.getTrack().getMinElevation(), elevationUnit));
		}
		// ------------------------------------------------------------------

		// average speed
		if (findViewById(R.id.averageSpeed) != null) {
			((TextView) findViewById(R.id.averageSpeed)).setText(Utils.formatSpeed(gpsService.getTrackRecorder()
					.getTrack().getAverageSpeed(), speedUnit));
		}

		// average moving speed
		if (findViewById(R.id.averageMovingSpeed) != null) {
			((TextView) findViewById(R.id.averageMovingSpeed)).setText(Utils.formatSpeed(gpsService.getTrackRecorder()
					.getTrack().getAverageMovingSpeed(), speedUnit));
		}

		// max speed
		if (findViewById(R.id.maxSpeed) != null) {
			((TextView) findViewById(R.id.maxSpeed)).setText(Utils.formatSpeed(gpsService.getTrackRecorder().getTrack()
					.getMaxSpeed(), speedUnit));
		}

		// acceleration
		if (findViewById(R.id.acceleration) != null) {

			// let's display last non-zero acceleration
			((TextView) findViewById(R.id.acceleration)).setText(Utils.formatNumber(gpsService.getTrackRecorder()
					.getTrack().getAcceleration(), 2));

		}

		// average pace
		if (findViewById(R.id.averagePace) != null) {
			((TextView) findViewById(R.id.averagePace)).setText(Utils.formatPace(gpsService.getTrackRecorder()
					.getTrack().getAverageSpeed(), speedUnit));
		}

		// average moving pace
		if (findViewById(R.id.averageMovingPace) != null) {
			((TextView) findViewById(R.id.averageMovingPace)).setText(Utils.formatPace(gpsService.getTrackRecorder()
					.getTrack().getAverageMovingSpeed(), speedUnit));
		}

		// max pace
		if (findViewById(R.id.maxPace) != null) {
			((TextView) findViewById(R.id.maxPace)).setText(Utils.formatPace(gpsService.getTrackRecorder().getTrack()
					.getMaxSpeed(), speedUnit));
		}

		// total distance
		if (findViewById(R.id.distance) != null) {
			((TextView) findViewById(R.id.distance)).setText(Utils.formatDistance(gpsService.getTrackRecorder()
					.getTrack().getDistance(), distanceUnit));
		}

		if (findViewById(R.id.distanceUnit) != null) {
			((TextView) findViewById(R.id.distanceUnit)).setText(Utils.getLocalizedDistanceUnit(this, gpsService
					.getTrackRecorder().getTrack().getDistance(), distanceUnit));
		}

	}

	/**
	 * Update sunrise/sunset times We update this only after GpsService bound or
	 * track recording stopped
	 */
	private void updateSunriseSunset() {

		Calendar calendar = Calendar.getInstance();
		TimeZone timeZone = TimeZone.getTimeZone(calendar.getTimeZone().getID());

		// sunrise/sunset times
		SunriseSunset ss = new SunriseSunset(currentLocation.getLatitude(), currentLocation.getLongitude(),
				calendar.getTime(), timeZone.getOffset(calendar.getTimeInMillis()) / 1000 / 60 / 60);

		if (findViewById(R.id.sunrise) != null) {
			String srise = (String) DateFormat.format("k:mm", ss.getSunrise());
			((TextView) findViewById(R.id.sunrise)).setText(srise);
		}

		if (findViewById(R.id.sunset) != null) {
			String sset = (String) DateFormat.format("k:mm", ss.getSunset());
			((TextView) findViewById(R.id.sunset)).setText(sset);
		}

	}

	/**
	 * Update compass image and azimuth text
	 */
	public void updateCompass(float azimuth) {

		boolean trueNorth = myApp.getPreferences().getBoolean("true_north", true);

		float trueAzimuth = 0;

		// let's not request declination on every compass update
		float declination = 0;
		if (trueNorth && currentLocation != null) {
			long now = System.currentTimeMillis();
			// let's request declination every 15 minutes, not every compass
			// update
			if (now - declinationLastUpdate > 15 * 60 * 1000) {
				declination = Utils.getDeclination(currentLocation, now);
				declinationLastUpdate = now;
			}
		}

		// magnetic north to true north
		trueAzimuth = azimuth + declination;
		if (trueAzimuth > 360) {
			trueAzimuth -= 360;
		}

		if (findViewById(R.id.azimuth) != null) {
			((TextView) findViewById(R.id.azimuth)).setText(Utils.formatNumber(trueAzimuth, 0) + Utils.DEGREE_CHAR
					+ " " + Utils.getDirectionCode(trueAzimuth));
		}

		int orientationAdjustment = 0;
		if (orientationHelper != null) {
			orientationAdjustment = orientationHelper.getOrientationAdjustment();
		}

		// update compass image
		if (findViewById(R.id.compassImage) != null) {
			CompassImage compassImage = (CompassImage) findViewById(R.id.compassImage);
			compassImage.setAngle(360 - trueAzimuth - orientationAdjustment);
			compassImage.invalidate();
		}

	}

	// -------------------------------------------------------------------------
	// UPDATE TIME IN TRACK RECORDING MODE
	// -------------------------------------------------------------------------

	/**
	 * Update total and moving time in track recording mode
	 */
	public void updateTime() {

		// update track statistics

		if (gpsService != null && gpsService.getTrackRecorder().isRecording()) {

			if (findViewById(R.id.totalTime) != null) {
				((TextView) findViewById(R.id.totalTime)).setText(Utils.formatInterval(gpsService.getTrackRecorder()
						.getTrack().getTotalTime(), false));
			}

			if (findViewById(R.id.movingTime) != null) {
				((TextView) findViewById(R.id.movingTime)).setText(Utils.formatInterval(gpsService.getTrackRecorder()
						.getTrack().getMovingTime(), false));
			}

		}

	}

	/**
	 * Intercepting Back button click to prevent accidental exit in track
	 * recording mode
	 */
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {

		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (gpsService != null) {

				if (gpsService.getTrackRecorder().isRecording()) {
					Toast.makeText(MainActivity.this, R.string.stop_track_recording, Toast.LENGTH_SHORT).show();
					return true;
				}

				if (gpsService.getScheduledTrackRecorder().isRecording()) {
					Toast.makeText(MainActivity.this, R.string.stop_scheduled_track_recording, Toast.LENGTH_SHORT)
							.show();
					return true;
				}

			}

		}

		return super.onKeyUp(keyCode, event);
	}

	/**
	 * Copy application database to sd card
	 */
	private void backupDatabase() {

		if (gpsService.getTrackRecorder().isRecording()) {
			Toast.makeText(MainActivity.this, R.string.stop_track_recording, Toast.LENGTH_SHORT).show();
			return;
		}

		try {

			File data = Environment.getDataDirectory();

			if (myApp.getExternalStorageWriteable()) {

				String currentDBPath = "/data/com.aripuca.tracker/databases/" + Constants.APP_NAME + ".db";

				String dateStr = (String) DateFormat.format("yyyyMMdd_kkmmss", new Date());

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

		if (gpsService.getTrackRecorder().isRecording()) {
			Toast.makeText(MainActivity.this, R.string.stop_track_recording, Toast.LENGTH_SHORT).show();
			return;
		}

		// show "select a file" dialog
		File importFolder = new File(myApp.getAppDir() + "/backup/");
		final String importFiles[] = importFolder.list();

		if (importFiles == null || importFiles.length == 0) {
			Toast.makeText(MainActivity.this, R.string.source_folder_empty, Toast.LENGTH_SHORT).show();
			return;
		}

		importDatabaseFileName = importFiles[0];

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.select_file);
		builder.setSingleChoiceItems(importFiles, 0, new DialogInterface.OnClickListener() {
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
						myApp.getAppDir() + "/backup/" + importDatabaseFileName, null, SQLiteDatabase.OPEN_READONLY);

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

					Toast.makeText(MainActivity.this, getString(R.string.restore_completed), Toast.LENGTH_SHORT).show();

				}

			} catch (Exception e) {

				Log.e(Constants.TAG, e.getMessage());

				myApp.setDatabase();

				Toast.makeText(MainActivity.this, getString(R.string.restore_error) + ": " + e.getMessage(),
						Toast.LENGTH_LONG).show();

			}

		}
	};

	/**
	 * Updating UI every second
	 */
	private Handler updateTimeHandler = new Handler();
	private Runnable updateTimeTask = new Runnable() {
		@Override
		public void run() {
			updateTime();
			updateTimeHandler.postDelayed(this, 200);
		}
	};

	/**
	 * GPS service connection
	 */
	private GpsService gpsService;
	private GpsServiceConnection gpsServiceConnection;
	private boolean isServiceBound;

	private class GpsServiceConnection implements ServiceConnection {

		public void onServiceConnected(ComponentName className, IBinder service) {

			Log.d(Constants.TAG, "onServiceConnected " + this.toString());

			gpsService = ((GpsService.LocalBinder) service).getService();

			gpsServiceBoundCallback();

			isServiceBound = true;

		}

		public void onServiceDisconnected(ComponentName className) {
			isServiceBound = false;
		}

	};

	/**
	 * called when gpsService bound
	 */
	private void gpsServiceBoundCallback() {

		if (gpsService.getTrackRecorder().isRecording()) {

			this.replaceDynamicView(R.layout.main_tracking);

			// change "Record" button text with "Stop"
			((Button) findViewById(R.id.trackRecordingButton)).setText(getString(R.string.stop));

			// show pause/resume button
			((Button) findViewById(R.id.pauseResumeTrackButton)).setVisibility(View.VISIBLE);

			// enabling control buttons
			this.enableControlButtons();

		} else {

			this.replaceDynamicView(R.layout.main_idle2);

			// start location updates after service bound if not recording track
			gpsService.startLocationUpdates();

		}

		gpsService.startSensorUpdates();

		Location location = gpsService.getCurrentLocation();
		// new location was received by the service when activity was paused
		if (location != null) {

			currentLocation = location;

			updateActivity();

			updateSunriseSunset();
		}

	}

	private void bindGpsService() {

		// GpsService.LocalBinder.flushPendingCommands();

		if (!bindService(new Intent(MainActivity.this, GpsService.class), gpsServiceConnection, 0)) {
			Toast.makeText(MainActivity.this, "System error: Can't connect to GPS service", Toast.LENGTH_SHORT).show();
		}

	}

	private void unbindGpsService() {

		if (this.isServiceBound) {

			Log.v(Constants.TAG, "!!! isGpsServiceBound");

			// detach our existing connection
			unbindService(gpsServiceConnection);

			this.isServiceBound = false;
		}

		gpsService = null;

	}

	/**
	 * Start/stop scheduled track recording
	 */
	private void startStopScheduledTrackRecording() {

		if (gpsService == null) {
			Toast.makeText(MainActivity.this, R.string.gps_service_not_connected, Toast.LENGTH_SHORT).show();
			return;
		}

		// testing location updates scheduler
		if (!gpsService.getScheduledTrackRecorder().isRecording()) {

			gpsService.startScheduler();

			Toast.makeText(MainActivity.this, R.string.scheduled_recording_started, Toast.LENGTH_SHORT).show();

		} else {

			// manually stop scheduled location updates
			gpsService.stopScheduler();

			Toast.makeText(MainActivity.this, R.string.scheduled_recording_stopped, Toast.LENGTH_SHORT).show();
		}

	}

}
