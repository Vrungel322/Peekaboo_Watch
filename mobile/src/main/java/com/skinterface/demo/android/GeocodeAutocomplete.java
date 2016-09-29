package com.skinterface.demo.android;


import android.content.Context;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

public class GeocodeAutocomplete extends AutoCompleteTextView implements OnItemClickListener {

    private static final String LOG_TAG = "geocode";
    private static final String PLACES_API_BASE = "https://maps.googleapis.com/maps/api/place";
    private static final String TIMEZONE_API_BASE = "https://maps.googleapis.com/maps/api/timezone";
    private static final String TYPE_AUTOCOMPLETE = "/autocomplete";
    private static final String TYPE_DETAILS = "/details";
    private static final String OUT_JSON = "/json";

    private static final String API_KEY = "AIzaSyCNytttK7SCCGuCmh8VufOGQ4nwH8BCLm8";

    public OnPlaceResolvedListener listener;

    public String description;
    public String address;
    public String placeId;
    public String latitude;
    public String longitude;
    public String timezone;

    static class PlaceInfo {
        String description;
        String placeId;
        public PlaceInfo(String description, String placeId) {
            this.description = description;
            this.placeId = placeId;
        }
        @Override
        public String toString() {
            return description;
        }
    }

    public interface OnPlaceResolvedListener {
        void onPlaceResolved(GeocodeAutocomplete geocode);
    }

    public GeocodeAutocomplete(Context context) {
        super(context);
        init();
    }

    public GeocodeAutocomplete(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GeocodeAutocomplete(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void init() {
        setAdapter(new GooglePlacesAutocompleteAdapter(getContext(), android.R.layout.simple_dropdown_item_1line));
        setOnItemClickListener(this);
    }

    public OnPlaceResolvedListener getOnPlaceResolvedListener() {
        return listener;
    }

    public void setOnPlaceResolvedListener(OnPlaceResolvedListener listener) {
        this.listener = listener;
    }

    public void onItemClick(AdapterView adapterView, View view, int position, long id) {
        PlaceInfo pi = (PlaceInfo) adapterView.getItemAtPosition(position);
        fillDetails(pi);
    }

    public static ArrayList<PlaceInfo> autocomplete(String input) {
        ArrayList<PlaceInfo> resultList = null;

        try {
            StringBuilder sb = new StringBuilder(PLACES_API_BASE + TYPE_AUTOCOMPLETE + OUT_JSON);
            sb.append("?key=" + API_KEY);
            sb.append("&types=geocode");
            sb.append("&input=" + URLEncoder.encode(input, "utf8"));
            URL url = new URL(sb.toString());
            JSONObject jobj = IOUtils.parseHTTPResponce(url.openConnection());
            if (jobj == null)
                return resultList;
            JSONArray predsJsonArray = jobj.getJSONArray("predictions");
            // Extract the Place descriptions from the results
            resultList = new ArrayList<PlaceInfo>(predsJsonArray.length());
            for (int i = 0; i < predsJsonArray.length(); i++) {
                JSONObject jpl = predsJsonArray.getJSONObject(i);
                PlaceInfo pi = new PlaceInfo(jpl.getString("description"), jpl.getString("place_id"));
                Log.d(LOG_TAG, i+":"+pi.placeId+":"+pi.description);
                resultList.add(pi);
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Cannot getting places", e);
        }

        return resultList;
    }

    private void fillDetails(final PlaceInfo pi) {
        new AsyncTask<Void,Void,Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    StringBuilder sb = new StringBuilder(PLACES_API_BASE + TYPE_DETAILS + OUT_JSON);
                    sb.append("?key=" + API_KEY);
                    sb.append("&placeid=" + pi.placeId);
                    URL url = new URL(sb.toString());
                    JSONObject jobj = IOUtils.parseHTTPResponce(url.openConnection());
                    placeId = pi.placeId;
                    description = pi.description;
                    address = jobj.getJSONObject("result").getString("formatted_address");
                    JSONObject loc = jobj.getJSONObject("result").getJSONObject("geometry").getJSONObject("location");
                    latitude = loc.getString("lat");
                    longitude = loc.getString("lng");
                    Log.i(LOG_TAG, "lat:"+latitude+"; lng:"+longitude+"; descr:"+description+"; adder:"+address);

                    sb = new StringBuilder(TIMEZONE_API_BASE + OUT_JSON);
                    sb.append("?key=" + API_KEY);
                    sb.append("&location=" + latitude + "," + longitude);
                    sb.append("&timestamp=0");
                    url = new URL(sb.toString());
                    jobj = IOUtils.parseHTTPResponce(url.openConnection());
                    timezone = jobj.getString("timeZoneId");
                    Log.i(LOG_TAG, "timezone:"+timezone);
                    return Boolean.TRUE;
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Cannot fill place info", e);
                }
                return Boolean.FALSE;
            }

            @Override
            protected void onPostExecute(Boolean res) {
                if (res != null && res.booleanValue() && listener != null)
                    listener.onPlaceResolved(GeocodeAutocomplete.this);
            }
        }.execute();
    }

    class GooglePlacesAutocompleteAdapter extends ArrayAdapter<PlaceInfo> implements Filterable {
        private ArrayList<PlaceInfo> resultList;

        public GooglePlacesAutocompleteAdapter(Context context, int textViewResourceId) {
            super(context, textViewResourceId);
        }

        @Override
        public int getCount() {
            return resultList.size();
        }

        @Override
        public PlaceInfo getItem(int index) {
            return resultList.get(index);
        }

        @Override
        public Filter getFilter() {
            Filter filter = new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults filterResults = new FilterResults();
                    if (constraint != null) {
                        // Retrieve the autocomplete results.
                        resultList = autocomplete(constraint.toString());
                        if (resultList != null) {
                            // Assign the data to the FilterResults
                            filterResults.values = resultList;
                            filterResults.count = resultList.size();
                        }
                    }
                    return filterResults;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    if (results != null && results.count > 0) {
                        notifyDataSetChanged();
                    } else {
                        notifyDataSetInvalidated();
                    }
                }
            };
            return filter;
        }
    }
}