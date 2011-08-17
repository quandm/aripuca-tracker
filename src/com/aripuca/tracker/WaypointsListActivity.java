package com.aripuca.tracker;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.aripuca.tracker.io.WaypointGpxExportTask;
import com.aripuca.tracker.util.Utils;
import com.aripuca.tracker.view.CompassImage;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;

import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import android.widget.AdapterView.AdapterContextMenuInfo;

/**
 * Waypoints list activity
 */
public class WaypointsListActivity extends ListActivity {

	/**
	 * Reference to myApp object
	 */
	private MyApp myApp;

	private String importWaypointsFileName;

	private WaypointsArrayAdapter waypointsArrayAdapter;

	private ArrayList<Waypoint> waypoints;

	/**
	 * Location updates broadcast receiver
	 */
	protected BroadcastReceiver locationBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

			//			Log.d(Constants.TAG, "WaypointsListActivity: LOCATION BROADCAST MESSAGE RECEIVED");

			waypointsArrayAdapter.sortByDistance();
			waypointsArrayAdapter.notifyDataSetChanged();
		}
	};
	/**
	 * Compass updates broadcast receiver
	 */
	protected BroadcastReceiver compassBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

			Bundle bundle = intent.getExtras();
			setAzimuth(bundle.getFloat("azimuth"));

			//Log.d(Constants.TAG, "WaypointsListActivity: COMPASS BROADCAST MESSAGE RECEIVED: " + bundle.getFloat("azimuth"));

			waypointsArrayAdapter.notifyDataSetChanged();
		}
	};

	protected class WaypointsArrayAdapter extends ArrayAdapter<Waypoint> {

		private final Comparator<Waypoint> distanceComparator = new Comparator<Waypoint>() {
			@Override
			public int compare(Waypoint wp1, Waypoint wp2) {
				return (wp1.getDistanceTo() < wp2.getDistanceTo() ? -1
						: (wp1.getDistanceTo() == wp2.getDistanceTo() ? 0 : 1));
			}
		};

		// private LayoutInflater mInflater;

		private ArrayList<Waypoint> items;

		Bitmap arrowBitmap;
		BitmapDrawable bmd;

		public WaypointsArrayAdapter(Context context, int textViewResourceId, ArrayList<Waypoint> items) {

			super(context, textViewResourceId, items);

			this.items = items;
		}

		public void setItems(ArrayList<Waypoint> items) {

			this.items = items;

		}

		public void sortByDistance() {

			this.sort(distanceComparator);
			this.notifyDataSetChanged();

		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			View v = convertView;

			if (v == null) {
				LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = vi.inflate(R.layout.waypoint_list_item, null);
			}

			String distStr = "";
			String bearingStr = "";

			float newAzimuth = 0;
			float newBearing = 0;

			Waypoint wp = items.get(position);
			if (wp != null) {

				if (myApp.getCurrentLocation() != null) {

					float distanceTo = myApp.getCurrentLocation().distanceTo(wp.getLocation());

					distStr = utils
							.formatDistance(distanceTo, myApp.getPreferences().getString("distance_units", "km"));

					wp.setDistanceTo(distanceTo);

					newBearing = myApp.getCurrentLocation().bearingTo(wp.getLocation());

					if ((int) newBearing < 0) {
						newBearing = 360 - Math.abs((int) newBearing);
					}

					newAzimuth = newBearing - getAzimuth();
					if ((int) newAzimuth < 0) {
						newAzimuth = 360 - Math.abs((int) newAzimuth);
					}

					bearingStr = Utils.formatNumber(newBearing, 0) + Utils.DEGREE_CHAR;

				}

				TextView text1 = (TextView) v.findViewById(R.id.text1);
				TextView text2 = (TextView) v.findViewById(R.id.text2);
				TextView text3 = (TextView) v.findViewById(R.id.text3);

				// Set value for the first text field
				if (text1 != null) {
					text1.setText(Utils.shortenStr(wp.getTitle(), 32));
				}

				// set value for the second text field
				if (text2 != null) {
					text2.setText(Utils.formatLat(wp.getLocation().getLatitude(),
							Integer.parseInt(myApp.getPreferences().getString("coord_units", "0")))
								+ " | "
								+ Utils.formatLng(wp.getLocation().getLongitude(),
										Integer.parseInt(myApp.getPreferences().getString("coord_units", "0")))
								+ " | " + bearingStr);
				}

				if (text3 != null) {
					text3.setText(distStr);
				}

				// rotating small arrow pointing to waypoint
				CompassImage im = (CompassImage) v.findViewById(R.id.compassImage);
				im.setAngle(newAzimuth);

				//				Log.d(Constants.TAG, "WaypointsListActivity: getView: " + im.getId());

			} else {

			}

			return v;

		}
	}

	private boolean inWaypointsArray(String title) {

		for (Iterator<Waypoint> it = waypoints.iterator(); it.hasNext();) {

			Waypoint curWp = (Waypoint) it.next();

			if (curWp.getTitle().equals(title)) {
				return true;
			}
		}

		return false;
	}

	// ////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Select all waypoints sql query
	 */
	private final String sqlSelectAllWaypoints = "SELECT * FROM waypoints";

	private WaypointGpxExportTask waypointToGpx;

	private Utils utils;

	/**
	 * Called when the activity is first created
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		myApp = ((MyApp) getApplicationContext());

		utils = new Utils(this);

		registerForContextMenu(this.getListView());

		updateWaypointsArray();

		// cursorAdapter = new WaypointsCursorAdapter(this, cursor);
		waypointsArrayAdapter = new WaypointsArrayAdapter(this, R.layout.waypoint_list_item, waypoints);

		// setListAdapter(cursorAdapter);
		setListAdapter(waypointsArrayAdapter);

	}

	@Override
	public void onPause() {

		unregisterReceiver(compassBroadcastReceiver);
		unregisterReceiver(locationBroadcastReceiver);

		super.onPause();
	}

	/**
	 * 
	 */
	@Override
	protected void onDestroy() {

		if (waypoints != null) {
			waypoints.clear();
			waypoints = null;
		}

		myApp = null;

		super.onDestroy();

	}

	/**
	 * onResume event handler
	 */
	@Override
	protected void onResume() {

		// registering receiver for compass updates 
		registerReceiver(compassBroadcastReceiver, new IntentFilter("com.aripuca.tracker.COMPASS_UPDATES_ACTION"));

		// registering receiver for location updates
		registerReceiver(locationBroadcastReceiver, new IntentFilter("com.aripuca.tracker.LOCATION_UPDATES_ACTION"));

		super.onResume();
	}

	/**
	 * onCreateOptionsMenu handler
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.waypoints_menu, menu);
		return true;
	}

	/**
     * 
     */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		// Handle item selection
		switch (item.getItemId()) {

			case R.id.deleteWaypointsMenuItem:

				// clear all waypoints with confirmation dialog
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setMessage("Are you sure?").setCancelable(true)
						.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {

								// delete all waypoints
								String sql = "DELETE FROM waypoints";
								myApp.getDatabase().execSQL(sql);

								updateWaypointsArray();// cursor.requery();
								waypointsArrayAdapter.setItems(waypoints);
								waypointsArrayAdapter.notifyDataSetChanged();

								Toast.makeText(WaypointsListActivity.this, R.string.all_waypoints_deleted,
										Toast.LENGTH_SHORT).show();

							}
						}).setNegativeButton("No", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
							}
						});
				AlertDialog alert = builder.create();

				alert.show();

				return true;

				// import waypoints from external file
			case R.id.importMenuItem:

				// this.importFromTextFile();

				this.importFromXMLFile();

				return true;

			case R.id.exportMenuItem:

				exportWaypoints();

				return true;

			default:

				return super.onOptionsItemSelected(item);

		}

	}

	/**
	 * Create context menu for selected item
	 */
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {

		super.onCreateContextMenu(menu, v, menuInfo);

		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;

		final long waypointId = waypointsArrayAdapter.getItem((int) info.id).getId();

		menu.setHeaderTitle(getString(R.string.waypoint));
		menu.add(Menu.NONE, 0, 0, R.string.edit);
		menu.add(Menu.NONE, 1, 1, R.string.delete);
		// menu.add(Menu.NONE, 2, 2, "Online sync");
		menu.add(Menu.NONE, 3, 3, R.string.show_on_map);

	}

	/**
	 * Handle activity menu
	 */
	@Override
	public boolean onContextItemSelected(MenuItem item) {

		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();

		final long waypointId = waypointsArrayAdapter.getItem((int) info.id).getId();

		Cursor tmpCursor;
		String sql;

		switch (item.getItemId()) {

			case 0:

				// update waypoint in db
				updateWaypoint(waypointId);

				return true;

			case 1:

				// delete one waypoint with confirmation dialog
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setMessage("Are you sure?").setCancelable(true)
						.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								// delete waypoint from db
								String sql = "DELETE FROM waypoints WHERE _id=" + waypointId + ";";
								myApp.getDatabase().execSQL(sql);

								// cursor.requery();
								updateWaypointsArray();
								waypointsArrayAdapter.setItems(waypoints);
								waypointsArrayAdapter.notifyDataSetChanged();

								Toast.makeText(WaypointsListActivity.this, R.string.waypoint_deleted,
										Toast.LENGTH_SHORT).show();
							}
						}).setNegativeButton("No", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
							}
						});

				AlertDialog alert = builder.create();
				alert.show();

				return true;

			case 2:

				// TODO: use a thread for online sync

				// sync one waypoint data with remote server

				sql = "SELECT * FROM waypoints WHERE _id=" + waypointId + ";";
				tmpCursor = myApp.getDatabase().rawQuery(sql, null);
				tmpCursor.moveToFirst();

				// create temp waypoint from current record
				Waypoint wp = new Waypoint(tmpCursor.getString(tmpCursor.getColumnIndex("title")),
						tmpCursor.getLong(tmpCursor.getColumnIndex("time")), tmpCursor.getDouble(tmpCursor
								.getColumnIndex("lat")), tmpCursor.getDouble(tmpCursor.getColumnIndex("lng")),
								tmpCursor.getDouble(tmpCursor.getColumnIndex("elevation")));

				tmpCursor.close();

				try {

					// preparing query string for calling web service
					String lat = Location.convert(wp.getLocation().getLatitude(), 0);
					String lng = Location.convert(wp.getLocation().getLongitude(), 0);
					String title = URLEncoder.encode(wp.getTitle());

					String userName = myApp.getPreferences().getString("user_name", "");
					String userPassword = myApp.getPreferences().getString("user_password", "");
					String sessionValue = userName + "@" + Utils.md5("aripuca_session" + userPassword);

					if (userName.equals("") || userPassword.equals("")) {
						Toast.makeText(WaypointsListActivity.this, R.string.username_or_password_required,
								Toast.LENGTH_SHORT).show();
						return false;
					}

					String queryString = "?do=ajax_map_handler&aripuca_session=" + sessionValue
							+ "&action=add_point&lat=" + lat + "&lng=" + lng + "&z=16&n=" + title + "&d=AndroidSync";

					// http connection
					HttpClient httpClient = new DefaultHttpClient();
					HttpContext localContext = new BasicHttpContext();
					HttpGet httpGet = new HttpGet(myApp.getPreferences().getString("online_sync_url",
							"http://tracker.aripuca.com")
							+ queryString);
					HttpResponse response = httpClient.execute(httpGet, localContext);

					ByteArrayOutputStream outstream = new ByteArrayOutputStream();
					response.getEntity().writeTo(outstream);

					// parsing JSON return
					JSONObject jsonObject = new JSONObject(outstream.toString());

					Toast.makeText(WaypointsListActivity.this, jsonObject.getString("message").toString(),
							Toast.LENGTH_SHORT).show();

				} catch (ClientProtocolException e) {
					Toast.makeText(WaypointsListActivity.this, "ClientProtocolException: " + e.getMessage(),
							Toast.LENGTH_SHORT).show();
				} catch (IOException e) {
					Toast.makeText(WaypointsListActivity.this, "IOException " + e.getMessage(), Toast.LENGTH_SHORT)
							.show();
				} catch (JSONException e) {
					Toast.makeText(WaypointsListActivity.this, "JSONException " + e.getMessage(), Toast.LENGTH_SHORT)
							.show();
				}

				return true;
			case 3:

				// showing waypoint on the google map
				showOnMap(waypointId);

				return true;

			default:
				return super.onContextItemSelected(item);
		}

	}

	/**
	 * Update waypoint in the database
	 * 
	 * @param waypointId
	 * @param title
	 * @param lat
	 * @param lng
	 */
	protected void updateWaypoint(long waypointId) {

		Context context = this;

		// update waypoint in db
		String sql = "SELECT * FROM waypoints WHERE _id=" + waypointId + ";";
		Cursor wpCursor = myApp.getDatabase().rawQuery(sql, null);
		wpCursor.moveToFirst();

		String title = wpCursor.getString(wpCursor.getColumnIndex("title"));

		String descr = wpCursor.getString(wpCursor.getColumnIndex("descr"));

		Double lat = wpCursor.getDouble(wpCursor.getColumnIndex("lat")) / 1E6;
		Double lng = wpCursor.getDouble(wpCursor.getColumnIndex("lng")) / 1E6;

		LayoutInflater inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.add_waypoint_dialog,
				(ViewGroup) findViewById(R.id.add_waypoint_dialog_layout_root));

		AlertDialog.Builder builder = new AlertDialog.Builder(context);

		builder.setTitle("Edit waypoint");
		builder.setView(layout);

		// creating reference to input field in order to use it in onClick
		// handler
		final EditText wpTitle = (EditText) layout.findViewById(R.id.waypointTitleInputText);
		wpTitle.setText(title);

		final EditText wpDescr = (EditText) layout.findViewById(R.id.waypointDescriptionInputText);
		wpDescr.setText(descr);

		final EditText wpLat = (EditText) layout.findViewById(R.id.waypointLatInputText);
		wpLat.setText(Double.toString(lat));

		final EditText wpLng = (EditText) layout.findViewById(R.id.waypointLngInputText);
		wpLng.setText(Double.toString(lng));

		final String wpId = Long.toString(waypointId);

		builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {

				// waypoint title from input dialog
				String titleStr = wpTitle.getText().toString().trim();
				String descrStr = wpDescr.getText().toString().trim();
				int latE6 = (int) (Double.parseDouble(wpLat.getText().toString()) * 1E6);
				int lngE6 = (int) (Double.parseDouble(wpLng.getText().toString()) * 1E6);

				if (titleStr.equals("")) {
					Toast.makeText(WaypointsListActivity.this, R.string.waypoint_title_required, Toast.LENGTH_SHORT)
							.show();
					return;
				}

				ContentValues values = new ContentValues();
				values.put("title", titleStr);
				values.put("descr", descrStr);
				values.put("lat", latE6);
				values.put("lng", lngE6);

				try {
					myApp.getDatabase().update("waypoints", values, "_id=" + wpId, null);
					Toast.makeText(WaypointsListActivity.this, R.string.waypoint_updated, Toast.LENGTH_SHORT).show();

					// cursor.requery();
					updateWaypointsArray();
					waypointsArrayAdapter.setItems(waypoints);
					waypointsArrayAdapter.notifyDataSetChanged();

				} catch (SQLiteException e) {
					Log.w(Constants.TAG, "SQLiteException: " + e.getMessage(), e);
				}

			}
		});

		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				dialog.dismiss();
			}
		});

		AlertDialog dialog = builder.create();

		dialog.show();

	}

	/*
	 * protected void importFromTextFile() {
	 * 
	 * try {
	 * 
	 * BufferedReader br = new BufferedReader(new
	 * FileReader(myApp.getWaypointFile()));
	 * 
	 * String s = null;
	 * 
	 * while ((s = br.readLine()) != null) {
	 * 
	 * // parsing string from waypoints file String[] tmpArr = s.split("\\|");
	 * if (tmpArr.length < 5) { continue; }
	 * 
	 * ContentValues values = new ContentValues(); values.put("title",
	 * tmpArr[0]); values.put("descr", "Exported from external source");
	 * values.put("lat", tmpArr[2]); values.put("lng", tmpArr[3]);
	 * values.put("elevation", tmpArr[4]); values.put("time", tmpArr[1]);
	 * 
	 * try { myApp.getDatabase().insertOrThrow("waypoints", null, values); }
	 * catch (SQLiteException e) { Toast.makeText(WaypointsListActivity.this,
	 * "SQLiteException: " + e.getMessage(), Toast.LENGTH_SHORT).show();
	 * Log.e(Constants.TAG, "SQLiteException: " + e.getMessage(), e); }
	 * 
	 * }
	 * 
	 * } catch (IOException e) {
	 * 
	 * Toast.makeText(WaypointsListActivity.this, "IOException: " +
	 * e.getMessage(), Toast.LENGTH_SHORT) .show();
	 * 
	 * }
	 * 
	 * updateWaypointsArray(); waypointsArrayAdapter.setItems(waypoints);
	 * waypointsArrayAdapter.notifyDataSetChanged();
	 * 
	 * }
	 */

	private void updateWaypointsArray() {

		if (waypoints != null) {
			waypoints.clear();
		} else {
			waypoints = new ArrayList<Waypoint>();
		}

		Cursor cursor = myApp.getDatabase().rawQuery(this.sqlSelectAllWaypoints, null);
		cursor.moveToFirst();

		while (cursor.isAfterLast() == false) {

			Waypoint wp = new Waypoint(cursor.getString(cursor.getColumnIndex("title")),
										cursor.getLong(cursor.getColumnIndex("time")),
										cursor.getDouble(cursor.getColumnIndex("lat")) / 1E6,
										cursor.getDouble(cursor.getColumnIndex("lng")) / 1E6,
										cursor.getDouble(cursor.getColumnIndex("elevation")));

			wp.setId(cursor.getLong(cursor.getColumnIndex("_id")));

			waypoints.add(wp);

			cursor.moveToNext();
		}

		cursor.close();

	}

	protected void importFromXMLFile() {

		File importFolder = new File(myApp.getAppDir() + "/waypoints");

		final String importFiles[] = importFolder.list();

		if (importFiles == null || importFiles.length == 0) {
			Toast.makeText(WaypointsListActivity.this, "Import folder is empty", Toast.LENGTH_SHORT).show();
			return;
		}

		// first file is preselected
		importWaypointsFileName = importFiles[0];

		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		builder.setSingleChoiceItems(importFiles, 0, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				importWaypointsFileName = importFiles[whichButton];
			}
		})

		.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {

				try {

					DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
					DocumentBuilder db = dbf.newDocumentBuilder();
					File file = new File(myApp.getAppDir() + "/waypoints", importWaypointsFileName);

					Document doc = db.parse(file);
					doc.getDocumentElement().normalize();

					NodeList waypointsList = doc.getElementsByTagName("wpt");

					boolean updateRequired = false;

					for (int i = 0; i < waypointsList.getLength(); i++) {

						int latE6 = (int) (Double.parseDouble(((Element) waypointsList.item(i)).getAttribute("lat")) * 1E6);
						int lngE6 = (int) (Double.parseDouble(((Element) waypointsList.item(i)).getAttribute("lon")) * 1E6);
						String title = "";
						String desc = "";
						double ele = 0;
						long time = 0;

						Node item = waypointsList.item(i);

						NodeList properties = item.getChildNodes();
						for (int j = 0; j < properties.getLength(); j++) {

							Node property = properties.item(j);
							String name = property.getNodeName();

							if (name.equalsIgnoreCase("ELE") && property.getFirstChild() != null) {
								ele = Double.parseDouble(property.getFirstChild().getNodeValue());
							}
							if (name.equalsIgnoreCase("TIME") && property.getFirstChild() != null) {
								time = (new SimpleDateFormat("yyyy-MM-dd H:mm:ss")).parse(
										property.getFirstChild().getNodeValue()).getTime();
							}
							if (name.equalsIgnoreCase("NAME") && property.getFirstChild() != null) {
								title = property.getFirstChild().getNodeValue();
							}

							if (name.equalsIgnoreCase("DESC") && property.getFirstChild() != null) {
								desc = property.getFirstChild().getNodeValue();
							}

						}

						// adding imported waypoint to db
						if (!inWaypointsArray(title)) {

							ContentValues values = new ContentValues();
							values.put("title", title);
							values.put("descr", desc);
							values.put("lat", latE6);
							values.put("lng", lngE6);
							values.put("elevation", ele);
							values.put("time", time);

							try {
								myApp.getDatabase().insertOrThrow("waypoints", null, values);
							} catch (SQLiteException e) {
								Log.e(Constants.TAG, "SQLiteException: " + e.getMessage(), e);
							}

							// if at least one record added, update waypoints
							// list
							updateRequired = true;

						}

					}

					if (updateRequired) {
						updateWaypointsArray();
						waypointsArrayAdapter.setItems(waypoints);
						waypointsArrayAdapter.notifyDataSetChanged();
					}

				} catch (IOException e) {
					Log.v(Constants.TAG, e.getMessage());
				} catch (ParserConfigurationException e) {
					Log.v(Constants.TAG, e.getMessage());
				} catch (ParseException e) {
					Log.v(Constants.TAG, e.getMessage());
				} catch (SAXException e) {
					Log.v(Constants.TAG, e.getMessage());
				}

				dialog.dismiss();
			}
		})

		.setTitle(R.string.select_file)
				.setCancelable(true);

		AlertDialog alert = builder.create();

		alert.show();

	}

	/**
	 * Show current waypoint on map
	 * 
	 * @param waypointId Id of the requested waypoint
	 */
	protected void showOnMap(long waypointId) {

		String sql = "SELECT * FROM waypoints WHERE _id=" + waypointId + ";";
		Cursor tmpCursor = myApp.getDatabase().rawQuery(sql, null);
		tmpCursor.moveToFirst();

		Intent i = new Intent(this, MyMapActivity.class);

		// using Bundle to pass track id into new activity
		Bundle b = new Bundle();
		b.putInt("mode", Constants.SHOW_WAYPOINT);
		b.putInt("latE6", tmpCursor.getInt(tmpCursor.getColumnIndex("lat")));
		b.putInt("lngE6", tmpCursor.getInt(tmpCursor.getColumnIndex("lng")));

		tmpCursor.close();

		i.putExtras(b);
		startActivity(i);

	}

	public WaypointsArrayAdapter getArrayAdapter() {

		return waypointsArrayAdapter;

	}

	/**
	 * azimuth (received from orientation sensor)
	 */
	private float azimuth = 0;

	public void setAzimuth(float a) {
		azimuth = a;
	}

	public float getAzimuth() {
		return azimuth;
	}

	/**
	 * Export waypoints to external file
	 */
	private void exportWaypoints() {

		Context mContext = this;

		LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.filename_dialog,
				(ViewGroup) findViewById(R.id.filename_dialog_layout_root));

		AlertDialog.Builder builder = new AlertDialog.Builder(mContext);

		builder.setTitle(R.string.export_waypoints);
		builder.setView(layout);

		final String defaultFilename = "wp_" + (new SimpleDateFormat("yyyy-MM-dd")).format((new Date()).getTime());

		// creating references to input fields in order to use them in
		// onClick handler
		final EditText filenameEditText = (EditText) layout.findViewById(R.id.filenameInputText);
		filenameEditText.setText(defaultFilename);

		builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int id) {

				// waypoint title from input dialog
				String filenameStr = filenameEditText.getText().toString().trim();

				if (filenameStr.equals("")) {
					filenameStr = defaultFilename;
				}

				waypointToGpx = new WaypointGpxExportTask(WaypointsListActivity.this, filenameStr);
				waypointToGpx.setApp(myApp);
				waypointToGpx.execute(0L);

				dialog.dismiss();

			}
		});

		builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
				// dialog.dismiss();
			}
		});

		AlertDialog dialog = builder.create();
		dialog.show();

	}

}
