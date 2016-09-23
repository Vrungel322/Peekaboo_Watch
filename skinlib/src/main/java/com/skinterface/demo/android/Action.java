package com.skinterface.demo.android;

import com.google.gson.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Action {

    public interface ActionExecutor {
        void executeAction(Action action);
    }

    // action to execute, like "show"
    private String action;
    // additional params, needed depending on action type
    public Map<String, String> params;

    private Action() {
        this.action = "none";
        this.params = new HashMap<>();
    }

    public Action(String action) {
        this.action = action;
        this.params = new HashMap<>();
    }

    public static Action create(String action) {
        Action a = new Action(action);
        return a;
    }

    public JsonObject serializeToCmd(String sessionId, int caps) {
        JsonObject jcmd = new JsonObject();
        if (sessionId != null)
            jcmd.addProperty("session", sessionId);
        if (caps != 0)
            jcmd.addProperty("caps", caps);
        if (action != null)
            jcmd.addProperty("action", action);
        if (params != null) {
            JsonObject jparams = new JsonObject();
            for (Map.Entry<String, String> e : params.entrySet()) {
                if (e.getValue() != null)
                    jparams.addProperty(e.getKey(), e.getValue());
            }
            jcmd.add("params", jparams);
        }
        return jcmd;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Action add(String name, Object value) {
        String str = value == null ? null : String.valueOf(value);
        if (params == null)
            params = new HashMap<>();
        params.put(name, str);
        return this;
    }

    public Action del(String name) {
        if (params == null)
            return this;
        params.remove(name);
        return this;
    }

    public String val(String name) {
        if (params == null)
            return null;
        return params.get(name);
    }

    public String val(String name, String dflt) {
        if (params == null)
            return dflt;
        String res = params.get(name);
        return (res == null) ? dflt : res;
    }
}
