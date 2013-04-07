package com.aripuca.tracker;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
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
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.aripuca.tracker.db.Waypoint;
import com.aripuca.tracker.db.Waypoints;
import com.aripuca.tracker.dialog.QuickHelpDialog;
import com.aripuca.tracker.service.AppService;
import com.aripuca.tracker.service.AppServiceConnection;
import com.aripuca.tracker.track.Track;
import com.aripuca.tracker.utils.AppLog;
import com.aripuca.tracker.utils.ContainerCarousel;
import com.aripuca.tracker.utils.MapUtils;
import com.aripuca.tracker.utils.Utils;
import com.aripuca.tracker.view.CompassImage;
import com.thirdparty.SunriseSunset;

/**
 * main application activity
 */
public class MainActivity extends Activity {

	/**
	 * Reference to Application object
	 */
	private App app;

	private String importDatabaseFileName;

	private Handler mainHandler = new Handler();

	private long declinationLastUpdate = 0;

	private long compassLastUpdate = 0;
	
	/**
	 * location received from AppService
	 */
	private Location currentLocation;

	private String speedUnit;
	private String distanceUnit;
	private String elevationUnit;
	private int coordUnit;

	/**
	 * Service connection object
	 */
	private AppServiceConnection serviceConnection;

	/**
	 * location updates broadcast receiver
	 */
	protected BroadcastReceiver locationBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

			Bundle bundle = intent.getExtras();

			currentLocation = (Location) bundle.getParcelable("location");

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

			// for some reason currentLocation.getTime is one day off
			// this is leap year bug on Nexus S and other Samsund devices
			// fixed in ICS 4.0.4
			if (fixAge < 0) {
				fixAge = Utils.ONE_DAY - Math.abs(fixAge);
				AppLog.d(this, "MainActivity: location time is one day ahead");
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

			if (app == null) {
				return;
			}

			Bundle bundle = intent.getExtras();

			// update compass every second
			long now = System.currentTimeMillis();
			if (now - compassLastUpdate > 500) {
				updateCompass(bundle.getFloat("azimuth"));
				compassLastUpdate = now;
			}

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

			AppService appService = serviceConnection.getService();

			if (appService != null) {

				if (appService.getTrackRecorder().isRecording()) {
					stopTracking();
					updateSunriseSunset();
				} else {
					startTracking();
				}
			}

			return true;
		}

	};

	private OnClickListener trackRecordingButtonClick = new OnClickListener() {
		@Override
		public void onClick(View v) {

			AppService appService = serviceConnection.getService();

			if (appService != null) {
				if (appService.getTrackRecorder().isRecording()) {
					Toast.makeText(MainActivity.this, R.string.press_and_hold_to_stop, Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(MainActivity.this, R.string.press_and_hold_to_start, Toast.LENGTH_SHORT).show();
				}
			}

		}

	};

	/**
	 * 
	 */
	private OnClickListener pauseResumeTrackListener = new OnClickListener() {

		@Override
		public void onClick(View v) {

			AppService appService = serviceConnection.getService();

			if (appService == null) {
				return;
			}

			if (appService.getTrackRecorder().isRecordingPaused()) {

				((Button) findViewById(R.id.pauseResumeTrackButton)).setText(getString(R.string.pause));

				appService.getTrackRecorder().resume();

				Toast.makeText(MainActivity.this, R.string.recording_resumed, Toast.LENGTH_SHORT).show();

			} else {

				((Button) findViewById(R.id.pauseResumeTrackButton)).setText(getString(R.string.resume));

				appService.getTrackRecorder().pause();

				Toast.makeText(MainActivity.this, R.string.recording_paused, Toast.LENGTH_SHORT).show();

			}
		}
	};

	/**
	 * Adding hidden data to application preferences
	 */
	private void initializeHiddenPreferences() {

		SharedPreferences.Editor editor = app.getPreferences().edit();

		if (!app.getPreferences().contains("speed_container_id")) {
			editor.putInt("speed_container_id", 0);
			editor.commit();
		}

		if (!app.getPreferences().contains("time_container_id")) {
			editor.putInt("time_container_id", 0);
		}

		if (!app.getPreferences().contains("distance_container_id")) {
			editor.putInt("distance_container_id", 0);
			editor.commit();
		}

		if (!app.getPreferences().contains("elevation_container_id")) {
			editor.putInt("elevation_container_id", 0);
			editor.commit();
		}

		if (!app.getPreferences().contains("coordinates_container_id")) {
			editor.putInt("coordinates_container_id", 0);
			editor.commit();
		}

		if (!app.getPreferences().contains("trackpoints_sort")) {
			editor.putInt("trackpoints_sort", 0);
			editor.commit();
		}

		speedContainerCarousel.setCurrentContainerId(app.getPreferences().getInt("speed_container_id", 0));
		timeContainerCarousel.setCurrentContainerId(app.getPreferences().getInt("time_container_id", 0));
		distanceContainerCarousel.setCurrentContainerId(app.getPreferences().getInt("distance_container_id", 0));
		elevationContainerCarousel.setCurrentContainerId(app.getPreferences().getInt("elavation_container_id", 0));
		coordinatesContainerCarousel.setCurrentContainerId(app.getPreferences().getInt("coordinates_container_id", 0));

	}

	private void saveHiddenPreferences() {

		// update preferences
		SharedPreferences.Editor editor = app.getPreferences().edit();

		editor.putInt("speed_container_id", speedContainerCarousel.getCurrentContainerId());
		editor.putInt("time_container_id", timeContainerCarousel.getCurrentContainerId());
		editor.putInt("distance_container_id", distanceContainerCarousel.getCurrentContainerId());
		editor.putInt("elevation_container_id", elevationContainerCarousel.getCurrentContainerId());
		editor.putInt("coordinates_container_id", coordinatesContainerCarousel.getCurrentContainerId());

		editor.commit();

	}

	private Runnable appServiceConnectionCallback = new Runnable() {
		@Override
		public void run() {

			if (serviceConnection == null) {
				return;
			}

			AppService appService = serviceConnection.getService();

			if (appService == null) {
				Toast.makeText(MainActivity.this, R.string.gps_service_not_connected, Toast.LENGTH_SHORT).show();
				return;
			}

			if (appService.getTrackRecorder().isRecording()) {

				replaceDynamicView(R.layout.main_tracking);

				// change "Record" button text with "Stop"
				((Button) findViewById(R.id.trackRecordingButton)).setText(getString(R.string.stop));

				// show pause/resume button
				((Button) findViewById(R.id.pauseResumeTrackButton)).setVisibility(View.VISIBLE);

				// enabling control buttons
				enableControlButtons();

			} else {

				replaceDynamicView(R.layout.main_idle);

				setMainIdleLayoutListeners();

				// start location updates after service bound if not recording
				// track
				appService.startLocationUpdates();

			}

			appService.startSensorUpdates();

			Location location = appService.getCurrentLocation();

			// new location was received by the service when activity was paused
			if (location != null) {

				currentLocation = location;

				updateActivity();

				updateSunriseSunset();
			}

		}
	};

	/**
	 * Initialize the activity
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		Log.i(Constants.TAG, "MainActivity: onCreate");

		// reference to application object
		app = ((App) getApplication());

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

		serviceConnection = new AppServiceConnection(this, appServiceConnectionCallback);

		// start GPS service only if not started
		startAppService();

		// show quick help only when activity first started
		if (savedInstanceState == null) {
			if (app.getPreferences().getBoolean("quick_help", true)) {
				showQuickHelp();
			}
		}

		this.setControlButtonListeners();

	}

	/**
	 * onPause event handler
	 */
	@Override
	protected void onPause() {

		Log.i(Constants.TAG, "MainActivity: onPause");

		unregisterReceiver(compassBroadcastReceiver);
		unregisterReceiver(locationBroadcastReceiver);
		unregisterReceiver(scheduledLocationBroadcastReceiver);

		AppService appService = serviceConnection.getService();

		if (appService != null) {

			if (!this.isFinishing()) {

				// activity will be recreated

				// stop location updates when not recording track
				if (!appService.getTrackRecorder().isRecording()) {

					// stop location updates with delay so next activity will have time to grab GPS sensor
					appService.stopLocationUpdates();
				}

				appService.stopSensorUpdates();

			} else {

				// activity will be destroyed and not recreated

				// stop tracking if active
				if (appService.getTrackRecorder().isRecording()) {
					AppLog.d(this, "MainActivity.onPause: Recording stopped by the system");
					stopTracking();
				}

				if (appService.getScheduledTrackRecorder().isRecording()) {
					AppLog.d(this, "MainActivity.onPause: Scheduled recording stopped by the system");
					appService.stopScheduler();
				}

			}
		}

		// unbind AppService
		serviceConnection.unbindAppService();

		if (this.isFinishing()) {

			// if activity is not going to be recreated - stop application service
			stopAppService();

			// save layout for next session
			this.saveHiddenPreferences();

		}

		this.updateTimeHandler.removeCallbacks(updateTimeTask);

		super.onPause();
	}

	/**
	 * onResume event handler
	 */
	@Override
	protected void onResume() {

		super.onResume();

		Log.i(Constants.TAG, "MainActivity: onResume");

		this.initializeMeasuringUnits();

		this.keepScreenOn();

		// registering receiver for compass updates
		registerReceiver(compassBroadcastReceiver, new IntentFilter(Constants.ACTION_COMPASS_UPDATES));

		// registering receiver for location updates
		registerReceiver(locationBroadcastReceiver, new IntentFilter(Constants.ACTION_LOCATION_UPDATES));

		// registering receiver for time updates
		registerReceiver(scheduledLocationBroadcastReceiver, new IntentFilter(
				Constants.ACTION_SCHEDULED_LOCATION_UPDATES));

		// bind to app service
		// once bound gpsServiceBoundCallback will be called
		serviceConnection.bindAppService();

		// start updating time of tracking every second
		updateTimeHandler.postDelayed(updateTimeTask, 1000);

	}

	/**
	 * onDestroy event handler
	 */
	@Override
	protected void onDestroy() {

		AppLog.d(this, "MainActivity.onDestroy");

		serviceConnection = null;

		app = null;

		super.onDestroy();
	}

	/**
	 * Restoring data saved in onSaveInstanceState
	 */
	private void restoreInstanceState(Bundle savedInstanceState) {

		// Log.i(Constants.TAG, "MainActivity: restoreInstanceState");

		// initializing carousel objects
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
	 * main_idle layout click listeners
	 */
	private void setMainIdleLayoutListeners() {

		// touch on small compass will open compass activity
		LinearLayout compassLayout = (LinearLayout) findViewById(R.id.compassLayout);
		if (compassLayout != null) {
			compassLayout.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					startActivity(new Intent(MainActivity.this, CompassActivity.class));
				}
			});
		}

	}

	/**
	 * preventing phone from sleeping
	 */
	private void keepScreenOn() {

		if (findViewById(R.id.dynamicView) != null) {
			findViewById(R.id.dynamicView).setKeepScreenOn(app.getPreferences().getBoolean("wake_lock", true));
		}

	}

	/**
	 * 
	 */
	private void setContainer(ContainerCarousel carousel) {

		final ViewGroup containerView = (ViewGroup) findViewById(carousel.getResourceId());

		// assigning click event listener to container
		if (containerView != null) {

			// ViewGroup containerView = (ViewGroup) findViewById(carousel.getResourceId());

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

		// in track recording mode the view will be switched to R.layout.main_tracking
		if (resourceId != R.layout.main_idle) {
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
	private void startAppService() {

		// starting GPS listener service
		serviceConnection.startService();

	}

	/**
	 * stop GPS listener service
	 */
	private void stopAppService() {

		serviceConnection.stopService();

	}

	/**
	 * Enable control buttons
	 */
	protected void enableControlButtons() {

		// ((Button) findViewById(R.id.addWaypointButton)).setEnabled(true);
		((Button) findViewById(R.id.trackRecordingButton)).setEnabled(true);
		((Button) findViewById(R.id.pauseResumeTrackButton)).setEnabled(true);

	}

	/**
	 * Disable control buttons
	 */
	protected void disableControlButtons() {

		// ((Button) findViewById(R.id.addWaypointButton)).setEnabled(false);
		((Button) findViewById(R.id.trackRecordingButton)).setEnabled(false);
		((Button) findViewById(R.id.pauseResumeTrackButton)).setEnabled(false);

	}

	/**
	 * start real time track recording
	 */
	private void startTracking() {

		this.initializeMeasuringUnits();

		AppService appService = serviceConnection.getService();

		if (appService == null) {
			Toast.makeText(this, R.string.gps_service_not_connected, Toast.LENGTH_SHORT).show();
			return;
		}

		// Change button label from Record to Stop
		((Button) findViewById(R.id.trackRecordingButton)).setText(getString(R.string.stop));

		// show pause/resume button
		((Button) findViewById(R.id.pauseResumeTrackButton)).setVisibility(View.VISIBLE);

		// enabling pause/resume button
		((Button) findViewById(R.id.pauseResumeTrackButton)).setEnabled(true);

		this.replaceDynamicView(R.layout.main_tracking);

		appService.startTrackRecording();

		Toast.makeText(this, R.string.recording_started, Toast.LENGTH_SHORT).show();

	}

	/**
	 * stop real time track recording
	 */
	private void stopTracking() {

		AppService appService = serviceConnection.getService();

		if (appService == null) {
			Toast.makeText(this, R.string.gps_service_not_connected, Toast.LENGTH_SHORT).show();
			return;
		}

		// disabling pause/resume button
		((Button) findViewById(R.id.pauseResumeTrackButton)).setEnabled(false);

		// hide pause/resume button
		((Button) findViewById(R.id.pauseResumeTrackButton)).setVisibility(View.GONE);

		// change button label from Stop to Record
		((Button) findViewById(R.id.trackRecordingButton)).setText(getString(R.string.record_track));

		appService.stopTrackRecording();

		// switching to initial layout
		this.replaceDynamicView(R.layout.main_idle);

		Toast.makeText(this, R.string.recording_finished, Toast.LENGTH_SHORT).show();

	}

	/**
	 * initialize measuring units with up to date values
	 */
	private void initializeMeasuringUnits() {
		speedUnit = app.getPreferences().getString("speed_units", "kph");
		distanceUnit = app.getPreferences().getString("distance_units", "km");
		elevationUnit = app.getPreferences().getString("elevation_units", "m");
		coordUnit = Integer.parseInt(app.getPreferences().getString("coord_units", "0"));
	}

	/**
	 * add waypoint button listener
	 */
	private OnClickListener addWaypointListener = new OnClickListener() {

		@Override
		public void onClick(View v) {

			if (currentLocation == null) {
				Toast.makeText(MainActivity.this, R.string.waiting_new_fix, Toast.LENGTH_SHORT).show();
				return;
			}

			// let's make reverse geocoder request and then show Add Waypoint
			// dialog
			// address will be inserted as default description for this waypoint

			// disable "add waypoint" button to avoid creating extra dialog on double click
			((Button) findViewById(R.id.addWaypointButton)).setEnabled(false);

			if (app.checkInternetConnection() && app.getPreferences().getBoolean("waypoint_default_description", false)) {

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
		wpLat.setText(Utils.formatCoord(currentLocation.getLatitude()));

		final EditText wpLng = (EditText) layout.findViewById(R.id.waypointLngInputText);
		wpLng.setText(Utils.formatCoord(currentLocation.getLongitude()));

		builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int id) {

				// waypoint title from input dialog
				String titleStr = wpTitle.getText().toString().trim();

				if (titleStr.equals("")) {
					Toast.makeText(MainActivity.this, R.string.waypoint_title_required, Toast.LENGTH_SHORT).show();
					dialog.dismiss();
				}

				Waypoint wp = new Waypoint();
				wp.setTitle(titleStr);
				wp.setDescr(wpDescr.getText().toString().trim());
				wp.setLat(Double.parseDouble(wpLat.getText().toString()));
				wp.setLng(Double.parseDouble(wpLng.getText().toString()));
				wp.setAccuracy(currentLocation.getAccuracy());
				wp.setElevation(currentLocation.getAltitude());
				wp.setTime(currentLocation.getTime());

				AppService appService = serviceConnection.getService();
				// if track recording started assign track_id
				if (appService != null && appService.getTrackRecorder().isRecording()) {
					wp.setTrack_id(appService.getTrackRecorder().getTrack().getTrackId());
				}

				try {
					// insert new waypoint to database
					Waypoints.insert(app.getDatabase(), wp);
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

				// enable "add waypoint" button
				((Button) findViewById(R.id.addWaypointButton)).setEnabled(true);
				dialog.dismiss();
			}
		});

		AlertDialog dialog = builder.create();
		dialog.setCanceledOnTouchOutside(false);
		dialog.show();

	}

	/**
	 * 
	 */
	@Override
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
		MenuItem testLabMenuItem = menu.findItem(R.id.testLabMenuItem);
		testLabMenuItem.setVisible(false);

		// setting icon and title for scheduled track recording menu
		MenuItem scheduledRecordingMenuItem = menu.findItem(R.id.scheduledRecordingMenuItem);

		AppService appService = serviceConnection.getService();

		if (appService != null) {
			if (appService.getScheduledTrackRecorder().isRecording()) {
				scheduledRecordingMenuItem.setTitle(R.string.stop_scheduler);
				scheduledRecordingMenuItem.setIcon(R.drawable.ic_menu_stoprecording);
			} else {
				scheduledRecordingMenuItem.setTitle(R.string.start_scheduler);
				scheduledRecordingMenuItem.setIcon(R.drawable.ic_menu_startrecording);
			}
		}

		return true;
	}

	/**
	 * Process main activity menu
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		Intent intent;
		// Handle item selection
		switch (item.getItemId()) {

			case R.id.compassMenuItem:

				startActivity(new Intent(this, CompassActivity.class));

				return true;

			case R.id.waypointsMenuItem:

				intent = new Intent(this, WaypointsListActivity.class);
				startActivity(intent);

				return true;

			case R.id.tracksMenuItem:

				intent = new Intent(this, TracksTabActivity.class);
				startActivity(intent);

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
				+ App.getVersionName(this));

		TextView messageView = (TextView) layout.findViewById(R.id.message);

		String aboutStr = getString(R.string.about_dialog_message);
		// adding links to "about" text
		final SpannableString s = new SpannableString(aboutStr);
		Linkify.addLinks(s, Linkify.ALL);

		messageView.setText(s);

		// textView.setText(getString(R.string.main_app_title) + " " +
		// getString(R.string.ver)
		// + app.getVersionName(this) + "\n\n" + s);
		messageView.setMovementMethod(LinkMovementMethod.getInstance());

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.about);
		builder.setIcon(R.drawable.icon);
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

		AppService appService = serviceConnection.getService();

		if (currentLocation == null || appService == null) {
			return;
		}

		// ///////////////////////////////////////////////////////////////////
		if (appService.isListening()) {
			// activate buttons if location updates come from GPS
			// ((Button) findViewById(R.id.addWaypointButton)).setEnabled(true);
			((Button) findViewById(R.id.trackRecordingButton)).setEnabled(true);
			this.hideWaitForFixMessage();
		} else {
			// disable recording buttons when waiting for new location
			// ((Button) findViewById(R.id.addWaypointButton)).setEnabled(false);
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

		AppService appService = serviceConnection.getService();

		if (appService == null || !appService.getTrackRecorder().isRecording()) {
			return;
		}

		Track track = appService.getTrackRecorder().getTrack();

		// number of track points recorded
		if (findViewById(R.id.pointsCount) != null) {
			((TextView) findViewById(R.id.pointsCount)).setText(Integer.toString(appService.getTrackRecorder()
					.getPointsCount()));
		}

		// number of track points recorded
		if (findViewById(R.id.segmentsCount) != null) {
			((TextView) findViewById(R.id.segmentsCount)).setText(Integer.toString(appService.getTrackRecorder()
					.getSegmentsCount()));
		}

		// ------------------------------------------------------------------
		// elevation gain
		if (findViewById(R.id.elevationGain) != null) {
			((TextView) findViewById(R.id.elevationGain)).setText(Utils.formatElevation(track.getElevationGain(),
					elevationUnit));
		}

		// elevation loss
		if (findViewById(R.id.elevationLoss) != null) {
			((TextView) findViewById(R.id.elevationLoss)).setText(Utils.formatElevation(track.getElevationLoss(),
					elevationUnit));
		}

		// max elevation
		if (findViewById(R.id.maxElevation) != null) {
			((TextView) findViewById(R.id.maxElevation)).setText(Utils.formatElevation(track.getMaxElevation(),
					elevationUnit));
		}

		// min elevation
		if (findViewById(R.id.minElevation) != null) {
			((TextView) findViewById(R.id.minElevation)).setText(Utils.formatElevation(track.getMinElevation(),
					elevationUnit));
		}
		// ------------------------------------------------------------------

		// average speed
		if (findViewById(R.id.averageSpeed) != null) {
			((TextView) findViewById(R.id.averageSpeed)).setText(Utils.formatSpeed(track.getAverageSpeed(), speedUnit));
		}

		// average moving speed
		if (findViewById(R.id.averageMovingSpeed) != null) {
			((TextView) findViewById(R.id.averageMovingSpeed)).setText(Utils.formatSpeed(track.getAverageMovingSpeed(),
					speedUnit));
		}

		// max speed
		if (findViewById(R.id.maxSpeed) != null) {
			((TextView) findViewById(R.id.maxSpeed)).setText(Utils.formatSpeed(track.getMaxSpeed(), speedUnit));
		}

		// acceleration
		if (findViewById(R.id.acceleration) != null) {

			// let's display last non-zero acceleration
			((TextView) findViewById(R.id.acceleration)).setText(Utils.formatNumber(track.getAcceleration(), 2));

		}

		// average pace
		if (findViewById(R.id.averagePace) != null) {
			((TextView) findViewById(R.id.averagePace)).setText(Utils.formatPace(track.getAverageSpeed(), speedUnit));
		}

		// average moving pace
		if (findViewById(R.id.averageMovingPace) != null) {
			((TextView) findViewById(R.id.averageMovingPace)).setText(Utils.formatPace(track.getAverageMovingSpeed(),
					speedUnit));
		}

		// max pace
		if (findViewById(R.id.maxPace) != null) {
			((TextView) findViewById(R.id.maxPace)).setText(Utils.formatPace(track.getMaxSpeed(), speedUnit));
		}

		// total distance
		if (findViewById(R.id.distance) != null) {
			((TextView) findViewById(R.id.distance)).setText(Utils.formatDistance(track.getDistance(), distanceUnit));
		}

		if (findViewById(R.id.distanceUnit) != null) {
			((TextView) findViewById(R.id.distanceUnit)).setText(Utils.getLocalizedDistanceUnit(this,
					track.getDistance(), distanceUnit));
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

		float trueAzimuth = 0;

		// true or magnetic north?
		boolean trueNorth = app.getPreferences().getBoolean(getString(R.string.settings_true_north), true);

		// let's not request declination on every compass update
		float declination = 0;
		if (trueNorth && currentLocation != null) {
			long now = System.currentTimeMillis();
			// let's request declination every 15 minutes, not every compass
			// update
			if (now - declinationLastUpdate > 15 * 60 * 1000) {
				declination = MapUtils.getDeclination(currentLocation, now);
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
					+ " " + getString(Utils.getCardinalPoint(trueAzimuth)));
		}

		// update compass image
		if (findViewById(R.id.compassImage) != null) {

			CompassImage compassImage = (CompassImage) findViewById(R.id.compassImage);

			AppLog.d(MainActivity.this, "Device rotation: " + Utils.getDeviceRotation(this));
			AppLog.d(MainActivity.this, "TruAzimuth: " + trueAzimuth);

			compassImage.setAngle(360 - trueAzimuth - Utils.getDeviceRotation(this));

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

		AppService appService = serviceConnection.getService();

		if (appService != null && appService.getTrackRecorder().isRecording()) {

			if (findViewById(R.id.totalTime) != null) {
				((TextView) findViewById(R.id.totalTime)).setText(Utils.formatInterval(appService.getTrackRecorder()
						.getTrack().getTotalTime(), false));
			}

			if (findViewById(R.id.movingTime) != null) {
				((TextView) findViewById(R.id.movingTime)).setText(Utils.formatInterval(appService.getTrackRecorder()
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

		AppService appService = serviceConnection.getService();

		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (appService != null) {

				if (appService.getTrackRecorder().isRecording()) {
					Toast.makeText(MainActivity.this, R.string.stop_track_recording, Toast.LENGTH_SHORT).show();
					return true;
				}

				if (appService.getScheduledTrackRecorder().isRecording()) {
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

		AppService appService = serviceConnection.getService();

		if (appService == null || appService.getTrackRecorder().isRecording()) {
			Toast.makeText(MainActivity.this, R.string.stop_track_recording, Toast.LENGTH_SHORT).show();
			return;
		}

		try {

			File data = Environment.getDataDirectory();

			if (app.getExternalStorageWriteable()) {

				String currentDBPath = Constants.PATH_DB + Constants.APP_NAME + ".db";

				String dateStr = (String) DateFormat.format("yyyyMMdd_kkmmss", new Date());

				File currentDB = new File(data, currentDBPath);
				File backupDB = new File(app.getAppDir() + "/" + Constants.PATH_BACKUP + "/" + dateStr + ".db");

				if (currentDB.exists()) {

					FileInputStream fis = new FileInputStream(currentDB);
					FileOutputStream fos = new FileOutputStream(backupDB);

					FileChannel src = fis.getChannel();
					FileChannel dst = fos.getChannel();
					dst.transferFrom(src, 0, src.size());

					src.close();
					dst.close();
					fis.close();
					fos.close();

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

		AppService appService = serviceConnection.getService();

		if (appService == null || appService.getTrackRecorder().isRecording()) {
			Toast.makeText(MainActivity.this, R.string.stop_track_recording, Toast.LENGTH_SHORT).show();
			return;
		}

		// show "select a file" dialog
		File importFolder = new File(app.getAppDir() + "/" + Constants.PATH_BACKUP + "/");
		final String importFiles[] = importFolder.list();

		if (importFiles == null || importFiles.length == 0) {
			Toast.makeText(MainActivity.this, R.string.source_folder_empty, Toast.LENGTH_SHORT).show();
			return;
		}

		importDatabaseFileName = importFiles[0];

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.select_file);
		builder.setSingleChoiceItems(importFiles, 0, new DialogInterface.OnClickListener() {
			@Override
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
				SQLiteDatabase db = SQLiteDatabase.openDatabase(app.getAppDir() + "/" + Constants.PATH_BACKUP + "/"
						+ importDatabaseFileName, null, SQLiteDatabase.OPEN_READONLY);

				// check version compatibility
				// only same version of the db can be restored
				if (app.getDatabase().getVersion() != db.getVersion()) {
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
			app.getDatabase().close();

			try {

				File data = Environment.getDataDirectory();

				if (app.getExternalStorageWriteable()) {

					String restoreDBPath = app.getAppDir() + "/" + Constants.PATH_BACKUP + "/" + importDatabaseFileName;

					File restoreDB = new File(restoreDBPath);
					File currentDB = new File(data, Constants.PATH_DB + Constants.APP_NAME + ".db");

					FileInputStream fis = new FileInputStream(restoreDB);
					FileOutputStream fos = new FileOutputStream(currentDB);

					FileChannel src = fis.getChannel();
					FileChannel dst = fos.getChannel();

					dst.transferFrom(src, 0, src.size());

					src.close();
					dst.close();
					fis.close();
					fos.close();

					app.setDatabase();

					Toast.makeText(MainActivity.this, getString(R.string.restore_completed), Toast.LENGTH_SHORT).show();

				}

			} catch (Exception e) {

				Log.e(Constants.TAG, e.getMessage());

				app.setDatabase();

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
			updateTimeHandler.postDelayed(this, 1000);
		}
	};

	/**
	 * Start/stop scheduled track recording
	 */
	private void startStopScheduledTrackRecording() {

		AppService appService = serviceConnection.getService();

		if (appService == null) {
			Toast.makeText(MainActivity.this, R.string.gps_service_not_connected, Toast.LENGTH_SHORT).show();
			return;
		}

		// testing location updates scheduler
		if (!appService.getScheduledTrackRecorder().isRecording()) {

			appService.startScheduler();

			Toast.makeText(MainActivity.this, R.string.scheduled_recording_started, Toast.LENGTH_SHORT).show();

		} else {

			// manually stop scheduled location updates
			appService.stopScheduler();

			Toast.makeText(MainActivity.this, R.string.scheduled_recording_stopped, Toast.LENGTH_SHORT).show();
		}

	}

}
