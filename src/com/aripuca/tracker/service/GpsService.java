package com.aripuca.tracker.service;

import com.aripuca.tracker.MyApp;
import com.aripuca.tracker.app.Constants;

import com.aripuca.tracker.track.TrackRecorder;

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
import android.widget.Toast;

public class GpsService extends Service {

	private MyApp myApp;

	private LocationManager locationManager;

	private SensorManager sensorManager;

	private Location currentLocation;

	/**
	 * defines a listener that responds to location updates
	 */
	private LocationListener locationListener = new LocationListener() {

		// Called when a new location is found by the network location provider.
		@Override
		public void onLocationChanged(Location location) {

			if (myApp == null) { return; }

			myApp.setCurrentLocation(location);

			currentLocation = location;

			// let's broadcast location data to any activity waiting for updates
			Intent intent = new Intent("com.aripuca.tracker.LOCATION_UPDATES_ACTION");

			Bundle bundle = new Bundle();
			bundle.putInt("location_provider", Constants.GPS_PROVIDER);

			intent.putExtras(bundle);

			sendBroadcast(intent);

		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {}

		@Override
		public void onProviderEnabled(String provider) {}

		@Override
		public void onProviderDisabled(String provider) {
			Toast.makeText(myApp.getMainActivity(), "GPS provider disabled", Toast.LENGTH_SHORT).show();
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

		myApp = ((MyApp) getApplicationContext());

		// GPS sensor
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);

		requestLastKnownLocation();

		// orientation sensor
		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		sensorManager.registerListener(sensorListener, sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
				SensorManager.SENSOR_DELAY_NORMAL);

		// start updating time of tracking every second
		updateTimeHandler.postDelayed(updateTimeTask, 100);

	}

	/**
	 * Requesting last location from GPS or Network provider
	 */
	private void requestLastKnownLocation() {

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

			myApp.setCurrentLocation(location);

			currentLocation = location;

			// let's broadcast location data to any activity waiting for updates
			Intent intent = new Intent("com.aripuca.tracker.LOCATION_UPDATES_ACTION");

			// attaching provider info
			Bundle bundle = new Bundle();
			bundle.putInt("location_provider", locationProvider);

			intent.putExtras(bundle);

			sendBroadcast(intent);

		}

	}

	/**
	 * Updating UI every second
	 */
	private Handler updateTimeHandler = new Handler();
	private Runnable updateTimeTask = new Runnable() {
		@Override
		public void run() {

			if (myApp.getMainActivity() != null) {

				// update track statistics
				TrackRecorder trackRecorder = TrackRecorder.getInstance(myApp);
				if (trackRecorder.isRecording() && currentLocation != null) {
					trackRecorder.updateStatistics(currentLocation);
				}

				myApp.getMainActivity().updateTime();
			}

			updateTimeHandler.postDelayed(this, 200);
		}
	};

	/**
	 * Service destructor
	 */
	@Override
	public void onDestroy() {

		Log.v(Constants.TAG, "GpsService: onDestroy");

		updateTimeHandler.removeCallbacks(updateTimeTask);

		locationManager.removeUpdates(locationListener);
		locationManager = null;

		sensorManager.unregisterListener(sensorListener);
		sensorManager = null;

		myApp = null;

		super.onDestroy();

	}

	public void addLocationListener() {
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
	}
	
	public void removeLocationListener() {
		locationManager.removeUpdates(locationListener);
	}
	
	

}
