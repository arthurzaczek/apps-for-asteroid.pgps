package net.zaczek.PGps;

import net.zaczek.PGps.Data.DatabaseManager;
import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.os.Bundle;
import android.os.IBinder;
import android.text.format.Time;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SimpleCursorAdapter;

public class Trips extends ListActivity {
	private static final int MENU_BACK = 1;
	private static final int MENU_EXPORT_TRIPS = 2;
	
	private GpsService _service;
	private DatabaseManager db;
	private SimpleCursorAdapter adapter;
	private Cursor cursor;

	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			_service = ((GpsService.LocalBinder) service).getService();
		}

		public void onServiceDisconnected(ComponentName className) {
			_service = null;
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		GpsService.start(getApplicationContext());
		bindService(new Intent(this, GpsService.class), mConnection,
				Context.BIND_AUTO_CREATE);

		db = DatabaseManager.getInstance(getApplicationContext());
		cursor = db.getAllTrips();
		startManagingCursor(cursor);

		String[] projection = new String[] { DatabaseManager.COL_TRIPS_START, DatabaseManager.COL_TRIPS_END, DatabaseManager.COL_TRIPS_DISTANCE, DatabaseManager.COL_TRIPS_START_ADR, DatabaseManager.COL_TRIPS_END_ADR };
		int[] ids = new int[] { R.id.list_trips_start, R.id.list_trips_end, R.id.list_trips_distance, R.id.list_trips_start_adr, R.id.list_trips_end_adr };

		CursorWrapper cw = new CursorWrapper(cursor) {
			public String getString(int columnIndex) {
				switch (columnIndex) {
				case DatabaseManager.COL_IDX_TRIPS_START:
				case DatabaseManager.COL_IDX_TRIPS_END:
					final long dt = super.getLong(columnIndex);
					if (dt > 0) {
						final Time time = new Time();
						time.set(dt);
						return time.format("%c");
					} else {
						return "";
					}
				case DatabaseManager.COL_IDX_TRIPS_DISTANCE:
					return String.format("%.2f", super.getFloat(columnIndex) / 1000.0f) + " km";
				default:
					return super.getString(columnIndex);
				}
			}
		};

		adapter = new SimpleCursorAdapter(this, R.layout.list_trips_row, cw, projection, ids);
		setListAdapter(adapter);
	}
	
	@Override
	protected void onDestroy() {
		unbindService(mConnection);
		super.onDestroy();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, MENU_BACK, 0, "Back");
		menu.add(1, MENU_EXPORT_TRIPS, 0, "Export trips");
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		int itemId = item.getItemId();
		switch (itemId) {
		case MENU_BACK:
			finish();
			return true;
		case MENU_EXPORT_TRIPS:
			new ExportTripsTask(this, _service, db).execute();
			cursor.requery();
			return true;
		}

		return super.onMenuItemSelected(featureId, item);
	}
}
