package com.skinterface.demo.android;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;

public class IOUtils {

    private static final String TAG = "ioutils";

    public static final String RSVP_ACTION_PATH  = "/rsvp/action";
    public static final String RSVP_REPLAY_PATH  = "/rsvp/replay/";

    public static final String HELP_ACTION_PATH  = "/help/action/";
    public static final String HELP_REPLAY_PATH  = "/help/replay/";

    public static final String CHAT_ACTION_PATH  = "/chat/action/";
    public static final String CHAT_REPLAY_PATH  = "/chat/replay/";
    public static final String CHAT_POST_PATH    = "/chat/post";

    static final String UpStars_Base_URL = "http://u-com.pro/UpStars/";
    //static final String UpStars_Base_URL = "http://192.168.2.157:8080/UpStars/";
    //static final String UpStars_Base_URL = "http://192.168.1.100:8888/";
    static final String UpStars_JSON_URL = UpStars_Base_URL + "Service";

    public final static Charset UTF8 = Charset.forName("UTF-8");

    private static final String PLACES_API_BASE = "https://maps.googleapis.com/maps/api/place";
    private static final String TIMEZONE_API_BASE = "https://maps.googleapis.com/maps/api/timezone";
    private static final String TYPE_AUTOCOMPLETE = "/autocomplete";
    private static final String TYPE_DETAILS = "/details";
    private static final String OUT_JSON = "/json";
    private static final String API_KEY = "AIzaSyCNytttK7SCCGuCmh8VufOGQ4nwH8BCLm8";

    private IOUtils() {}

    public static JSONObject parseHTTPResponce(URLConnection uconn)
            throws IOException, JSONException
    {
        if (!(uconn instanceof HttpURLConnection))
            return null;
        HttpURLConnection conn = (HttpURLConnection) uconn;
        InputStream is = null;
        try {
            int response = conn.getResponseCode();
            Log.d("io", "The response is: " + conn.getResponseMessage());
            if (response != 200)
                return null;
            Charset charset = null;
            {
                String contentType = conn.getContentType();
                String[] values = contentType.split(";"); // values.length should be 2
                for (String value : values) {
                    value = value.trim();
                    if (value.toLowerCase().startsWith("charset=")) {
                        try {
                            charset = Charset.forName(value.substring("charset=".length()));
                        } catch (Exception e) {}
                    }
                }
                if (charset == null)
                    charset = UTF8; //Assumption
            }
            StringBuilder sb = new StringBuilder();
            is = conn.getInputStream();
            InputStreamReader reader = new InputStreamReader(is, charset);
            char[] buffer = new char[4096];
            int sz;
            while ((sz = reader.read(buffer)) > 0)
                sb.append(buffer, 0, sz);
            reader.close();
            conn.disconnect();
            final String result = sb.toString();
            JSONObject jobj = new JSONObject(result);
            return jobj;
        } finally {
            safeClose(is);
        }
    }

    public static void safeClose(InputStream inp) {
        if (inp != null) {
            try {
                inp.close();
            } catch (IOException e) {
            }
        }
    }
    public static void safeClose(OutputStream out) {
        if (out != null) {
            try {
                out.close();
            } catch (IOException e) {
            }
        }
    }

    public static boolean copyStream(InputStream is, OutputStream os, boolean close) {
        final int buffer_size=1024;

        try {
            byte[] bytes=new byte[buffer_size];
            for(;;) {
                int count=is.read(bytes, 0, buffer_size);
                if (count==-1)
                    break;
                os.write(bytes, 0, count);
            }
            return true;
        }
        catch(Exception ex){
            return false;
        }
        finally {
            if (close) {
                safeClose(is);
                safeClose(os);
            }
        }
    }

    public static ArrayList<PlaceInfo> placesAutoComplete(String input) {
        ArrayList<PlaceInfo> resultList = null;

        try {
            StringBuilder sb = new StringBuilder(PLACES_API_BASE + TYPE_AUTOCOMPLETE + OUT_JSON);
            sb.append("?key=" + API_KEY);
            sb.append("&types=geocode");
            sb.append("&input=" + URLEncoder.encode(input, "utf8"));
            URL url = new URL(sb.toString());
            JSONObject jobj = parseHTTPResponce(url.openConnection());
            if (jobj == null)
                return resultList;
            JSONArray predsJsonArray = jobj.getJSONArray("predictions");
            // Extract the Place descriptions from the results
            resultList = new ArrayList<PlaceInfo>(predsJsonArray.length());
            for (int i = 0; i < predsJsonArray.length(); i++) {
                JSONObject jpl = predsJsonArray.getJSONObject(i);
                PlaceInfo pi = new PlaceInfo(jpl.getString("description"), jpl.getString("place_id"));
                Log.d(TAG, i+":"+pi.placeId+":"+pi.description);
                resultList.add(pi);
            }
        } catch (Exception e) {
            Log.e(TAG, "Cannot getting places", e);
        }
        return resultList;
    }

    public static boolean fillPlaceDetails(final PlaceInfo pi) {
        try {
            StringBuilder sb = new StringBuilder(IOUtils.PLACES_API_BASE + IOUtils.TYPE_DETAILS + IOUtils.OUT_JSON);
            sb.append("?key=" + IOUtils.API_KEY);
            sb.append("&placeid=" + pi.placeId);
            URL url = new URL(sb.toString());
            JSONObject jobj = IOUtils.parseHTTPResponce(url.openConnection());
            pi.address = jobj.getJSONObject("result").getString("formatted_address");
            JSONObject loc = jobj.getJSONObject("result").getJSONObject("geometry").getJSONObject("location");
            pi.latitude = loc.getString("lat");
            pi.longitude = loc.getString("lng");
            Log.i(TAG, "lat:"+pi.latitude+"; lng:"+pi.longitude+"; descr:"+pi.description+"; adder:"+pi.address);

            sb = new StringBuilder(IOUtils.TIMEZONE_API_BASE + IOUtils.OUT_JSON);
            sb.append("?key=" + IOUtils.API_KEY);
            sb.append("&location=" + pi.latitude + "," + pi.longitude);
            sb.append("&timestamp=0");
            url = new URL(sb.toString());
            jobj = IOUtils.parseHTTPResponce(url.openConnection());
            pi.timezone = jobj.getString("timeZoneId");
            Log.i(TAG, "timezone:"+pi.timezone);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Cannot fill place info", e);
            return false;
        }
    }

}
