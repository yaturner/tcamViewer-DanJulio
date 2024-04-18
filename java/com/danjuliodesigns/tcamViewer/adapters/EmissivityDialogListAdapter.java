package com.danjuliodesigns.tcamViewer.adapters;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.danjuliodesigns.tcamViewer.MainActivity;
import com.danjuliodesigns.tcamViewer.R;

public class EmissivityDialogListAdapter extends BaseAdapter {
    private LayoutInflater inflater;
    private Context context;
    private String[] emStirng;
    private MainActivity mainActivity;

    public EmissivityDialogListAdapter(Context context) {
        this.context = context;
        mainActivity = MainActivity.getInstance();
        emStirng = mainActivity.getResources().getStringArray(R.array.emissivity_strings);
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return emStirng.length;
    }

    @Override
    public Object getItem(int position) {
        return emStirng[position];
    }

    @Override
    public long getItemId(int position) {
        return (long)position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View listItem = inflater.inflate(R.layout.emissivity_list_item, parent, false);
        TextView tvEmissivity = listItem.findViewById(R.id.tvEmissivity);
        tvEmissivity.setTypeface(Typeface.MONOSPACE);
        tvEmissivity.setText(emStirng[position]);
        return tvEmissivity;
    }
}