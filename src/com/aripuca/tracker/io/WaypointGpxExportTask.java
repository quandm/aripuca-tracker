package com.aripuca.tracker.io;

import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.Context;
import android.database.Cursor;

import com.aripuca.tracker.App;
import com.aripuca.tracker.Constants;

public class WaypointGpxExportTask extends AbstractExportTask {

	protected Cursor wpCursor;
	
	public WaypointGpxExportTask(Context context, App app, String outputFile) {
		
		super(context, app);

		this.extension = "gpx";

		this.outputFile = outputFile;

		this.outputFolder = Constants.PATH_WAYPOINTS;
		
	}
	
	@Override
	protected void prepareCursors() {

		// only one cursor is required for waypoints export 
		
		// waypoints cursor
		String sql = "SELECT * FROM waypoints;";
		wpCursor = app.getDatabase().rawQuery(sql, null);
		wpCursor.moveToFirst();

	}

	@Override
	protected void writeHeader() {

		String todayDate = (new SimpleDateFormat("yyyy-MM-dd")).format((new Date()).getTime());

		// write gpx header
		pw.format("<?xml version=\"1.0\" encoding=\"%s\" standalone=\"yes\"?>\n", Charset.defaultCharset().name());
		pw.println("<gpx");
		pw.println(" version=\"1.1\"");
		pw.println(" creator=\"AripucaTracker for Android\"");
		pw.println(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
		pw.println(" xmlns=\"http://www.topografix.com/GPX/1/1\"");
		pw.print(" xmlns:topografix=\"http://www.topografix.com/GPX/Private/TopoGrafix/0/1\"");
		pw.print(" xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 ");
		pw.print("http://www.topografix.com/GPX/1/1/gpx.xsd ");
		pw.print("http://www.topografix.com/GPX/Private/TopoGrafix/0/1 ");
		pw.println("http://www.topografix.com/GPX/Private/TopoGrafix/0/1/topografix.xsd\">");

		pw.println("<time>" + todayDate + "</time>");

	}

	/**
	 * 
	 */
	@Override
	protected boolean writePoints() {
		
		// write track points
		int i = 0;
		while (wpCursor.isAfterLast() == false) {

			writePoint(pw, wpCursor);
			
			wpCursor.moveToNext();

			// safely stopping AsyncTask, removing file
			if (this.isCancelled()) {
				return false;
			}

			if (i % 5 == 0) {
				publishProgress(i);
			}

			i++;
		}
	
		return true;
	}
	
	@Override
	protected void writePoint(PrintWriter pw, Cursor cursor) {
		
		ExportHelper.writeGPXWaypoint(pw, cursor);
		
	}
	
	@Override
	protected void writeFooter() {

		// footer
		pw.println("</gpx>");

	}

	protected void closeWriter() {

		super.closeWriter();
		
		if (wpCursor != null) {
			wpCursor.close();
		}

	}
}
