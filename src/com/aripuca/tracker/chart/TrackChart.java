package com.aripuca.tracker.chart;

import com.aripuca.tracker.R;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

public class TrackChart extends AbstractChart {

	public TrackChart(View view) {

		super(view);

	}

	@Override
	protected void drawLabels(Canvas canvas) {

		Paint paint = new Paint();

		paint.setColor(Color.WHITE);
		paint.setTextSize(15);
		canvas.drawText(chartView.getContext().getString(R.string.distance),
				sizeX / 2 + offsetX - paint.measureText(chartView.getContext().getString(R.string.distance)) / 2,
				sizeY + offsetY + 15, paint);

		paint.setColor(Color.RED);

/*		Path path = new Path();
		path.moveTo(offsetX / 2 + 3,
				sizeY / 2 + paint.measureText(chartView.getContext().getString(R.string.elevation)) + 10);
		path.lineTo(offsetX / 2 + 3, sizeY / 2);
		canvas.drawTextOnPath(chartView.getContext().getString(R.string.elevation), path, 0, 0, paint);

		paint.setColor(Color.BLUE);

		path = new Path();
		path.moveTo(offsetX / 2 + 3, sizeY / 2);
		path.lineTo(offsetX / 2 + 3, sizeY / 2 - paint.measureText(chartView.getContext().getString(R.string.speed))
				- 10);
		canvas.drawTextOnPath(chartView.getContext().getString(R.string.speed), path, 0, 0, paint); */

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

		for (int i = 0; i < series.size(); i++) {

			paint.setColor(series.get(i).getColor());
			paint.setTextSize(16);

			canvas.drawText(series.get(i).getLabel(),
					sizeX - offsetX - paint.measureText(series.get(i).getLabel()),
					offsetY + 5 + i * 17, paint);

		}

	}

}
