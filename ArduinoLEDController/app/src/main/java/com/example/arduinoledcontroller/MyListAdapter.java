package com.example.arduinoledcontroller;

import android.content.Context;

import java.util.List;

public class MyListAdapter extends ListAdapter{
    public MyListAdapter(Context context, List<Object> deviceList) {
        super(context, deviceList);
    }
}
