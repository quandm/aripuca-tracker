package com.aripuca.tracker.service;

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
import android.util.Log;

public class GpsService extends Service {

//	private MyApp myApp;

	private LocationManager locationManager;

	private SensorManager sensorManager;
	
	private boolean onSchedule = false;

	private static boolean running = false;

	/**
	 * defines a listener that responds to location updates
	 */
	private LocationListener locationListener = new LocationListener() {

		// Called when a new location is found by the network location provider.
		@Override
		public void onLocationChanged(Location location) {

			if (onSchedule) {
			
				if (location.getAccuracy()<50) {
					
					// let's broadcast location data to any activity waiting for updates
					Intent intent = new Intent("com.aripuca.tracker.SCHEDULER_LOCATION_UPDATES_ACTION");

					Bundle bundle = new Bundle();
					bundle.putInt("location_provider", Constants.GPS_PROVIDER);
					bundle.putParcelable("location", location);

					intent.putExtras(bundle);

					sendBroadcast(intent);
					
					locationManager.removeUpdates(this);
					
				}
				
				return;
			} 
			
			// let's broadcast location data to any activity waiting for updates
			Intent intent = new Intent("com.aripuca.tracker.LOCATION_UPDATES_ACTION");

			Bundle bundle = new Bundle();
			bundle.putInt("location_provider", Constants.GPS_PROVIDER);
			bundle.putParcelable("location", location);

			intent.putExtras(bundle);

			sendBroadcast(intent);
			
			
			
			
			

		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
		}

		@Override
		public void onProviderEnabled(String provider) {
		}

		@Override
		public void onProviderDisabled(String provider) {
			//Toast.makeText(myApp.getMainActivity(), "GPS provider disabled", Toast.LENGTH_SHORT).show();
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

		// GPS sensor
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		this.startLocationUpdates();

		// orientation sensor
		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		sensorManager.registerListener(sensorListener, sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
				SensorManager.SENSOR_DELAY_NORMAL);

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

			// attaching provider info
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

		locationManager.removeUpdates(locationListener);

		this.stopLocationUpdates();

		sensorManager.unregisterListener(sensorListener);

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
	
	public void startScheduler() {
		
		onSchedule = true;

		schedulerHandler.postDelayed(schedulerTask, 100);
	}
	
	public void stopScheduler() {
		
		onSchedule = false;
		
		schedulerHandler.removeCallbacks(schedulerTask);
	}
	
	/**
	 * Updating UI every second
	 */
	private Handler schedulerHandler = new Handler();
	private Runnable schedulerTask = new Runnable() {
		@Override
		public void run() {

			startLocationUpdates();
			
			schedulerHandler.postDelayed(this, 60*1000);
			
		}
		
	};


}
