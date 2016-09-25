package com.skinterface.demo.android;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Server class for Entity (text, image, etc)
 */
class SEntity {

    public String media = "none"; // media (start of mime type: "text", "image" and so on)
    public String role; // entity or sub-section role (withing it's parent)
    public String name; // entity or sub-section name, unique withing it's section for it's role
    public String data; // text for text entity, url of image entity, node guid for a link and so on

    public Map<String, String> props;

    static SEntity fromJson(String json, SEntity de) {
        if (json == null)
            return de;
        JSONTokener tokener = new JSONTokener(json);
        Object obj = null;
        try { obj = tokener.nextValue(); } catch (JSONException e) {}
        if (!(obj instanceof JSONObject) || obj == JSONObject.NULL)
            return de;
        return SEntity.fromJson((JSONObject) obj, de);
    }

    static SEntity fromJson(JSONObject jobj, SEntity de) {
        if (jobj == null || jobj == JSONObject.NULL)
            return de;
        try {
            if (de == null)
                de = new SEntity();
            if (jobj.has("media"))
                de.media = jobj.getString("media");
            if (jobj.has("role"))
                de.role = jobj.getString("role");
            if (jobj.has("name"))
                de.name = jobj.getString("name");
            if (jobj.has("data"))
                de.data = jobj.getString("data");
            if (jobj.has("props")) {
                JSONObject jprops = jobj.getJSONObject("props");
                Iterator<String> keys = jprops.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    String val = jprops.getString(key);
                    de.padd(key, val);
                }
            }
        } catch (JSONException e) { de = null; }
        return de;
    }

    public JSONObject fillJson(JSONObject jobj) {
        try {
            jobj.put("media", media);
            if (role != null)
                jobj.put("role", role);
            if (name != null)
                jobj.put("name", name);
            if (data != null)
                jobj.put("data", data);
            if (props != null && !props.isEmpty()) {
                JSONObject jprops = new JSONObject();
                for (Map.Entry<String, String> entry : props.entrySet())
                    jprops.put(entry.getKey(), entry.getValue());
                jobj.put("props", jprops);
            }
        } catch (JSONException e) {}
        return jobj;
    }

    public void copyFrom(SEntity entity) {
        media = entity.media;
        role = entity.role;
        name = entity.name;
        data = entity.data;
        if (entity.props != null)
            props = new HashMap<>(entity.props);
    }

    public SEntity padd(String name, Object value) {
        String str = value == null ? null : String.valueOf(value);
        if (props == null)
            props = new HashMap<>();
        props.put(name, str);
        return this;
    }

    public String val(String name) {
        if (props == null)
            return null;
        return props.get(name);
    }

    public String val(String name, String dflt) {
        if (props == null)
            return dflt;
        String res = props.get(name);
        return (res == null) ? dflt : res;
    }
}
