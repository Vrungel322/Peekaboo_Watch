package com.skinterface.demo.android;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

class PlaceInfo {
    String description;
    String placeId;
    String address;
    String latitude;
    String longitude;
    String timezone;

    public PlaceInfo(String description, String placeId) {
        this.description = description;
        this.placeId = placeId;
    }

    @Override
    public String toString() {
        return description;
    }

    static PlaceInfo fromJson(String json) {
        if (json == null || json.length() == 0)
            return null;
        JSONTokener tokener = new JSONTokener(json);
        Object obj = null;
        try { obj = tokener.nextValue(); } catch (JSONException e) {}
        if (!(obj instanceof JSONObject) || obj == JSONObject.NULL)
            return null;
        return PlaceInfo.fromJson((JSONObject) obj);
    }

    static PlaceInfo fromJson(JSONObject jobj) {
        PlaceInfo pi = null;
        try {
            pi = new PlaceInfo(jobj.getString("description"), jobj.getString("placeId"));
            if (jobj.has("address"))
                pi.address = jobj.getString("address");
            if (jobj.has("latitude"))
                pi.latitude = jobj.getString("latitude");
            if (jobj.has("longitude"))
                pi.longitude = jobj.getString("longitude");
            if (jobj.has("timezone"))
                pi.timezone = jobj.getString("timezone");
        } catch (JSONException e) {}
        return pi;
    }

    public JSONObject fillJson(JSONObject jobj) {
        try {
            jobj.put("description", description);
            jobj.put("placeId", placeId);
            if (address != null)
                jobj.put("address", address);
            if (latitude != null)
                jobj.put("latitude", latitude);
            if (longitude != null)
                jobj.put("longitude", longitude);
            if (timezone != null)
                jobj.put("timezone", timezone);
        } catch (JSONException e) {}
        return jobj;
    }


}
