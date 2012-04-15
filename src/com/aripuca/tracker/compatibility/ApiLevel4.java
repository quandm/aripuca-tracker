package com.aripuca.tracker.compatibility;

import android.app.Activity;
import android.content.res.Configuration;
import android.view.Display;
import android.view.SurfaceView;

/**
 * API Level 4 compatibility class
 */
public class ApiLevel4 implements ApiLevel {

	/**
	 * 
	 */
	@Override
	public void setZOrderOnTop(SurfaceView surfaceView, boolean onTop) {
		// this method is not available in API4
		// let's just ignore it for now
	}

	@Override
	public int getDeviceRotation(Activity activity) {

		int orientation;

		// final int rotation = display.getOrientation();
		// if (rotation == Configuration.ORIENTATION_LANDSCAPE) { return 90; }

		Display display = activity.getWindowManager().getDefaultDisplay();

		// determining orientation based on display width and height 
		if (display.getWidth() == display.getHeight()) {
			orientation = Configuration.ORIENTATION_SQUARE;
		} else {
			if (display.getWidth() < display.getHeight()) {
				orientation = Configuration.ORIENTATION_PORTRAIT;
			} else {
				orientation = Configuration.ORIENTATION_LANDSCAPE;
			}
		}

		if (orientation == Configuration.ORIENTATION_LANDSCAPE) { return 90; }

		return 0;

	}

}
