package com.skinterface.demo.android;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.TextView;

import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.PendingResult;
import com.google.maps.model.GeocodingResult;
import com.google.maps.model.Geometry;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import static com.skinterface.demo.android.MainActivity.TAG;

/**
 * Editor for different value types
 */
public class EdtValue {

    final SSect value;
    final Action.ActionExecutor executor;
    final Context context;

    String error;

    EdtValue(SSect value, Action.ActionExecutor executor, Context context) {
        this.value= value;
        this.executor = executor;
        this.context = context;
    }

    void executeAction(Action action) {
        if (executor != null && action != null)
            executor.executeAction(action);
    }

//    View wrapToDetails(boolean isHtml, View widget) {
//        FlowPanel pan = null;
//        HTML title = null;
//        SSect ds = value;
//        if (isHtml) {
//            if (ds.title != null && ds.title.data != null) {
//                title = new HTML("<h3>" + ds.title.data + "</h3>", false);
//                pan = new FlowPanel();
//                pan.add(title);
//                pan.add(widget);
//            }
//            if (ds.descr != null && ds.descr.data != null) {
//                if (title != null)
//                    new TooltipListener(title, ds.descr.data);
//                else if (widget instanceof HasMouseOverHandlers)
//                    new TooltipListener((HasMouseOverHandlers) widget, ds.descr.data);
//            }
//        }
//        if (value.entity.val("typeID") == null) {
//            if (pan != null)
//                return pan;
//            return widget;
//        }
//        Button btn = new Button(DC.textEdtDetails(), new ClickHandler() {
//            @Override
//            public void onClick(ClickEvent event) {
//                executeAction(Action.create("enter")
//                        .add("sectID", value.entity.val("typeID"))
//                        .add("vname", value.entity.name)
//                );
//            }
//        });
//        if (pan == null) {
//            pan = new FlowPanel();
//            pan.add(widget);
//        }
//        pan.add(btn);
//        return pan;
//    }

    class ValueWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }
        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            executeAction(Action.create("set").add(value.entity.name, charSequence.toString()));
        }
        @Override
        public void afterTextChanged(Editable editable) {
        }
    }

    View makeWidget(boolean isHtml) {
        if ("int".equals(value.entity.media)) {
            EditText edt = new EditText(context);
            edt.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
            if (value.entity.data != null)
                edt.setText(value.entity.data);
            edt.addTextChangedListener(new ValueWatcher());
            return edt;
        }
        else if ("real".equals(value.entity.media)) {
            EditText edt = new EditText(context);
            edt.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            if (value.entity.data != null)
                edt.setText(value.entity.data);
            edt.addTextChangedListener(new ValueWatcher());
            return edt;
        }
        else if ("geolocation".equals(value.entity.media)) {
            final GeocodeAutocomplete edt = new GeocodeAutocomplete(context);
            if (value.entity.data != null) {
                try {
                    JSONObject jobj = new JSONObject(value.entity.data);
                    if (jobj != null && jobj.has("address"))
                        edt.setText(jobj.getString("address"));
                } catch (Exception e) {
                    error = e.toString();
                }
            }
//            edt.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//                @Override
//                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
//                    GeocodingResult place = (GeocodingResult)adapterView.getAdapter().getItem(i);
//                    String address = place.formattedAddress;
//                    Geometry geometry = place.geometry;
//                    double dlat = geometry.location.lat;
//                    double dlon = geometry.location.lng;
//                    address = saveGeoCode(address, dlat, dlon);
//                    edt.setText(address);
//                }
//                @Override
//                public void onNothingSelected(AdapterView<?> adapterView) {
//                }
//            });
            return edt;
        }
//        else if ("date".equals(value.entity.media) || "datetime".equals(value.entity.media)) {
//            final DateTimePicker box = new DateTimePicker(value.entity.data);
//            box.addValueChangeHandler(new ValueChangeHandler<String>() {
//                @Override
//                public void onValueChange(ValueChangeEvent<String> event) {
//                    String str = event.getValue();
//                    Action action = Action.create("set").add(value.entity.name, str);
//                    executeAction(action);
//                }
//            });
//            return wrapToDetails(isHtml, box);
//        }
        else {
            EditText edt = new EditText(context);
            if (value.entity.data != null)
                edt.setText(value.entity.data);
            edt.addTextChangedListener(new ValueWatcher());
            return edt;
        }
    }

    String saveGeoCode(String address, double dlat, double dlon) {
        address += " ("+Math.round(dlat)+"°"+context.getString(R.string.txt_edt_latitude)+" / "+Math.round(dlon)+"°"+context.getString(R.string.txt_edt_longitude)+")";
        String lat = "" + Math.round(dlat * 10000) / 10000.;
        String lon = "" + Math.round(dlon * 10000) / 10000.;
        Action action = Action.create("set");
        action.add(value.entity.name + ".address", address);
        action.add(value.entity.name + ".latitude", lat);
        action.add(value.entity.name + ".longitude", lon);
        try {
            JSONObject jobj = new JSONObject();
            jobj.put("address", address);
            jobj.put("latitude", lat);
            jobj.put("longitude", lon);
            value.entity.data = jobj.toString();
            action.add(value.entity.name, value.entity.data);
            executeAction(action);
            InputStream is = null;
            try {
                URL url = new URL("http://api.geonames.org/timezoneJSON?lat="+lat+"&lng="+lon+"&username=mkizub");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(5000 /* milliseconds */);
                conn.setConnectTimeout(5000 /* milliseconds */);
                conn.setRequestMethod("GET");
                conn.setDoInput(true);
                // Starts the query
                conn.connect();
                int response = conn.getResponseCode();
                Log.d(TAG, "The response is: " + conn.getResponseMessage());
                if (response == 200) {
                    StringBuilder sb = new StringBuilder();
                    is = conn.getInputStream();
                    InputStreamReader reader = new InputStreamReader(is, "UTF-8");
                    char[] buffer = new char[4096];
                    int sz;
                    while ((sz = reader.read(buffer)) > 0)
                        sb.append(buffer, 0, sz);
                    reader.close();
                    final String result = sb.toString();
                    JSONObject val = new JSONObject(result);
                    String timezone = val.getString("timezoneId");
                    Action set = Action.create("set").add(value.entity.name + ".timezone", timezone);
                    executeAction(set);
                } else {
                    error = "Error on server request";
                }
            } catch (Throwable e) {
                Log.e(TAG, "Server connection error", e);
                error = "Error on server request";
            } finally {
                if (is != null) {
                    try { is.close(); } catch (IOException e) {}
                }
            }
        } catch (Exception e) {
            error = e.toString();
        }
        return address;
    }
}
