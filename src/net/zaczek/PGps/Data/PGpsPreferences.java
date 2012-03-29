package net.zaczek.PGps.Data;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class PGpsPreferences {
	
	private static final String TAG = "PGps";
	public boolean use_comma_as_decimal_seperator;
	public boolean log_trips;
	public boolean show_last_without_fix;
	public boolean trips_geocode;
	public int merge_trips;
	public int record_positions;
	
	private PGpsPreferences()
	{
	}
	
	private static PGpsPreferences _single;
	public static synchronized PGpsPreferences getInstance(Context context) {
		if(_single == null) {
			_single = new PGpsPreferences();
			_single.load(context);
		}
		return _single;
	}

	public synchronized void load(Context context) {
		SharedPreferences prefs = PreferenceManager
			.getDefaultSharedPreferences(context);
		
		log_trips = prefs.getBoolean("log_trips", true);
		trips_geocode = prefs.getBoolean("trips_geocode", true);
		try {
			merge_trips = Integer.valueOf(prefs.getString("merge_trips", "0"));
		} catch(Exception e) {
			Log.e(TAG, "Unable to load merge_trips pref", e);
		}
		try {
			record_positions = Integer.valueOf(prefs.getString("record_positions", "0"));
		} catch(Exception e) {
			Log.e(TAG, "Unable to load record_positions pref", e);
		}		
		show_last_without_fix = prefs.getBoolean("show_last_without_fix", false);
		use_comma_as_decimal_seperator = prefs.getBoolean("use_comma_as_decimal_seperator", true);
		
	}
}
