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
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.aripuca.tracker.MyApp;
import com.aripuca.tracker.R;
import com.aripuca.tracker.app.Constants;
import com.aripuca.tracker.map.MyMapActivity.MapOverlay;
import com.aripuca.tracker.util.TrackPoint;
import com.aripuca.tracker.util.Utils;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;
import com.google.android.maps.Projection;

/**
 * Map activity
 */
public class OverlaysMapActivity extends MapActivity {

	/**
	 * 
	 */
	private MyApp myApp;

	/**
	 * 
	 */
	private MapView mapView;

	/**
	 * Track span values
	 */
	private int minLat = (int) (180 * 1E6);
	private int maxLat = (int) (-180 * 1E6);
	private int minLng = (int) (180 * 1E6);
	private int maxLng = (int) (-180 * 1E6);

	private ArrayList<OverlayItem> overlayItems = new ArrayList<OverlayItem>();

	/**
	 * Map overlay class
	 */
	class MyItemizedOverlay extends com.google.android.maps.ItemizedOverlay {

		public MyItemizedOverlay(Drawable defaultMarker) {
			super(defaultMarker);
			populate();
		}

		/**
		 * Overridden draw method
		 */
		@Override
		public boolean draw(Canvas canvas, MapView mapView, boolean shadow, long when) {

			super.draw(canvas, mapView, shadow);
			populate();
			return true;

		}

		@Override
		protected OverlayItem createItem(int i) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public int size() {
			// TODO Auto-generated method stub
			return 0;
		}

	}

	/**
	 * Called when the activity is first created
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		myApp = ((MyApp) getApplicationContext());

		setContentView(R.layout.map_view);

		mapView = (MapView) findViewById(R.id.mapview);

		mapView.setStreetView(true);
		mapView.setSatellite(false);

		mapView.setBuiltInZoomControls(true);

		// ---Add a location marker---
//		MyItemizedOverlay mapOverlay = new MyItemizedOverlay();

		Double lat = 37.422006 * 1E6;
		Double lng = -122.084095 * 1E6;
		GeoPoint point = new GeoPoint(lat.intValue(), lng.intValue());
		OverlayItem oi = new OverlayItem(point, "Marker", "Marker Text");

		List<Overlay> listOfOverlays = mapView.getOverlays();
		listOfOverlays.clear();
//		listOfOverlays.add();

		mapView.invalidate();

	}

	/**
	 * 
	 */
	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

}
