package com.aripuca.tracker.map;

import java.util.ArrayList;
import java.util.List;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.os.Bundle;
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

import com.aripuca.tracker.App;
import com.aripuca.tracker.Constants;
import com.aripuca.tracker.R;
import com.aripuca.tracker.TrackDetailsActivity;
import com.aripuca.tracker.util.TrackPoint;
import com.aripuca.tracker.util.Utils;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
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
	private GeoPoint waypoint;

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

	/**
	 * Map overlay class
	 */
	class MapOverlay extends com.google.android.maps.Overlay {

		/**
		 * Overridden draw method
		 */
		@Override
		public boolean draw(Canvas canvas, MapView mapView, boolean shadow, long when) {

			super.draw(canvas, mapView, shadow);

			final Projection projection = mapView.getProjection();

			// display waypoint
			if (mode == Constants.SHOW_WAYPOINT) {

				this.showMapPin(projection, canvas, waypoint, R.drawable.map_pin);

			}

			// drawing the track on the map
			if (mode == Constants.SHOW_TRACK || mode == Constants.SHOW_SCHEDULED_TRACK) {

				this.drawSegments(projection, canvas);

			}

			return true;

		}

		/**
		 * Drawing segments in different colors
		 * 
		 * @param projection
		 * @param canvas
		 */
		private void drawSegments(Projection projection, Canvas canvas) {

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
			ArrayList<Path> segmentPath = new ArrayList<Path>();

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

					/*
					 * if (i > 0) { // starting new segment at last segment's end point projection.toPixels(points.get(i
					 * - 1).getGeoPoint(), screenPts); path.moveTo(screenPts.x, screenPts.y);
					 * projection.toPixels(points.get(i).getGeoPoint(), screenPts); path.lineTo(screenPts.x,
					 * screenPts.y); } else { // for the very first segment just move path pointer
					 * path.moveTo(screenPts.x, screenPts.y); }
					 */

					// starting new segment
					path.moveTo(screenPts.x, screenPts.y);
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
			this.showMapPin(projection, canvas, points.get(0).getGeoPoint(), R.drawable.marker_flag_green);
			if (points.size() > 1) {
				this.showMapPin(projection, canvas, points.get(points.size() - 1).getGeoPoint(),
						R.drawable.marker_flag_pink);
			}

			// current position marker on top of start/stop ones
			this.showMapPin(projection, canvas, points.get(currentPointIndex).getGeoPoint(),
					R.drawable.marker_flag_blue);

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
	}

	/**
	 * Called when the activity is first created
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		app = ((App) getApplicationContext());

		setContentView(R.layout.map_view);

		mapView = (MapView) findViewById(R.id.mapview);

		// initial map mode is street view
		mapMode = Constants.MAP_STREET;
		mapView.setStreetView(true);
		mapView.setSatellite(false);

		mapView.setBuiltInZoomControls(false);

		// measure units
		speedUnit = app.getPreferences().getString("speed_units", "kph");
		distanceUnit = app.getPreferences().getString("distance_units", "km");
		elevationUnit = app.getPreferences().getString("elevation_units", "m");

		// getting extra data passed to the activity
		Bundle b = getIntent().getExtras();

		this.mode = b.getInt("mode");

		// ---------------------------------------------------------------------------
		// show waypoint
		if (this.mode == Constants.SHOW_WAYPOINT) {
			this.showWaypoint(b);
		}

		// ---------------------------------------------------------------------------
		// show track
		if (this.mode == Constants.SHOW_TRACK || this.mode == Constants.SHOW_SCHEDULED_TRACK) {
			this.showTrack(b);
		}

		// ---Add a location marker---
		MapOverlay mapOverlay = new MapOverlay();

		List<Overlay> listOfOverlays = mapView.getOverlays();
		listOfOverlays.clear();
		listOfOverlays.add(mapOverlay);

		mapView.invalidate();

	}

	private void showWaypoint(Bundle b) {

		waypoint = new GeoPoint(b.getInt("latE6"), b.getInt("lngE6"));

		mapView.getController().setZoom(17);
		mapView.getController().animateTo(waypoint);

		((LinearLayout) findViewById(R.id.infoPanel)).setVisibility(View.INVISIBLE);
		((LinearLayout) findViewById(R.id.seekBarPanel)).setVisibility(View.INVISIBLE);

	}

	private void showTrack(Bundle b) {

		this.trackId = b.getLong("track_id");

		this.loadTrackPoints(this.trackId);

		this.currentPointIndex = Math.round(points.size() / 2);

		this.updateInfoPanel(this.currentPointIndex);

		this.zoomToTrackSpanAndCenter();

		// update total distance in info panel
		float distance = this.getTrackDistance(this.trackId);

		// show track distance in track info area
		((TextView) findViewById(R.id.distance)).setText(getString(R.string.total_distance) + ": "
				+ Utils.formatDistance(distance, distanceUnit) + " " +
				Utils.getLocalizedDistanceUnit(this, distance, distanceUnit));

		setupSeekBar();

	}

	private void zoomToTrackSpanAndCenter() {

		// zoom to track points span
		mapView.getController().zoomToSpan(maxLat - minLat, maxLng - minLng);

		// mapView.getController().zoomToSpan(maxLat - minLat + Math.round((maxLat - minLat) / 10),
		// maxLng - minLng + Math.round((maxLng - minLng) / 10));

		// pan to the center of track occupied area
		mapView.getController().animateTo(new GeoPoint((maxLat + minLat) / 2, (maxLng + minLng) / 2));

	}

	private float getTrackDistance(long trackId) {

		// get track data
		String sql = "SELECT tracks.*, COUNT(track_points.track_id) AS count FROM tracks, track_points WHERE tracks._id="
				+ trackId + " AND tracks._id = track_points.track_id";

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
			}

		});

	}

	private void updateInfoPanel(int progress) {

		currentPointIndex = progress;

		TrackPoint tp = points.get(progress);

		((TextView) findViewById(R.id.info)).setText(
				getString(R.string.distance) + " " +
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

			calculateTrackSpan(latE6, lngE6);

			if (startPoint == null) {
				startPoint = gp;
			}

			points.add(gp);

			tpCursor.moveToNext();
		}

		tpCursor.close();
	}

	/**
	 * Calculates min and max coordinates range
	 * 
	 * @param lat
	 * @param lng
	 */
	private void calculateTrackSpan(int lat, int lng) {

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
			(menu.findItem(R.id.showTrack)).setVisible(false);
		} else {
			(menu.findItem(R.id.showWaypoint)).setVisible(false);
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
			mapView.getController().animateTo(waypoint);

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

		default:

			return super.onOptionsItemSelected(item);

		}

	}

}
