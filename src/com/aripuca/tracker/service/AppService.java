package com.aripuca.tracker.service;

import java.util.Calendar;

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
import android.os.IBinder;
import android.util.Log;

import com.aripuca.tracker.App;
import com.aripuca.tracker.Constants;
import com.aripuca.tracker.NotificationActivity;
import com.aripuca.tracker.R;
import com.aripuca.tracker.track.ScheduledTrackRecorder;
import com.aripuca.tracker.track.TrackRecorder;
import com.aripuca.tracker.utils.AppLog;
import com.aripuca.tracker.utils.Utils;

/**
 * this service handles real time and scheduled track recording as well as
 * compass updates
 */
public class AppService extends Service {

	private static boolean running = false;

	private App app;

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

	private Location lastRecordedLocation = null;

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

	/**
	 * 
	 */
	private TrackRecorder trackRecorder;

	/**
	 * 
	 */
	private ScheduledTrackRecorder scheduledTrackRecorder;

	/**
	 * gpsInUse setter
	 */
	public void setGpsInUse(boolean gpsInUse) {
		this.gpsInUse = gpsInUse;
	}

	/**
	 * 
	 * @return
	 */
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

			//			app.setCurrentLocation(location);

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

		/**
		 * Called when a new location is found by the network location provider.
		 */
		@Override
		public void onLocationChanged(Location location) {

			// first location update received
			schedulerListening = true;

			currentLocation = location;
			//			app.setCurrentLocation(location);

			// check minimum accuracy required for recording
			if (location.hasAccuracy() && location.getAccuracy() <= scheduledTrackRecorder.getMinAccuracy()) {

				float distance = 0;
				if (lastRecordedLocation != null) {

					distance = location.distanceTo(lastRecordedLocation);

					// check distance to lastRecordedLocation
					if (distance < scheduledTrackRecorder.getMinDistance()) {

						// we are too close to previous location - no point to
						// record one more

						AppLog.d(getApplicationContext(), "Min distance not accepted: " + distance);

						stopScheduledLocationUpdates();

						// schedule next location update in 5 minutes (min
						// request interval time)
						scheduleNextLocationRequest(5 * 60);

						return;
					}

				}

				scheduledTrackRecorder.recordTrackPoint(location, distance);

				// save last location for distance calculation
				lastRecordedLocation = location;

				// let's broadcast location data to any activity waiting for
				// updates
				broadcastLocationUpdate(location, Constants.GPS_PROVIDER, Constants.ACTION_SCHEDULED_LOCATION_UPDATES);

				// location of acceptable accuracy received - stop GPS
				stopScheduledLocationUpdates();

				AppLog.d(getApplicationContext(), "Scheduled location recorded. Accuracy: " + location.getAccuracy());

				// schedule next location update
				scheduleNextLocationRequest((int) scheduledTrackRecorder.getRequestInterval());

			} else {

				AppLog.d(getApplicationContext(), "Accuracy not accepted: " + location.getAccuracy());

				// waiting for acceptable accuracy
				// when we wait for acceptable accuracy GPS stays ON until we
				// receive accepted accuracy or request time limit reached
				if (scheduledTrackRecorder.gpsFixWaitTimeLimitReached()) {

					// stop trying to receive GPX signal
					stopScheduledLocationUpdates();

					AppLog.d(getApplicationContext(), "Scheduled request cancelled: UNACCEPTABLE ACCURACY");

					// schedule next location update
					// if current location request was unsuccessful let's try
					// again in 5 minutes (min request interval time)
					scheduleNextLocationRequest(5 * 60);
				}

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

	/**
	 * 
	 */
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

		AppLog.d(this.getApplicationContext(), "AppService: BOUND " + this.toString());

		return mBinder;
	}

	@Override
	public boolean onUnbind(Intent intent) {

		Log.d(Constants.TAG, "AppService: UNBOUND " + this.toString());

		return true;
	}

	public class LocalBinder extends Binder {
		public AppService getService() {
			return AppService.this;
		}
	}

	// //////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Initialize service
	 */
	@Override
	public void onCreate() {

		super.onCreate();

		//
		registerReceiver(nextTimeLimitCheckReceiver, new IntentFilter(Constants.ACTION_NEXT_TIME_LIMIT_CHECK));

		//
		registerReceiver(nextLocationRequestReceiver, new IntentFilter(Constants.ACTION_NEXT_LOCATION_REQUEST));

		Log.i(Constants.TAG, "AppService: onCreate");

		this.app = (App) getApplication();

		// track recorder instance
		this.trackRecorder = TrackRecorder.getInstance(app);

		// scheduled track recorder instance
		this.scheduledTrackRecorder = ScheduledTrackRecorder.getInstance(app);

		// location sensor
		// first time we call startLocationUpdates from MainActivity
		this.locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		// orientation sensor
		this.sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

		AppService.running = true;

		this.requestLastKnownLocation();

	}

	/**
	 * Service destructor
	 */
	@Override
	public void onDestroy() {

		AppLog.d(this.getApplicationContext(), "AppService: onDestroy");

		AppService.running = false;

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
	 * 
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		//TODO: test !!!
		Log.i(Constants.TAG, "Received start id " + startId + ": " + intent);
		return START_STICKY;
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

		if (currentLocation != null) {
			return;
		}

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
		//		this.app.setCurrentLocation(location);

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
	 * stop location updates without giving a chance for other activities to
	 * grab GPS sensor
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

		AppLog.d(getApplicationContext(), "AppService.startScheduledLocationUpdates");

		// schedulerListening is false until first location update received in
		// LocationListener
		this.schedulerListening = false;

		// control the time of location request before any updates received
		this.scheduleNextRequestTimeLimitCheck();

		this.locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, scheduledLocationListener);
	}

	/**
	 * stop scheduler location updates
	 */
	public void stopScheduledLocationUpdates() {
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
	 * Schedules next request for GPS location
	 * 
	 * @param interval Interval between 2 consecutive requests (in seconds)
	 */
	private void scheduleNextLocationRequest(int interval) {

		AppLog.d(getApplicationContext(),
				"AppService.scheduleNextLocationRequest interval: " + Utils.formatInterval(interval * 1000, false));

		Intent intent = new Intent(Constants.ACTION_NEXT_LOCATION_REQUEST);
		nextLocationRequestSender = PendingIntent.getBroadcast(AppService.this, 0, intent, 0);

		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(System.currentTimeMillis());
		calendar.add(Calendar.SECOND, interval);

		// schedule single alarm
		AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
		alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), nextLocationRequestSender);

	}

	/**
	 * Receives broadcast event for next location request
	 */
	private BroadcastReceiver nextLocationRequestReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {

			// new request for location started
			scheduledTrackRecorder.setRequestStartTime();

			// check scheduler global time limit
			if (scheduledTrackRecorder.timeLimitReached()) {

				AppLog.d(getApplicationContext(), "Scheduled track recording stopped: timeLimitReached");

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
	 * Scheduling regular checks for GPS signal availability
	 */
	private void scheduleNextRequestTimeLimitCheck() {

		AppLog.d(getApplicationContext(), "AppService.scheduleNextRequestTimeLimitCheck");

		Intent intent = new Intent(Constants.ACTION_NEXT_TIME_LIMIT_CHECK);
		nextTimeLimitCheckSender = PendingIntent.getBroadcast(AppService.this, 0, intent, 0);

		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(System.currentTimeMillis());
		calendar.add(Calendar.SECOND, 5);

		// schedule single alarm
		// if GPS signal is not available we will schedule this event again in
		// receiver
		AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
		alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), nextTimeLimitCheckSender);
	}

	/**
	 * Receives broadcast event every 5 seconds in order to control presence of
	 * GPS signal
	 */
	private BroadcastReceiver nextTimeLimitCheckReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

			// if first location update received by LocationListener - stop
			// controlling the time passed since requestStartTime
			if (schedulerListening) {
				// now we have to wait for acceptable GPS accuracy
				return;
			}

			if (scheduledTrackRecorder.gpsFixWaitTimeLimitReached()) {

				// we were unable to receive any GPS location update in this
				// session

				AppLog.d(getApplicationContext(), "Scheduled request cancelled: NO GPS SIGNAL");

				// canceling current location update request

				// stop trying to receive GPX signal
				stopScheduledLocationUpdates();

				// schedule next location update
				// let's try again in 5 minutes (min request interval time)
				scheduleNextLocationRequest(5 * 60);

			} else {
				// check request time limit in 5 seconds
				scheduleNextRequestTimeLimitCheck();
			}

		}
	};

	// ///////////////////////////////////////////////////////////////////////////////////////////////
	// ///////////////////////////////////////////////////////////////////////////////////////////////
	// ///////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Starts GPS scheduler
	 */
	public void startScheduler() {

		AppLog.d(getApplicationContext(), "AppService.startScheduler");

		this.scheduledTrackRecorder.start();

		// first request is scheduled in 5 seconds from now
		this.scheduleNextLocationRequest(5);

		this.showNotification(Constants.NOTIFICATION_SCHEDULED_TRACK_RECORDING,
				R.string.scheduled_track_recording_in_progress);

	}

	/**
	 * 
	 */
	public void startTrackRecording() {

		this.trackRecorder.start();

		// add notification icon in track recording mode
		this.showNotification(Constants.NOTIFICATION_TRACK_RECORDING, R.string.recording_track);
	}

	/**
	 * 
	 */
	public void stopTrackRecording() {

		this.trackRecorder.stop();

		this.clearNotification(Constants.NOTIFICATION_TRACK_RECORDING);
	}

	/**
	 * 
	 */
	public void stopScheduler() {

		AppLog.d(getApplicationContext(), "AppService.stopScheduler");

		this.scheduledTrackRecorder.stop();

		// cancel alarm
		AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
		alarmManager.cancel(nextLocationRequestSender);
		alarmManager.cancel(nextTimeLimitCheckSender);

		this.locationManager.removeUpdates(scheduledLocationListener);

		this.clearNotification(Constants.NOTIFICATION_SCHEDULED_TRACK_RECORDING);
	}

	/**
	 * Show ongoing notification
	 */
	public void showNotification(int notificationId, int contentTextResourceId) {

		int icon = R.drawable.ic_stat_notify_aripuca;

		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		long when = System.currentTimeMillis();

		Notification notification = new Notification(icon, getString(R.string.recording_started), when);

		// show notification under ongoing title
		notification.flags += Notification.FLAG_ONGOING_EVENT;

		CharSequence contentTitle = getString(R.string.main_app_title);
		CharSequence contentText = getString(contentTextResourceId);

		Intent notificationIntent = new Intent(this, NotificationActivity.class);

		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

		notification.setLatestEventInfo(app, contentTitle, contentText, contentIntent);

		mNotificationManager.notify(notificationId, notification);

	}

	/**
	 * 
	 */
	public void clearNotification(int notificationId) {

		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		// remove all notifications
		// mNotificationManager.cancelAll();
		mNotificationManager.cancel(notificationId);

	}

	/**
	 * 
	 * @return
	 */
	public Location getCurrentLocation() {
		return this.currentLocation;
	}

	/**
	 * @return scheduledTrackRecorder object
	 */
	public ScheduledTrackRecorder getScheduledTrackRecorder() {
		return scheduledTrackRecorder;
	}

	/**
	 * @return
	 */
	public TrackRecorder getTrackRecorder() {
		return trackRecorder;
	}

	/**
	 * stopping location updates with small delay giving us a chance not to
	 * restart listener if other activity requires GPS sensor too. new activity
	 * has to bind to AppService and set gpsInUse to true
	 */
	private class stopLocationUpdatesThread extends Thread {

		@Override
		public void run() {

			try {
				// wait for other activities to grab location updates
				sleep(5000);
			} catch (Exception e) {
			}

			// if no activities require location updates - stop them and save battery
			if (gpsInUse == false) {
				locationManager.removeUpdates(locationListener);
				listening = false;
			}

		}
	}

}
