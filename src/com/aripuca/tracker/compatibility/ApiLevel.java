package com.aripuca.tracker.compatibility;

import android.app.Activity;
import android.view.SurfaceView;

/**
 * ApiLevel compatibility interface 
 */
public interface ApiLevel {

	/**
	 * return device rotation angle 
	 */
	public int getDeviceRotation(Activity activity);

	/**
	 * SurfaceView.setZOrderOnTop call through reflection 
	 */
	public void setZOrderOnTop(SurfaceView surfaceView, boolean onTop);
	
}
