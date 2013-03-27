package com.aripuca.tracker.map;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Toast;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.aripuca.tracker.App;
import com.aripuca.tracker.Constants;
import com.aripuca.tracker.R;
import com.aripuca.tracker.WaypointsListActivity;
import com.aripuca.tracker.service.AppService;
import com.aripuca.tracker.service.AppServiceConnection;
import com.aripuca.tracker.utils.MapUtils;
import com.aripuca.tracker.utils.TrackPoint;
import com.aripuca.tracker.utils.Utils;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;
import com.google.android.maps.Projection;

//TODO: show current location on the map

/**
 * Map activity
 */
public class MyMapActivity extends MapActivity {

	/**
	 * 
	 */
	private App app;

	/**
	 * 
	 */
	private MapView mapView;

	/**
	 * 
	 */
	private GeoPoint geopoint;

	/**
	 * 
	 */
	private TrackPoint startPoint;

	/**
	 * track points array
	 */
	private ArrayList<TrackPoint> points;

	/**
	 * id of the track being shown
	 */
	private long trackId;

	private SeekBar seekBar;

	private int currentPointIndex;

	private float trackTotalDistance;

	/**
	 * measure units
	 */
	private String speedUnit;
	private String distanceUnit;
	private String elevationUnit;

	/**
	 * Track span values
	 */
	private int minLat = (int) (180 * 1E6);
	private int maxLat = (int) (-180 * 1E6);
	private int minLng = (int) (180 * 1E6);
	private int maxLng = (int) (-180 * 1E6);

	/**
	 * map display mode: SHOW_WAYPOINT or SHOW_TRACK
	 */
	private int mode;

	private int mapMode;

	private Location currentLocation;

	/**
	 * Service connection object
	 */
	private AppServiceConnection serviceConnection;

	/**
	 * Map overlay for displaying track path
	 */
	class TrackOverlay extends com.google.android.maps.Overlay {

		/**
		 * Overridden draw method
		 */
		@Override
		public boolean draw(Canvas canvas, MapView mapView, boolean shadow, long when) {

			super.draw(canvas, mapView, shadow);

			this.drawSegments(mapView, canvas);

			return true;

		}

		/**
		 * Drawing segments in different colors
		 * 
		 * @param projection
		 * @param canvas
		 */
		private void drawSegments(MapView mapView, Canvas canvas) {

			final Projection projection = mapView.getProjection();

			if (points.size() <= 1) {
				return;
			}

			Paint paint = new Paint();
			paint.setStrokeWidth(3);
			paint.setStyle(Paint.Style.STROKE);
			paint.setAntiAlias(true);

			boolean pathStarted = false;
			Point screenPts = new Point();

			//
			int currentSegmentIndex = -1;

			Path path = null;

			// array of path segments
			ArrayList<Path> segmentPath = new ArrayList<Path>();

			// create Path objects for each segment
			for (int i = 0; i < points.size(); i++) {

				// start new segment
				if (currentSegmentIndex != points.get(i).getSegmentIndex()) {

					if (i == 0) {
						// first segment created
						path = new Path();
					} else {
						// saving previous segment
						segmentPath.add(path);
						// create new segment
						path = new Path();
					}

					pathStarted = false;

					// new current segment
					currentSegmentIndex = points.get(i).getSegmentIndex();

				}

				// converting geopoint coordinates to screen ones
				projection.toPixels(points.get(i).getGeoPoint(), screenPts);

				// populating path object
				if (!pathStarted) {

					if (i > 0) {

						// starting new segment at last segment's end point
						projection.toPixels(points.get(i - 1).getGeoPoint(), screenPts);
						path.moveTo(screenPts.x, screenPts.y);

						projection.toPixels(points.get(i).getGeoPoint(), screenPts);
						path.lineTo(screenPts.x, screenPts.y);

					} else { // for the very first segment just move path pointer
						path.moveTo(screenPts.x, screenPts.y);
					}

					// starting new segment
					// path.moveTo(screenPts.x, screenPts.y);

					pathStarted = true;

				} else {
					path.lineTo(screenPts.x, screenPts.y);
				}

				// adding last segment if not empty
				if (i == points.size() - 1 && pathStarted) {
					segmentPath.add(path);
				}

			}

			// drawing segments in different colors
			for (int i = 0; i < segmentPath.size(); i++) {

				if (i % 2 == 0) {
					paint.setColor(getResources().getColor(R.color.red));
				} else {
					paint.setColor(getResources().getColor(R.color.blue));
				}

				canvas.drawPath(segmentPath.get(i), paint);
			}

			// drawing start and end map pins
			showMapPin(projection, canvas, points.get(0).getGeoPoint(), R.drawable.marker_flag_green);
			if (points.size() > 1) {
				showMapPin(projection, canvas, points.get(points.size() - 1).getGeoPoint(),
						R.drawable.marker_flag_pink);
			}

			// current position marker on top of start/stop ones
			showMapPin(projection, canvas, points.get(currentPointIndex).getGeoPoint(),
					R.drawable.marker_flag_blue);

			// show current location marker
			if (currentLocation != null) {
				GeoPoint gp = MapUtils.locationToGeoPoint(currentLocation);
				showMapPinCentered(projection, canvas, gp, R.drawable.ic_maps_indicator_current_position);
			}

		}

	}

	/**
	 * 
	 */
	class WaypointOverlay extends com.google.android.maps.Overlay {

		/**
		 * Overridden draw method
		 */
		@Override
		public boolean draw(Canvas canvas, MapView mapView, boolean shadow, long when) {

			super.draw(canvas, mapView, shadow);

			final Projection projection = mapView.getProjection();

			showMapPin(projection, canvas, geopoint, R.drawable.map_pin);

			// show current location marker
			if (currentLocation != null) {
				GeoPoint gp = MapUtils.locationToGeoPoint(currentLocation);
				showMapPinCentered(projection, canvas, gp, R.drawable.ic_maps_indicator_current_position);
			}

			return true;

		}

	}

	/**
	 * Itemized overlay for displaying waypoints with popup dialog
	 */
	class MyItemizedOverlay extends com.google.android.maps.ItemizedOverlay<OverlayItem> {

		private ArrayList<OverlayItem> overlayItems = new ArrayList<OverlayItem>();

		private Context mContext;

		public MyItemizedOverlay(Drawable defaultMarker, Context context) {
			super(boundCenterBottom(defaultMarker));
			mContext = context;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.google.android.maps.ItemizedOverlay#draw(android.graphics.Canvas, com.google.android.maps.MapView,
		 * boolean)
		 */
		@Override
		public void draw(Canvas canvas, MapView mapView, boolean shadow) {
			// TODO Auto-generated method stub
			super.draw(canvas, mapView, shadow);

			final Projection projection = mapView.getProjection();

			// show current location marker
			if (currentLocation != null) {
				GeoPoint gp = MapUtils.locationToGeoPoint(currentLocation);
				showMapPinCentered(projection, canvas, gp, R.drawable.ic_maps_indicator_current_position);
			}

		}

		public MyItemizedOverlay(Drawable defaultMarker) {
			super(boundCenterBottom(defaultMarker));
		}

		public void addOverlay(OverlayItem overlay) {
			overlayItems.add(overlay);
			populate();
		}

		@Override
		protected OverlayItem createItem(int i) {
			return overlayItems.get(i);
		}

		@Override
		public int size() {
			return overlayItems.size();
		}

		@Override
		protected boolean onTap(int index) {
			OverlayItem item = overlayItems.get(index);
			AlertDialog.Builder dialog = new AlertDialog.Builder(mContext);
			dialog.setTitle(item.getTitle());
			dialog.setMessage(item.getSnippet());
			dialog.show();
			return true;
		}

	}

	/**
	 * Shows standard map pin on the map
	 * 
	 * @param projection
	 * @param canvas
	 * @param point
	 */
	private void showMapPin(Projection projection, Canvas canvas, GeoPoint point, int resourceId) {

		// translate the GeoPoint to screen pixels
		Point screenPts = new Point();
		projection.toPixels(point, screenPts);

		// add the marker
		Bitmap bmp = BitmapFactory.decodeResource(getResources(), resourceId);

		canvas.drawBitmap(bmp, screenPts.x - bmp.getWidth() / 2, screenPts.y - bmp.getHeight(), null);

	}

	private void showMapPinCentered(Projection projection, Canvas canvas, GeoPoint point, int resourceId) {

		// translate the GeoPoint to screen pixels
		Point screenPts = new Point();
		projection.toPixels(point, screenPts);

		// add the marker
		Bitmap bmp = BitmapFactory.decodeResource(getResources(), resourceId);

		canvas.drawBitmap(bmp, screenPts.x - bmp.getWidth() / 2, screenPts.y - bmp.getHeight() / 2, null);

	}

	/**
	 * Location updates broadcast receiver
	 */
	protected BroadcastReceiver locationBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Bundle bundle = intent.getExtras();

			currentLocation = (Location) bundle.getParcelable("location");

			mapView.invalidate();
		}
	};

	/**
	 * Compass updates broadcast receiver
	 */
	protected BroadcastReceiver compassBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			// Bundle bundle = intent.getExtras();
			// setAzimuth(bundle.getFloat("azimuth"));
		}
	};

	/**
	 * Called when the activity is first created
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		setContentView(R.layout.map_view);

		app = ((App) getApplicationContext());

		serviceConnection = new AppServiceConnection(this, appServiceConnectionCallback);

		// measure units
		speedUnit = app.getPreferences().getString("speed_units", "kph");
		distanceUnit = app.getPreferences().getString("distance_units", "km");
		elevationUnit = app.getPreferences().getString("elevation_units", "m");

		this.setupMapView();

		// getting extra data passed to the activity
		Bundle b = getIntent().getExtras();
		this.mode = b.getInt("mode");

		// getting all map overlays
		List<Overlay> listOfOverlays = mapView.getOverlays();

		switch (this.mode) {

		// show waypoint
		case Constants.SHOW_WAYPOINT:

			this.hideInfoPanels();

			this.setupWaypoint(b);

			// add overlays to the map
			WaypointOverlay waypointOverlay = new WaypointOverlay();

			listOfOverlays.clear();
			listOfOverlays.add(waypointOverlay);

			break;

		case Constants.SHOW_ALL_WAYPOINTS:

			this.hideInfoPanels();

			Drawable drawable = this.getResources().getDrawable(R.drawable.marker_flag_pink);
			drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());

			MyItemizedOverlay itemizedOverlay = new MyItemizedOverlay(drawable, this);

			this.loadWaypoints(itemizedOverlay);

			listOfOverlays.clear();
			listOfOverlays.add(itemizedOverlay);

			break;

		// show track
		case Constants.SHOW_TRACK:
		case Constants.SHOW_SCHEDULED_TRACK:

			this.trackId = b.getLong("track_id");

			this.showInfoPanels();

			this.setupTrack();

			this.setupSeekBar();

			// add overlays to the map
			TrackOverlay trackOverlay = new TrackOverlay();

			listOfOverlays.clear();
			listOfOverlays.add(trackOverlay);

			break;

		}

		mapView.invalidate();

	}

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

		serviceConnection = null;

		app = null;

		super.onDestroy();

	}

	/**
	 * 
	 */
	private Runnable appServiceConnectionCallback = new Runnable() {

		@Override
		public void run() {

			AppService appService = serviceConnection.getService();

			if (appService == null) {
				Toast.makeText(MyMapActivity.this, R.string.gps_service_not_connected, Toast.LENGTH_SHORT)
						.show();
				return;
			}

			if (!appService.isListening()) {

				// location updates stopped at this time, so let's start them
				appService.startLocationUpdates();

			} else {

				// gpsInUse = false means we are in process of stopping
				// listening
				if (!appService.isGpsInUse()) {
					appService.setGpsInUse(true);
				}

				// if both isListening and isGpsInUse are true - do nothing
				// most likely we are in the process of recording track

			}

			// this activity requires compass data
			appService.startSensorUpdates();

		}
	};

	private void setupMapView() {

		mapView = (MapView) findViewById(R.id.mapview);

		mapView.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				showInfoPanels();
				return false;
			}
		});

		// initial map mode is street view
		mapMode = Constants.MAP_STREET;
		mapView.setStreetView(true);
		mapView.setSatellite(false);

		mapView.setBuiltInZoomControls(false);

	}

	private void setupWaypoint(Bundle b) {

		geopoint = new GeoPoint(b.getInt("latE6"), b.getInt("lngE6"));

		mapView.getController().setZoom(17);
		mapView.getController().animateTo(geopoint);

	}

	/**
	 * 
	 */
	private void setupTrack() {

		this.loadTrackPoints(this.trackId);

		this.trackTotalDistance = this.getTrackDistance(this.trackId);

		this.currentPointIndex = Math.round(points.size() / 2);

		this.updateInfoPanel(this.currentPointIndex);

		this.zoomToTrackSpanAndCenter();

	}

	/**
	 * Show entire track on the map with maximum zoom level
	 */
	private void zoomToTrackSpanAndCenter() {

		// zoom to track points span
		mapView.getController().zoomToSpan(maxLat - minLat, maxLng - minLng);

		// pan to the center of track occupied area
		mapView.getController().animateTo(new GeoPoint((maxLat + minLat) / 2, (maxLng + minLng) / 2));

	}

	/**
	 * Show entire track and current location on the map with maximum zoom level
	 */
	private void gotoCurrentLocation() {
		
		if (currentLocation != null) {
			
			// include current location in track span
			this.updateTrackSpan((int)(currentLocation.getLatitude()*1E6), (int)(currentLocation.getLongitude()*1E6));
			
			this.zoomToTrackSpanAndCenter();
			
			// mapView.getController().animateTo(MapUtils.locationToGeoPoint(currentLocation));
			
		} else {
			Toast.makeText(MyMapActivity.this, R.string.waiting_for_fix, Toast.LENGTH_SHORT).show();
		}

	}

	/**
	 * Returns total distance of the track from "tracks" table
	 * 
	 * @param trackId
	 * @return float
	 */
	private float getTrackDistance(long trackId) {

		// String sql =
		// "SELECT tracks.distance, COUNT(track_points.track_id) AS count FROM tracks, track_points WHERE tracks._id="
		// + trackId + " AND tracks._id = track_points.track_id";

		String sql = "SELECT tracks.distance FROM tracks, track_points WHERE tracks._id=" + trackId;

		Cursor cursor = app.getDatabase().rawQuery(sql, null);
		cursor.moveToFirst();

		float distance = cursor.getInt(cursor.getColumnIndex("distance"));

		cursor.close();

		return distance;

	}

	/**
	 * setup seekbar control to show current position marker on a track
	 */
	private void setupSeekBar() {

		// prevent SeekBar panel from sending touch events to map layout
		((LinearLayout) findViewById(R.id.seekBarPanel)).setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				Log.v(Constants.TAG, "seek bar touch");
				return true;
			}
		});

		seekBar = (SeekBar) findViewById(R.id.seekBar);
		seekBar.setMax(points.size() - 1);
		seekBar.setProgress(this.currentPointIndex);

		seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				updateInfoPanel(progress);
				showInfoPanels();
			}

		});

	}

	/**
	 * 
	 * @param progress
	 */
	private void updateInfoPanel(int progress) {

		currentPointIndex = progress;

		TrackPoint tp = points.get(progress);

		((TextView) findViewById(R.id.info)).setText(
				getString(R.string.distance) + ": " +
						Utils.formatDistance(tp.getDistance(), distanceUnit) + " " +
						Utils.getLocalizedDistanceUnit(MyMapActivity.this, tp.getDistance(), distanceUnit)
						+ " | " +
						getString(R.string.speed) + ": " +
						Utils.formatSpeed(tp.getSpeed(), speedUnit) + " " +
						Utils.getLocalizedSpeedUnit(MyMapActivity.this, speedUnit) + " | " +
						getString(R.string.elevation) + ": " +
						Utils.formatElevation(tp.getElevation(), elevationUnit) + " " +
						Utils.getLocalizedElevationUnit(MyMapActivity.this, elevationUnit)
				);

		// show track distance in track info area
		((TextView) findViewById(R.id.distance)).setText(getString(R.string.total_distance) + ": "
				+ Utils.formatDistance(this.trackTotalDistance, distanceUnit) + " " +
				Utils.getLocalizedDistanceUnit(this, this.trackTotalDistance, distanceUnit) + " | Point: "
				+ Integer.toString(currentPointIndex + 1));

		mapView.invalidate();

	}

	/**
	 * Loads track points from DB and populates points array
	 * 
	 * @param trackId
	 */
	private void loadTrackPoints(long trackId) {

		String sql = "SELECT * FROM track_points WHERE track_id=" + trackId + ";";
		Cursor tpCursor = app.getDatabase().rawQuery(sql, null);
		tpCursor.moveToFirst();

		points = new ArrayList<TrackPoint>();

		int latE6, lngE6, segmentIndex;

		while (tpCursor.isAfterLast() == false) {

			latE6 = tpCursor.getInt(tpCursor.getColumnIndex("lat"));
			lngE6 = tpCursor.getInt(tpCursor.getColumnIndex("lng"));

			segmentIndex = tpCursor.getInt(tpCursor.getColumnIndex("segment_index"));

			// track point object
			TrackPoint gp = new TrackPoint(new GeoPoint(latE6, lngE6), segmentIndex);
			gp.setSpeed(tpCursor.getFloat(tpCursor.getColumnIndex("speed")));
			gp.setDistance(tpCursor.getFloat(tpCursor.getColumnIndex("distance")));
			gp.setElevation(tpCursor.getFloat(tpCursor.getColumnIndex("elevation")));

			this.updateTrackSpan(latE6, lngE6);

			if (startPoint == null) {
				startPoint = gp;
			}

			points.add(gp);

			tpCursor.moveToNext();
		}

		tpCursor.close();
	}

	/**
	 * Loads waypoints to itemizedOverlay object
	 * 
	 * @param itemizedOverlay
	 */
	protected void loadWaypoints(MyItemizedOverlay itemizedOverlay) {

		Cursor cursor = app.getDatabase().rawQuery("SELECT * FROM waypoints", null);
		cursor.moveToFirst();

		while (cursor.isAfterLast() == false) {

			GeoPoint point = new GeoPoint(cursor.getInt(cursor.getColumnIndex("lat")), cursor.getInt(cursor
					.getColumnIndex("lng")));

			String snippet = Utils.formatLat(cursor.getDouble(cursor.getColumnIndex("lat")) / 1E6,
					Integer.parseInt(app.getPreferences().getString("coord_units", "0")))
					+ "\n"
					+ Utils.formatLng(cursor.getDouble(cursor.getColumnIndex("lng")) / 1E6,
							Integer.parseInt(app.getPreferences().getString("coord_units", "0")));

			if (cursor.getString(cursor.getColumnIndex("descr")) != null) {
				snippet = cursor.getString(cursor.getColumnIndex("descr")) + "\n" + snippet;
			}

			OverlayItem overlayitem = new OverlayItem(point, cursor.getString(cursor.getColumnIndex("title")), snippet);

			itemizedOverlay.addOverlay(overlayitem);

			cursor.moveToNext();
		}

		cursor.close();

	}

	/**
	 * Calculates min and max coordinates range
	 * 
	 * @param lat
	 * @param lng
	 */
	private void updateTrackSpan(int lat, int lng) {

		if (lat < minLat) {
			minLat = lat;
		}
		if (lat > maxLat) {
			maxLat = lat;
		}
		if (lng < minLng) {
			minLng = lng;
		}
		if (lng > maxLng) {
			maxLng = lng;
		}

	}

	/**
	 * 
	 */
	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	/**
	 * onCreateOptionsMenu handler
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// options menu only in track mode

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.map_menu, menu);

		return true;

	}

	/**
	 * Changes activity menu on the fly
	 */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {

		if (mode == Constants.SHOW_WAYPOINT) {
			(menu.findItem(R.id.showWaypoint)).setVisible(true);
		}

		if (mode == Constants.SHOW_TRACK || mode == Constants.SHOW_SCHEDULED_TRACK) {
			(menu.findItem(R.id.showTrack)).setVisible(true);
		}

		MenuItem mapMenuItem = menu.findItem(R.id.mapMode);
		if (mapMode == Constants.MAP_STREET) {
			mapMenuItem.setTitle(R.string.satellite);
		} else {
			mapMenuItem.setTitle(R.string.street);
		}

		return true;
	}

	/**
	 * Process main activity menu
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		// Handle item selection
		switch (item.getItemId()) {

		case R.id.showWaypoint:

			mapView.getController().setZoom(16);
			mapView.getController().animateTo(geopoint);

			return true;

		case R.id.showTrack:

			this.zoomToTrackSpanAndCenter();

			return true;

		case R.id.mapMode:

			if (mapMode == Constants.MAP_STREET) {
				mapView.setStreetView(false);
				mapView.setSatellite(true);
				mapMode = Constants.MAP_SATELLITE;
			} else {
				mapView.setSatellite(false);
				mapView.setStreetView(true);
				mapMode = Constants.MAP_STREET;
			}

			return true;

		case R.id.gotoCurrentLocation:

			this.gotoCurrentLocation();

			return true;

		default:

			return super.onOptionsItemSelected(item);

		}

	}

	/**
	 * Show info panels for track modes only
	 */
	private void showInfoPanels() {

		if (mode == Constants.SHOW_TRACK || mode == Constants.SHOW_SCHEDULED_TRACK) {
			((LinearLayout) findViewById(R.id.infoPanel)).setVisibility(View.VISIBLE);
			((LinearLayout) findViewById(R.id.seekBarPanel)).setVisibility(View.VISIBLE);
			// let's schedule hiding controls
			restartDelayedHidingControls();
		}

	}

	/**
	 * 
	 */
	private void hideInfoPanels() {
		((LinearLayout) findViewById(R.id.infoPanel)).setVisibility(View.INVISIBLE);
		((LinearLayout) findViewById(R.id.seekBarPanel)).setVisibility(View.INVISIBLE);
	}

	/**
	 * 
	 */
	private Handler delayedHidingControlsHandler = new Handler();
	private Runnable delayedHidingControlsTask = new Runnable() {
		public void run() {
			hideInfoPanels();
		}
	};

	/**
	 * 
	 */
	private void restartDelayedHidingControls() {

		delayedHidingControlsHandler.removeCallbacks(delayedHidingControlsTask);

		if (this.mode == Constants.SHOW_TRACK || this.mode == Constants.SHOW_SCHEDULED_TRACK) {
			delayedHidingControlsHandler.postDelayed(delayedHidingControlsTask, 3000);
		}
	}

}
