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

import com.aripuca.tracker.R;
import com.aripuca.tracker.db.Waypoints;
import com.aripuca.tracker.io.WaypointGpxExportTask;
import com.aripuca.tracker.map.MyMapActivity;
import com.aripuca.tracker.service.AppService;
import com.aripuca.tracker.service.AppServiceConnection;
import com.aripuca.tracker.db.Waypoint;

import com.aripuca.tracker.utils.Utils;
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
import android.widget.ArrayAdapter;

import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import android.widget.AdapterView.AdapterContextMenuInfo;

/**
 * Waypoints list activity
 */
public class WaypointsListActivity extends ListActivity {

	/**
	 * Reference to app object
	 */
	private App app;

	private String importWaypointsFileName;

	private WaypointsArrayAdapter waypointsArrayAdapter;

	private ArrayList<Waypoint> waypoints;

	private Location currentLocation;

	/**
	 * Service connection object
	 */
	private AppServiceConnection serviceConnection;

	/**
	 * Location updates broadcast receiver
	 */
	protected BroadcastReceiver locationBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

			// Log.d(Constants.TAG,
			// "WaypointsListActivity: LOCATION BROADCAST MESSAGE RECEIVED");

			Bundle bundle = intent.getExtras();

			currentLocation = (Location) bundle.getParcelable("location");

			waypointsArrayAdapter.sortByDistance();
			// waypointsArrayAdapter.notifyDataSetChanged();
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

		}
	};

	/**
	 * 
	 */
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

			String elevationUnit = app.getPreferences().getString("elevation_units", "m");

			Waypoint wp = items.get(position);
			if (wp != null) {

				if (currentLocation != null) {

					float distanceTo = currentLocation.distanceTo(wp.getLocation());

					String distanceUnit = app.getPreferences().getString("distance_units", "km");

					distStr = Utils.formatDistance(distanceTo, distanceUnit) + " "
							+ Utils.getLocalizedDistanceUnit(WaypointsListActivity.this, distanceTo, distanceUnit);

					wp.setDistanceTo(distanceTo);

					newBearing = currentLocation.bearingTo(wp.getLocation());

					if ((int) newBearing < 0) {
						newBearing = 360 - Math.abs((int) newBearing);
					}

					// newAzimuth = newBearing - getAzimuth() -
					// orientationAdjustment;
					newAzimuth = newBearing - getAzimuth()
							- Utils.getDeviceRotation(WaypointsListActivity.this);
					if ((int) newAzimuth < 0) {
						newAzimuth = 360 - Math.abs((int) newAzimuth);
					}

					bearingStr = Utils.formatNumber(newBearing, 0) + Utils.DEGREE_CHAR;

				}

				TextView waypointTitle = (TextView) v.findViewById(R.id.waypoint_title);
				TextView waypointDetails = (TextView) v.findViewById(R.id.waypoint_details);
				TextView waypointDistance = (TextView) v.findViewById(R.id.waypoint_distance);

				// Set value for the first text field
				if (waypointTitle != null) {
					waypointTitle.setText(Utils.shortenStr(wp.getTitle(), 32));
				}

				// set value for the second text field
				if (waypointDetails != null) {
					waypointDetails.setText(Utils.formatLat(wp.getLocation().getLatitude(),
							Integer.parseInt(app.getPreferences().getString("coord_units", "0")))
							+ "|"
							+ Utils.formatLng(wp.getLocation().getLongitude(),
									Integer.parseInt(app.getPreferences().getString("coord_units", "0")))
							+ "|"
							+ Utils.formatNumber(wp.getLocation().getAltitude(), 0)
							+ ""
							+ Utils.getLocalizedElevationUnit(WaypointsListActivity.this, elevationUnit)
							+ "|"
							+ bearingStr);
				}

				if (waypointDistance != null) {
					waypointDistance.setText(distStr);
				}

				// rotating small arrow pointing to waypoint
				CompassImage im = (CompassImage) v.findViewById(R.id.compassImage);
				im.setAngle(newAzimuth);

				// Log.d(Constants.TAG, "WaypointsListActivity: getView: " +
				// im.getId());

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

	private WaypointGpxExportTask waypointToGpx;

	/**
	 * Called when the activity is first created
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		app = (App) getApplication();

		// initializing with last known location, so we can calculate distance
		// to waypoints
		currentLocation = app.getCurrentLocation();

		serviceConnection = new AppServiceConnection(this, appServiceConnectionCallback);

		registerForContextMenu(this.getListView());

		updateWaypointsArray();

		// cursorAdapter = new WaypointsCursorAdapter(this, cursor);
		waypointsArrayAdapter = new WaypointsArrayAdapter(this, R.layout.waypoint_list_item, waypoints);

		// setListAdapter(cursorAdapter);
		setListAdapter(waypointsArrayAdapter);

	}

	private Runnable appServiceConnectionCallback = new Runnable() {

		@Override
		public void run() {

			AppService appService = serviceConnection.getService();

			if (appService == null) {
				Toast.makeText(WaypointsListActivity.this, R.string.gps_service_not_connected, Toast.LENGTH_SHORT)
						.show();
				return;
			}

			// this activity is started by MainActivity which is always
			// listening for location updates

			// by setting gpsInUse to true we insure that listening will not
			// stop in AppService.stopLocationUpdatesThread
			appService.setGpsInUse(true);

			// this activity requires compass data
			appService.startSensorUpdates();

		}
	};

	/**
	 * Edit waypoint
	 */
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {

		final long waypointId = waypointsArrayAdapter.getItem((int) id).getId();

		this.updateWaypoint(waypointId);

	}

	/**
	 * 
	 */
	@Override
	public void onPause() {

		unregisterReceiver(compassBroadcastReceiver);
		unregisterReceiver(locationBroadcastReceiver);

		serviceConnection.unbindAppService();

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

		serviceConnection = null;

		app = null;

		super.onDestroy();

	}

	/**
	 * onResume event handler
	 */
	@Override
	protected void onResume() {

		super.onResume();

		// registering receiver for compass updates
		registerReceiver(compassBroadcastReceiver, new IntentFilter(Constants.ACTION_COMPASS_UPDATES));

		// registering receiver for location updates
		registerReceiver(locationBroadcastReceiver, new IntentFilter(Constants.ACTION_LOCATION_UPDATES));

		// bind to AppService
		// appServiceConnectionCallback will be called once bound
		serviceConnection.bindAppService();

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
			builder.setMessage(R.string.are_you_sure).setCancelable(true)
					.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {

							Waypoints.deleteAll(app.getDatabase());

							updateWaypointsArray();// cursor.requery();

							waypointsArrayAdapter.setItems(waypoints);
							waypointsArrayAdapter.notifyDataSetChanged();

							Toast.makeText(WaypointsListActivity.this, R.string.all_waypoints_deleted,
									Toast.LENGTH_SHORT).show();

						}

					}).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
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

		case R.id.showMapMenuItem:

			// startActivity(new Intent(this, WaypointsMapActivity.class));

			Intent i = new Intent(this, MyMapActivity.class);

			// using Bundle to pass track id into new activity
			Bundle b = new Bundle();
			b.putInt("mode", Constants.SHOW_ALL_WAYPOINTS);

			i.putExtras(b);
			startActivity(i);

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

		// AdapterView.AdapterContextMenuInfo info =
		// (AdapterView.AdapterContextMenuInfo) menuInfo;

		menu.setHeaderTitle(getString(R.string.waypoint));
		menu.add(Menu.NONE, 0, 0, R.string.edit);
		menu.add(Menu.NONE, 1, 1, R.string.delete);
		menu.add(Menu.NONE, 2, 2, R.string.email_to);
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

			deleteWaypoint(waypointId);

			return true;

		case 2:

			// email waypoint data using default email client

			String elevationUnit = app.getPreferences().getString("elevation_units", "m");
			String elevationUnitLocalized = Utils.getLocalizedElevationUnit(this, elevationUnit);

			sql = "SELECT * FROM waypoints WHERE _id=" + waypointId + ";";
			tmpCursor = app.getDatabase().rawQuery(sql, null);
			tmpCursor.moveToFirst();

			double lat1 = tmpCursor.getDouble(tmpCursor.getColumnIndex("lat")) / 1E6;
			double lng1 = tmpCursor.getDouble(tmpCursor.getColumnIndex("lng")) / 1E6;

			String messageBody = getString(R.string.title)
					+ ": "
					+ tmpCursor.getString(tmpCursor.getColumnIndex("title"))
					+ "\n\n"
					+ getString(R.string.lat)
					+ ": "
					+ Utils.formatLat(lat1, 0)
					+ "\n"
					+ getString(R.string.lng)
					+ ": "
					+ Utils.formatLng(lng1, 0)
					+ "\n"
					+ getString(R.string.elevation)
					+ ": "
					+ Utils.formatElevation(tmpCursor.getFloat(tmpCursor.getColumnIndex("elevation")),
							elevationUnit) + elevationUnitLocalized + "\n\n" + "http://maps.google.com/?ll=" + lat1
					+ "," + lng1 + "&z=10";

			tmpCursor.close();

			final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);

			emailIntent.setType("plain/text");
			emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,
					getResources().getString(R.string.email_subject_waypoint));
			emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, messageBody);

			this.startActivity(Intent.createChooser(emailIntent, getString(R.string.sending_email)));

			return true;

		case 3:

			this.showOnMap(waypointId);

			return true;

		case 4:

			// TODO: use a thread for online sync

			// sync one waypoint data with remote server

			// create temp waypoint from current record
			Waypoint wp = Waypoints.get(app.getDatabase(), waypointId);

			try {

				// preparing query string for calling web service
				String lat = Location.convert(wp.getLocation().getLatitude(), 0);
				String lng = Location.convert(wp.getLocation().getLongitude(), 0);
				String title = URLEncoder.encode(wp.getTitle());

				String userName = app.getPreferences().getString("user_name", "");
				String userPassword = app.getPreferences().getString("user_password", "");
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
				HttpGet httpGet = new HttpGet(app.getPreferences().getString("online_sync_url",
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

		// get waypoint data from db
		final Waypoint wp = Waypoints.get(app.getDatabase(), waypointId);

		LayoutInflater inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.add_waypoint_dialog,
				(ViewGroup) findViewById(R.id.add_waypoint_dialog_layout_root));

		AlertDialog.Builder builder = new AlertDialog.Builder(context);

		builder.setTitle("Edit waypoint");
		builder.setView(layout);

		// creating reference to input field in order to use it in onClick
		// handler
		final EditText wpTitle = (EditText) layout.findViewById(R.id.waypointTitleInputText);
		wpTitle.setText(wp.getTitle());

		final EditText wpDescr = (EditText) layout.findViewById(R.id.waypointDescriptionInputText);
		wpDescr.setText(wp.getDescr());

		final EditText wpLat = (EditText) layout.findViewById(R.id.waypointLatInputText);
		wpLat.setText(Double.toString(wp.getLat()));

		final EditText wpLng = (EditText) layout.findViewById(R.id.waypointLngInputText);
		wpLng.setText(Double.toString(wp.getLng()));

		builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int id) {

				// waypoint title from input dialog

				if (wpTitle.getText().toString().trim().equals("")) {
					Toast.makeText(WaypointsListActivity.this, R.string.waypoint_title_required, Toast.LENGTH_SHORT)
							.show();
					return;
				}

				wp.setTitle(wpTitle.getText().toString().trim());
				wp.setDescr(wpDescr.getText().toString().trim());
				wp.setLat(Double.parseDouble(wpLat.getText().toString()));
				wp.setLng(Double.parseDouble(wpLng.getText().toString()));

				try {

					// update waypoint data in db
					Waypoints.update(app.getDatabase(), wp);

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

	/**
	 * 
	 */
	private void updateWaypointsArray() {

		Log.d(Constants.TAG, "updateWaypointsArray");

		if (waypoints != null) {
			waypoints.clear();
		} else {
			waypoints = new ArrayList<Waypoint>();
		}

		Waypoints.getAll(app.getDatabase(), waypoints);

	}

	/**
	 * Delete waypoint by id with confirmation dialog
	 * 
	 * @param wpid
	 */
	private void deleteWaypoint(long wpid) {

		final long waypointId = wpid;

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("Are you sure?").setCancelable(true)
				.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {

						// delete waypoint from db

						Waypoints.delete(app.getDatabase(), waypointId);

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

	}

	/**
	 * Imports waypoints from gpx file
	 */
	protected void importFromXMLFile() {

		File importFolder = new File(app.getAppDir() + "/" + Constants.PATH_WAYPOINTS);

		final String importFiles[] = importFolder.list();

		if (importFiles == null || importFiles.length == 0) {
			Toast.makeText(WaypointsListActivity.this, "Import folder is empty", Toast.LENGTH_SHORT).show();
			return;
		}

		// first file is preselected
		// TODO: remove class variable
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
							File file = new File(app.getAppDir() + "/" + Constants.PATH_WAYPOINTS,
									importWaypointsFileName);

							Document doc = db.parse(file);
							doc.getDocumentElement().normalize();

							NodeList waypointsList = doc.getElementsByTagName("wpt");

							boolean updateRequired = false;

							for (int i = 0; i < waypointsList.getLength(); i++) {

								Waypoint wp = new Waypoint();
								
								wp.setLat(Double.parseDouble(((Element) waypointsList.item(i)).getAttribute("lat")));
								wp.setLng(Double.parseDouble(((Element) waypointsList.item(i)).getAttribute("lon")));

								int latE6 = (int) (Double.parseDouble(((Element) waypointsList.item(i))
										.getAttribute("lat")) * 1E6);
								int lngE6 = (int) (Double.parseDouble(((Element) waypointsList.item(i))
										.getAttribute("lon")) * 1E6);
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
										wp.setElevation(Double.parseDouble(property.getFirstChild().getNodeValue()));
									}
									if (name.equalsIgnoreCase("TIME") && property.getFirstChild() != null) {
										wp.setTime((new SimpleDateFormat("yyyy-MM-dd H:mm:ss")).parse(
												property.getFirstChild().getNodeValue()).getTime());
									}
									if (name.equalsIgnoreCase("NAME") && property.getFirstChild() != null) {
										wp.setTitle(property.getFirstChild().getNodeValue());
									}

									if (name.equalsIgnoreCase("DESC") && property.getFirstChild() != null) {
										wp.setDescr(property.getFirstChild().getNodeValue());
									}

								}

								// adding imported waypoint to db
								if (!inWaypointsArray(wp.getTitle())) {

									try {

										Waypoints.insert(app.getDatabase(), wp);

										// if at least one record added, update waypoints list
										updateRequired = true;

									} catch (SQLiteException e) {
										Log.e(Constants.TAG, "SQLiteException: " + e.getMessage(), e);
									}

								}

							}

							if (updateRequired) {
								updateWaypointsArray();
								waypointsArrayAdapter.setItems(waypoints);
								waypointsArrayAdapter.notifyDataSetChanged();
							}

							Toast.makeText(WaypointsListActivity.this, R.string.import_completed, Toast.LENGTH_SHORT)
									.show();

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

				.setTitle(R.string.select_file).setCancelable(true);

		AlertDialog alert = builder.create();

		alert.show();

	}

	/**
	 * Show current waypoint on map
	 * 
	 * @param waypointId
	 *            Id of the requested waypoint
	 */
	protected void showOnMap(long waypointId) {

		com.aripuca.tracker.db.Waypoint wp = com.aripuca.tracker.db.Waypoints.get(app.getDatabase(), waypointId);

		Intent i = new Intent(this, MyMapActivity.class);

		// using Bundle to pass track id into new activity
		Bundle b = new Bundle();

		b.putInt("mode", Constants.SHOW_WAYPOINT);
		b.putInt("latE6", wp.getLat1E6());
		b.putInt("lngE6", wp.getLng1E6());

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
				waypointToGpx.setApp(app);
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
