package com.aripuca.tracker.chart;

import java.util.ArrayList;

import com.aripuca.tracker.R;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;

public class AbstractChart {

	private ArrayList<Series> series;

	private int sizeX;
	private int sizeY;

	private int offsetX = 20;
	private int offsetY = 20;

	public AbstractChart(int sizeX, int sizeY) {

		this.sizeX = sizeX;
		this.sizeY = sizeY;

		this.series = new ArrayList<Series>();

	}

	public void addSeries(Series s) {
		series.add(s);
	}

	public void draw(Canvas canvas) {

		Paint paint = new Paint();

		// chart
		paint.setStrokeWidth(1);
		for (int i = 0; i < series.size(); i++) {
			paint.setColor(series.get(i).getColor());
			
			float scaleX = sizeX/series.get(i).getRangeX();
			float scaleY = sizeY/series.get(i).getRangeY();
			
			for (int j = 0; j < series.get(i).getCount(); j++) {
				Point p = series.get(i).getPoint(j);
				canvas.drawPoint(offsetX + p.getValueX() * scaleX, sizeY + offsetY
						- (p.getValueY() - series.get(i).getMinY()) * scaleY, paint);
			}
		}

		// axes
		paint.setColor(Color.WHITE);
		paint.setStrokeWidth(2);
		canvas.drawLine(offsetX, offsetY, offsetX, offsetY + sizeY, paint);
		canvas.drawLine(offsetX, offsetY + sizeY, offsetX + sizeX, offsetY + sizeY, paint);

		paint.setColor(Color.WHITE);
		paint.setTextSize(15);
		canvas.drawText("Distance", sizeX / 2, sizeY + offsetY + 15, paint);
		
		paint.setColor(Color.RED);
	
		Path path = new Path();
		path.moveTo(offsetX/2+3, sizeY/2 + paint.measureText("Elevation") + 10);
		path.lineTo(offsetX/2+3, sizeY/2);
		canvas.drawTextOnPath("Elevation", path, 0, 0, paint);

		paint.setColor(Color.BLUE);
		
		path = new Path();
		path.moveTo(offsetX/2+3, sizeY/2);
		path.lineTo(offsetX/2+3, sizeY/2 - paint.measureText("Speed") - 10);
		canvas.drawTextOnPath("Speed", path, 0, 0, paint);
		

	}

}
