package com.aripuca.tracker;

import com.aripuca.tracker.app.Constants;
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

		elevationSeries = new Series();
		speedSeries = new Series();

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

		private int sizeX;
		private int sizeY;
		private int offsetX = 20;
		private int offsetY = 20;
		private float scaleX;
		private float scaleY;
		private float scaleX1;
		private float scaleY1;

		public SampleView(Context context) {
			super(context);

		}

		@Override
		protected void onDraw(Canvas canvas) {

			sizeX = this.getWidth() - offsetX * 2;
			sizeY = this.getHeight() - offsetY * 2;

			Log.d(Constants.TAG, "sizeX " + sizeX);
			Log.d(Constants.TAG, "sizeY " + sizeY);

			Log.d(Constants.TAG, "elevationSeries.getRangeX() " + elevationSeries.getRangeX());
			Log.d(Constants.TAG, "elevationSeries.getRangeY() " + elevationSeries.getRangeY());

			scaleX = sizeX / elevationSeries.getRangeX();
			scaleY = sizeY / elevationSeries.getRangeY();

			scaleX1 = sizeX / speedSeries.getRangeX();
			scaleY1 = sizeY / speedSeries.getRangeY();
			
			Log.d(Constants.TAG, "scaleX " + scaleX);
			Log.d(Constants.TAG, "scaleY " + scaleY);

			Paint paint = new Paint();

			//canvas.translate(offsetX, offsetY);

			canvas.drawColor(Color.WHITE);

			// background
			paint.setColor(Color.BLACK);
			Rect r = new Rect(0, 0, this.getWidth(), this.getHeight());
			canvas.drawRect(r, paint);

			// axes labels

			// chart
			paint.setColor(Color.RED);
			paint.setStrokeWidth(2);

			for (int i = 0; i < elevationSeries.getCount(); i++) {
				Point p = elevationSeries.getPoint(i);
				canvas.drawPoint(offsetX + p.getValueX() * scaleX,
						sizeY + offsetY - (p.getValueY() - elevationSeries.getMinY()) * scaleY, paint);
			}

			
			paint.setColor(Color.BLUE);

			for (int i = 0; i < speedSeries.getCount(); i++) {
				Point p = speedSeries.getPoint(i);
				canvas.drawPoint(offsetX + p.getValueX() * scaleX1,
						sizeY + offsetY - (p.getValueY() - speedSeries.getMinY()) * scaleY1, paint);
			}
			
			// axes
			paint.setColor(Color.WHITE);
			paint.setStrokeWidth(2);
			canvas.drawLine(offsetX, offsetY, offsetX, offsetY + sizeY, paint);
			canvas.drawLine(offsetX, offsetY + sizeY, offsetX + sizeX, offsetY + sizeY, paint);

			paint.setColor(Color.WHITE);
			paint.setTextSize(15);
			canvas.drawText(getString(R.string.distance), this.getWidth()/2 - 20, sizeY+offsetY+15, paint);

		}

	}

}
