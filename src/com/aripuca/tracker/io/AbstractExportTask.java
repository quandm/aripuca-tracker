package com.aripuca.tracker.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.aripuca.tracker.App;
import com.aripuca.tracker.Constants;
import com.aripuca.tracker.R;
import com.aripuca.tracker.TracksListActivity;
import com.aripuca.tracker.utils.AppLog;

abstract public class AbstractExportTask extends AsyncTask<Long, Integer, String> {

	protected App app;
	
	protected Context context;

	protected ProgressDialog progressDialog;
	
	protected String outputFolder;

	protected String outputFile;

	protected String extension;
	
	/**
	 * destination file
	 */
	protected File file;

	/**
	 * print writer
	 */
	protected PrintWriter pw;

	/**
	 * has email to be sent with file attached
	 */
	protected boolean sendAttachment = false;

	abstract protected void writeHeader();

	abstract protected void writeFooter();

	abstract protected boolean writePoints();
	abstract protected void writePoint(PrintWriter pw, Cursor cursor);

	/**
	 * Creates database cursors
	 */
	abstract protected void prepareCursors();

	public AbstractExportTask(Context context, App app) {

		super();

		this.app = app;
		
		this.context = context;

	}

	public void setProgressDialog(ProgressDialog pd) {

		progressDialog = pd;

	}

	public void setSendAttachment(boolean sa) {

		this.sendAttachment = sa;

	}

	protected void prepareWriter() throws IOException {

		// create file named as track title on sd card
		File outputPath = new File(app.getAppDir() + "/" + this.outputFolder);
		
		file = new File(outputPath, this.outputFile + "." + extension);

		if (!file.exists()) {
			file.createNewFile();
		}

		// overwrite existing file
		pw = new PrintWriter(new FileWriter(file, false));

	}

	/**
	 * Closes writer and cursors
	 */
	protected void closeWriter() {
		
		pw.flush();
		pw.close();
		pw = null;

	}

	@Override
	protected String doInBackground(Long... params) {

//		trackId = params[0];

		prepareCursors();

		try {

			prepareWriter();

			writeHeader();

			boolean result = writePoints();
			if (!result) {
				
				closeWriter();
				
				if (file.exists()) {
					file.delete();
				}
				
				return "Export cancelled";
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
		File outputPath = new File(app.getAppDir() + "/" + this.outputFolder);
		File zipFile = new File(outputPath, file.getName() + ".zip");

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
