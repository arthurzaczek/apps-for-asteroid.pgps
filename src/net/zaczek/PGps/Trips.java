package net.zaczek.PGps;

import net.zaczek.PGps.Data.DatabaseManager;
import android.app.ListActivity;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.os.Bundle;
import android.text.format.Time;
import android.widget.SimpleCursorAdapter;

public class Trips extends ListActivity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		DatabaseManager db = new DatabaseManager(this);
		Cursor cursor = db.getAllTrips();
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

		SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, R.layout.list_trips_row, cw, projection, ids);
		setListAdapter(adapter);
	}
}
