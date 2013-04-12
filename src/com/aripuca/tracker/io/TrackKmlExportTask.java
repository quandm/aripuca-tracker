package com.aripuca.tracker.io;

import android.content.Context;

import com.aripuca.tracker.utils.Utils;

/**
 * Export to KML AsyncTask class
 */
public class TrackKmlExportTask extends TrackExportTask {

	public TrackKmlExportTask(Context c) {
		super(c);

		extension = "kml";
	}

	@Override
	protected void writeHeader() {

		pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		pw.print("<kml");
		pw.print(" xmlns=\"http://earth.google.com/kml/2.0\"");
		pw.println(" xmlns:atom=\"http://www.w3.org/2005/Atom\">");
		pw.println("<Document>");
		pw.println("<atom:author><atom:name>Aripuca GPS Tracker for Android" + "</atom:name></atom:author>");
		pw.println("<name>" + tCursor.getString(tCursor.getColumnIndex("title")) + "</name>");
		pw.println("<description>" + tCursor.getString(tCursor.getColumnIndex("descr")) + "</description>");

		// track start
		pw.println("<Placemark>");
		pw.println("<name>" + tCursor.getString(tCursor.getColumnIndex("title")) + "</name>");
		pw.println("<description>" + tCursor.getString(tCursor.getColumnIndex("descr")) + "</description>");
		pw.println("<MultiGeometry><LineString><coordinates>");

	}

	@Override
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

		String lat = Utils.formatCoord(tpCursor.getInt(tpCursor.getColumnIndex("lat")) / 1E6);
		String lng = Utils.formatCoord(tpCursor.getInt(tpCursor.getColumnIndex("lng")) / 1E6);

		pw.println(lng + "," + lat + ","
				+ Utils.formatNumberUS(tpCursor.getFloat(tpCursor.getColumnIndex("elevation")), 1) + " ");

	}

	@Override
	protected void writeFooter() {

		// end track
		pw.println("</coordinates></LineString></MultiGeometry>");
		pw.println("</Placemark>");

		// footer
		pw.println("</Document>");
		pw.println("</kml>");

	}

}
