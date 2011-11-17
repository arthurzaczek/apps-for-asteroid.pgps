package net.zaczek.PGps;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import net.zaczek.PGps.Data.DataManager;

import android.R.string;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.GpsStatus.Listener;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.text.format.Time;
import android.util.Log;

public class GpsService extends Service implements LocationListener, Listener {

	private final static String TAG = "GpsService";
	private static Object lock = new Object();

	private WakeLock wl;
	private Handler hRefresh;
	private MediaPlayer mp;

	private LocationManager locationManager;
	private GpsStatus status = null;
	private Location location = null;
	private Location lastLocation = null;
	private Location lastDistanceLocation = null;

	private String _lat = "";
	private String _lon = "";
	private float _speed = 0;
	private double _altitude = 0;
	private float _accuracy = 0;
	private Time _time = new Time();
	private float _distance = 0;
	private float _tripDistance = 0;

	private int _maxSatellites = 0;
	private int _satellitesInFix = 0;

	private boolean log_trips = false;
	private boolean isLoggingTrips = false;

	public void registerUpdateListener(Handler h) {
		synchronized (lock) {
			hRefresh = h; // There can be only one
		}
	}

	public void unregisterUpdateListener() {
		synchronized (lock) {
			hRefresh = null;
		}
	}

	public Location getLocation() {
		return location;
	}

	public Location getLastLocation() {
		return lastLocation;
	}

	public float getDistance() {
		return _distance;
	}

	public float getTripDistance() {
		return _tripDistance;
	}

	public void clearDistance() {
		_distance = 0;
	}

	public float getAccuracy() {
		return _accuracy;
	}

	public String getLat() {
		return _lat;
	}

	public String getLon() {
		return _lon;
	}

	public float getSpeed() {
		return _speed;
	}

	public double getAltitude() {
		return _altitude;
	}

	public Time getTime() {
		return _time;
	}

	public int getMaxSatellites() {
		return _maxSatellites;
	}

	public int getSatellitesInFix() {
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
			if (pm != null) {
				wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK,
						"StartGPSServiceAndStayAwake");
				wl.acquire();
			}
		}

		mp = new MediaPlayer();
		mp.setOnPreparedListener(new OnPreparedListener() {
			public void onPrepared(MediaPlayer mp) {
				mp.start();
			}
		});
		initLocationManager();

		// playNotification();

		loadPreferences();

		super.onStart(intent, startId);
	}

	public void loadPreferences() {
		SharedPreferences sharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(this);

		log_trips = sharedPreferences.getBoolean("log_trips", false);
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
		lastLocation = location;
		location = l;

		if (lastDistanceLocation == null)
			lastDistanceLocation = location;

		final float d = lastDistanceLocation.distanceTo(location);
		if (location.hasAccuracy() && d > location.getAccuracy()) {
			_distance += d;
			_tripDistance += d;
			lastDistanceLocation = location;
		}

		handleTripStart();
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
		handleTripStop();

		if (locationManager != null) {
			locationManager.removeUpdates(this);
			locationManager = null;
		}
		if (wl != null) {
			wl.release();
			wl = null;
		}

		if (mp != null) {
			mp.reset();
			mp.release();
		}
		super.onDestroy();
	}

	public class LocalBinder extends Binder {
		GpsService getService() {
			return GpsService.this;
		}
	};

	private final IBinder mBinder = new LocalBinder();

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
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
			double lat = location.getLatitude();
			double lon = location.getLongitude();
			try {
				_lat = Location.convert(lat, Location.FORMAT_DEGREES);
				_lon = Location.convert(lon, Location.FORMAT_DEGREES);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
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

		synchronized (lock) {
			if (hRefresh != null) {
				hRefresh.sendEmptyMessage(Main.REFRESH);
			}
		}
	}

	private void playNotification() {
		try {
			mp.reset();
			Uri alert = RingtoneManager
					.getDefaultUri(RingtoneManager.TYPE_ALARM);
			if (alert != null) {
				mp.setDataSource(this, alert);
				mp.prepareAsync();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void handleTripStart() {
		if (log_trips && !isLoggingTrips) {
			try {
				final FileWriter w = DataManager.openWrite("TripLog.txt", true);
				try {
					final Date date = new Date();
					final Locale loc = Locale.getDefault();
					Log.i(TAG, "Locale = " + loc.toString());
					w.append(String.format("\n%s;%s", 
							SimpleDateFormat.getDateInstance(SimpleDateFormat.SHORT, loc).format(date),
							SimpleDateFormat.getTimeInstance(SimpleDateFormat.MEDIUM, loc).format(date)));
					isLoggingTrips = true;
				} finally {
					w.close();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private void handleTripStop() {
		if (log_trips && isLoggingTrips) {
			try {
				final FileWriter w = DataManager.openWrite("TripLog.txt", true);
				try {
					final Date date = new Date();
					final Locale loc = Locale.getDefault();
					w.append(String.format(";%s;%s;%.2f", 
							SimpleDateFormat.getDateInstance(SimpleDateFormat.SHORT, loc).format(date),
							SimpleDateFormat.getTimeInstance(SimpleDateFormat.MEDIUM, loc).format(date),
							_tripDistance / 1000.0f));
					isLoggingTrips = false;
				} finally {
					w.close();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
