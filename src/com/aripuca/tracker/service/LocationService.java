package com.aripuca.tracker.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;

public class LocationService extends Service {

	private LocationManager locationManager;

	/**
	 * defines a listener that responds to location updates
	 */
	private LocationListener locationListener = new LocationListener() {

		// Called when a new location is found by the network location provider.
		@Override
		public void onLocationChanged(Location location) {

			// let's broadcast location data to any activity waiting for updates
			Intent intent = new Intent("com.aripuca.tracker.LOCATION_UPDATES_ACTION");
			
			// packing azimuth value into bundle  
			Bundle bundle = new Bundle();
			bundle.putDouble("lat", location.getLatitude());
			bundle.putDouble("lng", location.getLongitude());
			
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
		}

	};

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	/**
	 * Initialize service
	 */
	@Override
	public void onCreate() {

		super.onCreate();

		// GPS sensor 
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);

	}

	/**
	 * Service destructor
	 */
	@Override
	public void onDestroy() {

		locationManager.removeUpdates(locationListener);
		locationManager = null;

		super.onDestroy();

	}

}
