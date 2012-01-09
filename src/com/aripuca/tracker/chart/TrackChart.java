package com.aripuca.tracker.chart;

import com.aripuca.tracker.R;
import com.aripuca.tracker.app.Constants;
import com.aripuca.tracker.util.Utils;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Paint.Align;
import android.util.Log;
import android.view.View;

public class TrackChart extends AbstractChart {

	public TrackChart(View v, String t) {
		super(v, t);
	}

	@Override
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

				// canvas.drawPoint(offsetX + p.getValueX() * scaleX, sizeY +
				// offsetY
				// - (p.getValueY() - series.get(i).getMinY()) * scaleY, paint);

			}
		}

	}

	@Override
	protected void drawLegend(Canvas canvas) {

		Paint paint = new Paint();

		paint.setTextSize(17);
		paint.setAntiAlias(true);
		paint.setTextAlign(Align.RIGHT);

		for (int i = 0; i < series.size(); i++) {
			paint.setColor(series.get(i).getColor());
			canvas.drawText(series.get(i).getLabel(), offsetLeft + sizeX, offsetTop / 2 + i * 17, paint);
		}

	}

	@Override
	protected void drawAxisXLabels(Canvas canvas) {

		Paint paint = new Paint();

		paint.setStrokeWidth(1);
		paint.setTextSize(12);
		paint.setAntiAlias(true);
		paint.setTextAlign(Align.CENTER);

		for (int i = 0; i <= 5; i++) {

			paint.setColor(0xffcccccc);

			float scaleX = (float) (sizeX / series.get(0).getMaxX());

			float step = (float) (Utils.roundIntFloor((int) series.get(0).getMaxX() / 5, 2));

			// String label = Utils.formatDistance(i * step, "km");
			String label = Utils.formatNumber(i * step / 1000, 2);

			canvas.drawText(label, offsetLeft + i * step * scaleX, offsetTop + sizeY + 18, paint);

			if (i != 0) {
				canvas.drawLine(offsetLeft + i * step * scaleX, sizeY + offsetTop, offsetLeft + i * step * scaleX,
						sizeY + offsetTop + 5, paint);
			}
		}

	}

	@Override
	protected void drawAxisYLabels(Canvas canvas) {

		Paint paint = new Paint();

		// chart
		paint.setStrokeWidth(1);

		paint.setTextSize(12);
		paint.setAntiAlias(true);
		paint.setTextAlign(Align.CENTER);

		for (int i = 0; i <= 5; i++) {


			float scaleY = (float) (sizeY / series.get(0).getRangeY());

//			float step = (float) (Utils.roundIntFloor((int) series.get(0).getRangeY() / 5, 2));
			float step = series.get(0).getRangeY() / 5;
			
			Log.d(Constants.TAG, "series.get(0).getRangeY(): "+series.get(0).getRangeY());
			Log.d(Constants.TAG, "step: "+step);

			String label = Utils.formatNumber(i * step + series.get(0).getMinY(), 0);

			if (i != 0) {

				paint.setColor(series.get(0).getColor());
				canvas.drawText(label, offsetLeft / 2, offsetTop + sizeY - i * step * scaleY, paint);

				paint.setColor(0xFFCCCCCC);
				canvas.drawLine(offsetLeft - 5, sizeY + offsetTop - i * step * scaleY, offsetLeft, sizeY + offsetTop
						- i * step * scaleY, paint);
			}
		}

	}

	@Override
	protected void drawLabels(Canvas canvas) {

		/*
		 * Paint paint = new Paint(); paint.setColor(Color.WHITE);
		 * paint.setTextSize(15); paint.setTextAlign(Align.CENTER);
		 * canvas.drawText(chartView.getContext().getString(R.string.distance),
		 * sizeX / 2 + offsetX, sizeY + offsetY + 15, paint);
		 */

		/*
		 * Path path = new Path(); path.moveTo(offsetX / 2 + 3, sizeY / 2 +
		 * paint.measureText(chartView.getContext().getString(R.string
		 * .elevation)) + 10); path.lineTo(offsetX / 2 + 3, sizeY / 2);
		 * canvas.drawTextOnPath
		 * (chartView.getContext().getString(R.string.elevation ), path, 0, 0,
		 * paint); paint.setColor(Color.BLUE); path = new Path();
		 * path.moveTo(offsetX / 2 + 3, sizeY / 2); path.lineTo(offsetX / 2 + 3,
		 * sizeY / 2 -
		 * paint.measureText(chartView.getContext().getString(R.string.speed)) -
		 * 10);
		 * canvas.drawTextOnPath(chartView.getContext().getString(R.string.speed
		 * ) , path, 0, 0, paint);
		 */

	}

}
