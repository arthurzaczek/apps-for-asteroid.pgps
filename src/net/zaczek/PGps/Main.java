package net.zaczek.PGps;

import net.zaczek.PGps.Data.POI;
import android.app.Activity;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
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
	private static final int MENU_EXIT = 2;
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
	public final static int MODES = 3;

	public int currentMode = MODE_POI;
	
	int currentPOI = -1;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		GpsService.start(getApplicationContext());

		POI.load();
		if(POI.size() != 0) currentPOI = 0;

		txtStatus = (TextView) findViewById(R.id.txtStatus);
		txtSpeed = (TextView) findViewById(R.id.txtSpeed);
		txtAccuracy = (TextView) findViewById(R.id.txtAccuracy);
		txtInfo = (TextView) findViewById(R.id.txtInfo);
		txtHeader = (TextView) findViewById(R.id.txtHeader);

		setHeader();
		updateGps();

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
		GpsService.unregisterUpdateListener();
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		GpsService.registerUpdateListener(hRefresh);
	}

	private void updateGps() {
		txtStatus.setText(GpsService.getSatellitesInFix() + "/" + GpsService.getMaxSatellites() + " Satellites");

		txtSpeed.setText(String.format("%.0f km/h", GpsService.getSpeed() * 3.6));
		txtAccuracy.setText(String.format("%.2f m", GpsService.getAccuracy()));
		switch (currentMode) {
		case MODE_ALTITUDE:
			txtInfo.setText(String.format("%.2f m", GpsService.getAltitude()));
			break;
		case MODE_LAT_LON:
			txtInfo.setText(String.format("lat: %s lon: %s", GpsService.getLat(), GpsService.getLon()));
			break;
		case MODE_POI:
			if(currentPOI >= 0) {
				final Location current = GpsService.getLocation();
				if(current != null) {
					POI poi = POI.get(currentPOI);
					float meters = current.distanceTo(poi.getLocation());
					if(meters < 1000.0f) {
						txtInfo.setText(String.format("%s: %.2f m", poi.getName(), meters));
					} else {
						txtInfo.setText(String.format("%s: %.2f km", poi.getName(),meters / 1000.0f));
					}
				} else {
					txtInfo.setText("No location");
				}				
			} else {
				txtInfo.setText("No POI selected");
			}
			break;
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
			if(--currentMode < 0) currentMode = MODES - 1;
			setHeader();
			updateGps();
			return true;
		case KeyEvent.KEYCODE_DPAD_RIGHT:
		case KeyEvent.KEYCODE_MEDIA_NEXT:
			switch(currentMode) {
			case MODE_POI:
				currentPOI = (++currentPOI) % POI.size(); 
				updateGps();
				break;
			}
			return true;
		case KeyEvent.KEYCODE_DPAD_LEFT:
		case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
			switch(currentMode) {
			case MODE_POI:
				if(--currentPOI < 0) currentPOI = POI.size() - 1;
				updateGps();
				break;
			}
			return true;
		case KeyEvent.KEYCODE_DPAD_CENTER:
			switch(currentMode) {
			case MODE_POI:
				savePOI();
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
		final Location current = GpsService.getLocation();
		if(current != null) {
			POI.save(current);
			Toast.makeText(this, "POI saved", Toast.LENGTH_LONG).show();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		Log.d(TAG, "Creating options menu");
		menu.add(0, MENU_PREFERENCES, 0, "Settings");
		menu.add(1, MENU_EXIT, 0, "Exit");
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		int itemId = item.getItemId();
		Log.d(TAG, "Menu item selected: " + itemId);
		switch (itemId) {
		case MENU_PREFERENCES:
			startActivity(new Intent(this, Preferences.class));
			return true;
		case MENU_EXIT:
			finish();
			return true;
		}

		return super.onMenuItemSelected(featureId, item);
	}
}
