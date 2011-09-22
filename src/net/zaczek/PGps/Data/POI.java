package net.zaczek.PGps.Data;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import android.location.Location;
import android.text.TextUtils;

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

	public synchronized static void load() {
		if(_pois != null) return;
		
		_pois = new ArrayList<POI>();

		try {
			final FileReader reader = DataManager.openRead("POI.txt");
			try {
				final BufferedReader in = new BufferedReader(reader);
				while (true) {
					try {
						final String line = DataManager.readLine(in);
						if(line == null) break;
						
						if (TextUtils.isEmpty(line)) continue;
						final String[] parts = line.split(":");
						if (parts.length != 2)
							continue;
						POI poi = new POI();
						poi.setName(parts[0]);

						final String[] strLoc = parts[1].trim().split("[ ,;]");
						if (strLoc.length != 2)
							continue;

						Location loc = new Location("");
						loc.setLatitude(Location.convert(strLoc[0]));
						loc.setLongitude(Location.convert(strLoc[1]));

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

}
