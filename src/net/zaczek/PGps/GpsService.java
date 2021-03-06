package net.zaczek.PGps;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Timer;
import java.util.TimerTask;

import net.zaczek.PGps.Data.DataManager;
import net.zaczek.PGps.Data.DatabaseManager;
import net.zaczek.PGps.Data.PGpsPreferences;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.GpsStatus.Listener;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.text.format.Time;
import android.util.Log;

public class GpsService extends Service implements LocationListener, Listener {

	private final static String TAG = "PGps";
	private static Object lock = new Object();

	private WakeLock wl;
	private Handler hRefresh;
	private MediaPlayer mp;
	private DatabaseManager db;

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
	private Timer _updateTripTimer;

	private int _maxSatellites = 0;
	private int _satellitesInFix = 0;

	private long log_trip_id = -1;
	private Time _last_trip_update = new Time();

	private Time _last_record_positions_update = new Time();
	private OutputStreamWriter _record_positions_gpxwriter = null;

	class UpdateTimeTask extends TimerTask {
		public void run() {
			DataManager.updateTripsGeoLocations(getApplicationContext(), db);
			try {
				DataManager.postTrips(getApplicationContext(), db);
			} catch (IOException e) {
				Log.e(TAG, "Unable to post trips to google", e);
				e.printStackTrace();
			}
		}
	}

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
		Log.i(TAG, "Constructing GpsService");
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
		Log.i(TAG, "GpsService.onStart");
		if (wl == null) {
			PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			if (pm != null) {
				wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK,
						"StartGPSServiceAndStayAwake");
				wl.acquire();
			}
		}

		db = DatabaseManager.getInstance(getApplicationContext());

		mp = new MediaPlayer();
		mp.setOnPreparedListener(new OnPreparedListener() {
			public void onPrepared(MediaPlayer mp) {
				mp.start();
			}
		});
		initLocationManager();

		// playNotification();

		applyPreferences();

		Log.i(TAG, "GpsService.onStart finished");
		super.onStart(intent, startId);
	}

	public void applyPreferences() {
		Log.i(TAG, "GpsService.applyPreferences");
		if (PGpsPreferences.getInstance(this).log_trips) {
			if (_updateTripTimer == null) {
				_updateTripTimer = new Timer();
				_updateTripTimer.schedule(new UpdateTimeTask(), 10000, 60000);
			}
		} else if (_updateTripTimer != null) {
			_updateTripTimer.cancel();
			_updateTripTimer = null;
		}
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
		if (locationManager != null) {
			status = locationManager.getGpsStatus(status);
			updateGps();
		}
	}

	public void onLocationChanged(Location l) {
		lastLocation = location;
		location = new Location(l); // make a copy! This is not a C# struct

		if (lastDistanceLocation == null)
			lastDistanceLocation = location;

		final float d = lastDistanceLocation.distanceTo(location);
		final boolean hasAccuracy = location.hasAccuracy();
		if (!hasAccuracy || (hasAccuracy && d > location.getAccuracy())) {
			_distance += d;
			_tripDistance += d;
			lastDistanceLocation = location;
		}

		updateTrip();
		updateGps();
		updateGpsLog();
	}

	public void onStatusChanged(String provider, int status, Bundle extras) {
	}

	public void onProviderEnabled(String provider) {
	}

	public void onProviderDisabled(String provider) {
	}

	@Override
	public void onDestroy() {
		Log.i(TAG, "GpsService.onDestroy");
		stopTrip();

		DataManager.endGPSLog(_record_positions_gpxwriter);
		_record_positions_gpxwriter = null;

		if (_updateTripTimer != null) {
			_updateTripTimer.cancel();
			_updateTripTimer = null;
		}

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

		Log.i(TAG, "GpsService.onDestroy finished");
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

	// private void playNotification() {
	// try {
	// mp.reset();
	// Uri alert = RingtoneManager
	// .getDefaultUri(RingtoneManager.TYPE_ALARM);
	// if (alert != null) {
	// mp.setDataSource(this, alert);
	// mp.prepareAsync();
	// }
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	// }

	public void updateGpsLog() {
		int record_positions = PGpsPreferences.getInstance(this).record_positions;
		if (location == null || record_positions == 0)
			return;
		Time now = new Time();
		now.setToNow();
		long timeDiff = now.toMillis(true)
				- _last_record_positions_update.toMillis(true);

		if (_record_positions_gpxwriter == null) {
			_record_positions_gpxwriter = DataManager.beginGPSLog();
		}

		if (timeDiff > (record_positions * 1000)) {
			DataManager.writeGPSLog(_record_positions_gpxwriter, _lat, _lon,
					_time, _speed, _accuracy);
			_last_record_positions_update = now;
		}
	}

	public void updateTrip() {
		if (location == null
				|| PGpsPreferences.getInstance(this).log_trips == false)
			return;
		Time now = new Time();
		now.setToNow();
		long timeDiff = now.toMillis(true) - _last_trip_update.toMillis(true);

		if (log_trip_id == -1) {
			_tripDistance = 0;
			log_trip_id = db.newTripEntry(now, location,
					PGpsPreferences.getInstance(this).merge_trips);
			_last_trip_update = now;
		} else if (log_trip_id != -1 && timeDiff > 10000) {
			db.updateTripEntry(log_trip_id, now, location, _tripDistance, false);
			_last_trip_update = now;
		}
	}

	public void stopTrip() {
		if (PGpsPreferences.getInstance(this).log_trips && log_trip_id != -1) {
			Time now = new Time();
			now.setToNow();
			db.updateTripEntry(log_trip_id, now, location, _tripDistance, true);
			log_trip_id = -1;
			_tripDistance = 0;
		}
	}
}
