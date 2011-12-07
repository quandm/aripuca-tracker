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
import android.os.SystemClock;
import android.util.Log;

public class GpsService extends Service {

	private static boolean running = false;

	private MyApp myApp;

	private LocationManager locationManager;

    private PendingIntent alarmSender;
	
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

	public TrackRecorder trackRecorder;

	private ScheduledTrackRecorder scheduledTrackRecorder;

	/**
	 * gpsInUse setter
	 */
	public void setGpsInUse(boolean gpsInUse) {
		this.gpsInUse = gpsInUse;
	}

	/**
	 * Defines a listener that responds to location updates
	 */
	private LocationListener locationListener = new LocationListener() {

		// Called when a new location is found by the network location provider.
		@Override
		public void onLocationChanged(Location location) {

			listening = true;

			currentLocation = location;

			// update track statistics
			if (trackRecorder.isRecording()) {
				trackRecorder.updateStatistics(location);
			}

			broadcastLocationUpdate(location, Constants.GPS_PROVIDER, "com.aripuca.tracker.LOCATION_UPDATES_ACTION");

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
		public void onProviderEnabled(String provider) {
		}

		@Override
		public void onProviderDisabled(String provider) {
		}

	};

	/**
	 * Defines a listener that responds to location updates
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

				// let's broadcast location data to any activity waiting for
				// updates
				broadcastLocationUpdate(location, Constants.GPS_PROVIDER,
						"com.aripuca.tracker.SCHEDULED_LOCATION_UPDATES_ACTION");

				// location of acceptable accuracy received - stop GPS
				stopScheduledLocationUpdates();

				// schedule next location update
				//schedulerHandler.postDelayed(schedulerTask, scheduledTrackRecorder.getRequestInterval());
				scheduleNextLocationRequest((int)scheduledTrackRecorder.getRequestInterval());

				myApp.log("Accuracy accepted: " + location.getAccuracy());
				Log.v(Constants.TAG, "Accuracy accepted: " + location.getAccuracy());

			} else {

				// waiting for acceptable accuracy

				if (wptGpsFixWaitTimeStart + scheduledTrackRecorder.getGpsFixWaitTime() < SystemClock.elapsedRealtime()) {

					// stop trying to receive GPX signal
					stopScheduledLocationUpdates();

					// schedule next location update
					//schedulerHandler.postDelayed(schedulerTask,	scheduledTrackRecorder.getRequestInterval());
					scheduleNextLocationRequest((int)scheduledTrackRecorder.getRequestInterval());

				}

				myApp.log("Accuracy not accepted: " + location.getAccuracy());
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

	////////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * This is the object that receives interactions from clients
	 */
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

	////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Initialize service
	 */
	@Override
	public void onCreate() {

		super.onCreate();

		registerReceiver(alarmReceiver, new IntentFilter("com.aripuca.tracker.SCHEDULED_LOCATION_UPDATES_ALARM"));

		Log.v(Constants.TAG, "GpsService: onCreate");

		this.myApp = (MyApp) getApplicationContext();

		// track recorder instance
		this.trackRecorder = TrackRecorder.getInstance(myApp);

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

		unregisterReceiver(alarmReceiver);

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

	/**
	 * 
	 */
	public void startLocationUpdates() {

		if (!gpsInUse) {
			this.locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
			gpsInUse = true;
		}

	}

	/**
	 * Stopping location updates with delay, leaving a chance for new activity
	 * not to
	 * restart location listener
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

	protected BroadcastReceiver alarmReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

			Log.v(Constants.TAG, "alarmReceiver");
			myApp.log("alarmReceiver: onReceive");

			// new request for location started
			wptGpsFixWaitTimeStart = SystemClock.elapsedRealtime();

			// has recording time limit been reached?
			if (scheduledTrackRecorder.getStopRecordingAfter() != 0
					&& wptStartTime + scheduledTrackRecorder.getStopRecordingAfter() < SystemClock.elapsedRealtime()) {

				stopScheduler();

			} else {

				// scheduling next location update
				startScheduledLocationUpdates();
			}

		}
	};

	public void startScheduler() {

		this.scheduledTrackRecorder.start();

		this.wptStartTime = SystemClock.elapsedRealtime();

		Log.v(Constants.TAG, "Scheduler started");

		this.scheduleNextLocationRequest(5);

		this.showOngoingNotification();
		
	}
	
	private void scheduleNextLocationRequest(int interval) {
		
		Intent intent = new Intent("com.aripuca.tracker.SCHEDULED_LOCATION_UPDATES_ALARM");
		alarmSender = PendingIntent.getBroadcast(GpsService.this, 0, intent, 0);

		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(System.currentTimeMillis());
		calendar.add(Calendar.SECOND, interval);

		// Schedule the alarm!
		AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
		am.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), alarmSender);
		
	}

	public void stopScheduler() {

		Log.v(Constants.TAG, "Scheduler stopped");
		myApp.log("Scheduler stopped");

		this.scheduledTrackRecorder.stop();

		this.clearNotification();

		//this.schedulerHandler.removeCallbacks(schedulerTask);

		AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
		am.cancel(alarmSender);
		
		this.locationManager.removeUpdates(scheduledLocationListener);

	}

	/**
	 * scheduled track recording handler
	 */
	private Handler schedulerHandler = new Handler();
	private Runnable schedulerTask = new Runnable() {
		@Override
		public void run() {

			myApp.log("GPS Service scheduler task");

			// new request for location started
			GpsService.this.wptGpsFixWaitTimeStart = SystemClock.elapsedRealtime();

			// has recording time limit been reached?
			if (GpsService.this.scheduledTrackRecorder.getStopRecordingAfter() != 0
					&& GpsService.this.wptStartTime + GpsService.this.scheduledTrackRecorder.getStopRecordingAfter() < SystemClock
							.elapsedRealtime()) {

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
	 * 
	 * new activity has to bind to GpsService and set gpsInUse to true
	 */
	private class stopLocationUpdatesThread extends Thread {

		@Override
		public void run() {

			try {
				sleep(2500);
			} catch (Exception e) {
			}

			if (gpsInUse == false) {

				Log.v(Constants.TAG, "GPS Service: stopLocationUpdatesThread: location updates stopped");
				myApp.log("stopLocationUpdatesThread: GPS Service: location updates stopped");

				locationManager.removeUpdates(locationListener);
				listening = false;
			}

		}
	}

}
