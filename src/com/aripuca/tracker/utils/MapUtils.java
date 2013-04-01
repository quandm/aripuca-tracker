package com.aripuca.tracker.utils;

import android.hardware.GeomagneticField;
import android.location.Location;

import com.google.android.maps.GeoPoint;

public class MapUtils {

	/**
	 * Get current magnetic declination
	 * 
	 * @param location
	 * @param timestamp
	 * @return
	 */
	public static float getDeclination(Location location, long timestamp) {

		GeomagneticField field = new GeomagneticField((float) location.getLatitude(), (float) location.getLongitude(),
				(float) location.getAltitude(), timestamp);

		return field.getDeclination();
	}

	/**
	 * Convert Location object to GeoPoint
	 * 
	 * @param location
	 * @return
	 */
	public static GeoPoint locationToGeoPoint(Location location) {
		return new GeoPoint((int) (location.getLatitude() * 1E6),
				(int) (location.getLongitude() * 1E6));
	}

}
