package net.zaczek.PGps;

import java.io.IOException;

import net.zaczek.PGps.Data.PGpsPreferences;
import net.zaczek.PGps.Data.POI;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

public class Main extends Activity {
	private static final String TAG = "PGps";
	private static final int MENU_PREFERENCES = 1;
	private static final int MENU_SHOW_TRIPS = 3;
	private static final int MENU_ABOUT = 4;
	private static final int MENU_EXIT = 5;
	private static final int MENU_SIMULATE = 6;
	private TextView txtStatus;
	private TextView txtSpeed;
	private TextView txtAccuracy;
	private TextView txtInfo;
	private TextView txtHeader;

	private Handler hRefresh;
	public final static int REFRESH = 1;

	public final static int MODE_ALTITUDE = 0;
	public final static int MODE_POI = 1;
	public final static int MODE_LAT_LON = 2;
	public final static int MODE_DISTANCE = 3;
	public final static int MODES = 4;

	private int currentMode = MODE_POI;

	private int currentPOI = -1;

	private GpsService _service;

	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			_service = ((GpsService.LocalBinder) service).getService();
			_service.registerUpdateListener(hRefresh);
			updateGps();
		}

		public void onServiceDisconnected(ComponentName className) {
			_service = null;
		}
	};

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		GpsService.start(getApplicationContext());
		bindService(new Intent(this, GpsService.class), mConnection,
				Context.BIND_AUTO_CREATE);

		POI.load();
		if (POI.size() != 0)
			currentPOI = 0;

		txtStatus = (TextView) findViewById(R.id.txtStatus);
		txtSpeed = (TextView) findViewById(R.id.txtSpeed);
		txtAccuracy = (TextView) findViewById(R.id.txtAccuracy);
		txtInfo = (TextView) findViewById(R.id.txtInfo);
		txtHeader = (TextView) findViewById(R.id.txtHeader);

		hRefresh = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case REFRESH:
					updateGps();
					break;
				}
			}
		};
	}

	@Override
	protected void onPause() {
		if (_service != null)
			_service.unregisterUpdateListener();
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();		

		if (_service != null)
			_service.registerUpdateListener(hRefresh);

		setHeader();
		updateGps();
	}

	@Override
	protected void onDestroy() {
		unbindService(mConnection);
		super.onDestroy();
	}

	private void updateGps() {
		if (_service == null) {
			txtInfo.setText("Not connected to Service");
			return;
		}
		txtStatus.setText(_service.getSatellitesInFix() + "/"
				+ _service.getMaxSatellites() + " Satellites");
		boolean inFix = _service.getSatellitesInFix() > 0;

		if (inFix || PGpsPreferences.getInstance(this).show_last_without_fix) {
			txtSpeed.setText(String.format("%.0f km/h",
					_service.getSpeed() * 3.6));
			txtAccuracy
					.setText(String.format("%.2f m", _service.getAccuracy()));

			switch (currentMode) {
			case MODE_ALTITUDE:
				txtInfo.setText(String.format("%.2f m", _service.getAltitude()));
				break;
			case MODE_LAT_LON:
				txtInfo.setText(String.format("lat: %s lon: %s",
						_service.getLat(), _service.getLon()));
				break;
			case MODE_POI:
				if (currentPOI >= 0) {
					final Location current = _service.getLocation();
					if (current != null) {
						POI poi = POI.get(currentPOI);
						final float meters = current.distanceTo(poi
								.getLocation());
						String name = poi.getName();
						if (name.length() > 10) {
							name = name.substring(0, 10) + "...";
						}
						if (meters < 1000.0f) {
							txtInfo.setText(String.format("%s: %.0f m", name,
									meters));
						} else {
							txtInfo.setText(String.format("%s: %.2f km", name,
									meters / 1000.0f));
						}
					} else {
						txtInfo.setText("No location");
					}
				} else {
					txtInfo.setText("No POI selected");
				}
				break;
			case MODE_DISTANCE:
				final float meters = _service.getDistance();
				if (meters < 1000.0f) {
					txtInfo.setText(String.format("%.0f m", meters));
				} else {
					txtInfo.setText(String.format("%.2f km", meters / 1000.0f));
				}
				break;
			}
		} else {
			txtSpeed.setText("--- km/h");
			txtAccuracy.setText("---.-- m");
			txtInfo.setText("No fix");
		}
	}

	private void setHeader() {
		switch (currentMode) {
		case MODE_ALTITUDE:
			txtHeader.setText("PGps - current altitude");
			break;
		case MODE_POI:
			txtHeader.setText("PGps - distance to POI");
			break;
		case MODE_LAT_LON:
			txtHeader.setText("PGps - Lat/Lon");
			break;
		case MODE_DISTANCE:
			txtHeader.setText("PGps - distance");
			break;
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_DPAD_DOWN:
			currentMode = (++currentMode) % MODES;
			setHeader();
			updateGps();
			return true;
		case KeyEvent.KEYCODE_DPAD_UP:
			if (--currentMode < 0)
				currentMode = MODES - 1;
			setHeader();
			updateGps();
			return true;
		case KeyEvent.KEYCODE_DPAD_RIGHT:
		case KeyEvent.KEYCODE_MEDIA_NEXT:
			switch (currentMode) {
			case MODE_POI:
				currentPOI = (++currentPOI) % POI.size();
				updateGps();
				break;
			}
			return true;
		case KeyEvent.KEYCODE_DPAD_LEFT:
		case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
			switch (currentMode) {
			case MODE_POI:
				if (--currentPOI < 0)
					currentPOI = POI.size() - 1;
				updateGps();
				break;
			}
			return true;
		case KeyEvent.KEYCODE_DPAD_CENTER:
			switch (currentMode) {
			case MODE_POI:
				savePOI();
				break;
			case MODE_DISTANCE:
				if (_service != null) {
					_service.clearDistance();
					Toast.makeText(this, "Distance cleared", Toast.LENGTH_LONG)
							.show();
				}
				updateGps();
				break;
			}
			return true;
		case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
			return true;
		default:
			return super.onKeyDown(keyCode, event);
		}
	}

	private void savePOI() {
		if (_service == null)
			return;
		final Location current = _service.getLocation();
		if (current != null) {
			try {
				POI.save(current);
				Toast.makeText(this, "POI saved", Toast.LENGTH_LONG).show();
			} catch (IOException e) {
				Toast.makeText(this, "Error saving POI", Toast.LENGTH_LONG)
						.show();
				Log.e(TAG, "Error saving POI", e);
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		Log.d(TAG, "Creating options menu");
		menu.add(0, MENU_SHOW_TRIPS, 0, "Show trips");
		menu.add(1, MENU_PREFERENCES, 0, "Settings");
		menu.add(2, MENU_ABOUT, 0, "About");
		menu.add(3, MENU_EXIT, 0, "Exit");
		// Only for debugging
		// menu.add(4, MENU_SIMULATE, 0, "Simulate");
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		int itemId = item.getItemId();
		Log.d(TAG, "Menu item selected: " + itemId);
		switch (itemId) {
		case MENU_PREFERENCES:
			startActivityForResult(new Intent(this, Preferences.class), 0);
			return true;
		case MENU_SHOW_TRIPS:
			startActivity(new Intent(this, Trips.class));
			return true;
		case MENU_ABOUT:
			startActivity(new Intent(this, About.class));
			return true;
		case MENU_EXIT:
			finish();
			return true;
		case MENU_SIMULATE:
			if (_service != null) {
				final Location l = new Location("MOCK");
				l.setLongitude(16.4);
				for(double lat = 0;lat<10.0;lat+=1.0) {
					l.setLatitude(48 + (lat / 10.0));
					_service.onLocationChanged(l);
				}
				_service.stopTrip();
				updateGps();
			}
			return true;
		}

		return super.onMenuItemSelected(featureId, item);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		PGpsPreferences.getInstance(this).load(this);
		if (_service != null)
			_service.applyPreferences();
	}
}
