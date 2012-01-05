package com.aripuca.tracker;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;

public class TrackChartActivity extends Activity {

	private long trackId;
	
	/**
	 * Initialize activity
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		setContentView(R.layout.track_chart);

		Bundle b = getIntent().getExtras();

		this.trackId = b.getLong("track_id", 0);

	}
	
}
