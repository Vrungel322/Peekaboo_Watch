package com.skinterface.demo.android;

import com.google.gson.*;

import java.util.HashMap;
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
        JsonElement jel = new Gson().toJsonTree(json);
        if (jel == null || !jel.isJsonObject())
            return de;
        return SEntity.fromJson(jel.getAsJsonObject(), de);
    }

    static SEntity fromJson(JsonObject jobj, SEntity de) {
        if (jobj == null || jobj.isJsonNull())
            return de;
        if (de == null)
            de = new SEntity();
        if (jobj.has("media"))
            de.media = jobj.get("media").getAsString();
        if (jobj.has("role"))
            de.role = jobj.get("role").getAsString();
        if (jobj.has("name"))
            de.name = jobj.get("name").getAsString();
        if (jobj.has("data"))
            de.data = jobj.get("data").getAsString();
        if (jobj.has("props")) {
            JsonObject jprops = jobj.get("props").getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : jprops.entrySet())
                de.padd(entry.getKey(), entry.getValue().getAsString());
        }
        return de;
    }

    public JsonObject fillJson(JsonObject jobj) {
        jobj.addProperty("media", media);
        if (role != null)
            jobj.addProperty("role", role);
        if (name != null)
            jobj.addProperty("name", name);
        if (data != null)
            jobj.addProperty("data", data);
        if (props != null && !props.isEmpty()) {
            JsonObject jprops = new JsonObject();
            for (Map.Entry<String,String> entry : props.entrySet())
                jprops.addProperty(entry.getKey(), entry.getValue());
            jobj.add("props", jprops);
        }
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
