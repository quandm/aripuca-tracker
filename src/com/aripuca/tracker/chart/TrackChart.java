package com.aripuca.tracker.chart;

import com.aripuca.tracker.R;
import com.aripuca.tracker.util.Utils;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Paint.Align;
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

			float scaleX = sizeX / series.get(i).getRangeX();
			float scaleY = sizeY / series.get(i).getRangeY();

			for (int j = 0; j < series.get(i).getCount(); j++) {
				Point p = series.get(i).getPoint(j);
				canvas.drawPoint(offsetX + p.getValueX() * scaleX, sizeY + offsetY
						- (p.getValueY() - series.get(i).getMinY()) * scaleY, paint);
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
			canvas.drawText(series.get(i).getLabel(),
					sizeX + offsetX,
					offsetY * 2 + i * 18, paint);
		}

	}

	@Override
	protected void drawAxisXLabels(Canvas canvas) {

		Paint paint = new Paint();

		// chart
		paint.setStrokeWidth(1);

		paint.setTextSize(12);
		paint.setAntiAlias(true);
		paint.setTextAlign(Align.CENTER);

		for (int i = 0; i <= 10; i++) {

			paint.setColor(0xffcccccc);

			float scaleX = (float) (sizeX / series.get(0).getRangeX());

			float step = (float) (Utils.roundToNearest((int) series.get(0).getRangeX() / 10, 2));

			String label = Utils.formatDistance(i * step, "km");

			canvas.drawText(label,
					offsetX + i * step * scaleX,
					sizeY + offsetY + 18, paint);

			canvas.drawLine(offsetX + i * step * scaleX,
					sizeY + offsetY - 5,
					offsetX + i * step * scaleX,
					sizeY + offsetY + 5, paint);
		}

	}

	@Override
	protected void drawLabels(Canvas canvas) {

		/*
		 * Paint paint = new Paint();
		 * 
		 * paint.setColor(Color.WHITE);
		 * paint.setTextSize(15);
		 * paint.setTextAlign(Align.CENTER);
		 * canvas.drawText(chartView.getContext().getString(R.string.distance),
		 * sizeX / 2 + offsetX, sizeY + offsetY + 15, paint);
		 */

		/*
		 * Path path = new Path();
		 * path.moveTo(offsetX / 2 + 3,
		 * sizeY / 2 +
		 * paint.measureText(chartView.getContext().getString(R.string
		 * .elevation)) + 10);
		 * path.lineTo(offsetX / 2 + 3, sizeY / 2);
		 * canvas.drawTextOnPath(chartView.getContext().getString(R.string.elevation
		 * ), path, 0, 0, paint);
		 * 
		 * paint.setColor(Color.BLUE);
		 * 
		 * path = new Path();
		 * path.moveTo(offsetX / 2 + 3, sizeY / 2);
		 * path.lineTo(offsetX / 2 + 3, sizeY / 2 -
		 * paint.measureText(chartView.getContext().getString(R.string.speed))
		 * - 10);
		 * canvas.drawTextOnPath(chartView.getContext().getString(R.string.speed)
		 * , path, 0, 0, paint);
		 */

	}

}
