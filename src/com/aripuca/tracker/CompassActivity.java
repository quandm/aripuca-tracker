package com.aripuca.tracker;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.widget.TextView;
import android.widget.Toast;

import com.aripuca.tracker.service.AppService;
import com.aripuca.tracker.service.AppServiceConnection;
import com.aripuca.tracker.utils.AppLog;
import com.aripuca.tracker.utils.MapUtils;
import com.aripuca.tracker.utils.Utils;
import com.aripuca.tracker.view.BubbleSurfaceView;
import com.aripuca.tracker.view.CompassImage;
import com.google.analytics.tracking.android.EasyTracker;

public class CompassActivity extends Activity implements OnTouchListener {

	private Location currentLocation;

	private float downX, downY, upX, upY;

	protected BubbleSurfaceView bubbleView;

	/**
	 * Reference to Application object
	 */
	private App app;

	/**
	 * declination
	 */
	private float declination;

	/**
	 * reference to compass image
	 */
	private CompassImage compass;

	/**
	 * vibration flag
	 */
	private boolean vibrationOn;

	/**
	 * Service connection object
	 */
	private AppServiceConnection serviceConnection;

	/**
	 * Location updates broadcast receiver
	 */
	protected BroadcastReceiver locationBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

			Bundle bundle = intent.getExtras();

			currentLocation = (Location) bundle.getParcelable("location");

			// location here is required only for calculating declination
			// after first location received, no matter the accuracy - turn off
			// gps updates

			long now = System.currentTimeMillis();

			declination = MapUtils.getDeclination(currentLocation, now);

			// stop location updates when not recording track
			if (!serviceConnection.getService().getTrackRecorder().isRecording()) {
				serviceConnection.getService().stopLocationUpdates();
			}

		}
	};

	/**
	 * Compass updates broadcast receiver
	 */
	protected BroadcastReceiver compassBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

			Bundle bundle = intent.getExtras();

			updateCompass(bundle.getFloat("azimuth"));

			float roll, pitch;
			//
			int rotation = Utils.getDeviceRotation(CompassActivity.this);
			if (rotation == 90) {
				roll = bundle.getFloat("pitch");
				pitch = -bundle.getFloat("roll");
			} else if (rotation == 270) {
				roll = -bundle.getFloat("pitch");
				pitch = bundle.getFloat("roll");
			} else {
				roll = bundle.getFloat("roll");
				pitch = bundle.getFloat("pitch");
			}

			bubbleView.setSensorData(bundle.getFloat("azimuth"), roll, pitch);

		}
	};

	/**
	 * Initialize the activity
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		setContentView(R.layout.compass);

		// set activity orientation to default device orientation
		// required for level and compass to work correctly

		// setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		// reference to application object
		app = ((App) getApplicationContext());

		// vibrate or not?
		vibrationOn = app.getPreferences().getBoolean("compass_vibration", true);

		if (findViewById(R.id.bubbleSurfaceView) != null) {
			bubbleView = (BubbleSurfaceView) findViewById(R.id.bubbleSurfaceView);
		}

		serviceConnection = new AppServiceConnection(this, appServiceConnectionCallback);

		// magnetic north compass
		if (findViewById(R.id.compass) != null) {

			compass = (CompassImage) findViewById(R.id.compass);

			compass.setOnTouchListener(this);
			// compass.setOnLongClickListener(resetCompass);
		}

		if (findViewById(R.id.azimuth) != null) {
			((TextView) findViewById(R.id.azimuth)).setOnLongClickListener(resetCompass);
		}

	}
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onStart()
	 */
	@Override
	protected void onStart() {
		super.onStart();
		
		EasyTracker.getInstance(this).activityStart(this);		
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onStop()
	 */
	@Override
	protected void onStop() {
		super.onStop();
		
		EasyTracker.getInstance(this).activityStop(this);
	}
	

	private Runnable appServiceConnectionCallback = new Runnable() {

		@Override
		public void run() {

			if (serviceConnection == null) {
				return;
			}

			AppService appService = serviceConnection.getService();

			if (appService == null) {
				Toast.makeText(CompassActivity.this, R.string.gps_service_not_connected, Toast.LENGTH_SHORT).show();
				AppLog.e(CompassActivity.this, "appServiceConnectionCallback: AppService not available");
				return;
			}

			if (!appService.isListening()) {

				// location updates stopped at this time, so let's start them
				appService.startLocationUpdates();

			} else {

				// keep listening for location updates
				appService.setGpsInUse(true);
			}

			// this activity requires compass data
			appService.startSensorUpdates();

			// let's not wait for LocationListener to receive updates and get last known location 
			currentLocation = appService.getCurrentLocation();
		}
	};

	/**
	 * 
	 */
	private OnLongClickListener resetCompass = new OnLongClickListener() {

		@Override
		public boolean onLongClick(View v) {

			if (compass != null) {
				compass.setAngle(0);
				compass.invalidate();
			}

			return true;
		}

	};

	@Override
	public void onResume() {

		super.onResume();

		bubbleView.resume();

		if (findViewById(R.id.compassView) != null) {
			findViewById(R.id.compassView).setKeepScreenOn(app.getPreferences().getBoolean("wake_lock", true));
		}

		// registering receiver for compass updates
		registerReceiver(compassBroadcastReceiver, new IntentFilter(Constants.ACTION_COMPASS_UPDATES));

		// registering receiver for location updates
		registerReceiver(locationBroadcastReceiver, new IntentFilter(Constants.ACTION_LOCATION_UPDATES));

		this.serviceConnection.bindAppService();

	}

	@Override
	public void onPause() {

		bubbleView.pause();

		unregisterReceiver(compassBroadcastReceiver);
		unregisterReceiver(locationBroadcastReceiver);

		// stop location updates when not recording track
		if (serviceConnection.getService() != null) {

			// at this point we most likely received one location update and
			// already stopped listening
			if (!serviceConnection.getService().getTrackRecorder().isRecording()) {
				serviceConnection.getService().stopLocationUpdates();
			}

			serviceConnection.getService().stopSensorUpdates();

		}

		this.serviceConnection.unbindAppService();

		super.onPause();

	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {

		switch (event.getAction()) {

			case MotionEvent.ACTION_DOWN:
				downX = event.getX();
				downY = event.getY();
				return true;

			case MotionEvent.ACTION_MOVE:

				upX = event.getX();
				upY = event.getY();

				double downR = Math.atan2(v.getHeight() / 2 - downY, downX - v.getWidth() / 2);
				int angle1 = (int) Math.toDegrees(downR);

				double upR = Math.atan2(v.getHeight() / 2 - upY, upX - v.getWidth() / 2);
				int angle2 = (int) Math.toDegrees(upR);

				this.rotateCompass(angle1 - angle2);

				if (vibrationOn) {
					app.getVibrator().vibrate(5);
				}

				// update starting point for next move event
				downX = upX;
				downY = upY;

				return true;

		}

		return false;

	}

	/**
	 * rotate compass bezel
	 */
	private void rotateCompass(float angle) {

		// magnetic north compass
		if (compass != null) {
			compass.setAngle(compass.getAngle() + angle);
			compass.invalidate();
		}

	}

	/**
	 * Update compass image and azimuth text
	 */
	public void updateCompass(float azimuth) {

		boolean trueNorth = app.getPreferences().getBoolean("true_north", true);
		boolean showMagnetic = app.getPreferences().getBoolean("show_magnetic", true);

		float rotation = 0;

		// are we taking declination into account?
		if (!trueNorth || currentLocation == null) {
			declination = 0;
		}

		// magnetic north to true north, compensate for device's physical
		// rotation
		rotation = getAzimuth(azimuth + declination + Utils.getDeviceRotation(this));

		if (findViewById(R.id.azimuth) != null) {
			((TextView) findViewById(R.id.azimuth)).setText(Utils.formatNumber(rotation, 0) + Utils.DEGREE_CHAR + " "
					+ getString(Utils.getCardinalPoint(rotation)));
		}

		// true north compass
		if (findViewById(R.id.compassNeedle) != null) {

			CompassImage compassNeedle = (CompassImage) findViewById(R.id.compassNeedle);

			if (compassNeedle.getVisibility() == View.VISIBLE) {
				compassNeedle.setAngle(360 - rotation);
				compassNeedle.invalidate();
			}
		}

		// magnetic north compass
		if (findViewById(R.id.compassNeedleMagnetic) != null) {

			CompassImage compassNeedleMagnetic = (CompassImage) findViewById(R.id.compassNeedleMagnetic);

			if (showMagnetic) {

				if (compassNeedleMagnetic.getVisibility() != View.VISIBLE) {
					compassNeedleMagnetic.setVisibility(View.VISIBLE);
				}

				compassNeedleMagnetic.setAngle(360 - rotation + declination);
				compassNeedleMagnetic.setAlpha(50);
				compassNeedleMagnetic.invalidate();

			} else {
				compassNeedleMagnetic.setVisibility(View.INVISIBLE);
			}

		}

	}

	protected float getAzimuth(float az) {

		if (az > 360) {
			return az - 360;
		}

		return az;

	}

	// //////////////////////////////////////////////////////////////////////////////////////////////

}
