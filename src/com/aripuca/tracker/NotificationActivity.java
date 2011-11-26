package com.aripuca.tracker;

import android.app.Activity;
import android.os.Bundle;

/**
 * This activity is created by Notification Manager when clicked on application
 * icon in the list of ongoing notifications in track recording mode
 */
public class NotificationActivity extends Activity {

	/**
	 * Initialize activity
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		// returning to the last activity in the stack
		finish();

	}

}
