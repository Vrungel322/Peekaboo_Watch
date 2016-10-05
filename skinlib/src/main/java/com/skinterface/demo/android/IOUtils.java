package com.skinterface.demo.android;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.nio.charset.Charset;

public class IOUtils {

    public final static Charset UTF8 = Charset.forName("UTF-8");

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
}
