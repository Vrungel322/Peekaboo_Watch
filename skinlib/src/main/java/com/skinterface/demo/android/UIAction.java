package com.skinterface.demo.android;

import java.util.HashMap;

public class UIAction {

    public String title;
    public Action action;

    public UIAction(String title, Action action) {
        this.title = title;
        this.action = action;
    }

    public UIAction(String title, String action) {
        this(title, new Action(action));
    }

    public UIAction(SSect sect) {
        title = "";
        action = new Action(sect.entity.data);
        if (sect.entity.props != null) {
            for (String key : sect.entity.props.keySet())
                padd(key, sect.entity.props.get(key));
        }
        if (sect.title != null)
            title = sect.title.data;
    }

    public UIAction prependTitle(String prefix) {
        if (title != null)
            title = prefix + title;
        else
            title = prefix;
        return this;
    }

    public UIAction padd(String name, Object value) {
        String str = value == null ? null : String.valueOf(value);
        if (action.params == null)
            action.params = new HashMap<>();
        action.params.put(name, str);
        return this;
    }

}
