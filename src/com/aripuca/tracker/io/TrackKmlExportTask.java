package com.aripuca.tracker.io;

import android.content.Context;

/**
 * Export to KML AsyncTask class
 */
public class TrackKmlExportTask extends TrackExportTask {

	public TrackKmlExportTask(Context c) {
		super(c);

		extension = "kml";
	}

	protected void writeHeader() {

		pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		pw.print("<kml");
		pw.print(" xmlns=\"http://earth.google.com/kml/2.0\"");
		pw.println(" xmlns:atom=\"http://www.w3.org/2005/Atom\">");
		pw.println("<Document>");
		pw.println("<atom:author><atom:name>AripucaTracker for Android"
						+ "</atom:name></atom:author>");
		pw.println("<name>" + tCursor.getString(tCursor.getColumnIndex("title")) + "</name>");
		pw.println("<description>" + tCursor.getString(tCursor.getColumnIndex("descr")) + "</description>");

		// track start
		pw.println("<Placemark>");
		pw.println("<name>" + tCursor.getString(tCursor.getColumnIndex("title")) + "</name>");
		pw.println("<description>" + tCursor.getString(tCursor.getColumnIndex("descr")) + "</description>");
		pw.println("<MultiGeometry><LineString><coordinates>");

	}

	protected void writeTrackPoint() {
		
		if (!segmentOpen) {
			prevSegmentIndex = tpCursor.getInt(tpCursor.getColumnIndex("segment_index"));
			segmentOpen = true;
		} 
		
		if (prevSegmentIndex != tpCursor.getInt(tpCursor.getColumnIndex("segment_index"))) {
			pw.println("</coordinates></LineString>");
			pw.println("<LineString><coordinates>");
			segmentOpen = false;
		}
		
		pw.println(tpCursor.getFloat(tpCursor.getColumnIndex("lng")) + ","
						+ tpCursor.getFloat(tpCursor.getColumnIndex("lat")) + ","
							+ tpCursor.getFloat(tpCursor.getColumnIndex("elevation")) + " ");

	}

	protected void writeFooter() {

		// end track
		pw.println("</coordinates></LineString></MultiGeometry>");
		pw.println("</Placemark>");

		// footer
		pw.println("</Document>");
		pw.println("</kml>");

	}

}
