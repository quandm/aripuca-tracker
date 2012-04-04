package com.aripuca.tracker.view;

import com.aripuca.tracker.R;
import com.aripuca.tracker.app.Constants;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class BubbleSurfaceView extends SurfaceView implements Runnable {

	private Thread thread;

	private SurfaceHolder holder;

	private boolean isRunning = false;

	private Bitmap bubble;

	float azimuth, roll, pitch;

	float x, y;

	public BubbleSurfaceView(Context context) {
		super(context);
		init();
	}

	public void setSensorData(float a, float r, float p) {

		this.azimuth = a;
		this.roll = r;
		this.pitch = p;

	}

	public BubbleSurfaceView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	public BubbleSurfaceView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	private void init() {

		holder = getHolder();

		// making surface transparent
		this.setZOrderOnTop(true);
		holder.setFormat(PixelFormat.TRANSPARENT);

		bubble = BitmapFactory.decodeResource(getResources(), R.drawable.ball);

	}

	@Override
	public void run() {

		while (isRunning) {

			if (!holder.getSurface().isValid()) {
				continue;
			}

			try {
				Thread.sleep(300);
			} catch (Exception e) {
				
			}

			Canvas canvas = holder.lockCanvas();

			//canvas.drawCircle(cx, cy, 300, );
			
			canvas.drawARGB(255, 0, 0, 0);

			Matrix m = new Matrix();
			m.setScale(0.25F, 0.25F);

			float scaleRoll = 90F / this.getWidth();
			float scalePitch = 90F / this.getHeight();

			x = this.roll / scaleRoll + this.getWidth() / 2;
			y = this.pitch / scalePitch + this.getHeight() / 2;

			Log.d(Constants.TAG, "Scale: " + scaleRoll + " x: " + x + " y: " + y
					+ " roll: " + this.roll + " pitch: " + this.pitch);

			//canvas.drawBitmap(bubble, m, null);
			canvas.drawBitmap(bubble, x + bubble.getWidth()/2, y + bubble.getHeight()/2, null);

			//canvas.drawBitmap(bubble, 10, 10, null);

			holder.unlockCanvasAndPost(canvas);

		}

	}

	public void pause() {

		isRunning = false;

		while (true) {

			try {
				thread.join();
			} catch (InterruptedException e) {

			}

			break;

		}

		thread = null;

	}

	public void resume() {

		isRunning = true;

		thread = new Thread(this);
		thread.start();

	}

}
