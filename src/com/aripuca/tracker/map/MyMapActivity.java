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
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.aripuca.tracker.App;
import com.aripuca.tracker.Constants;
import com.aripuca.tracker.R;
import com.aripuca.tracker.db.Track;
import com.aripuca.tracker.db.TrackPoint;
import com.aripuca.tracker.db.TrackPoints;
import com.aripuca.tracker.db.Tracks;
import com.aripuca.tracker.db.Waypoint;
import com.aripuca.tracker.db.Waypoints;
import com.aripuca.tracker.service.AppService;
import com.aripuca.tracker.service.AppServiceConnection;
import com.aripuca.tracker.utils.MapSpan;
import com.aripuca.tracker.utils.MapUtils;
import com.aripuca.tracker.utils.Utils;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;
import com.google.android.maps.Projection;

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
	 * Map span object
	 */
	private MapSpan mapSpan;

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

		@Override
		public boolean draw(Canvas canvas, MapView mapView, boolean shadow, long when) {

			super.draw(canvas, mapView, shadow);

			final Projection projection = mapView.getProjection();

			// draw path only if at least 3 points exist
			if (points.size() > 2) {

				ArrayList<Path> segmentPath = new ArrayList<Path>();

				createSegmentPathArray(projection, segmentPath);

				drawSegments(canvas, segmentPath);

				// drawing start and end map pins
				showMapPin(projection, canvas, points.get(0).getGeoPoint(), R.drawable.marker_flag_green);
				if (points.size() > 1) {
					showMapPin(projection, canvas, points.get(points.size() - 1).getGeoPoint(),
							R.drawable.marker_flag_pink);
				}

				// current position marker on top of start/stop ones
				showMapPin(projection, canvas, points.get(currentPointIndex).getGeoPoint(),
						R.drawable.marker_flag_blue);

			}

			// show current location marker
			if (currentLocation != null) {
				GeoPoint gp = MapUtils.locationToGeoPoint(currentLocation);
				showMapPinCentered(projection, canvas, gp, R.drawable.ic_maps_indicator_current_position);
			}

			return true;

		}

		/**
		 * Create an array of path segments for drawing in alternate colors
		 * 
		 * @param projection
		 * @return
		 */
		private void createSegmentPathArray(Projection projection, ArrayList<Path> segmentPaths) {

			boolean pathStarted = false;
			Point screenPts = new Point();

			int currentSegmentIndex = -1;

			Path path = null;

			// create Path objects for each segment
			for (int i = 0; i < points.size(); i++) {

				// start new segment
				if (currentSegmentIndex != points.get(i).getSegmentIndex()) {

					if (i == 0) {
						// first segment created
						path = new Path();
					} else {
						// saving previous segment
						segmentPaths.add(path);
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
					segmentPaths.add(path);
				}

			}

		}

		/**
		 * Drawing segments in different colors
		 * 
		 * @param projection
		 * @param canvas
		 */
		private void drawSegments(Canvas canvas, ArrayList<Path> segmentPath) {

			Paint paint = new Paint();
			paint.setStrokeWidth(3);
			paint.setStyle(Paint.Style.STROKE);
			paint.setAntiAlias(true);

			// drawing segments in alternate colors
			for (int i = 0; i < segmentPath.size(); i++) {

				if (i % 2 == 0) {
					paint.setColor(getResources().getColor(R.color.red));
				} else {
					paint.setColor(getResources().getColor(R.color.blue));
				}

				canvas.drawPath(segmentPath.get(i), paint);
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

			showMapPin(projection, canvas, geopoint, R.drawable.marker_flag_pink);

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
		 * @see
		 * com.google.android.maps.ItemizedOverlay#draw(android.graphics.Canvas,
		 * com.google.android.maps.MapView, boolean)
		 */
		@Override
		public void draw(Canvas canvas, MapView mapView, boolean shadow) {

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

		this.mapSpan = new MapSpan();

		switch (this.mode) {

		// show waypoint
			case Constants.SHOW_WAYPOINT:

				mapView.setBuiltInZoomControls(true);

				this.hideInfoPanels();

				this.setupWaypoint(b);

				// add overlays to the map
				WaypointOverlay waypointOverlay = new WaypointOverlay();

				listOfOverlays.clear();
				listOfOverlays.add(waypointOverlay);

			break;

			case Constants.SHOW_ALL_WAYPOINTS:

				mapView.setBuiltInZoomControls(true);

				this.hideInfoPanels();

				Drawable drawable = this.getResources().getDrawable(R.drawable.marker_flag_pink);
				drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());

				// put all waypoints into itemized overlay
				MyItemizedOverlay itemizedOverlay = new MyItemizedOverlay(drawable, this);
				this.populateItemizedOverlay(itemizedOverlay);

				listOfOverlays.clear();
				listOfOverlays.add(itemizedOverlay);

				this.zoomToSpanAndCenter();

			break;

			// show track
			case Constants.SHOW_TRACK:
			case Constants.SHOW_SCHEDULED_TRACK:

				mapView.setBuiltInZoomControls(false);

				this.trackId = b.getLong("track_id");

				this.showInfoPanels();

				this.setupTrack();

				this.setupSeekBar();

				// add overlays to the map
				TrackOverlay trackOverlay = new TrackOverlay();

				listOfOverlays.clear();
				listOfOverlays.add(trackOverlay);

				this.zoomToSpanAndCenter();

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

			if (serviceConnection == null) {
				return;
			}

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
				// keep listening for location
				appService.setGpsInUse(true);
			}

			// this activity requires compass data
			appService.startSensorUpdates();

			currentLocation = appService.getCurrentLocation();
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
		// mapView.setStreetView(true);
		mapView.setSatellite(false);

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

		// load all track points to array list
		this.points = TrackPoints.getAll(app.getDatabase(), this.trackId, mapSpan);

		this.trackTotalDistance = Tracks.get(app.getDatabase(), this.trackId).getDistance();

		this.currentPointIndex = Math.round(points.size() / 2);

		this.updateInfoPanel(this.currentPointIndex);

	}

	/**
	 * Show entire track on the map with maximum zoom level
	 */
	private void zoomToSpanAndCenter() {

		// zoom to track points span
		mapView.getController().zoomToSpan(mapSpan.getLatRange(), mapSpan.getLngRange());

		// pan to the center of track occupied area
		mapView.getController().animateTo(mapSpan.getCenter());

	}

	/**
	 * Show entire track and current location on the map with maximum zoom level
	 */
	private void gotoCurrentLocation() {

		if (currentLocation != null) {

			// include current location in track span
			//			mapSpan.updateMapSpan((int) (currentLocation.getLatitude() * 1E6),
			//					(int) (currentLocation.getLongitude() * 1E6));
			//			this.zoomToSpanAndCenter();

			mapView.getController().setZoom(16); // 1..21
			mapView.getController().animateTo(MapUtils.locationToGeoPoint(currentLocation));

		} else {
			Toast.makeText(MyMapActivity.this, R.string.waiting_for_fix, Toast.LENGTH_SHORT).show();
		}

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

		if (points.size() == 0) {
			// no points recorded
			Toast.makeText(MyMapActivity.this, R.string.no_points_recorded, Toast.LENGTH_SHORT)
					.show();

			return;
		}

		currentPointIndex = progress;

		TrackPoint tp = points.get(progress);

		float distanceToFinish = Math.abs(this.trackTotalDistance - tp.getDistance());

		// show track distance in track info area
		((TextView) findViewById(R.id.distance)).setText(
				getString(R.string.distance) + ": " +
						Utils.formatDistance(this.trackTotalDistance, distanceUnit) + " " +
						Utils.getLocalizedDistanceUnit(MyMapActivity.this, this.trackTotalDistance, distanceUnit)
						+ " | " +
						getString(R.string.distance_from_start) + ": " +
						Utils.formatDistance(tp.getDistance(), distanceUnit) + " " +
						Utils.getLocalizedDistanceUnit(MyMapActivity.this, tp.getDistance(), distanceUnit) + " | " +
						getString(R.string.distance_to_end) + ": " +
						Utils.formatDistance(distanceToFinish, distanceUnit) + " " +
						Utils.getLocalizedDistanceUnit(MyMapActivity.this, distanceToFinish,
								distanceUnit));

		((TextView) findViewById(R.id.info)).setText(
				getString(R.string.speed) + ": " +
						Utils.formatSpeed(tp.getSpeed(), speedUnit) + " " +
						Utils.getLocalizedSpeedUnit(MyMapActivity.this, speedUnit) + " | " +
						getString(R.string.elevation) + ": " +
						Utils.formatElevation(tp.getElevation(), elevationUnit) + " " +
						Utils.getLocalizedElevationUnit(MyMapActivity.this, elevationUnit) + " | " +
						getString(R.string.point) + ": " +
						Integer.toString(currentPointIndex + 1));

		mapView.invalidate();

	}

	/**
	 * Loads waypoints to itemizedOverlay object
	 * 
	 * @param itemizedOverlay
	 */
	protected void populateItemizedOverlay(MyItemizedOverlay itemizedOverlay) {

		ArrayList<Waypoint> waypoints = new ArrayList<Waypoint>();

		// load all waypoints
		Waypoints.getAll(app.getDatabase(), waypoints);

		// populate itemized overlay
		for (Waypoint waypoint : waypoints) {

			GeoPoint point = new GeoPoint(waypoint.getLatE6(), waypoint.getLngE6());

			String snippet = Utils.formatLat(waypoint.getLat(),
					Integer.parseInt(app.getPreferences().getString("coord_units", "0")))
					+ "\n"
					+ Utils.formatLng(waypoint.getLng(),
							Integer.parseInt(app.getPreferences().getString("coord_units", "0")));

			if (waypoint.getDescr() != null) {
				snippet = waypoint.getDescr() + "\n" + snippet;
			}

			OverlayItem overlayitem = new OverlayItem(point, waypoint.getTitle(), snippet);

			itemizedOverlay.addOverlay(overlayitem);

			mapSpan.updateMapSpan(waypoint.getLatE6(), waypoint.getLngE6());

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

				this.zoomToSpanAndCenter();

				return true;

			case R.id.mapMode:

				// mapView.setStreetView(true|false) should not be called in order to prevent displaying map tiles with
				// crosses (API8)

				if (mapMode == Constants.MAP_STREET) {
					mapMode = Constants.MAP_SATELLITE;
					mapView.setSatellite(true);
				} else {
					mapMode = Constants.MAP_STREET;
					mapView.setSatellite(false);
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
		@Override
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
