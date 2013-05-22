package com.aripuca.tracker.io;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;

import android.app.ProgressDialog;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.aripuca.tracker.App;
import com.aripuca.tracker.Constants;
import com.aripuca.tracker.db.Track;
import com.aripuca.tracker.db.Tracks;

abstract public class TrackExportTask extends AbstractExportTask {

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

	protected String extension;

	protected boolean segmentOpen = false;
	protected int prevSegmentIndex = 0;
	protected int curSegmentIndex = 0;

	abstract protected void writeSegmentStart(PrintWriter pw);

	/**
	 * Creates database cursors
	 */
	abstract protected void prepareCursors();

	public TrackExportTask(App app, long trackId) {

		super(app);

		this.trackId = trackId;

		this.outputFolder = Constants.PATH_TRACKS;
		
		Track track = Tracks.get(app.getDatabase(), trackId);

		this.outputFile = "tr_" + (new SimpleDateFormat("yyyy-MM-dd_HH-mm")).format(track.getStartTime());

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
		File outputPath = new File(app.getAppDir() + "/" + this.outputFolder);

		String fileName = (new SimpleDateFormat("yyyy-MM-dd_HH-mm")).format(tCursor.getLong(tCursor
				.getColumnIndex("start_time")));

		file = new File(outputPath, "tr_" + fileName + "." + extension);

		if (!file.exists()) {
			file.createNewFile();
		}

		// overwrite existing file
		pw = new PrintWriter(new FileWriter(file, false));

	}

	protected boolean writePoints() {

		// write track points
		int i = 0;
		while (tpCursor.isAfterLast() == false) {

			if (!segmentOpen) {
				prevSegmentIndex = tpCursor.getInt(tpCursor.getColumnIndex("segment_index"));
				segmentOpen = true;
			}

			if (prevSegmentIndex != tpCursor.getInt(tpCursor.getColumnIndex("segment_index"))) {
				writeSegmentStart(pw);
				segmentOpen = false;
			}

			writePoint(pw, tpCursor);

			tpCursor.moveToNext();

			// safely stopping AsyncTask, removing file
			if (this.isCancelled()) {

				closeWriter();

				if (file.exists()) {
					file.delete();
				}

				return false;
			}

			if (i % 5 == 0) {
				publishProgress(i);
			}

			i++;
		}

		return true;
	}

}
