package com.aripuca.tracker;

import java.lang.ref.WeakReference;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.aripuca.tracker.db.Track;
import com.aripuca.tracker.db.TrackPoints;
import com.aripuca.tracker.db.Tracks;
import com.aripuca.tracker.io.TrackExportTask;
import com.aripuca.tracker.io.TrackGpxExportTask;
import com.aripuca.tracker.io.TrackKmlExportTask;
import com.aripuca.tracker.map.MyMapActivity;
import com.aripuca.tracker.recorder.ScheduledTrackRecorder;
import com.aripuca.tracker.recorder.TrackRecorder;
import com.aripuca.tracker.utils.Utils;

//TODO: compare 2 tracks on the map, select track to compare

/**
 * Tracks list activity
 */
public abstract class AbstractTracksListActivity extends ListActivity {

	/**
	 * Reference to app object
	 */
	protected App app;

	protected TracksCursorAdapter cursorAdapter;

	protected Cursor cursor;

	protected int listItemResourceId;
	/**
	 * Select all tracks sql query
	 */
	protected String sqlSelectAllTracks;

	/**
	 * Workaround Issue 7139: MenuItem.getMenuInfo() returns null for sub-menu
	 * items
	 */
	protected ContextMenuInfo prevMenuInfo;

	protected TrackExportTask trackExportTask;

	/**
	 * 
	 */
	protected ProgressDialog progressDialog;

	protected int mapMode;

	/**
	 * Delete all tracks from database
	 */
	protected abstract void deleteAllTracks();
	
	/**
	 * Add track details menu based on track type
	 *  
	 * @param menu
	 */
	protected abstract void addTrackDetailsMenu(Menu menu);

	/**
	 * View track details
	 */
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {

		this.viewTrackDetails(id);

	}

	/**
	 * Start the track details activity
	 * 
	 * @param id Track id
	 */
	protected void viewTrackDetails(long id) {

		Intent intent = new Intent(this, TrackDetailsActivity.class);

		// using Bundle to pass track id into new activity
		Bundle b = new Bundle();
		b.putLong("track_id", id);

		intent.putExtras(b);

		startActivity(intent);

	}

	protected class TracksCursorAdapter extends CursorAdapter {

		/**
		 * 
		 */
		private LayoutInflater mInflater;

		/**
		 * 
		 */
		public TracksCursorAdapter(Context context, Cursor cursor, boolean autoRequery) {

			super(context, cursor, autoRequery);

			// Cache the LayoutInflate to avoid asking for a new one each time.
			mInflater = LayoutInflater.from(context);

		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {

			// Creates a view to display the items in
			final View view = mInflater.inflate(listItemResourceId, parent, false);

			return view;

		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {

			if (cursor.getCount() == 0) {
				return;
			}

			String distanceUnit = app.getPreferences().getString("distance_units", "km");

			float distance = cursor.getFloat(cursor.getColumnIndex("distance"));

			String distanceStr = Utils.formatDistance(distance, distanceUnit)
					+ Utils.getLocalizedDistanceUnit(AbstractTracksListActivity.this, distance, distanceUnit);

			String elevationUnits = app.getPreferences().getString("elevation_units", "m");

			String elevationGain = Utils.formatElevation(cursor.getFloat(cursor.getColumnIndex("elevation_gain")),
					elevationUnits) + Utils.getLocalizedElevationUnit(AbstractTracksListActivity.this, elevationUnits);

			String elevationLoss = Utils.formatElevation(cursor.getFloat(cursor.getColumnIndex("elevation_loss")),
					elevationUnits) + Utils.getLocalizedElevationUnit(AbstractTracksListActivity.this, elevationUnits);

			TextView trackTitle = (TextView) view.findViewById(R.id.track_title);
			TextView trackDetails = (TextView) view.findViewById(R.id.track_details);
			TextView scheduledTrackDetails = (TextView) view.findViewById(R.id.scheduled_track_details);

			if (trackTitle != null) {
				trackTitle.setText(Utils.shortenStr(cursor.getString(cursor.getColumnIndex("title")), 25));
			}

			if (trackDetails != null) {
				trackDetails.setText(distanceStr + " | "
						+ Utils.formatInterval(cursor.getLong(cursor.getColumnIndex("total_time")), false) + " | +"
						+ elevationGain + " | -" + elevationLoss);
			}

			if (scheduledTrackDetails != null) {
				scheduledTrackDetails.setText(DateFormat.format("yyyy-MM-dd kk:mm",
						cursor.getLong(cursor.getColumnIndex("start_time")))
						+ " - "
						+ DateFormat.format("yyyy-MM-dd kk:mm", cursor.getLong(cursor.getColumnIndex("finish_time"))));
				// "Points: " + cursor.getInt(cursor.getColumnIndex("count"))
			}

		}

	}

	/**
	 * Called when the activity is created
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {

		Log.v(Constants.TAG, "AbstractTracksListActivity onCreate");

		super.onCreate(savedInstanceState);

		this.app = (App) this.getApplication();

		this.registerForContextMenu(this.getListView());

		this.setQuery();

		this.cursor = app.getDatabase().rawQuery(this.sqlSelectAllTracks, null);

		this.cursorAdapter = new TracksCursorAdapter(this, cursor, false);
		this.setListAdapter(cursorAdapter);

	}

	/**
	 * onResume event handler
	 */
	@Override
	protected void onResume() {

		super.onResume();

		Log.v(Constants.TAG, "AbstractTracksListActivity onResume");

	}

	/**
	 * onPause event handler
	 */
	@Override
	protected void onPause() {

		Log.v(Constants.TAG, "AbstractTracksListActivity onPause");

		super.onPause();
	}

	/**
	 * 
	 */
	@Override
	protected void onDestroy() {

		Log.v(Constants.TAG, "AbstractTracksListActivity onDestroy");

		cursor.close();
		cursor = null;

		app = null;

		super.onDestroy();

	}

	/**
	 * onCreateOptionsMenu handler
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.tracks_menu, menu);
		return true;
	}

	/**
     * 
     */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		// Handle item selection
		switch (item.getItemId()) {

			case R.id.deleteTracksMenuItem:

				// check if track recording is in progress
				if (isRecordingTrack()) {
					return true;
				}

				// delete all tracks with confirmation dialog
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setMessage(R.string.are_you_sure).setCancelable(true)
						.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int id) {

								deleteTracksInOtherThread(new UpdateAfterDeleteHandler(AbstractTracksListActivity.this));

							}
						}).setNegativeButton("No", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int id) {
								Toast.makeText(AbstractTracksListActivity.this, R.string.cancelled, Toast.LENGTH_SHORT)
										.show();
								dialog.cancel();
							}
						});
				AlertDialog alert = builder.create();

				alert.show();

				return true;

			default:

				return super.onOptionsItemSelected(item);

		}

	}

	/**
	 * Update UI after delete thread finished
	 */
	private static class UpdateAfterDeleteHandler extends Handler {

		
		
		private final WeakReference<AbstractTracksListActivity> weakReference;

		UpdateAfterDeleteHandler(AbstractTracksListActivity ta) {
			weakReference = new WeakReference<AbstractTracksListActivity>(ta);
		}
		
		@Override
		public void handleMessage(Message message) {
		
			// accessing outer class using WeakReference to avoid possible memory leaks
			
			AbstractTracksListActivity tracksListActivity = weakReference.get();			
			
			tracksListActivity.cursor.requery();

			Toast.makeText(tracksListActivity, R.string.all_tracks_deleted, Toast.LENGTH_SHORT).show();
		}
	};

	/**
	 * Deleting track records in separate thread with progress dialog
	 */
	protected void deleteTracksInOtherThread(final UpdateAfterDeleteHandler handler) {

		final ProgressDialog progressDailog = ProgressDialog.show(AbstractTracksListActivity.this,
				getString(R.string.please_wait), getString(R.string.deleting_records), true);

		Thread thread = new Thread() {

			@Override
			public void run() {

				deleteAllTracks();

				// sending message to update UI handler
				handler.sendEmptyMessage(0);

				progressDailog.dismiss();
			}
		};

		thread.start();

	}

	/**
	 * 
	 */
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {

		super.onCreateContextMenu(menu, v, menuInfo);

		menu.setHeaderTitle(getString(R.string.track));
		
//		this.addTrackDetailsMenu(menu);
		
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.track_context_menu, menu);

		// AdapterView.AdapterContextMenuInfo info =
		// (AdapterView.AdapterContextMenuInfo) menuInfo;

		
		/*
		
		menu.add(Menu.NONE, 2, 2, R.string.edit);
		menu.add(Menu.NONE, 3, 3, R.string.delete);

		SubMenu exportSubMenu = menu.addSubMenu(Menu.NONE, 4, 3, R.string.export);
		exportSubMenu.add(Menu.NONE, 41, 1, R.string.export_to_gpx);
		exportSubMenu.add(Menu.NONE, 42, 2, R.string.export_to_kml);

		SubMenu exportSubMenu2 = menu.addSubMenu(Menu.NONE, 5, 4, R.string.send_as_attachment);
		exportSubMenu2.add(Menu.NONE, 51, 1, R.string.send_as_gpx);
		exportSubMenu2.add(Menu.NONE, 52, 2, R.string.send_as_kml);

		menu.add(Menu.NONE, 6, 5, R.string.show_on_map);
		
		*/

	}

	/**
	 * Handle activity menu
	 */
	@Override
	public boolean onContextItemSelected(MenuItem item) {

		AdapterContextMenuInfo mInfo = (AdapterContextMenuInfo) item.getMenuInfo();

		if (mInfo == null) {
			mInfo = (AdapterContextMenuInfo) prevMenuInfo;
		} else {
			prevMenuInfo = item.getMenuInfo();
		}

		final AdapterContextMenuInfo info = mInfo;

		switch (item.getItemId()) {

		// view track info
			case R.id.show_track_details:
				this.viewTrackDetails(info.id);
			break;

			// edit track info
			case R.id.edit:

				if (isRecordingTrack(info.id)) {
					Toast.makeText(AbstractTracksListActivity.this, R.string.cant_edit_track_being_recorded,
							Toast.LENGTH_SHORT).show();
					return true;
				}

				this.updateTrack(info.id);

			break;

			// delete track
			case R.id.delete:

				if (isRecordingTrack(info.id)) {
					Toast.makeText(AbstractTracksListActivity.this, R.string.cant_delete_track_being_recorded,
							Toast.LENGTH_SHORT).show();
					return true;
				}

				this.deleteTrack(info.id);

			break;

			// export to GPX
			case R.id.export_to_gpx:
				this.exportTrackToGpx(info.id, false);
			break;

			// export to KML
			case R.id.export_to_kml:
				this.exportTrackToKml(info.id, false);
			break;

			// export to GPX and send as attachment
			case R.id.send_as_gpx:
				this.exportTrackToGpx(info.id, true);
			break;

			// export to KML and send as attachment
			case R.id.send_as_kml:
				this.exportTrackToKml(info.id, true);
			break;

			// sync track online
			case 5:

			break;

			case R.id.show_on_map:

				this.showTrackOnMap(info.id);

			break;

			default:
				return super.onContextItemSelected(item);
		}

		return true;

	}

	protected boolean isRecordingTrack(long trackId) {

		if (TrackRecorder.getInstance(app).isRecording() && TrackRecorder.getInstance(app).getTrackStats().getTrack().getId() == trackId) {
			return true;
		}

		if (ScheduledTrackRecorder.getInstance(app).isRecording()
				&& ScheduledTrackRecorder.getInstance(app).getTrack().getId() == trackId) {
			return true;
		}

		return false;

	}

	protected boolean isRecordingTrack() {

		if (TrackRecorder.getInstance(app).isRecording()) {
			Toast.makeText(AbstractTracksListActivity.this, R.string.track_recording_in_progress, Toast.LENGTH_SHORT)
					.show();
			return true;
		}

		if (ScheduledTrackRecorder.getInstance(app).isRecording()) {
			Toast.makeText(AbstractTracksListActivity.this, R.string.scheduled_track_recording_in_progress,
					Toast.LENGTH_SHORT).show();
			return true;
		}

		return false;

	}

	/**
	 * Update track in the database
	 */
	protected void updateTrack(long id) {

		Context context = this;

		final long trackId = id;
		
		final Track track = Tracks.get(app.getDatabase(), trackId);

		LayoutInflater inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.edit_track_dialog,
				(ViewGroup) findViewById(R.id.edit_track_dialog_layout_root));

		AlertDialog.Builder builder = new AlertDialog.Builder(context);

		builder.setTitle(R.string.edit_track);
		builder.setView(layout);

		// creating reference to input field in order to use it in onClick
		// handler
		final EditText trTitle = (EditText) layout.findViewById(R.id.titleInputText);
		trTitle.setText(track.getTitle());

		final EditText trDescr = (EditText) layout.findViewById(R.id.descriptionInputText);
		trDescr.setText(track.getDescr());

		builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {

			}
		});

		builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
				dialog.dismiss();
			}
		});

		final AlertDialog dialog = builder.create();

		// override setOnShowListener in order to validate dialog without closing it
		dialog.setOnShowListener(new DialogInterface.OnShowListener() {

			@Override
			public void onShow(DialogInterface dialogInterface) {

				Button b = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
				b.setOnClickListener(new View.OnClickListener() {

					@Override
					public void onClick(View view) {

						String titleStr = trTitle.getText().toString().trim();
						String descrStr = trDescr.getText().toString().trim();

						// validate title
						if (titleStr.equals("")) {
							Toast.makeText(AbstractTracksListActivity.this, R.string.track_title_required,
									Toast.LENGTH_SHORT)
									.show();
							return;
						}
						
						// update track details in db
						Tracks.update(app.getDatabase(), trackId, titleStr, descrStr);

						cursor.requery();

						Toast.makeText(AbstractTracksListActivity.this, R.string.track_updated, Toast.LENGTH_SHORT)
								.show();

						dialog.dismiss();

					}
				});
			}
		});

		dialog.show();

	}

	/**
	 * delete track and all related track points from db
	 */
	private void deleteTrack(long id) {

		final long trackId = id;

		// delete track with confirmation dialog
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.are_you_sure).setCancelable(true)
				.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {

						Tracks.delete(app.getDatabase(), trackId);

						cursor.requery();

						Toast.makeText(AbstractTracksListActivity.this, R.string.track_deleted, Toast.LENGTH_SHORT)
								.show();
					}
				}).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});
		AlertDialog alert = builder.create();
		alert.show();

	}

	// --------------------------------------------------------------------------------------------

	/**
	 * locking device orientation. export process not to be interrupted
	 */
	private void lockOrientationChange() {

		// Stop the screen orientation changing during an event
		switch (this.getResources().getConfiguration().orientation) {
			case Configuration.ORIENTATION_PORTRAIT:
				this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			break;
			case Configuration.ORIENTATION_LANDSCAPE:
				this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
			break;
		}

	}

	public void unlockOrientationChange() {

		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

	}

	private ProgressDialog createProgressDialog(int totalPoints, String message) {

		// setting up progress dialog
		ProgressDialog pd = new ProgressDialog(this);

		pd.setMessage(message);
		pd.setCancelable(true);
		pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		pd.setProgress(0);
		pd.setMax(totalPoints);

		pd.setOnCancelListener(new OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				Log.i(Constants.TAG, "AsyncTask: onCancel()");
				// cancel exporting track task
				trackExportTask.cancel(false);
			}
		});

		return pd;

	}

	/**
	 * 
	 */
	private void exportTrackToGpx(long trackId, boolean sendAttachment) {

		// lock orientation of the screen during progress
		this.lockOrientationChange();

		int totalPoints = TrackPoints.getCount(app.getDatabase(), trackId);

		progressDialog = createProgressDialog(totalPoints, getString(R.string.creating_gpx));
		progressDialog.show();

		// starting track exporting in separate thread
		trackExportTask = new TrackGpxExportTask(AbstractTracksListActivity.this, app, trackId);
		trackExportTask.setSendAttachment(sendAttachment);
		trackExportTask.setProgressDialog(progressDialog);

		trackExportTask.execute();

	}

	/**
	 * 
	 */
	private void exportTrackToKml(long trackId, boolean sendAttachment) {

		// lock orientation of the screen during progress
		this.lockOrientationChange();

		int totalPoints = TrackPoints.getCount(app.getDatabase(), trackId);

		progressDialog = createProgressDialog(totalPoints, getString(R.string.creating_gpx));
		progressDialog.show();

		// starting track exporting in separate thread
		trackExportTask = new TrackKmlExportTask(AbstractTracksListActivity.this, app, trackId);
		trackExportTask.setSendAttachment(sendAttachment);
		trackExportTask.setProgressDialog(progressDialog);

		trackExportTask.execute();

	}

	/**
	 * 
	 */
	private void showTrackOnMap(long trackId) {

		Intent i = new Intent(this, MyMapActivity.class);

		// using Bundle to pass track id into new activity
		Bundle b = new Bundle();
		b.putInt("mode", this.mapMode);
		b.putLong("track_id", trackId);

		i.putExtras(b);
		startActivity(i);

	}

	protected void setQuery() {

	}

}
