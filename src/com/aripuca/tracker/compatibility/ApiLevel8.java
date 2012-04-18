package com.aripuca.tracker.compatibility;

import android.app.Activity;

import android.view.Display;
import android.view.Surface;

/**
 * API Level 8 compatibility class
 */
public class ApiLevel8 extends ApiLevel5 {

	/**
	 * return device rotation angle
	 */
	@Override
	public int getDeviceRotation(Activity activity) {

		Display display = activity.getWindowManager().getDefaultDisplay();

		final int rotation = display.getRotation();

		if (rotation == Surface.ROTATION_90) {
			return 90;
		} else if (rotation == Surface.ROTATION_180) {
			return 180;
		} else if (rotation == Surface.ROTATION_270) { return 270; }

		return 0;
	}

}
