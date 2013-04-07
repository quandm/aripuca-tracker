package com.aripuca.tracker;

import java.util.ArrayList;
import java.util.Comparator;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.aripuca.tracker.db.Waypoint;
import com.aripuca.tracker.db.Waypoints;
import com.aripuca.tracker.map.MyMapActivity;
import com.aripuca.tracker.service.AppService;
import com.aripuca.tracker.service.AppServiceConnection;
import com.aripuca.tracker.utils.Utils;
import com.aripuca.tracker.view.CompassImage;

//TODO: create abstract activity for track points and waypoints lists

/**
 * Track points list activity. Displays list of track points related to one
 * scheduled track. The list can be sorted by recording time or distance to
 * specific point from current location
 */
public class TrackpointsListActivity extends ListActivity {

	/**
	 * Reference to app object
	 */
	private App app;

	private WaypointsArrayAdapter waypointsArrayAdapter;

	private ArrayList<Waypoint> waypoints;

	private Location currentLocation;

	private String elevationUnit;
	private String distanceUnit;

	private int sortMethod;

	private long trackId;
	
	/**
	 * Service connection object
	 */
	private AppServiceConnection serviceConnection;

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
			// waypointsArrayAdapter.notifyDataSetChanged();
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

		}
	};

	protected class WaypointsArrayAdapter extends ArrayAdapter<Waypoint> {

		private final Comparator<Waypoint> distanceComparator = new Comparator<Waypoint>() {
			@Override
			public int compare(Waypoint wp1, Waypoint wp2) {
				if (sortMethod == 0) {
					return (wp1.getDistanceTo() < wp2.getDistanceTo() ? -1 : (wp1.getDistanceTo() == wp2
							.getDistanceTo() ? 0 : 1));
				} else {
					return (wp1.getTime() < wp2.getTime() ? -1 : (wp1.getTime() == wp2.getTime() ? 0 : 1));
				}
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
							+ Utils.getLocalizedDistanceUnit(TrackpointsListActivity.this, distanceTo, distanceUnit);

					wp.setDistanceTo(distanceTo);

					newBearing = currentLocation.bearingTo(wp.getLocation());

					if ((int) newBearing < 0) {
						newBearing = 360 - Math.abs((int) newBearing);
					}

					newAzimuth = newBearing - getAzimuth()
							- Utils.getDeviceRotation(TrackpointsListActivity.this);
					if ((int) newAzimuth < 0) {
						newAzimuth = 360 - Math.abs((int) newAzimuth);
					}

					bearingStr = Utils.formatNumber(newBearing, 0) + Utils.DEGREE_CHAR;

				}

				elevationStr = Utils.formatElevation(wp.getElevation(), elevationUnit)
						+ Utils.getLocalizedElevationUnit(TrackpointsListActivity.this, elevationUnit);

				accuracyStr = Utils.PLUSMINUS_CHAR + Utils.formatDistance(wp.getAccuracy(), distanceUnit)
						+ Utils.getLocalizedDistanceUnit(TrackpointsListActivity.this, wp.getAccuracy(), distanceUnit);

				// speedStr = Utils.formatSpeed(speed, speedUnit) +
				// Utils.getLocalizedSpeedUnit(TrackpointsListActivity.this,
				// speedUnit);

				TextView coordinatesTextView = (TextView) v.findViewById(R.id.coordinates);
				TextView detailsTextView = (TextView) v.findViewById(R.id.details);
				TextView distanceTextView = (TextView) v.findViewById(R.id.distance);

				// setting track point coordinates
				if (coordinatesTextView != null) {
					coordinatesTextView.setText(Utils.formatLat(wp.getLat(),
							Integer.parseInt(app.getPreferences().getString("coord_units", "0")))
							+ " "
							+ Utils.formatLng(wp.getLng(),
									Integer.parseInt(app.getPreferences().getString("coord_units", "0"))));

				}

				// setting track point details
				if (detailsTextView != null) {
					detailsTextView.setText(DateFormat.format("k:mm", wp.getTime()) + " " + accuracyStr + " "
							+ elevationStr + " " + bearingStr);
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
	 * Called when the activity is first created
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		Bundle bundle = getIntent().getExtras();
		this.trackId = bundle.getLong("track_id", 0);

		app = ((App) getApplicationContext());

		// initializing with last known location, so we can calculate distance
		// to track points
//		currentLocation = app.getCurrentLocation();

		serviceConnection = new AppServiceConnection(this, appServiceConnectionCallback);

		registerForContextMenu(this.getListView());

		updateWaypointsArray();

		// cursorAdapter = new WaypointsCursorAdapter(this, cursor);
		waypointsArrayAdapter = new WaypointsArrayAdapter(this, R.layout.waypoint_list_item, waypoints);

		// setListAdapter(cursorAdapter);
		setListAdapter(waypointsArrayAdapter);

		elevationUnit = app.getPreferences().getString("elevation_units", "m");
		distanceUnit = app.getPreferences().getString("distance_units", "km");

		sortMethod = app.getPreferences().getInt("trackpoints_sort", 0);
	}

	private Runnable appServiceConnectionCallback = new Runnable() {

		@Override
		public void run() {

			AppService appService = serviceConnection.getService();

			if (appService == null) {
				Toast.makeText(TrackpointsListActivity.this, R.string.gps_service_not_connected, Toast.LENGTH_SHORT)
						.show();
				return;
			}

			if (!appService.isListening()) {

				// location updates stopped at this time, so let's start them
				appService.startLocationUpdates();

			} else {

				// gpsInUse = false means we are in process of stopping
				// listening
				appService.setGpsInUse(true);

				// if both isListening and isGpsInUse are true - do nothing
				// most likely we are in the process of recording track

			}

			// this activity requires compass data
			appService.startSensorUpdates();

			// let's not wait for LocationListener to receive updates and get last known location 
			currentLocation = appService.getCurrentLocation();
			
			waypointsArrayAdapter.notifyDataSetChanged();			
			
		}
	};

	/**
	 * onResume event handler
	 */
	@Override
	protected void onResume() {

		// registering receiver for compass updates
		registerReceiver(compassBroadcastReceiver, new IntentFilter(Constants.ACTION_COMPASS_UPDATES));

		// registering receiver for location updates
		registerReceiver(locationBroadcastReceiver, new IntentFilter(Constants.ACTION_LOCATION_UPDATES));

		// bind to GPS service
		// once bound appServiceConnectionCallback will be called
		serviceConnection.bindAppService();

		super.onResume();
	}

	@Override
	public void onPause() {

		unregisterReceiver(compassBroadcastReceiver);
		unregisterReceiver(locationBroadcastReceiver);

		AppService appService = serviceConnection.getService();

		if (appService != null) {

			// stop location updates when not recording track
			if (!appService.getTrackRecorder().isRecording()) {
				appService.stopLocationUpdates();
			}

			// stop compass updates in any case
			appService.stopSensorUpdates();
		}

		serviceConnection.unbindAppService();

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

		serviceConnection = null;

		app = null;

		super.onDestroy();

	}

	/**
	 * onCreateOptionsMenu handler
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.trackpoints_menu, menu);
		return true;
	}

	/**
     * 
     */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		// Handle item selection
		switch (item.getItemId()) {

			case R.id.sortBy:

				selectSortMethod();

				return true;
				
			case R.id.showOnMap:
				
				this.showTrackOnMap(this.trackId);
				
				return true;

			default:

				return super.onOptionsItemSelected(item);

		}

	}

	private void selectSortMethod() {

		// show "select a file" dialog
		final String sortMethods[] = { getString(R.string.sort_by_distance), getString(R.string.sort_by_time) };

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.sort_by);

		builder.setSingleChoiceItems(sortMethods, sortMethod, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {

				sortMethod = whichButton;

				// save current sort method in preferences
				SharedPreferences.Editor editor = app.getPreferences().edit();
				editor.putInt("trackpoints_sort", sortMethod);
				editor.commit();

				// resort the list
				waypointsArrayAdapter.notifyDataSetChanged();
				dialog.dismiss();
			}
		});

		AlertDialog alert = builder.create();
		alert.show();

	}

	/**
	 * 
	 */
	private void updateWaypointsArray() {

		if (waypoints != null) {
			waypoints.clear();
		} else {
			waypoints = new ArrayList<Waypoint>();
		}

		Waypoints.getAllTrackPoints(app.getDatabase(), waypoints, this.trackId);
		
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

	/**
	 * 
	 */
	private void showTrackOnMap(long trackId) {

		Intent i = new Intent(this, MyMapActivity.class);

		// using Bundle to pass track id into new activity
		Bundle b = new Bundle();
		b.putInt("mode", Constants.SHOW_SCHEDULED_TRACK);
		b.putLong("track_id", trackId);

		i.putExtras(b);
		startActivity(i);

	}
	
}
