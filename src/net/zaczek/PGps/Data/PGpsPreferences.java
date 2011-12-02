package net.zaczek.PGps.Data;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class PGpsPreferences {
	
	public boolean use_comma_as_decimal_seperator;
	public boolean log_trips;
	public boolean show_last_without_fix;
	public boolean trips_geocode;
	public int merge_trips;
	
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
		merge_trips = prefs.getInt("trips_geocode", 0);

		show_last_without_fix = prefs.getBoolean("show_last_without_fix", false);
		use_comma_as_decimal_seperator = prefs.getBoolean("use_comma_as_decimal_seperator", true);
		
	}
}
