package net.zaczek.PGps.Data;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

import android.location.Location;
import android.text.TextUtils;
import android.text.format.Time;

public class POI {
	private static ArrayList<POI> _pois;

	public static ArrayList<POI> getPois() {
		return _pois;
	}

	public static int size() {
		return _pois.size();
	}

	public static POI get(int idx) {
		return _pois.get(idx);
	}
	
	public static POI get(Location loc) {
		load();
		float min = Float.MAX_VALUE;
		POI result =null;
		for(POI current : _pois) {
			final float dist = current.location.distanceTo(loc);
			if(dist < min) {
				result = current;
				min = dist;
			}
		}
		return result;
	}

	public synchronized static void load() {
		if (_pois != null)
			return;

		_pois = new ArrayList<POI>();

		try {
			final FileReader reader = DataManager.openRead("POI.txt");
			try {
				final BufferedReader in = new BufferedReader(reader);
				while (true) {
					try {
						final String line = DataManager.readLine(in);
						if (line == null)
							break;

						if (TextUtils.isEmpty(line))
							continue;
						final String[] parts = line.split(":");
						if (parts.length != 2)
							continue;
						POI poi = new POI();
						poi.setName(parts[0]);

						final String[] strLoc = parts[1].trim().split(",");
						if (strLoc.length != 2)
							continue;

						Location loc = new Location("");
						loc.setLatitude(Location.convert(strLoc[0].trim()));
						loc.setLongitude(Location.convert(strLoc[1].trim()));
						poi.setLocation(loc);

						_pois.add(poi);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			} finally {
				reader.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private String name;
	private Location location;

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setLocation(Location location) {
		this.location = location;
	}

	public Location getLocation() {
		return location;
	}

	public synchronized static void save(Location loc) throws IOException {
		Time now = new Time();
		now.setToNow();
		POI poi = new POI();
		poi.setName(now.format("%c").replace(':', '.'));
		poi.setLocation(loc);
		_pois.add(poi);

		final OutputStreamWriter writer = DataManager
				.openWrite("POI.txt", true);
		try {
			final BufferedWriter out = new BufferedWriter(writer);
			out.write(String.format("\n%s: %s,%s", poi.getName(), Location
					.convert(loc.getLatitude(), Location.FORMAT_DEGREES),
					Location.convert(loc.getLongitude(),
							Location.FORMAT_DEGREES)));
			out.flush();
		} finally {
			writer.close();
		}

	}
}
