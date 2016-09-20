package com.skinterface.demo.android;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Demo class for Entity (text, image, etc)
 */
public class DEntity implements Serializable {

    public String media = "none"; // media (start of mime type: "text", "image" and so on)
    public String role; // entity or sub-section role (withing it's parent)
    public String name; // entity or sub-section name, unique withing it's section for it's role
    public String data; // text for text entity, url of image entity, node guid for a link and so on

    public Map<String, String> props;

    public void copyFrom(DEntity entity) {
        media = entity.media;
        role = entity.role;
        name = entity.name;
        data = entity.data;
        if (entity.props != null)
            props = new HashMap<>(entity.props);
    }

    public DEntity padd(String name, Object value) {
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
