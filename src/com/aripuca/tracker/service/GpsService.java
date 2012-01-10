package com.aripuca.tracker.service;

import java.util.Calendar;

import com.aripuca.tracker.MyApp;
import com.aripuca.tracker.NotificationActivity;
import com.aripuca.tracker.R;
import com.aripuca.tracker.app.Constants;

import com.aripuca.tracker.track.ScheduledTrackRecorder;
import com.aripuca.tracker.track.TrackRecorder;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

/**
 * this service handles real time and scheduled track recording as well as
 * compass updates
 */
public class GpsService extends Service {

	private static boolean running = false;

	private MyApp myApp;

	private LocationManager locationManager;

	/**
	 * 
	 */

	/**
     * 
     */
	private SensorManager sensorManager;

	/**
	 * current device location
	 */
	private Location currentLocation;

	/**
	 * is GPS in use?
	 */
	private boolean gpsInUse;

	/**
	 * listening for location updates flag
	 */
	private boolean listening;

	/**
	 * sets to true once first location update received
	 */
	private boolean schedulerListening;

	/**
	 * listening getter
	 */
	public boolean isListening() {
		return listening;
	}

	private TrackRecorder trackRecorder;

	private ScheduledTrackRecorder scheduledTrackRecorder;

	/**
	 * gpsInUse setter
	 */
	public void setGpsInUse(boolean gpsInUse) {
		this.gpsInUse = gpsInUse;
	}

	public boolean isGpsInUse() {
		return this.gpsInUse;
	}

	/**
	 * Defines a listener that responds to location updates
	 */
	private LocationListener locationListener = new LocationListener() {

		/**
		 * Called when a new location is found by the network location provider.
		 */
		@Override
		public void onLocationChanged(Location location) {

			listening = true;

			currentLocation = location;

			// update track statistics
			if (trackRecorder.isRecording()) {
				trackRecorder.updateStatistics(location);
			}

			broadcastLocationUpdate(location, Constants.GPS_PROVIDER, Constants.ACTION_LOCATION_UPDATES);

		}

		/**
		 * Called when the provider status changes. This method is called when a
		 * provider is unable to fetch a location or if the provider has
		 * recently become available after a period of unavailability.
		 */
		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			if (status == LocationProvider.TEMPORARILY_UNAVAILABLE) {
				listening = false;
			}
		}

		@Override
		public void onProviderEnabled(String provider) {}

		@Override
		public void onProviderDisabled(String provider) {}

	};

	/**
	 * Defines a listener that responds to location updates
	 */
	private LocationListener scheduledLocationListener = new LocationListener() {

		// Called when a new location is found by the network location provider.
		@Override
		public void onLocationChanged(Location location) {

			Log.i(Constants.TAG, "scheduledLocationListener: " + location.getAccuracy());

			// first location update received
			schedulerListening = true;

			currentLocation = location;

			if (location.hasAccuracy() && location.getAccuracy() <= scheduledTrackRecorder.getMinAccuracy()) {

				scheduledTrackRecorder.recordTrackPoint(location);

				// let's broadcast location data to any activity waiting for
				// updates
				broadcastLocationUpdate(location, Constants.GPS_PROVIDER, Constants.ACTION_SCHEDULED_LOCATION_UPDATES);

				// location of acceptable accuracy received - stop GPS
				stopScheduledLocationUpdates();

				// schedule next location update
				scheduleNextLocationRequest((int) scheduledTrackRecorder.getRequestInterval());

			} else {

				// waiting for acceptable accuracy

				if (scheduledTrackRecorder.requestTimeLimitReached()) {
					// stop trying to receive GPX signal
					stopScheduledLocationUpdates();

					// schedule next location update
					// if current location request was unsuccessful let's try
					// again in 5 minutes (min request interval time)
					scheduleNextLocationRequest(5 * 60);
				}

			}

		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {}

		@Override
		public void onProviderEnabled(String provider) {}

		@Override
		public void onProviderDisabled(String provider) {}

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
			Intent intent = new Intent(Constants.ACTION_COMPASS_UPDATES);

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

	// //////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * This is the object that receives interactions from clients
	 */
	private final IBinder mBinder = new LocalBinder();

	@Override
	public IBinder onBind(Intent intent) {

		Log.d(Constants.TAG, "BOUND");

		return mBinder;
	}

	@Override
	public boolean onUnbind(Intent intent) {

		Log.d(Constants.TAG, "UNBIND");

		return true;
	}

	public class LocalBinder extends Binder {
		public GpsService getService() {
			return GpsService.this;
		}
	}

	// //////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Initialize service
	 */
	@Override
	public void onCreate() {

		super.onCreate();

		registerReceiver(nextTimeLimitCheckReceiver, new IntentFilter(Constants.ACTION_NEXT_TIME_LIMIT_CHECK));
		registerReceiver(nextLocationRequestReceiver, new IntentFilter(Constants.ACTION_NEXT_LOCATION_REQUEST));

		Log.i(Constants.TAG, "GpsService: onCreate");

		this.myApp = (MyApp) getApplicationContext();

		// track recorder instance
		this.trackRecorder = TrackRecorder.getInstance(myApp);

		// scheduled track recorder instance
		this.scheduledTrackRecorder = ScheduledTrackRecorder.getInstance(myApp);

		// location sensor
		// first time we call startLocationUpdates from MainActivity
		this.locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		// orientation sensor
		this.sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

		GpsService.running = true;

		this.requestLastKnownLocation();

	}

	/**
	 * Service destructor
	 */
	@Override
	public void onDestroy() {

		Log.i(Constants.TAG, "GpsService: onDestroy");

		GpsService.running = false;

		if (scheduledTrackRecorder.isRecording()) {
			stopScheduler();
		}

		// stop listener without delay
		this.locationManager.removeUpdates(locationListener);

		this.stopSensorUpdates();

		this.locationManager = null;
		this.sensorManager = null;

		unregisterReceiver(nextLocationRequestReceiver);
		unregisterReceiver(nextTimeLimitCheckReceiver);

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
			broadcastLocationUpdate(location, locationProvider, Constants.ACTION_LOCATION_UPDATES);
		}

		currentLocation = location;
	}

	/**
	 * 
	 */
	public void startLocationUpdates() {

		this.listening = false;

		this.locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);

		// setting gpsInUse to true, but listening is still false at this point
		// listening is set to true with first location update in
		// LocationListener.onLocationChanged
		gpsInUse = true;
	}

	/**
	 * Stopping location updates with delay, leaving a chance for new activity
	 * not to restart location listener
	 */
	public void stopLocationUpdates() {

		gpsInUse = false;

		(new stopLocationUpdatesThread()).start();

	}

	/**
	 * 
	 */
	public void stopLocationUpdatesNow() {

		locationManager.removeUpdates(locationListener);

		listening = false;

		gpsInUse = false;

	}

	/**
	 * start waiting for scheduler location updates
	 */
	public void startScheduledLocationUpdates() {

		// myApp.log("startScheduledLocationUpdates");

		this.schedulerListening = false;

		// control the time of location request before any updates received
		this.scheduleNextRequestTimeLimitCheck();

		this.locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, scheduledLocationListener);
	}

	/**
	 * stop scheduler location updates
	 */
	public void stopScheduledLocationUpdates() {
		// this.schedulerListening = false;
		this.locationManager.removeUpdates(scheduledLocationListener);
	}

	public void startSensorUpdates() {
		this.sensorManager.registerListener(sensorListener, sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
				SensorManager.SENSOR_DELAY_NORMAL);
	}

	/**
	 * stop compass listener
	 */
	public void stopSensorUpdates() {
		this.sensorManager.unregisterListener(sensorListener);
	}

	// ///////////////////////////////////////////////////////////////////////////////////////////////
	// ///////////////////////////////////////////////////////////////////////////////////////////////
	// ///////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * 
	 */
	private PendingIntent nextLocationRequestSender;

	/**
	 * 
	 */
	private void scheduleNextLocationRequest(int interval) {

		// myApp.log("scheduleNextLocationRequest");

		Intent intent = new Intent(Constants.ACTION_NEXT_LOCATION_REQUEST);
		nextLocationRequestSender = PendingIntent.getBroadcast(GpsService.this, 0, intent, 0);

		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(System.currentTimeMillis());
		calendar.add(Calendar.SECOND, interval);

		// schedule single alarm
		AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
		alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), nextLocationRequestSender);

	}

	private BroadcastReceiver nextLocationRequestReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

			// new request for location started
			scheduledTrackRecorder.setRequestStartTime();

			// check scheduler global time limit
			if (scheduledTrackRecorder.timeLimitReached()) {
				stopScheduler();
			} else {
				// scheduling next location update
				startScheduledLocationUpdates();
			}

		}
	};

	// ///////////////////////////////////////////////////////////////////////////////////////////////
	// ///////////////////////////////////////////////////////////////////////////////////////////////
	// ///////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * 
	 */
	private PendingIntent nextTimeLimitCheckSender;

	/**
	 * 
	 */
	private void scheduleNextRequestTimeLimitCheck() {

		// myApp.log("scheduleNextRequestTimeLimitCheck");
		Log.d(Constants.TAG, "scheduleNextRequestTimeLimitCheck");

		Intent intent = new Intent(Constants.ACTION_NEXT_TIME_LIMIT_CHECK);
		nextTimeLimitCheckSender = PendingIntent.getBroadcast(GpsService.this, 0, intent, 0);

		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(System.currentTimeMillis());
		calendar.add(Calendar.SECOND, 5);

		// schedule single alarm
		AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
		alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), nextTimeLimitCheckSender);
	}

	/**
	 * 
	 */
	private BroadcastReceiver nextTimeLimitCheckReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

			Log.d(Constants.TAG, "nextTimeLimitCheckReceiver " + schedulerListening);

			if (!schedulerListening) {

				Log.d(Constants.TAG, "nextTimeLimitCheckReceiver FALSE");

				// myApp.log("CHECK REQUEST TIME LIMIT");

				if (scheduledTrackRecorder.requestTimeLimitReached()) {

					Log.d(Constants.TAG, "CANCEL REQUEST");
					// myApp.log("CANCEL REQUEST");

					// canceling current location update request

					// stop trying to receive GPX signal
					stopScheduledLocationUpdates();

					// schedule next location update
					// if current location request was unsuccessful let's try
					// again in 5 minutes (min request interval time)
					scheduleNextLocationRequest(5 * 60);

				} else {
					// check request time limit in 5 seconds
					scheduleNextRequestTimeLimitCheck();
				}

			}

		}
	};

	// ///////////////////////////////////////////////////////////////////////////////////////////////
	// ///////////////////////////////////////////////////////////////////////////////////////////////
	// ///////////////////////////////////////////////////////////////////////////////////////////////

	public void startScheduler() {

		this.scheduledTrackRecorder.start();

		Log.i(Constants.TAG, "Scheduler started");

		// first request is scheduled in 5 seconds from now
		this.scheduleNextLocationRequest(5);

		this.showOngoingNotification();

	}

	/**
	 * 
	 */
	public void stopScheduler() {

		Log.i(Constants.TAG, "Scheduler stopped");

		this.scheduledTrackRecorder.stop();

		// cancel alarm
		AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
		alarmManager.cancel(nextLocationRequestSender);
		alarmManager.cancel(nextTimeLimitCheckSender);

		this.locationManager.removeUpdates(scheduledLocationListener);

		this.clearNotification();
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

	public TrackRecorder getTrackRecorder() {
		return trackRecorder;
	}

	/**
	 * stopping location updates with small delay giving us a chance not to
	 * restart listener if other activity requires GPS sensor too new activity
	 * has to bind to GpsService and set gpsInUse to true
	 */
	private class stopLocationUpdatesThread extends Thread {

		@Override
		public void run() {

			try {
				// wait for other activities to grab location updates
				sleep(2500);
			} catch (Exception e) {}

			// if no activities require location updates - stop them and save
			// battery
			if (gpsInUse == false) {
				locationManager.removeUpdates(locationListener);
				listening = false;
			}

		}
	}

}
