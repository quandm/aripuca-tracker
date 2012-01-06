package com.aripuca.tracker;

import com.aripuca.tracker.app.Constants;
import com.aripuca.tracker.chart.LineChart;
import com.aripuca.tracker.chart.Point;
import com.aripuca.tracker.chart.Series;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class TrackChartActivity extends Activity {

	private long trackId;

	/**
	 * Reference to myApp object
	 */
	private MyApp myApp;

	private Series elevationSeries;
	private Series speedSeries;

	/**
	 * Initialize activity
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		//		setContentView(R.layout.track_chart);
		setContentView(new SampleView(this));

		myApp = ((MyApp) getApplicationContext());

		Bundle b = getIntent().getExtras();

		this.trackId = b.getLong("track_id", 0);

		// get track data
		String sql = "SELECT distance, elevation, speed FROM track_points WHERE track_id=" + this.trackId;

		Cursor cursor = myApp.getDatabase().rawQuery(sql, null);
		cursor.moveToFirst();

		elevationSeries = new Series(Color.RED);
		speedSeries = new Series(Color.BLUE);

		while (cursor.isAfterLast() == false) {
			
			float distance = cursor.getFloat(cursor.getColumnIndex("distance"));
			float elevation = cursor.getFloat(cursor.getColumnIndex("elevation"));
			float speed = cursor.getFloat(cursor.getColumnIndex("speed"));

			elevationSeries.addPoint(new Point(distance, elevation));
			speedSeries.addPoint(new Point(distance, speed));
			
			cursor.moveToNext();
		}

		cursor.close();

	}

	private class SampleView extends View {

		private int offsetX = 20;
		private int offsetY = 20;
		
		public SampleView(Context context) {
			super(context);

		}

		@Override
		protected void onDraw(Canvas canvas) {

			int sizeX = this.getWidth() - offsetX * 2;
			int sizeY = this.getHeight() - offsetY * 2;
			
			// background
			Paint paint = new Paint();
			paint.setColor(Color.BLACK);
			Rect r = new Rect(0, 0, this.getWidth(), this.getHeight());
			canvas.drawRect(r, paint);

			LineChart lineChart = new LineChart(sizeX, sizeY);
			lineChart.addSeries(elevationSeries);
			lineChart.addSeries(speedSeries);
			lineChart.draw(canvas);
			
		}

	}

}
