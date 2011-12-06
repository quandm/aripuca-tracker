package com.aripuca.tracker.service;

import java.text.SimpleDateFormat;

import com.aripuca.tracker.MyApp;
import com.aripuca.tracker.NotificationActivity;
import com.aripuca.tracker.R;
import com.aripuca.tracker.app.Constants;
import com.aripuca.tracker.track.ScheduledTrackRecorder;
import com.aripuca.tracker.util.Utils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
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

	private static boolean running = false;

	private MyApp myApp;

	private LocationManager locationManager;

	private SensorManager sensorManager;

	private Location currentLocation;

	/**
	 * waypoint track recording start time
	 */
	private long wptStartTime;

	private long wptGpsFixWaitTimeStart;

	/**
	 * is GPS in use?
	 */
	private boolean gpsInUse;

	/**
	 * listening for location updates flag
	 */
	private boolean listening = false;

	/**
	 * listening getter
	 */
	public boolean isListening() {
		return listening;
	}

	private ScheduledTrackRecorder scheduledTrackRecorder;

	/**
	 * gpsInUse setter
	 */
	public void setGpsInUse(boolean gpsInUse) {
		this.gpsInUse = gpsInUse;
	}

	/**
	 * defines a listener that responds to location updates
	 */
	private LocationListener locationListener = new LocationListener() {

		// Called when a new location is found by the network location provider.
		@Override
		public void onLocationChanged(Location location) {

			listening = true;

			currentLocation = location;

			// update track statistics
			if (myApp.trackRecorder.isRecording()) {
				myApp.trackRecorder.updateStatistics(location);
			}

			GpsService.this.broadcastLocationUpdate(location, Constants.GPS_PROVIDER,
					"com.aripuca.tracker.LOCATION_UPDATES_ACTION");

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

	/**
	 * defines a listener that responds to location updates
	 */
	private LocationListener scheduledLocationListener = new LocationListener() {

		// Called when a new location is found by the network location provider.
		@Override
		public void onLocationChanged(Location location) {

			Log.v(Constants.TAG, "scheduledLocationListener");

			currentLocation = location;

			// TODO: minimum distance check

			if (location.hasAccuracy() && location.getAccuracy() < scheduledTrackRecorder.getMinAccuracy()) {

				GpsService.this.scheduledTrackRecorder.recordTrackPoint(location);

				// let's broadcast location data to any activity waiting for
				// updates
				GpsService.this.broadcastLocationUpdate(location, Constants.GPS_PROVIDER,
						"com.aripuca.tracker.SCHEDULED_LOCATION_UPDATES_ACTION");

				// location of acceptable accuracy received - stop GPS
				GpsService.this.stopScheduledLocationUpdates();

				// schedule next location update
				GpsService.this.schedulerHandler
						.postDelayed(schedulerTask, scheduledTrackRecorder.getRequestInterval());

				// SAVE LOCATION
				// log location data to debug log file
				String locationTime = (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")).format(location.getTime());
				myApp.log(locationTime + " " + Utils.formatLat(location.getLatitude()) + " "
						+ Utils.formatLng(location.getLongitude()) + " " + location.getAccuracy() + " "
						+ location.getAltitude());

				Log.v(Constants.TAG, "Accuracy accepted: " + location.getAccuracy() + " at "
						+ (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")).format(location.getTime()));

			} else {

				// waiting for acceptable accuracy

				if (wptGpsFixWaitTimeStart + scheduledTrackRecorder.getGpsFixWaitTime() < SystemClock.uptimeMillis()) {

					// stop trying to receive GPX signal
					GpsService.this.stopScheduledLocationUpdates();

					// schedule next location update
					GpsService.this.schedulerHandler.postDelayed(schedulerTask,
							scheduledTrackRecorder.getRequestInterval());

				}

				Log.v(Constants.TAG, "Accuracy not accepted: " + location.getAccuracy());

			}

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

	/**
	 * Broadcasting location update
	 */
	private void broadcastLocationUpdate(Location location, int locationProvider, String action) {

		// let's broadcast location data to any activity waiting for updates
		Intent intent = new Intent(action);

		Bundle bundle = new Bundle();
		bundle.putInt("location_provider", locationProvider);
		bundle.putParcelable("location", location);

		intent.putExtras(bundle);

		sendBroadcast(intent);

	}

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

		this.myApp = (MyApp) getApplicationContext();

		// scheduled track recorder instance
		this.scheduledTrackRecorder = ScheduledTrackRecorder.getInstance(myApp);

		// GPS sensor
		this.locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		this.startLocationUpdates();

		// orientation sensor
		this.sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

		this.startSensorUpdates();

		GpsService.running = true;

		this.requestLastKnownLocation();

	}

	/**
	 * Service destructor
	 */
	@Override
	public void onDestroy() {

		Log.v(Constants.TAG, "GpsService: onDestroy");

		GpsService.running = false;

		if (scheduledTrackRecorder.isRecording()) {
			stopScheduler();
		}

		// stop listener without delay
		this.locationManager.removeUpdates(locationListener);

		// stop compass listener
		this.sensorManager.unregisterListener(sensorListener);

		this.locationManager = null;
		this.sensorManager = null;

		super.onDestroy();

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
		location = this.locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

		if (location != null) {
			locationProvider = Constants.GPS_PROVIDER_LAST;
		} else {
			// let's try network provider
			location = this.locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		}

		if (location != null) {
			locationProvider = Constants.NETWORK_PROVIDER_LAST;
			broadcastLocationUpdate(location, locationProvider, "com.aripuca.tracker.LOCATION_UPDATES_ACTION");
		}

		currentLocation = location;

	}

	public void startLocationUpdates() {

		if (!gpsInUse) {
			this.locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
			gpsInUse = true;
		}

	}

	public void stopLocationUpdates() {

		gpsInUse = false;

		(new stopLocationUpdatesThread()).start();

	}

	public void stopLocationUpdatesNow() {

		Log.v(Constants.TAG, "GPS Service: location updates stopped");

		locationManager.removeUpdates(locationListener);
		listening = false;

		gpsInUse = false;
	}

	public void startScheduledLocationUpdates() {
		this.locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, scheduledLocationListener);
	}

	public void stopScheduledLocationUpdates() {
		this.locationManager.removeUpdates(scheduledLocationListener);
	}

	private void startSensorUpdates() {
		this.sensorManager.registerListener(sensorListener, sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
				SensorManager.SENSOR_DELAY_NORMAL);
	}

	private void stopSensorUpdates() {
		this.sensorManager.unregisterListener(sensorListener);
	}

	public void startScheduler() {

		this.showOngoingNotification();

		this.scheduledTrackRecorder.start();

		this.wptStartTime = SystemClock.uptimeMillis();

		Log.v(Constants.TAG, "Scheduler started at: " + (new SimpleDateFormat("HH:mm:ss")).format(wptStartTime));

		this.schedulerHandler.postDelayed(schedulerTask, 5000);

	}

	public void stopScheduler() {

		Log.v(Constants.TAG, "Scheduler stopped");

		this.scheduledTrackRecorder.stop();

		this.clearNotification();

		this.locationManager.removeUpdates(scheduledLocationListener);

		this.schedulerHandler.removeCallbacks(schedulerTask);

	}

	/**
	 * scheduled track recording handler
	 */
	private Handler schedulerHandler = new Handler();
	private Runnable schedulerTask = new Runnable() {
		@Override
		public void run() {

			// new request for location started
			GpsService.this.wptGpsFixWaitTimeStart = SystemClock.uptimeMillis();

			// has recording time limit been reached?
			if (GpsService.this.scheduledTrackRecorder.getStopRecordingAfter() != 0
					&& GpsService.this.wptStartTime + GpsService.this.scheduledTrackRecorder.getStopRecordingAfter() < SystemClock
							.uptimeMillis()) {

				GpsService.this.stopScheduler();

			} else {

				// scheduling next location update
				GpsService.this.startScheduledLocationUpdates();
			}

		}

	};

	/**
	 * Show ongoing notification
	 */
	private void showOngoingNotification() {

		int icon = R.drawable.aripuca_a;

		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		long when = System.currentTimeMillis();

		Notification notification = new Notification(icon, getString(R.string.recording_started), when);

		// show notification under ongoing title
		notification.flags += Notification.FLAG_ONGOING_EVENT;

		CharSequence contentTitle = getString(R.string.main_app_title);
		CharSequence contentText = getString(R.string.scheduled_track_recording_in_progress);

		Intent notificationIntent = new Intent(this, NotificationActivity.class);

		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

		notification.setLatestEventInfo(myApp, contentTitle, contentText, contentIntent);

		mNotificationManager.notify(Constants.NOTIFICATION_SCHEDULED_TRACK_RECORDING, notification);

	}

	private void clearNotification() {

		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		// remove all notifications
		// mNotificationManager.cancelAll();
		mNotificationManager.cancel(Constants.NOTIFICATION_SCHEDULED_TRACK_RECORDING);

	}

	public Location getCurrentLocation() {
		return this.currentLocation;
	}

	public ScheduledTrackRecorder getScheduledTrackRecorder() {
		return scheduledTrackRecorder;
	}

	/**
	 * stopping location updates with small delay giving us a chance not to
	 * restart listener if other activity requires GPS sensor too
	 */
	private class stopLocationUpdatesThread extends Thread {

		@Override
		public void run() {

			try {
				sleep(2500);
			} catch (Exception e) {
			}

			if (gpsInUse == false) {
				Log.v(Constants.TAG, "GPS Service: location updates stopped");
				locationManager.removeUpdates(locationListener);
				listening = false;
			}

		}
	}

}
