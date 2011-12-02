package com.aripuca.tracker.service;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.aripuca.tracker.MyApp;
import com.aripuca.tracker.app.Constants;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

public class GpsService extends Service {

	// private MyApp myApp;

	private LocationManager locationManager;

	private SensorManager sensorManager;

	public boolean onSchedule;

	private static boolean running = false;

	private MyApp myApp;

	/**
	 * waypoint track recording start time
	 */
	private long wptStartTime;
	private long wptGpsFixWaitTimeStart;

	private long wptGpsFixWaitTime;
	private int wptRequestInterval;
	private int wptMinAccuracy;
	private int wptStopRecordingAfter;

	/**
	 * defines a listener that responds to location updates
	 */
	private LocationListener locationListener = new LocationListener() {

		// Called when a new location is found by the network location provider.
		@Override
		public void onLocationChanged(Location location) {

			Log.v(Constants.TAG, "locationListener");
			
			// let's broadcast location data to any activity waiting for updates
			Intent intent = new Intent("com.aripuca.tracker.LOCATION_UPDATES_ACTION");

			Bundle bundle = new Bundle();
			bundle.putInt("location_provider", Constants.GPS_PROVIDER);
			bundle.putParcelable("location", location);

			intent.putExtras(bundle);

			sendBroadcast(intent);

		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {}

		@Override
		public void onProviderEnabled(String provider) {}

		@Override
		public void onProviderDisabled(String provider) {
			// Toast.makeText(myApp.getMainActivity(), "GPS provider disabled",
			// Toast.LENGTH_SHORT).show();
		}

	};

	/**
	 * defines a listener that responds to location updates
	 */
	private LocationListener schedulerLocationListener = new LocationListener() {

		// Called when a new location is found by the network location provider.
		@Override
		public void onLocationChanged(Location location) {

			Log.v(Constants.TAG, "schedulerLocationListener");
			
			if (location.hasAccuracy() && location.getAccuracy() < wptMinAccuracy) {

				Log.v(Constants.TAG, "Accuracy accepted: " + location.getAccuracy() + " at "
						+ (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")).format(location.getTime()));

				// let's broadcast location data to any activity waiting for
				// updates
				Intent intent = new Intent("com.aripuca.tracker.SCHEDULER_LOCATION_UPDATES_ACTION");

				Bundle bundle = new Bundle();
				bundle.putInt("location_provider", Constants.GPS_PROVIDER);
				bundle.putParcelable("location", location);

				intent.putExtras(bundle);

				sendBroadcast(intent);

				// location of acceptable accuracy received - stop GPS
				stopSchedulerLocationUpdates();
				
				return;

			} else {

				Log.v(Constants.TAG, "Accuracy not accepted: " + location.getAccuracy());

			}

			if (wptGpsFixWaitTimeStart + wptGpsFixWaitTime * 60 * 1000 < SystemClock.uptimeMillis()) {

				// stop trying to receive GPX signal and wait until next
				// session
				stopSchedulerLocationUpdates();
			}

		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {}

		@Override
		public void onProviderEnabled(String provider) {}

		@Override
		public void onProviderDisabled(String provider) {
			// Toast.makeText(myApp.getMainActivity(), "GPS provider disabled",
			// Toast.LENGTH_SHORT).show();
		}

	};

	private SensorEventListener sensorListener = new SensorEventListener() {

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {

		}

		@Override
		public void onSensorChanged(SensorEvent event) {

			// let's broadcast compass data to any activity waiting for updates
			Intent intent = new Intent("com.aripuca.tracker.COMPASS_UPDATES_ACTION");

			// packing azimuth value into bundle
			Bundle bundle = new Bundle();
			bundle.putFloat("azimuth", event.values[0]);
			bundle.putFloat("pitch", event.values[1]);
			bundle.putFloat("roll", event.values[2]);

			intent.putExtras(bundle);

			// broadcasting compass updates
			sendBroadcast(intent);

		}

	};

	// This is the object that receives interactions from clients
	private final IBinder mBinder = new LocalBinder();

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	public class LocalBinder extends Binder {
		public GpsService getService() {
			return GpsService.this;
		}
	}

	/**
	 * Initialize service
	 */
	@Override
	public void onCreate() {

		super.onCreate();

		Log.v(Constants.TAG, "GpsService: onCreate");

		myApp = (MyApp) getApplicationContext();

		// GPS sensor
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		this.startLocationUpdates();

		// orientation sensor
		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

		this.startSensorUpdates();

		running = true;

		this.requestLastKnownLocation();

	}

	/**
	 * is service running?
	 */
	public static boolean isRunning() {
		return running;
	}

	/**
	 * Requesting last location from GPS or Network provider
	 */
	public void requestLastKnownLocation() {

		Location location;
		int locationProvider;

		// get last known location from gps provider
		location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

		if (location != null) {
			locationProvider = Constants.GPS_PROVIDER_LAST;
		} else {
			// let's try network provider
			location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		}

		if (location != null) {

			locationProvider = Constants.NETWORK_PROVIDER_LAST;

			// let's broadcast location data to any activity waiting for updates
			Intent intent = new Intent("com.aripuca.tracker.LOCATION_UPDATES_ACTION");

			Bundle bundle = new Bundle();
			bundle.putInt("location_provider", locationProvider);
			bundle.putParcelable("location", location);

			intent.putExtras(bundle);

			sendBroadcast(intent);

		}

	}

	/**
	 * Service destructor
	 */
	@Override
	public void onDestroy() {

		Log.v(Constants.TAG, "GpsService: onDestroy");

		running = false;

		if (onSchedule) {
			stopScheduler();
		}

		this.stopLocationUpdates();

		this.stopSensorUpdates();

		locationManager = null;
		sensorManager = null;

		super.onDestroy();

	}

	public void startLocationUpdates() {
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
	}

	public void stopLocationUpdates() {
		locationManager.removeUpdates(locationListener);
	}

	public void stopSchedulerLocationUpdates() {
		locationManager.removeUpdates(schedulerLocationListener);
	}
	
	private void startSensorUpdates() {
		sensorManager.registerListener(sensorListener, sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
				SensorManager.SENSOR_DELAY_NORMAL);
	}

	private void stopSensorUpdates() {
		sensorManager.unregisterListener(sensorListener);
	}

	public void startScheduler() {

		// waypoint track settings
		wptRequestInterval = Integer.parseInt(myApp.getPreferences().getString("wpt_request_interval", "10"));
		wptMinAccuracy = Integer.parseInt(myApp.getPreferences().getString("wpt_min_accuracy", "30"));
		wptStopRecordingAfter = Integer.parseInt(myApp.getPreferences().getString("wpt_stop_recording_after", "4"));
		wptGpsFixWaitTime = Integer.parseInt(myApp.getPreferences().getString("wpt_gps_fix_wait_time", "2"));

		onSchedule = true;

		wptStartTime = SystemClock.uptimeMillis();

		Log.v(Constants.TAG, "Scheduler started at: " + (new SimpleDateFormat("HH:mm:ss")).format(wptStartTime));

		locationManager.removeUpdates(locationListener);
		
		schedulerHandler.postDelayed(schedulerTask, 5000);

	}

	public void stopScheduler() {
		
		Log.v(Constants.TAG, "Scheduler stopped");

		onSchedule = false;

		locationManager.removeUpdates(schedulerLocationListener);
		
		schedulerHandler.removeCallbacks(schedulerTask);
		
		this.startLocationUpdates();

	}

	/**
	 * Updating UI every second
	 */
	private Handler schedulerHandler = new Handler();
	private Runnable schedulerTask = new Runnable() {
		@Override
		public void run() {

			// new request for location started
			wptGpsFixWaitTimeStart = SystemClock.uptimeMillis();

			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, schedulerLocationListener);

			// has recording time limit been reached?
			if (wptStopRecordingAfter != 0
					&& wptStartTime + wptStopRecordingAfter * 60 * 60 * 1000 < SystemClock.uptimeMillis()) {
				
				stopScheduler();
				
				return;
			}

			schedulerHandler.postDelayed(this, wptRequestInterval * 60 * 1000);

		}

	};

}
