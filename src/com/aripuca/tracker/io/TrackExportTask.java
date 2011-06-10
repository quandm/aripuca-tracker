package com.aripuca.tracker.io;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;

import com.aripuca.tracker.MyApp;
import com.aripuca.tracker.R;
import com.aripuca.tracker.TracksListActivity;

import android.app.ProgressDialog;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.widget.Toast;

abstract public class TrackExportTask extends AsyncTask<Long, Integer, String> {
	
	protected MyApp myApp;
	
	protected Context context;
	
	protected ProgressDialog progressDialog;
	
	protected long trackId;

	protected Cursor tCursor = null;

	protected Cursor tpCursor = null;

	protected File file;

	protected PrintWriter pw;

	abstract protected void writeHeader();

	abstract protected void writeTrackPoint();

	abstract protected void writeFooter();
	
	protected String extension;
	
	protected boolean segmentOpen = false;
	protected int prevSegmentId = 0;
	protected int curSegmentId = 0;
	
	public TrackExportTask(Context c) {
		
		super();
		
		context = c;
		
	}
	
	public void setApp(MyApp m) {
		
		myApp = m;
		
	}
	
	public void setProgressDialog(ProgressDialog pd) {
		
		progressDialog = pd;
				
	}
	
	protected void prepareWriter() throws IOException {

		// create file named as track title on sd card
		File outputFolder = new File(myApp.getAppDir() + "/tracks");

		String fileName = (new SimpleDateFormat("yyyy-MM-dd_HH-mm")).format(tCursor.getLong(tCursor
					.getColumnIndex("start_time")));

		file = new File(outputFolder, "tr_" + fileName + "."+extension);

		if (!file.exists()) {
			file.createNewFile();
		}

		// overwrite existing file
		pw = new PrintWriter(new FileWriter(file, false));

	}
	
	protected void prepareCursors() {

		// track cursor
		String sql = "SELECT * FROM tracks WHERE _id=" + trackId + ";";
		tCursor = myApp.getDatabase().rawQuery(sql, null);
		tCursor.moveToFirst();

		sql = "SELECT * FROM track_points WHERE track_id=" + trackId + ";";
		tpCursor = myApp.getDatabase().rawQuery(sql, null);
		tpCursor.moveToFirst();

	}

	protected void closeWriter() {

		pw.flush();
		pw.close();
		pw = null;

		if (tCursor!=null) {
			tCursor.close();
		}
		
		if (tpCursor!=null) {
			tpCursor.close();
		}

	}
	

	@Override
	protected String doInBackground(Long... params) {

		trackId = params[0];

		prepareCursors();

		try {

			prepareWriter();

			// write format header
			writeHeader();

			// write track points
			int i = 0;
			while (tpCursor.isAfterLast() == false) {

				writeTrackPoint();

				tpCursor.moveToNext();

				// safely stopping AsyncTask, removing file
				if (this.isCancelled()) {

					closeWriter();

					if (file.exists()) {
						file.delete();
					}

					return "Export cancelled";
				}

				if (i % 5 == 0) {
					publishProgress(i);
				}

				i++;
			}

			writeFooter();

			closeWriter();

		} catch (IOException e) {
			cancel(true);
			return e.getMessage();
		}

		return "Export completed";
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();

	}
	
	/**
	 * 
	 */
	@Override
	protected void onCancelled() {
		super.onCancelled();
		
		Toast.makeText(context, R.string.cancelled, Toast.LENGTH_SHORT).show();
		
		myApp = null;
		progressDialog = null;

	}
	
	/**
	 * Update UI thread safely
	 * 
	 * @param values
	 */
	@Override
	protected void onProgressUpdate(Integer... values) {
		super.onProgressUpdate(values);

		if (progressDialog != null) {
			progressDialog.incrementProgressBy(5);
		}
		
	}
	
	/**
	 * Update UI thread from here
	 */
	@Override
	protected void onPostExecute(String result) {
		super.onPostExecute(result);

		if (progressDialog != null) {
			progressDialog.dismiss();
		}

//		Toast.makeText(context, result, Toast.LENGTH_SHORT).show();

		if (context instanceof TracksListActivity) {
			((TracksListActivity) context).unlockOrientationChange();
		}
		
		myApp = null;
		progressDialog = null;

	}
	
}
