package com.aripuca.tracker.util;

import android.content.Context;
import android.content.res.Configuration;
import android.util.Log;

import com.aripuca.tracker.app.Constants;

public class OrientationHelper {

	private OrientationValues orientationValues;
	/**
	 * reverse landscape orientation workaround
	 */
	private int realOrientation;
	
	private Context context;
	
	public OrientationHelper(Context context) {
		
		this.context = context;

	}
	
	public void setOrientationValues(float azimuth, float pitch, float roll) {
		
		orientationValues = new OrientationValues(azimuth, pitch, roll);

		setRealOrientation(this.context.getResources().getConfiguration().orientation);
		
	}
	
	
	/**
	 * Returns compass rotation angle when orientation of the phone changes
	 */
	public int getOrientationAdjustment() {

		if (orientationValues == null) {
			return 0;
		}

		switch (realOrientation) {

			case Constants.ORIENTATION_PORTRAIT:
				return 0;
			case Constants.ORIENTATION_LANDSCAPE:
				return 90;
			case Constants.ORIENTATION_REVERSE_LANDSCAPE:
				return -90;

		}

		return 0;
	}
	
	/**
	 * reverse landscape orientation workaround
	 * 
	 * @param orientation
	 */
	private void setRealOrientation(int orientation) {

		if (orientationValues == null) {
			return;
		}

//		Log.d(Constants.TAG, "getRoll(): "+orientationValues.getRoll());
		
		if (orientation != Configuration.ORIENTATION_PORTRAIT) {

			if (orientationValues.getRoll() >= 25
					&& realOrientation != Constants.ORIENTATION_LANDSCAPE) {
				
				realOrientation = Constants.ORIENTATION_LANDSCAPE;
			}

			if (orientationValues.getRoll() <= -25
					&& realOrientation != Constants.ORIENTATION_REVERSE_LANDSCAPE) {

				realOrientation = Constants.ORIENTATION_REVERSE_LANDSCAPE;
			}

		} else {
			realOrientation = Constants.ORIENTATION_PORTRAIT;
		}

	}
	
}
