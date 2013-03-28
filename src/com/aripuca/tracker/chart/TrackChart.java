package com.aripuca.tracker.chart;

import java.util.ArrayList;

import com.aripuca.tracker.Constants;
import com.aripuca.tracker.R;
import com.aripuca.tracker.utils.Utils;

import android.content.SharedPreferences;
import android.graphics.Canvas;

import android.graphics.Paint;

import android.graphics.Paint.Align;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;

public class TrackChart {

	protected ArrayList<Series> series;

	protected int sizeX;
	protected int sizeY;
	protected int totalSizeX;
	protected int totalSizeY;

	protected int offsetTop = 40;
	protected int offsetLeft = 35;
	protected int offsetRight = 20;
	protected int offsetBottom = 40;

	protected View chartView;

	private String speedUnit;
	private String speedUnitLocalized;

	private String distanceUnit;
	private String distanceUnitLocalized;

	private String elevationUnit;
	private String elevationUnitLocalized;

	public TrackChart(View v) {

		chartView = v;

		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(chartView.getContext());

		// measuring units
		speedUnit = preferences.getString("speed_units", "kph");
		speedUnitLocalized = Utils.getLocalizedSpeedUnit(chartView.getContext(), speedUnit);
		distanceUnit = preferences.getString("distance_units", "km");
		elevationUnit = preferences.getString("elevation_units", "m");
		elevationUnitLocalized = Utils.getLocalizedElevationUnit(chartView.getContext(), elevationUnit);

		totalSizeX = chartView.getWidth();
		totalSizeY = chartView.getHeight();

		sizeX = chartView.getWidth() - offsetLeft - offsetRight;
		sizeY = chartView.getHeight() - offsetTop - offsetBottom;

		this.series = new ArrayList<Series>();
	}

	protected void drawChart(Canvas canvas) {

		Paint paint = new Paint();

		// chart
		paint.setStrokeWidth(1);
		for (int i = 0; i < series.size(); i++) {

			paint.setColor(series.get(i).getColor());

			float scaleX = sizeX / series.get(i).getMaxX();
			float scaleY = sizeY / series.get(i).getRangeY();

			float startX = 0;
			float startY = 0;

			for (int j = 0; j < series.get(i).getCount(); j++) {

				ChartPoint p = series.get(i).getPoint(j);

				if (startX == 0 && startY == 0) {

					startX = offsetLeft + p.getValueX() * scaleX;
					startY = sizeY + offsetTop - (p.getValueY() - series.get(i).getMinY()) * scaleY;

					continue;
				}

				canvas.drawLine(startX, startY, offsetLeft + p.getValueX() * scaleX, sizeY + offsetTop
						- (p.getValueY() - series.get(i).getMinY()) * scaleY, paint);

				startX = offsetLeft + p.getValueX() * scaleX;
				startY = sizeY + offsetTop - (p.getValueY() - series.get(i).getMinY()) * scaleY;

			}
		}

	}

	protected void drawLegend(Canvas canvas) {

		Paint paint = new Paint();

		paint.setTextSize(20);
		paint.setAntiAlias(true);
		paint.setTextAlign(Align.CENTER);

		// elevation
		paint.setColor(series.get(0).getColor());
		canvas.drawText(series.get(0).getLabel() + ", " + elevationUnitLocalized, offsetLeft + sizeX / 3,
				offsetTop / 2, paint);

		// speed
		paint.setColor(series.get(1).getColor());
		canvas.drawText(series.get(1).getLabel() + ", " + speedUnitLocalized, offsetLeft + sizeX / 3 * 2,
				offsetTop / 2, paint);

		distanceUnitLocalized = Utils.getLocalizedDistanceUnit(chartView.getContext(), series.get(0).getMaxX(),
				distanceUnit);

		paint.setColor(0xFFCCCCCC);
		canvas.drawText(chartView.getContext().getString(R.string.distance) + ", " + distanceUnitLocalized, offsetLeft
				+ sizeX / 2, offsetTop + sizeY + offsetBottom - 5, paint);

	}

	protected void drawAxisXLabels(Canvas canvas) {

		Paint paint = new Paint();

		paint.setStrokeWidth(1);
		paint.setTextSize(13);
		paint.setAntiAlias(true);
		paint.setTextAlign(Align.CENTER);

		for (int i = 0; i <= 5; i++) {

			paint.setColor(0xffcccccc);

			float scaleX = (sizeX / series.get(0).getMaxX());

			float step = (Utils.roundToNearestFloor((int) series.get(0).getMaxX() / 5));

			String label = Utils.formatDistance(i * step, distanceUnit);

			if (step > 0) {
				canvas.drawText(label, offsetLeft + i * step * scaleX, offsetTop + sizeY + 16, paint);
				if (i != 0) {
					canvas.drawLine(offsetLeft + i * step * scaleX, sizeY + offsetTop, offsetLeft + i * step * scaleX,
							sizeY + offsetTop + 5, paint);
				}
			}

		}

	}

	protected void drawAxisYLabels(Canvas canvas) {

		Paint paint = new Paint();

		// chart
		paint.setStrokeWidth(1);

		paint.setTextSize(13);
		paint.setAntiAlias(true);
		paint.setTextAlign(Align.RIGHT);

		float scaleY1 = sizeY / series.get(0).getRangeY();
		float scaleY2 = sizeY / series.get(1).getMaxY();

		float step1 = series.get(0).getRangeY() / 5;
		float step2 = series.get(1).getMaxY() / 5;

		Log.d(Constants.TAG, "step2: " + step2 + " " + series.get(1).getRangeY());

		for (int i = 0; i <= 5; i++) {

			String label = Utils.formatElevation((double) i * step1 + series.get(0).getMinY(), elevationUnit);
			String label2 = Utils.formatSpeed((i * step2), speedUnit);

			// elevation
			paint.setColor(series.get(0).getColor());
			canvas.drawText(label, offsetLeft - 3, offsetTop + sizeY - i * step1 * scaleY1 - 3, paint);

			if (i != 0) {

				// speed
				paint.setColor(series.get(1).getColor());
				canvas.drawText(label2, offsetLeft - 3, offsetTop + sizeY - i * step2 * scaleY2 + 13, paint);

				paint.setColor(0xFFCCCCCC);
				canvas.drawLine(offsetLeft - 5, sizeY + offsetTop - i * step1 * scaleY1, offsetLeft, sizeY + offsetTop
						- i * step1 * scaleY1, paint);

				paint.setColor(0x33AAAAAA);
				canvas.drawLine(offsetLeft, sizeY + offsetTop - i * step1 * scaleY1, offsetLeft + sizeX, sizeY
						+ offsetTop - i * step1 * scaleY1, paint);

			}

		}

	}

	public void addSeries(Series s) {
		series.add(s);
	}

	protected void drawAxes(Canvas canvas) {

		Paint paint = new Paint();

		// axes
		paint.setColor(0xffcccccc);
		paint.setStrokeWidth(1);

		// y axis
		canvas.drawLine(offsetLeft, offsetTop - 10, offsetLeft, offsetTop + sizeY + 5, paint);

		// x axis
		canvas.drawLine(offsetLeft - 5, offsetTop + sizeY, offsetLeft + sizeX + 5, offsetTop + sizeY, paint);

	}

	public void draw(Canvas canvas) {

		this.drawAxisXLabels(canvas);

		this.drawAxisYLabels(canvas);

		this.drawChart(canvas);

		this.drawLegend(canvas);

		this.drawAxes(canvas);

	}

}
