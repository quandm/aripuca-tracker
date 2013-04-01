package com.aripuca.tracker.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import com.aripuca.tracker.App;
import com.aripuca.tracker.R;

/**
 * Quick help custom dialog Shows random help advice every time it's shown
 */
public class QuickHelpDialog extends Dialog {

	private Context context;

	/**
	 * Reference to Application object
	 */
	private App app;

	public QuickHelpDialog(Context context) {

		super(context);

		this.context = context;

		app = ((App) context.getApplicationContext());

	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.quick_help_dialog);
		setTitle(R.string.do_you_know);

		LayoutParams params = getWindow().getAttributes();

		// params.height = LayoutParams.FILL_PARENT;
		params.width = android.view.ViewGroup.LayoutParams.FILL_PARENT;

		getWindow().setAttributes(params);

		// setting "showNextTime" initial value
		CheckBox checkBox = (CheckBox) findViewById(R.id.showNextTime);
		checkBox.setChecked(app.getPreferences().getBoolean("quick_help", true));

		TextView text = (TextView) findViewById(R.id.helpText);
		text.setText(getNextHelpAdvice());

		// set close button event listener
		Button closeButton = (Button) findViewById(R.id.closeButton);
		closeButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {

				// check dontShowNextTime checkbox
				CheckBox checkBox = (CheckBox) findViewById(R.id.showNextTime);
				if (!checkBox.isChecked()) {

					if (app.getPreferences().getBoolean("quick_help", true) == true) {
						SharedPreferences.Editor editor = app.getPreferences().edit();
						editor.putBoolean("quick_help", false);
						editor.commit();
					}

				} else {

					if (app.getPreferences().getBoolean("quick_help", true) == false) {
						SharedPreferences.Editor editor = app.getPreferences().edit();
						editor.putBoolean("quick_help", true);
						editor.commit();
					}

				}

				dismiss();
			}
		});

		// set next button event listener
		Button nextButton = (Button) findViewById(R.id.nextButton);
		nextButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {

				// show next help advice
				TextView text = (TextView) findViewById(R.id.helpText);
				text.setText(getNextHelpAdvice());

			}
		});

	}

	private String getNextHelpAdvice() {

		String[] items = context.getResources().getStringArray(R.array.quick_help);

		int id = (int) Math.round(Math.random() * (items.length - 1));

		return items[id];

	}

}
