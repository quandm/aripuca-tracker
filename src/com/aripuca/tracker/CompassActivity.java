package com.aripuca.tracker;

import com.aripuca.tracker.R;
import com.aripuca.tracker.app.Constants;
import com.aripuca.tracker.service.GpsService;
import com.aripuca.tracker.util.Utils;
import com.aripuca.tracker.view.CompassImage;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.TextView;
import android.widget.Toast;

public class CompassActivity extends Activity implements OnTouchListener {

	private Location currentLocation;

	static final int MIN_DISTANCE = 10;

	private float downX, downY, upX, upY;

	private int rotateDirection = 1;

	/**
	 * Location updates broadcast receiver
	 */
	protected BroadcastReceiver locationBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

			Bundle bundle = intent.getExtras();

			currentLocation = (Location) bundle.getParcelable("location");

		}
	};

	/**
	 * Compass updates broadcast receiver
	 */
	protected BroadcastReceiver compassBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Bundle bundle = intent.getExtras();
			updateCompass(bundle.getFloat("azimuth"));
		}
	};

	/**
	 * Reference to Application object
	 */
	private MyApp myApp;

	private long declinationLastUpdate = 0;

	private float declination;

	/**
	 * Initialize the activity
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		setContentView(R.layout.compass);

		// reference to application object
		myApp = ((MyApp) getApplicationContext());

		currentLocation = myApp.getCurrentLocation();

		if (findViewById(R.id.compassLayout) != null) {
			findViewById(R.id.compassLayout).setOnTouchListener(this);
		}

	}

	@Override
	public void onResume() {

		super.onResume();

		if (findViewById(R.id.compassView) != null) {
			findViewById(R.id.compassView).setKeepScreenOn(myApp.getPreferences().getBoolean("wake_lock", true));
		}

		// registering receiver for compass updates
		registerReceiver(compassBroadcastReceiver, new IntentFilter(Constants.ACTION_COMPASS_UPDATES));

		// registering receiver for location updates
		registerReceiver(locationBroadcastReceiver, new IntentFilter(Constants.ACTION_LOCATION_UPDATES));

		// staring compass updates in GpsService
		// Intent intent = new Intent(Constants.ACTION_START_SENSOR_UPDATES);
		// sendBroadcast(intent);

		// bind to GPS service
		// once bound gpsServiceBoundCallback will be called
		this.bindGpsService();

	}

	@Override
	public void onPause() {

		unregisterReceiver(compassBroadcastReceiver);
		unregisterReceiver(locationBroadcastReceiver);

		this.unbindGpsService();

		super.onPause();

	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {

		switch (event.getAction()) {

			case MotionEvent.ACTION_DOWN:
				downX = event.getX();
				downY = event.getY();
				return true;

			case MotionEvent.ACTION_UP:

				upX = event.getX();
				upY = event.getY();

				Log.d(Constants.TAG,
						"ACTION_UP: " + downX + " " + downY + " " + upX + " " + upY + " " + v.getMeasuredWidth());

				rotateDirection = 0;
				return true;

			case MotionEvent.ACTION_MOVE:

				upX = event.getX();
				upY = event.getY();

				float deltaX = upX - downX;
				float deltaY = upY - downY;
				
				double bc = Math.sqrt(Math.pow((double)Math.abs(upX - downX), 2) + Math.pow((double)Math.abs(upY - downY), 2));
				float angle = (float) (Math.asin( bc/2/v.getMeasuredWidth()/2) * 180 / Math.PI * 2);
				
/*				float ad1 = Math.abs(v.getMeasuredHeight()/2 - upY);
				float bd1 = Math.abs(v.getMeasuredWidth()/2 - upX); 
				float alpha1 = (float) (Math.atan(bd1/ad1) * 180 / Math.PI);
				float ad2 = Math.abs(v.getMeasuredWidth()/2 - downX);
				float cd2 = Math.abs(v.getMeasuredHeight()/2 - downY); 
				float alpha2 = (float) (Math.atan(cd2/ad2) * 180 / Math.PI); */
				//float angle = 90 - alpha1 - alpha2;
				//Log.d(Constants.TAG, "ANGLE: " + alpha1 + " " + alpha2 + " " + angle);

				// swipe horizontal?
				//if (Math.abs(deltaX) < MIN_DISTANCE) { return true; }

				if (upX >= 0 && upX <= v.getMeasuredWidth() / 2 && upY >= 0 && upY <= v.getMeasuredHeight() / 2) {

					if (deltaX > 0 || deltaY < 0) {
						rotateDirection = 1;
					} else {
						rotateDirection = -1;
					}

				}

				if (upX > v.getMeasuredWidth() / 2 && upX <= v.getMeasuredWidth() && upY >= 0
						&& upY <= v.getMeasuredHeight() / 2) {

					if (deltaX > 0 || deltaY > 0) {
						rotateDirection = 1;
					} else {
						rotateDirection = -1;
					}
				}

				if (upX > 0 && upX <= v.getMeasuredWidth() && upY > v.getMeasuredHeight() / 2
						&& upY <= v.getMeasuredHeight()) {

					if (deltaX < 0 || deltaY < 0) {
						rotateDirection = 1;
					} else {
						rotateDirection = -1;
					}
				}

				if (upX > v.getMeasuredWidth() / 2 && upX <= v.getMeasuredWidth() && upY > v.getMeasuredHeight() / 2
						&& upY <= v.getMeasuredHeight()) {
					if (deltaX < 0 || deltaY > 0) {
						rotateDirection = 1;
					} else {
						rotateDirection = -1;
					}
				}

				if (rotateDirection == -1) {
					this.rotateCompass(-angle);

				}
				if (rotateDirection == 1) {
					this.rotateCompass(angle);
				}

				Log.d(Constants.TAG, "onTouch: " + rotateDirection + " " + downX + " " + downY + " " + upX + " " + upY);

				return true;

		}

		return false;

	}

	private void rotateCompass(float angle) {

		// magnetic north compass
		if (findViewById(R.id.compass) != null) {
			CompassImage compass = (CompassImage) findViewById(R.id.compass);
			compass.setAngle(compass.getAngle() + angle);
			compass.invalidate();
		}

	}

	/**
	 * Update compass image and azimuth text
	 */
	public void updateCompass(float azimuth) {

		boolean trueNorth = myApp.getPreferences().getBoolean("true_north", true);
		boolean showMagnetic = myApp.getPreferences().getBoolean("show_magnetic", true);

		float rotation = 0;

		if (trueNorth && currentLocation != null) {

			long now = System.currentTimeMillis();

			// let's request declination every 15 minutes, not every compass
			// update
			if (now - declinationLastUpdate > 15 * 60 * 1000) {
				declination = Utils.getDeclination(currentLocation, now);
				Log.d(Constants.TAG, "declination update: " + declination);
				declinationLastUpdate = now;
			}

		} else {
			declination = 0;
		}

		// magnetic north to true north
		rotation = getAzimuth(azimuth + declination);

		if (findViewById(R.id.azimuth) != null) {
			((TextView) findViewById(R.id.azimuth)).setText(Utils.formatNumber(rotation, 0) + Utils.DEGREE_CHAR + " "
					+ Utils.getDirectionCode(rotation));
		}

		// true north compass
		if (findViewById(R.id.compassImage) != null) {

			CompassImage compassImage = (CompassImage) findViewById(R.id.compassImage);

			if (compassImage.getVisibility() == View.VISIBLE) {
				compassImage.setAngle(360 - rotation);
				compassImage.invalidate();
			}
		}

		// magnetic north compass
		if (findViewById(R.id.compassImage2) != null) {

			CompassImage compassImage2 = (CompassImage) findViewById(R.id.compassImage2);

			if (showMagnetic) {

				if (compassImage2.getVisibility() != View.VISIBLE) {
					compassImage2.setVisibility(View.VISIBLE);
				}

				compassImage2.setAngle(360 - rotation + declination);
				compassImage2.setAlpha(50);
				compassImage2.invalidate();

			} else {
				compassImage2.setVisibility(View.INVISIBLE);
			}

		}

	}

	protected float getAzimuth(float az) {

		if (az > 360) { return az - 360; }

		return az;

	}

	// //////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * GPS service connection
	 */
	private GpsService gpsService;
	private boolean isGpsServiceBound = false;
	private ServiceConnection gpsServiceConnection = new ServiceConnection() {

		public void onServiceConnected(ComponentName className, IBinder service) {
			gpsService = ((GpsService.LocalBinder) service).getService();
			gpsServiceBoundCallback();
			isGpsServiceBound = true;
		}

		public void onServiceDisconnected(ComponentName className) {
			isGpsServiceBound = false;
		}
	};

	private void bindGpsService() {
		if (!bindService(new Intent(this, GpsService.class), gpsServiceConnection, Context.BIND_AUTO_CREATE)) {
			Toast.makeText(this, "Can't connect to GPS service", Toast.LENGTH_SHORT).show();
		}
	}

	private void unbindGpsService() {

		if (isGpsServiceBound) {
			// Detach our existing connection.
			unbindService(gpsServiceConnection);
			isGpsServiceBound = false;
		}

		gpsService = null;

	}

	/**
	 * called when gpsService bound
	 */
	private void gpsServiceBoundCallback() {

		// this activity is started by MainActivity which is always
		// listening for location updates

		// by setting gpsInUse to true we insure that listening will not stop in
		// GpsService.stopLocationUpdatesThread
		gpsService.setGpsInUse(true);

		// this activity requires compass data
		gpsService.startSensorUpdates();

	}

	// //////////////////////////////////////////////////////////////////////////////////////////////

}
