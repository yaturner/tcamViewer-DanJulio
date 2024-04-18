package com.danjuliodesigns.tcamViewer.adapters;

import android.content.Context;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.danjuliodesigns.tcamViewer.R;

import java.util.ArrayList;

public class CameraDiscoveryAdapter extends ArrayAdapter<Pair<String, String>> {

    private ArrayList<Pair<String, String>> items;
    private Context context;

    public CameraDiscoveryAdapter(Context context, ArrayList<Pair<String, String>> items) {
        super(context, 0, items);
        this.context = context;
        this.items = items;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
//        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.camera_discovery_item, parent, false);
  //      }

        TextView name = convertView.findViewById(R.id.tvCameraName);
        TextView address = convertView.findViewById(R.id.tvCameraAddress);
        Pair<String, String> item = items.get(position);
        name.setText(item.first);
        address.setText(item.second);

        return convertView;
    }
    public void updateItems(Pair<String, String> item) {
        boolean match = false;
        for(Pair<String, String> pair : items) {
            if(pair.first.equals(item.first) && pair.second.equals(item.second)) {
                match = true;
            }
        }
        if(!match) {
            items.add(item);
            notifyDataSetChanged();
        }
    }
}
