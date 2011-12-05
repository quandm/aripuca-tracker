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

	// private MyApp myApp;

	private LocationManager locationManager;

	private SensorManager sensorManager;

	private Location currentLocation;

	public boolean onSchedule;

	private static boolean running = false;;

	private MyApp myApp;

	/**
	 * waypoint track recording start time
	 */
	private long wptStartTime;
	private long wptGpsFixWaitTimeStart;

	private ScheduledTrackRecorder scheduledTrackRecorder;

	/**
	 * defines a listener that responds to location updates
	 */
	private LocationListener locationListener = new LocationListener() {

		// Called when a new location is found by the network location provider.
		@Override
		public void onLocationChanged(Location location) {

			currentLocation = location;

			// update track statistics 
			if (myApp.trackRecorder.isRecording()) {
				myApp.trackRecorder.updateStatistics(location);
			}

			broadcastLocationUpdate(location, "com.aripuca.tracker.LOCATION_UPDATES_ACTION");

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

				scheduledTrackRecorder.recordTrackPoint(location);

				// let's broadcast location data to any activity waiting for updates
				broadcastLocationUpdate(location, "com.aripuca.tracker.SCHEDULED_LOCATION_UPDATES_ACTION");

				// location of acceptable accuracy received - stop GPS
				stopScheduledLocationUpdates();

				// schedule next location update
				schedulerHandler.postDelayed(schedulerTask, scheduledTrackRecorder.getRequestInterval());

				// SAVE LOCATION
				// log location data to debug log file
				String locationTime = (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")).format(location.getTime());
				myApp.log(locationTime + " " + Utils.formatLat(location.getLatitude()) + " "
						+ Utils.formatLng(location.getLongitude()) + " "
						+ location.getAccuracy() + " " + location.getAltitude());
				
				Log.v(Constants.TAG, "Accuracy accepted: " + location.getAccuracy() + " at "
						+ (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")).format(location.getTime()));

			} else {

				// waiting for acceptable accuracy
				if (wptGpsFixWaitTimeStart + scheduledTrackRecorder.getGpsFixWaitTime() < SystemClock.uptimeMillis()) {

					// stop trying to receive GPX signal
					stopScheduledLocationUpdates();

					// schedule next location update
					schedulerHandler
							.postDelayed(schedulerTask, scheduledTrackRecorder.getRequestInterval());

				}

				Log.v(Constants.TAG, "Accuracy not accepted: " + location.getAccuracy());

			}

			// has recording time limit been reached?
			checkStopRecordingAfter();

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
	private void broadcastLocationUpdate(Location location, String action) {

		// let's broadcast location data to any activity waiting for updates
		Intent intent = new Intent(action);

		Bundle bundle = new Bundle();
		bundle.putInt("location_provider", Constants.GPS_PROVIDER);
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

		myApp = (MyApp) getApplicationContext();

		// scheduled track recorder instance
		scheduledTrackRecorder = ScheduledTrackRecorder.getInstance(myApp);

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

	public void startScheduledLocationUpdates() {
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, scheduledLocationListener);
	}

	public void stopScheduledLocationUpdates() {
		locationManager.removeUpdates(scheduledLocationListener);
	}

	private void startSensorUpdates() {
		sensorManager.registerListener(sensorListener, sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
				SensorManager.SENSOR_DELAY_NORMAL);
	}

	private void stopSensorUpdates() {
		sensorManager.unregisterListener(sensorListener);
	}

	public void startScheduler() {

		this.showOngoingNotification();

		this.scheduledTrackRecorder.start();

		onSchedule = true;

		wptStartTime = SystemClock.uptimeMillis();

		Log.v(Constants.TAG, "Scheduler started at: " + (new SimpleDateFormat("HH:mm:ss")).format(wptStartTime));

		schedulerHandler.postDelayed(schedulerTask, 5000);

	}

	public void stopScheduler() {

		Log.v(Constants.TAG, "Scheduler stopped");

		this.scheduledTrackRecorder.stop();

		this.clearNotification();

		this.onSchedule = false;

		this.stopScheduledLocationUpdates();
		this.schedulerHandler.removeCallbacks(schedulerTask);

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

			// has recording time limit been reached?
			checkStopRecordingAfter();

			startScheduledLocationUpdates();

		}

	};

	/**
	 * has recording time limit been reached?
	 */
	private void checkStopRecordingAfter() {

		// has recording time limit been reached?
		if (scheduledTrackRecorder.getStopRecordingAfter() != 0
				&& wptStartTime + scheduledTrackRecorder.getStopRecordingAfter() < SystemClock.uptimeMillis()) {

			stopScheduler();
		}

	}

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

}
