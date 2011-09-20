package net.zaczek.PGps;

import java.util.Timer;
import java.util.TimerTask;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class Main extends Activity {
	private static final String TAG = "PGps";
	private static final int MENU_PREFERENCES = 1;
	private static final int MENU_EXIT = 2;
	private TextView txtStatus;
	private TextView txtSpeed;
	private TextView txtAccuracy;
	private TextView txtAltitude;

	private Timer timer;
	private Handler hRefresh;
	private final int REFRESH = 1;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		GpsService.start(getApplicationContext());

		txtStatus = (TextView) findViewById(R.id.txtStatus);
		txtSpeed = (TextView) findViewById(R.id.txtSpeed);
		txtAccuracy = (TextView) findViewById(R.id.txtAccuracy);
		txtAltitude = (TextView) findViewById(R.id.txtAltitude);

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

		timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				hRefresh.sendEmptyMessage(REFRESH);
			}
		}, 1000, 1000);

	}

	private void updateGps() {
		txtStatus.setText(GpsService.getSatellitesInFix() + "/" + GpsService.getMaxSatellites() + " Satellites");

		txtSpeed.setText(String.format("%.0f km/h", GpsService.getSpeed() * 3.6));
		txtAccuracy.setText(String.format("%.2f m", GpsService.getAccuracy()));
		txtAltitude.setText(String.format("%.2f m", GpsService.getAltitude()));
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