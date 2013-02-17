package com.aripuca.tracker.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.aripuca.tracker.App;
import com.aripuca.tracker.Constants;
import com.aripuca.tracker.R;
import com.aripuca.tracker.TracksListActivity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

abstract public class TrackExportTask extends AsyncTask<Long, Integer, String> {

	protected App app;

	protected Context context;

	protected ProgressDialog progressDialog;

	protected long trackId;

	/**
	 * tracks table cursor
	 */
	protected Cursor tCursor = null;

	/**
	 * track points or waypoints table cursor
	 */
	protected Cursor tpCursor = null;

	/**
	 * destination file
	 */
	protected File file;

	/**
	 * print writer
	 */
	protected PrintWriter pw;

	abstract protected void writeHeader();

	abstract protected void writeTrackPoint();

	abstract protected void writeFooter();

	protected String extension;

	protected boolean segmentOpen = false;
	protected int prevSegmentIndex = 0;
	protected int curSegmentIndex = 0;

	/**
	 * has email to be sent with file attached
	 */
	protected boolean sendAttachment = false;

	public TrackExportTask(Context c) {

		super();

		context = c;

	}

	public void setApp(App m) {

		app = m;

	}

	public void setProgressDialog(ProgressDialog pd) {

		progressDialog = pd;

	}

	public void setSendAttachment(boolean sa) {

		this.sendAttachment = sa;

	}

	protected void prepareWriter() throws IOException {

		// create file named as track title on sd card
		File outputFolder = new File(app.getAppDir() + "/tracks");

		String fileName = (new SimpleDateFormat("yyyy-MM-dd_HH-mm")).format(tCursor.getLong(tCursor
				.getColumnIndex("start_time")));

		file = new File(outputFolder, "tr_" + fileName + "." + extension);

		if (!file.exists()) {
			file.createNewFile();
		}

		// overwrite existing file
		pw = new PrintWriter(new FileWriter(file, false));

	}

	/**
	 * Creates database cursors
	 */
	protected void prepareCursors() {

		// tracks table cursor
		String sql = "SELECT * FROM tracks WHERE _id=" + trackId + ";";
		tCursor = app.getDatabase().rawQuery(sql, null);
		tCursor.moveToFirst();

		// track points table cursor
		sql = "SELECT * FROM track_points WHERE track_id=" + trackId + ";";
		tpCursor = app.getDatabase().rawQuery(sql, null);
		tpCursor.moveToFirst();

	}

	/**
	 * Closes writer and cursors
	 */
	protected void closeWriter() {

		pw.flush();
		pw.close();
		pw = null;

		if (tCursor != null) {
			tCursor.close();
		}

		if (tpCursor != null) {
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

		Log.d(Constants.TAG, "onCancelled");

		app = null;
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

		Toast.makeText(context, R.string.export_completed, Toast.LENGTH_SHORT).show();

		if (context instanceof TracksListActivity) {
			((TracksListActivity) context).unlockOrientationChange();
		}

		// send email with file attached
		if (this.sendAttachment) {
			
			this.zipAndSendAttachment();
			
		}

		app = null;
		progressDialog = null;

	}

	/**
	 * 
	 */
	protected void zipAndSendAttachment() {

		// let's compress file before attaching
		File outputFolder = new File(app.getAppDir() + "/tracks");
		File zipFile = new File(outputFolder, file.getName() + ".zip");

		//TODO: bug: zip file created incorrectly
		try {

			final int BUFFER = 2048;

			BufferedInputStream origin = null;
			FileOutputStream dest = new FileOutputStream(zipFile);

			ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));
			out.setMethod(ZipOutputStream.DEFLATED);
			out.setLevel(5);

			byte data[] = new byte[BUFFER];

			FileInputStream fi = new FileInputStream(file);

			origin = new BufferedInputStream(fi, BUFFER);

			ZipEntry entry = new ZipEntry(file.getName());
			out.putNextEntry(entry);

			int count;
			while ((count = origin.read(data, 0, BUFFER)) != -1) {
				out.write(data, 0, count);
			}

			out.closeEntry();

			origin.close();
			out.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

		String messageBody = context.getString(R.string.email_body_track) + "\n\n"
				+ context.getString(R.string.market_url);

		// sending file by email using default Android email client
		
		final Intent emailIntent = new Intent(Intent.ACTION_SEND);
		emailIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		emailIntent.setType("plain/text");
		emailIntent.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.email_subject_track));
		emailIntent.putExtra(Intent.EXTRA_TEXT, messageBody);
		emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + zipFile.getAbsolutePath()));
		
		context.startActivity(Intent.createChooser(emailIntent, context.getString(R.string.sending_email)));

	}

}
