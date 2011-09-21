package net.zaczek.PGps;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.GpsStatus.Listener;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.text.format.Time;
import android.util.Log;

public class GpsService extends Service implements LocationListener, Listener {

	private final static String TAG = "GpsService";
	private WakeLock wl;

	private LocationManager locationManager;
	private GpsStatus status = null;
	private Location location = null;

	private static String _lon = "";
	private static String _lat = "";
	private static float _speed = 0;
	private static double _altitude = 0;
	private static float _accuracy = 0;
	private static Time _time = new Time();

	private static int _maxSatellites = 0;
	private static int _satellitesInFix = 0;

	public static String getLon() {
		return _lon;
	}

	public static float getAccuracy() {
		return _accuracy;
	}

	public static String getLat() {
		return _lat;
	}

	public static float getSpeed() {
		return _speed;
	}

	public static double getAltitude() {
		return _altitude;
	}

	public static Time getTime() {
		return _time;
	}

	public static int getMaxSatellites() {
		return _maxSatellites;
	}

	public static int getSatellitesInFix() {
		return _satellitesInFix;
	}

	public GpsService() {
		super();
	}

	public static void start(Context context) {
		Log.i(TAG, "starting service");
		context.startService(new Intent(context, GpsService.class));
	}

	public static void stop(Context context) {
		Log.i(TAG, "stopping service");
		context.stopService(new Intent(context, GpsService.class));
	}

	@Override
	public void onStart(Intent intent, int startId) {
		if (wl == null) {
			PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK,
					"StartGPSServiceAndStayAwake");
			wl.acquire();
		}

		initLocationManager();
		super.onStart(intent, startId);
	}

	private void initLocationManager() {
		if (locationManager == null) {
			// Acquire a reference to the system Location Manager
			locationManager = (LocationManager) this
					.getSystemService(Context.LOCATION_SERVICE);

			locationManager.addGpsStatusListener(this);
			locationManager.requestLocationUpdates(
					LocationManager.GPS_PROVIDER, 0, 0, this);
		}
	}

	public void onGpsStatusChanged(int event) {
		status = locationManager.getGpsStatus(status);
		updateGps();
	}

	public void onLocationChanged(Location l) {
		location = l;
		updateGps();
	}

	public void onStatusChanged(String provider, int status, Bundle extras) {
	}

	public void onProviderEnabled(String provider) {
	}

	public void onProviderDisabled(String provider) {
	}

	@Override
	public void onDestroy() {
		if (locationManager != null) {
			locationManager.removeUpdates(this);
			locationManager = null;
		}
		if(wl != null) {
			wl.release();
			wl = null;
		}
		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	private void updateGps() {
		if (status != null) {
			int n = 0;
			int max = 0;
			for (GpsSatellite s : status.getSatellites()) {
				max++;
				if (s.usedInFix()) {
					n++;
				}
			}
			_maxSatellites = max;
			_satellitesInFix = n;
		} else {
			_maxSatellites = 0;
			_satellitesInFix = 0;
		}

		if (location != null) {
			_accuracy = location.getAccuracy();
			double lon = location.getLongitude();
			double lat = location.getLatitude();
			_lon = Location.convert(lon, Location.FORMAT_DEGREES);
			_lat = Location.convert(lat, Location.FORMAT_DEGREES);
			_speed = location.getSpeed();
			_altitude = location.getAltitude();
			_time = new Time();
			_time.set(location.getTime());
		} else {
			_accuracy = 0;
			_speed = 0;
			_time = null;
			_lon = _lat = "";
		}
	}
}
