package com.aripuca.tracker;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;

import com.aripuca.tracker.R;

import com.aripuca.tracker.app.Constants;
import com.aripuca.tracker.service.GpsService;
import com.aripuca.tracker.track.Waypoint;
import com.aripuca.tracker.util.OrientationHelper;
import com.aripuca.tracker.util.Utils;
import com.aripuca.tracker.view.CompassImage;

import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.Bitmap;

import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import android.widget.TextView;
import android.widget.Toast;

/**
 * Waypoints list activity
 */
public class TrackpointsListActivity extends ListActivity {

	/**
	 * Reference to myApp object
	 */
	private MyApp myApp;

	private WaypointsArrayAdapter waypointsArrayAdapter;

	private ArrayList<Waypoint> waypoints;

	private OrientationHelper orientationHelper;

	private Location currentLocation;

	private String elevationUnit;
	private String distanceUnit;
	private String speedUnit;

	/**
	 * Location updates broadcast receiver
	 */
	protected BroadcastReceiver locationBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

			// Log.d(Constants.TAG,
			// "TrackpointsListActivity: LOCATION BROADCAST MESSAGE RECEIVED");

			Bundle bundle = intent.getExtras();

			currentLocation = (Location) bundle.getParcelable("location");

			waypointsArrayAdapter.sortByDistance();
			waypointsArrayAdapter.notifyDataSetChanged();
		}
	};
	/**
	 * Compass updates broadcast receiver
	 */
	protected BroadcastReceiver compassBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

			Bundle bundle = intent.getExtras();
			setAzimuth(bundle.getFloat("azimuth"));

			orientationHelper.setOrientationValues(bundle.getFloat("azimuth"), bundle.getFloat("pitch"),
					bundle.getFloat("roll"));

			waypointsArrayAdapter.notifyDataSetChanged();
		}
	};

	protected class WaypointsArrayAdapter extends ArrayAdapter<Waypoint> {

		private final Comparator<Waypoint> distanceComparator = new Comparator<Waypoint>() {
			@Override
			public int compare(Waypoint wp1, Waypoint wp2) {
				return (wp1.getDistanceTo() < wp2.getDistanceTo() ? -1
						: (wp1.getDistanceTo() == wp2.getDistanceTo() ? 0 : 1));
			}
		};

		// private LayoutInflater mInflater;

		private ArrayList<Waypoint> items;

		Bitmap arrowBitmap;
		BitmapDrawable bmd;

		public WaypointsArrayAdapter(Context context, int textViewResourceId, ArrayList<Waypoint> items) {

			super(context, textViewResourceId, items);

			this.items = items;
		}

		public void setItems(ArrayList<Waypoint> items) {

			this.items = items;

		}

		public void sortByDistance() {

			this.sort(distanceComparator);
			this.notifyDataSetChanged();

		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			View v = convertView;

			if (v == null) {
				LayoutInflater layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = layoutInflater.inflate(R.layout.trackpoint_list_item, null);
			}

			String distStr = "distance";
			String bearingStr = "0" + Utils.DEGREE_CHAR;
			String elevationStr = "";
			String accuracyStr = "";

			float newAzimuth = 0;
			float newBearing = 0;

			Waypoint wp = items.get(position);
			if (wp != null) {

				if (currentLocation != null) {

					float distanceTo = currentLocation.distanceTo(wp.getLocation());

					distStr = Utils.formatDistance(distanceTo, distanceUnit)
							+ Utils.getLocalaziedDistanceUnit(TrackpointsListActivity.this, distanceTo, distanceUnit);

					wp.setDistanceTo(distanceTo);

					newBearing = currentLocation.bearingTo(wp.getLocation());

					if ((int) newBearing < 0) {
						newBearing = 360 - Math.abs((int) newBearing);
					}

					int orientationAdjustment = 0;
					if (orientationHelper != null) {
						orientationAdjustment = orientationHelper.getOrientationAdjustment();
					}

					newAzimuth = newBearing - getAzimuth() - orientationAdjustment;
					if ((int) newAzimuth < 0) {
						newAzimuth = 360 - Math.abs((int) newAzimuth);
					}

					bearingStr = Utils.formatNumber(newBearing, 0) + Utils.DEGREE_CHAR;

				}

				elevationStr = Utils.formatElevation(wp.getElevation(), elevationUnit)
						+ Utils.getLocalizedElevationUnit(TrackpointsListActivity.this, elevationUnit);

				accuracyStr = Utils.PLUSMINUS_CHAR + Utils.formatDistance(wp.getAccuracy(), distanceUnit)
						+ Utils.getLocalaziedDistanceUnit(TrackpointsListActivity.this, wp.getAccuracy(), distanceUnit);
				
//				speedStr = Utils.formatSpeed(speed, speedUnit) + Utils.getLocalizedSpeedUnit(TrackpointsListActivity.this, speedUnit);
						
				TextView coordinatesTextView = (TextView) v.findViewById(R.id.coordinates);
				TextView detailsTextView = (TextView) v.findViewById(R.id.details);
				TextView distanceTextView = (TextView) v.findViewById(R.id.distance);

				// setting track point coordinates
				if (coordinatesTextView != null) {
					coordinatesTextView.setText(Utils.formatLat(wp.getLatitude(),
							Integer.parseInt(myApp.getPreferences().getString("coord_units", "0")))
							+ " "
							+ Utils.formatLng(wp.getLongitude(),
									Integer.parseInt(myApp.getPreferences().getString("coord_units", "0"))));

				}

				// setting track point details
				if (detailsTextView != null) {
					detailsTextView.setText(accuracyStr + " " + elevationStr + " " + bearingStr);
				}

				if (distanceTextView != null) {
					distanceTextView.setText(distStr);
				}

				// rotating small arrow pointing to waypoint
				CompassImage im = (CompassImage) v.findViewById(R.id.compassImage);
				im.setAngle(newAzimuth);

			} else {

			}

			return v;

		}
	}

	/**
	 * Select all waypoints sql query
	 */
	private String sqlSelectAllWaypoints;;

	/**
	 * Called when the activity is first created
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		Bundle bundle = getIntent().getExtras();

		long trackId = bundle.getLong("track_id", 0);

		sqlSelectAllWaypoints = "SELECT * FROM track_points WHERE track_id=" + trackId + ";";

		myApp = ((MyApp) getApplicationContext());

		registerForContextMenu(this.getListView());

		updateWaypointsArray();

		orientationHelper = new OrientationHelper(this);

		// cursorAdapter = new WaypointsCursorAdapter(this, cursor);
		waypointsArrayAdapter = new WaypointsArrayAdapter(this, R.layout.waypoint_list_item, waypoints);

		// setListAdapter(cursorAdapter);
		setListAdapter(waypointsArrayAdapter);

		elevationUnit = myApp.getPreferences().getString("elevation_units", "m");
		distanceUnit = myApp.getPreferences().getString("distance_units", "km");
		speedUnit = myApp.getPreferences().getString("speed_units", "kph");

	}

	/**
	 * onResume event handler
	 */
	@Override
	protected void onResume() {

		// registering receiver for compass updates
		registerReceiver(compassBroadcastReceiver, new IntentFilter("com.aripuca.tracker.COMPASS_UPDATES_ACTION"));

		// registering receiver for location updates
		registerReceiver(locationBroadcastReceiver, new IntentFilter("com.aripuca.tracker.LOCATION_UPDATES_ACTION"));

		// bind to GPS service
		// once bound gpsServiceBoundCallback will be called
		this.bindGpsService();

		super.onResume();
	}

	@Override
	public void onPause() {

		unregisterReceiver(compassBroadcastReceiver);
		unregisterReceiver(locationBroadcastReceiver);

		// stop location updates when not recording track
		if (gpsService != null) {

			if (!gpsService.getTrackRecorder().isRecording()) {
				gpsService.stopLocationUpdates();
			}

			gpsService.stopSensorUpdates();
		}

		this.unbindGpsService();

		super.onPause();
	}

	/**
	 * 
	 */
	@Override
	protected void onDestroy() {

		if (waypoints != null) {
			waypoints.clear();
			waypoints = null;
		}

		gpsServiceConnection = null;

		myApp = null;

		super.onDestroy();

	}

	private void updateWaypointsArray() {

		if (waypoints != null) {
			waypoints.clear();
		} else {
			waypoints = new ArrayList<Waypoint>();
		}

		Cursor cursor = myApp.getDatabase().rawQuery(this.sqlSelectAllWaypoints, null);
		cursor.moveToFirst();

		int i = 1;
		while (cursor.isAfterLast() == false) {

			Waypoint wp = new Waypoint(Integer.toString(i), cursor.getLong(cursor.getColumnIndex("time")),
					cursor.getDouble(cursor.getColumnIndex("lat")) / 1E6,
					cursor.getDouble(cursor.getColumnIndex("lng")) / 1E6, cursor.getDouble(cursor
							.getColumnIndex("elevation")), cursor.getFloat(cursor.getColumnIndex("accuracy")));

			wp.setId(cursor.getLong(cursor.getColumnIndex("_id")));

			waypoints.add(wp);

			cursor.moveToNext();

			i++;
		}

		cursor.close();

	}

	public WaypointsArrayAdapter getArrayAdapter() {

		return waypointsArrayAdapter;

	}

	/**
	 * azimuth (received from orientation sensor)
	 */
	private float azimuth = 0;

	public void setAzimuth(float a) {
		azimuth = a;
	}

	public float getAzimuth() {
		return azimuth;
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

		if (!gpsService.isListening()) {

			// location updates stopped at this time, so let's start them
			gpsService.startLocationUpdates();

		} else {

			// gpsInUse = false means we are in process of stopping listening
			if (!gpsService.isGpsInUse()) {
				gpsService.setGpsInUse(true);
			}

			// if both isListening and isGpsInUse are true - do nothing
			// most likely we are in the process of recording track

		}

		gpsService.startSensorUpdates();

	}

	// //////////////////////////////////////////////////////////////////////////////////////////////

}
