package com.aripuca.tracker;

import com.aripuca.tracker.R;
import com.aripuca.tracker.app.Constants;
import com.aripuca.tracker.service.GpsService;
import com.aripuca.tracker.util.Utils;
import com.aripuca.tracker.view.CompassImage;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class CompassActivity extends Activity {

	private Location currentLocation;

	/**
	 * Location updates broadcast receiver
	 */
	protected BroadcastReceiver locationBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

			Bundle bundle = intent.getExtras();
			
			currentLocation = (Location) bundle.getParcelable("location");

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
		}
	};

	/**
	 * Reference to Application object
	 */
	private MyApp myApp;

	private long declinationLastUpdate = 0;

	private float declination;

	/**
	 * Initialize the activity
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		// reference to application object
		myApp = ((MyApp) getApplicationContext());

		setContentView(R.layout.compass);
		
	}

	@Override
	public void onResume() {

		super.onResume();

		// registering receiver for compass updates
		registerReceiver(compassBroadcastReceiver, new IntentFilter(Constants.ACTION_COMPASS_UPDATES));
		
		// registering receiver for location updates
		registerReceiver(locationBroadcastReceiver, new IntentFilter(Constants.ACTION_LOCATION_UPDATES));
		
		// staring compass updates in GpsService
//		Intent intent = new Intent(Constants.ACTION_START_SENSOR_UPDATES);
//		sendBroadcast(intent);
		
		// bind to GPS service
		// once bound gpsServiceBoundCallback will be called
		this.bindGpsService();
		
	}

	
	@Override
	public void onPause() {

		unregisterReceiver(compassBroadcastReceiver);
		unregisterReceiver(locationBroadcastReceiver);

		this.unbindGpsService();
		
		super.onPause();
		
	}

	/**
	 * Update compass image and azimuth text
	 */
	public void updateCompass(float azimuth) {

		boolean trueNorth = myApp.getPreferences().getBoolean("true_north", true);
		boolean showMagnetic = myApp.getPreferences().getBoolean("show_magnetic", true);

		float rotation = 0;

		if (trueNorth && currentLocation != null) {

			long now = System.currentTimeMillis();

			// let's request declination every 15 minutes, not every compass update
			if (now - declinationLastUpdate > 15 * 60 * 1000) {
				declination = Utils.getDeclination(currentLocation, now);
				Log.d(Constants.TAG, "declination update: " + declination);
				declinationLastUpdate = now;
			}

		} else {
			declination = 0;
		}

		// magnetic north to true north
		rotation = getAzimuth(azimuth + declination);

		if (findViewById(R.id.azimuth) != null) {
			((TextView) findViewById(R.id.azimuth)).setText(Utils.formatNumber(rotation, 0)
					+ Utils.DEGREE_CHAR + " "
					+ Utils.getDirectionCode(rotation));
		}

		// true north compass
		if (findViewById(R.id.compassImage) != null) {

			CompassImage compassImage = (CompassImage) findViewById(R.id.compassImage);

			if (compassImage.getVisibility() == View.VISIBLE) {
				compassImage.setAngle(360 - rotation);
				compassImage.invalidate();
			}
		}

		// magnetic north compass
		if (findViewById(R.id.compassImage2) != null) {

			CompassImage compassImage2 = (CompassImage) findViewById(R.id.compassImage2);

			if (showMagnetic) {

				if (compassImage2.getVisibility() != View.VISIBLE) {
					compassImage2.setVisibility(View.VISIBLE);
				}

				compassImage2.setAngle(360 - rotation + declination);
				compassImage2.setAlpha(50);
				compassImage2.invalidate();

			} else {
				compassImage2.setVisibility(View.INVISIBLE);
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
	/**
	 * GPS service connection
	 */
	private GpsService gpsService;
	private boolean isGpsServiceBound = false;
	private ServiceConnection gpsServiceConnection = new ServiceConnection() {

		public void onServiceConnected(ComponentName className, IBinder service) {
			gpsService = ((GpsService.LocalBinder) service).getService();
			gpsServiceBoundCallback();
			isGpsServiceBound = true;
		}

		public void onServiceDisconnected(ComponentName className) {
			isGpsServiceBound = false;
		}
	};

	private void bindGpsService() {
		if (!bindService(new Intent(this, GpsService.class), gpsServiceConnection, Context.BIND_AUTO_CREATE)) {
			Toast.makeText(this, "Can't connect to GPS service", Toast.LENGTH_SHORT).show();
		}
	}

	private void unbindGpsService() {
		
		if (isGpsServiceBound) {
			// Detach our existing connection.
			unbindService(gpsServiceConnection);
			isGpsServiceBound = false;
		}
		
		gpsService = null;
		
	}

	/**
	 * called when gpsService bound
	 */
	private void gpsServiceBoundCallback() {

		// this activity is started by MainActivity which is always
		// listening for location updates

		// by setting gpsInUse to true we insure that listening will not stop in
		// GpsService.stopLocationUpdatesThread
		gpsService.setGpsInUse(true);
		
	}

	// //////////////////////////////////////////////////////////////////////////////////////////////
	

}
