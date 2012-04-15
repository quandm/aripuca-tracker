package com.aripuca.tracker.compatibility;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.view.SurfaceView;

/**
 * API Level 5 compatibility class
 */
public class ApiLevel5  extends ApiLevel4 {

	/**
	 * SurfaceView.setZOrderOnTop call through reflection 
	 */
	@Override
	public void setZOrderOnTop(SurfaceView surfaceView, boolean onTop) {

		Method method;
		try {
			
			method = Class.forName("android.view.SurfaceView").getMethod("setZOrderOnTop", boolean.class);
			
			if (method != null) {
				method.invoke(surfaceView, true);
			}
			
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}

	}

}
