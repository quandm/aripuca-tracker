package com.aripuca.tracker;

import com.aripuca.tracker.chart.ChartPoint;
import com.aripuca.tracker.chart.Series;
import com.aripuca.tracker.view.TrackChartView;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;

public class TrackChartActivity extends Activity {

	private long trackId;

	/**
	 * Reference to app object
	 */
	private App app;

	private Series elevationSeries;
	private Series speedSeries;
	
	private TrackChartView trackChartView;

	/**
	 * Initialize activity
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		app = ((App) getApplicationContext());
		
		setContentView(R.layout.track_chart);

	    ViewGroup layout = (ViewGroup) findViewById(R.id.trackChartLayout);
	    trackChartView = new TrackChartView(this);

		this.createSeries();
	    
	    trackChartView.setElevationSeries(elevationSeries);
	    trackChartView.setSpeedSeries(speedSeries);    
	    
	    LayoutParams params = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
	    layout.addView(trackChartView, params);
		
	}
	
	private void createSeries() {

		Bundle b = getIntent().getExtras();
		this.trackId = b.getLong("track_id", 0);

		// get track data
		String sql = "SELECT distance, elevation, speed FROM track_points WHERE track_id=" + this.trackId;

		Cursor cursor = app.getDatabase().rawQuery(sql, null);
		cursor.moveToFirst();

		elevationSeries = new Series(0xFFF2811D, getString(R.string.elevation));
		speedSeries = new Series(0xFFA2BF39, getString(R.string.speed));

		while (cursor.isAfterLast() == false) {
			
			float distance = cursor.getFloat(cursor.getColumnIndex("distance"));
			float elevation = cursor.getFloat(cursor.getColumnIndex("elevation"));
			float speed = cursor.getFloat(cursor.getColumnIndex("speed"));

			elevationSeries.addPoint(new ChartPoint(distance, elevation));
			speedSeries.addPoint(new ChartPoint(distance, speed));
			
			cursor.moveToNext();
		}

		cursor.close();
		
	}

}
