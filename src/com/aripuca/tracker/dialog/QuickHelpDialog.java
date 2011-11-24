package com.aripuca.tracker.dialog;

import com.aripuca.tracker.MainActivity;
import com.aripuca.tracker.MyApp;
import com.aripuca.tracker.R;
import com.aripuca.tracker.track.TrackRecorder;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

public class QuickHelpDialog extends Dialog {

	private Context mContext;
	
	/**
	 * Reference to Application object
	 */
	private MyApp myApp;
	
	public QuickHelpDialog(Context context) {
		super(context);
		
		mContext = context;
		
		myApp = ((MyApp) mContext.getApplicationContext());

	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.quick_help_dialog);
		setTitle(R.string.do_you_know);

		LayoutParams params = getWindow().getAttributes();
		
		//params.height = LayoutParams.FILL_PARENT;
		params.width = LayoutParams.FILL_PARENT;
		
		getWindow().setAttributes((android.view.WindowManager.LayoutParams) params);
		
		TextView text = (TextView) findViewById(R.id.helpText);
		text.setText(getNextHelpAdvice());
		
		// set close button event listener
		Button closeButton = (Button) findViewById(R.id.closeButton);
		closeButton.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				
				// check dontShowNextTime checkbox
				CheckBox checkBox = (CheckBox) findViewById(R.id.showNextTime);
				if (!checkBox.isChecked()) {

					if (myApp.getPreferences().getBoolean("quick_help", true)==true) {
						SharedPreferences.Editor editor = myApp.getPreferences().edit();
						editor.putBoolean("quick_help", false);
						editor.commit();
					}
					
				} else {
					
					if (myApp.getPreferences().getBoolean("quick_help", true)==false) {
						SharedPreferences.Editor editor = myApp.getPreferences().edit();
						editor.putBoolean("quick_help", true);
						editor.commit();
					}
					
				}
				
				dismiss();
			}
		}
		);
		
		// set next button event listener
		Button nextButton = (Button) findViewById(R.id.nextButton);
		nextButton.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {

				// show next help advice
				TextView text = (TextView) findViewById(R.id.helpText);
				text.setText(getNextHelpAdvice());
				
			}
		}
		);

		
	}
	
	private String getNextHelpAdvice() {
		
		String[] items = mContext.getResources().getStringArray(R.array.quick_help);
		
		int id = (int) Math.round(Math.random() * (items.length-1));
		
        return items[id];
		
	}
	

}
