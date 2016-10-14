package com.skinterface.demo.android;


import android.content.Context;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Filter;
import android.widget.Filterable;

import java.util.ArrayList;

public class GeocodeAutocomplete extends AutoCompleteTextView implements OnItemClickListener {

    private static final String LOG_TAG = "geocode";

    public OnPlaceResolvedListener listener;

    public String description;
    public String address;
    public String placeId;
    public String latitude;
    public String longitude;
    public String timezone;

    public interface OnPlaceResolvedListener {
        void onPlaceResolved(PlaceInfo pi);
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

    private void fillDetails(final PlaceInfo pi) {
        new AsyncTask<Void,Void,Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                return IOUtils.fillPlaceDetails(pi);
            }
            @Override
            protected void onPostExecute(Boolean res) {
                if (res != null && res.booleanValue()) {
                    GeocodeAutocomplete.this.description = pi.description;
                    GeocodeAutocomplete.this.address     = pi.address;
                    GeocodeAutocomplete.this.placeId     = pi.placeId;
                    GeocodeAutocomplete.this.latitude    = pi.latitude;
                    GeocodeAutocomplete.this.longitude   = pi.longitude;
                    GeocodeAutocomplete.this.timezone    = pi.timezone;
                    if (listener != null)
                        listener.onPlaceResolved(pi);
                }
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
                        resultList = IOUtils.placesAutoComplete(constraint.toString());
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