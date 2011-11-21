/*
 * Copyright (c) 2011, Parrot
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 *   * Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *
 *   * Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   * Neither the name of "Parrot SA" nor the names of its
 *     contributors may be used to endorse or promote products derived from
 *     this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR 
 * PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.parrot.parrotmaps.geocoding;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.location.Address;

import com.parrot.parrotmaps.BufferedReaderFactory;
import com.parrot.parrotmaps.log.PLog;

public class Geocoder {

	
	private static final String TAG = "Geocoder";
	
	static private String BASE_URL = "http://maps.googleapis.com/maps/api/geocode/json?";
	
	static private String LAT_LNG_PARAMETER = "latlng=";
	static private String ADDRESS_PARAMETER = "address=";
	static private String LANGUAGE_PARAMETER = "language=";
	static private String BOUNDS_PARAMETER = "bounds=";

	protected Context mContext = null;
	private String mLanguage;
	private boolean mInterrupted = false;
	
	/**
	 * Initiate a Geocoder with the given locale
	 * @param context The current application context
	 * @param locale The locale to use
	 */
	public Geocoder(Context context, Locale locale)
	{
		mContext = context;
		mLanguage = locale.getLanguage();
	}
	
	/**
	 * Initiate a Geocoder with the default locale
	 * @param context The current application context
	 */
	public Geocoder(Context context)
	{
		mContext = context;
		mLanguage = Locale.getDefault().getLanguage();
		
	}
	
	
	/**
     * Returns a list of Addresses that are known to describe the
     * named location, 
     * <p> You have to specify a bounding box for the search results by including
     * the Latitude and Longitude of the Lower Left point and Upper Right
     * point of the box. If you don't want to specify a bounding box, use 
     * @see #getFromLocationName(String, int) if you don't need a bounding box

     *
     * @param locationName a user-supplied description of a location
     * @param maxResults max number of addresses to return. Smaller numbers (1 to 5) are recommended
     * @param lowerLeftLatitude the latitude of the lower left corner of the bounding box
     * @param lowerLeftLongitude the longitude of the lower left corner of the bounding box
     * @param upperRightLatitude the latitude of the upper right corner of the bounding box
     * @param upperRightLongitude the longitude of the upper right corner of the bounding box
     *
     * @return a list of Address objects. If no address is found, it returns an empty list.
     *
     * @throws IllegalArgumentException if locationName is null
     * @throws IllegalArgumentException if any latitude is
     * less than -90 or greater than 90
     * @throws IllegalArgumentException if any longitude is
     * less than -180 or greater than 180
     * @throws IOException if the network is unavailable or any other
     * I/O problem occurs
     */
	public List<Address> getFromLocationName(String locationName, int maxResults,
	        double lowerLeftLatitude, double lowerLeftLongitude,
	        double upperRightLatitude, double upperRightLongitude) throws IOException
	{
	        if (locationName == null) 
	        {
	            throw new IllegalArgumentException("locationName == null");
	        }
	        if (lowerLeftLatitude < -90.0 || lowerLeftLatitude > 90.0) 
	        {
	            throw new IllegalArgumentException("lowerLeftLatitude == " + lowerLeftLatitude);
	        }
	        if (lowerLeftLongitude < -180.0 || lowerLeftLongitude > 180.0) 
	        {
	            throw new IllegalArgumentException("lowerLeftLongitude == " + lowerLeftLongitude);
	        }
	        if (upperRightLatitude < -90.0 || upperRightLatitude > 90.0) 
	        {
	            throw new IllegalArgumentException("upperRightLatitude == " + upperRightLatitude);
	        }
	        if (upperRightLongitude < -180.0 || upperRightLongitude > 180.0) 
	        {
	            throw new IllegalArgumentException("upperRightLongitude == " + upperRightLongitude);
	        }
	        PLog.d(TAG, "Requesting address from location name = ", locationName, "into the bounding box (", 
	        		lowerLeftLatitude, ",", lowerLeftLongitude, "|", upperRightLatitude, ",", upperRightLongitude, ")");
	        List<Address> addresses = new ArrayList<Address>();
	        
	        Address address = null;
			URL url;
			StringBuffer result = new StringBuffer("");
			if( isInterrupted() )
				return addresses;
			try {
				url = new URL(BASE_URL + ADDRESS_PARAMETER + locationName + 
						"&" + BOUNDS_PARAMETER + lowerLeftLatitude + "," + lowerLeftLongitude + "|" + upperRightLatitude + "," + upperRightLongitude + 
						((!mLanguage.equals("")) ? "&" + LANGUAGE_PARAMETER + mLanguage : "") +
						"&sensor=true");
				BufferedReader resultsReader = BufferedReaderFactory.openBufferedReader(url);
				for (String line; !isInterrupted() && (line = resultsReader.readLine()) != null;)
				{
					result.append(line).append("\n");
				}
				resultsReader.close();
				JSONObject root = new JSONObject(result.toString());
				if (root.getString("status").equals("OK"))
				{
					
					JSONArray results = root.optJSONArray("results");
					JSONArray address_components = null;
					if (results != null)
					{
						
						JSONObject jobject = null;
						int nbResult = (results.length() > maxResults) ? maxResults : results.length();
						for (int i = 0; i < nbResult; i++)
						{
							
							jobject = results.optJSONObject(i);
							if (jobject != null)
							{
								address = new Address(Locale.getDefault());
								JSONObject geometry = jobject.optJSONObject("geometry");
								fillGeoLocation(geometry, address, Double.NaN, Double.NaN);
								
								String formatted_address = jobject.optString("formatted_address");
								if (!formatted_address.equals("") && address.getAddressLine(0) == null)
								{
									fillAddressLines(formatted_address, address);
								}
								
								address_components = jobject.optJSONArray("address_components");
								if (address_components != null)
								{
									fillAddressField(address_components, address);
								}
								addresses.add(address);
							}
						}
					}
				}
			} catch (SocketTimeoutException e) {
				PLog.e(TAG, "Network timeout", e.getMessage());
				throw e;
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			} catch (UnknownHostException e)
			{
				PLog.e(TAG, "Unknown Host Exception", e.getMessage());
				throw e;
			}
			if( addresses.size() > 0 ) {
				PLog.d(TAG, "Found ", addresses.size(), " address(es)");
			}
			else {
				PLog.d(TAG, "No address found");
			}
	        
	        return addresses;
	}
	
	
	
	
	/**
     * Returns a list of Addresses that are known to describe the
     * named location
     *
     *
     * @param locationName a user-supplied description of a location
     * @param maxResults max number of results to return. Smaller numbers (1 to 5) are recommended
     *
     * @return a list of Address objects.If no address is found, it returns an empty list.
     *
     * @throws IllegalArgumentException if locationName is null
     * @throws IOException if the network is unavailable or any other
     * I/O problem occurs
     */
	public List<Address> getFromLocationName(String locationName, int maxResults) throws IOException
	{
        if (locationName == null) {
            throw new IllegalArgumentException("locationName == null");
        }
        PLog.d(TAG, "Requesting address from location name = ", locationName);
        List<Address> addresses = new ArrayList<Address>();
        Address address = null;
		URL url;
		StringBuffer result = new StringBuffer("");
		if( isInterrupted() )
			return addresses;
		try {
			url = new URL(BASE_URL + ADDRESS_PARAMETER + locationName + 
					((!mLanguage.equals("")) ? "&" + LANGUAGE_PARAMETER + mLanguage : "") +
					"&sensor=true");
			BufferedReader resultsReader = BufferedReaderFactory.openBufferedReader(url);
			for (String line; !isInterrupted() && (line = resultsReader.readLine()) != null;)
			{
				result.append(line).append("\n");
			}
			resultsReader.close();
			JSONObject root = new JSONObject(result.toString());
			if (root.getString("status").equals("OK"))
			{
				
				JSONArray results = root.optJSONArray("results");
				JSONArray address_components = null;
				if (results != null)
				{
					
					JSONObject jobject = null;
					int nbResult = (results.length() > maxResults) ? maxResults : results.length();
					for (int i = 0; i < nbResult; i++)
					{
						
						jobject = results.optJSONObject(i);
						if (jobject != null)
						{
							address = new Address(Locale.getDefault());
							JSONObject geometry = jobject.optJSONObject("geometry");
							fillGeoLocation(geometry, address, Double.NaN, Double.NaN);
							
							String formatted_address = jobject.optString("formatted_address");
							if (!formatted_address.equals("") && address.getAddressLine(0) == null)
							{
								fillAddressLines(formatted_address, address);
							}
							
							address_components = jobject.optJSONArray("address_components");
							if (address_components != null)
							{
								fillAddressField(address_components, address);
							}
							addresses.add(address);
						}
					}
				}
			}
		} catch (SocketTimeoutException e) {
			PLog.e(TAG, "Network timeout ", e.getMessage());
			throw e;
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (UnknownHostException e)
		{
			PLog.e(TAG, "Unknown Host Exception ", e.getMessage());
			throw e;
		}
		if( addresses.size() > 0 ) {
			PLog.d(TAG, "Found ", addresses.size(), " address(es)");
		}
		else {
			PLog.d(TAG, "No address found");
		}
		return addresses;
    }
	
	

	/**
	 * Return a list of Addresses that describes the area immediately surrounding the given coordinates
	 * @param latitude The latitude of a point to search
	 * @param longitude The longitude of a point to search 
	 * @param maxResults number of addresses to return. Smaller numbers (1 to 5) are recommended
	 * @return A list of Address object. If no address is found, it returns an empty list.
	 * 
	 * @throws IllegalArgumentException if latitude is
     * less than -90 or greater than 90
     * @throws IllegalArgumentException if longitude is
     * less than -180 or greater than 180
     * @throws IOException if the network is unavailable or any other
     * I/O problem occurs
	 */
	public List<Address> getFromLocation(double latitude, double longitude, int maxResults) throws IOException
	{
		if (latitude < -90.0 || latitude > 90.0) 
		{
            throw new IllegalArgumentException("latitude == " + latitude);
        }
        if (longitude < -180.0 || longitude > 180.0) 
        {
            throw new IllegalArgumentException("longitude == " + longitude);
        }
		PLog.d(TAG, "Requesting address from latitude = " , latitude , " and longitude = " , longitude);
		List<Address> addresses = new ArrayList<Address>();
		Address address = null;
		URL url;
		StringBuffer result = new StringBuffer("");
		if( isInterrupted() )
			return addresses;
		try {
			url = new URL(BASE_URL + LAT_LNG_PARAMETER + latitude + "," + longitude + 
					((!mLanguage.equals("")) ? "&" + LANGUAGE_PARAMETER + mLanguage : "") + 
					"&sensor=true");
			BufferedReader resultsReader = BufferedReaderFactory.openBufferedReader(url);
			for (String line; !isInterrupted() && ((line = resultsReader.readLine()) != null);)
			{
				result.append(line).append("\n");
			}
			resultsReader.close();
			JSONObject root = new JSONObject(result.toString());
			if (root.getString("status").equals("OK"))
			{
				
				JSONArray results = root.optJSONArray("results");
				JSONArray address_components = null;
				if (results != null)
				{
					
					JSONObject jobject = null;
					int nbResult = (results.length() > maxResults) ? maxResults : results.length();
					for (int i = 0; i < nbResult; i++)
					{
						
						jobject = results.optJSONObject(i);
						if (jobject != null)
						{
							address = new Address(Locale.getDefault());
							JSONObject geometry = jobject.optJSONObject("geometry");
							fillGeoLocation(geometry, address, latitude, longitude);
							
							String formatted_address = jobject.optString("formatted_address");
							if (!formatted_address.equals("") && address.getAddressLine(0) == null)
							{
								fillAddressLines(formatted_address, address);
							}
							
							address_components = jobject.optJSONArray("address_components");
							if (address_components != null)
							{
								fillAddressField(address_components, address);
							}
							addresses.add(address);
						}
					}
				}
			}
		} catch (SocketTimeoutException e) {
			PLog.e(TAG, "Network timeout ", e.getMessage());
			throw e;
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (UnknownHostException e)
		{
			PLog.e(TAG, "Unknown Host Exception", e.getMessage());
			throw e;
		}
		if( addresses.size() > 0 ) {
			PLog.d(TAG, "Found ", addresses.size(), " address(es)");
		}
		else {
			PLog.d(TAG, "No address found");
		}
		return addresses;
	}
	
	
	
	/**
	 * Fill the address individual component field of the given address object
	 * @param address_components The JSON object in which the data should be searched
	 * @param address The address object to modify
	 */
	private void fillAddressField(JSONArray address_components, Address address)
	{
		JSONObject address_object = null;
		JSONArray types = null;
		for (int j = 0; j < address_components.length(); j++)
		{
			address_object = address_components.optJSONObject(j);
			if (address_object != null)
			{
				types = address_object.optJSONArray("types");
				if (types != null)
				{
					for (int k = 0; k < types.length(); k++)
					{
						if (types.optString(k).equals("locality"))
						{
							/*Found locality*/
							address.setLocality(address_object.optString("long_name"));
						}
						else if (types.optString(k).equals("street_number"))
						{
							/*Found street number*/
							address.setFeatureName(address_object.optString("long_name"));
						}
						else if (types.optString(k).equals("route"))
						{
							/*Found route*/
							address.setThoroughfare(address_object.optString("long_name"));
						}
						else if (types.optString(k).equals("administrative_area_level_1"))
						{
							/*Found admin area level 1*/
							address.setAdminArea(address_object.optString("long_name"));
						}
						else if (types.optString(k).equals("administrative_area_level_2"))
						{
							/*Found admin area level 2*/
							address.setSubAdminArea(address_object.optString("long_name"));
						}
						else if (types.optString(k).equals("postal_code"))
						{
							/*Found postal code*/
							address.setPostalCode(address_object.optString("long_name"));
						}
						else if (types.optString(k).equals("country"))
						{
							/*Found country code & name*/
							address.setCountryName(address_object.optString("long_name"));
							address.setCountryCode(address_object.optString("short_name"));
						}
					}
				}
			}
		}
	}
	
	
	/**
	 * Fill the address lines field of the given address object
	 * @param formatted_address The formatted address that should be split in lines
	 * @param address The address object to modify
	 */
	private void fillAddressLines(String formatted_address, Address address)
	{
		String[] a = formatted_address.split(", ");
		StringBuilder second_line = new StringBuilder(""); 
		for (int u = 0; u < a.length; u++)
		{
			/*The first element is always the first line*/
			if (u == 0)
			{
				address.setAddressLine(0, a[0]);
			}
			/*The last element is reached*/
			else if (u == a.length - 1)
			{
				/*If there are more than 2 elements in the address, the last element is set as the third line*/
				/*The other elements are concatenated into the second line */
				if (!second_line.toString().equals(""))
				{
					address.setAddressLine(1, second_line.toString());
					address.setAddressLine(2, a[u]);
				}
				/*If it is the second (& last) element, it is set as the second line*/
				else
				{
					address.setAddressLine(1, a[u]);
				}
			}
			/*The element is not the first nor the last*/
			else
			{
				/*It is concatenated into a string to become the second line*/
				if (second_line.toString().length() > 0)
				{
					second_line.append(", ");
				}
				second_line.append(a[u]);
			}
		}
	}
	
	
	/**
	 * Fill the latitude and longitude field of the given address object
	 * @param geometry The JSON object in which the data should be searched
	 * @param address The address object to modify
	 * @param lat The latitude value that will be set if no latitude was found in the JSON object
	 * @param lng The longitude value that will be set if no longitude was found in the JSON object
	 */
	private void fillGeoLocation(JSONObject geometry, Address address, double lat, double lng)
	{
		if (geometry != null)
		{
			JSONObject geoLocation = geometry.optJSONObject("location");
			if (geoLocation != null)
			{
				address.setLatitude((geoLocation.optDouble("lat") != Double.NaN) ? geoLocation.optDouble("lat") : lat);
				address.setLongitude((geoLocation.optDouble("lng") != Double.NaN) ? geoLocation.optDouble("lng") : lng);
				return;
			}
		}
		if (lat != Double.NaN && lng != Double.NaN)
		{
			address.setLatitude(lat);
			address.setLongitude(lng);
		}
	}
	
	/**
	 * Interrupt the current geocoding query.
	 */
	public synchronized void interruptQuery() {
		mInterrupted = true;
	}

	/**
	 * Check if the query is interrupted
	 * @return True if interrupted, false otherwise
	 */
	public synchronized boolean isInterrupted() {
		return mInterrupted;
	}
}
