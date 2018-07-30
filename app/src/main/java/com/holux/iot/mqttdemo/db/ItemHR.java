package com.holux.iot.mqttdemo.db;

import android.content.ContentValues;

public class ItemHR {
    private String DeviceID;
    private long time;
    private int bpm;
    private long sid;

    public ContentValues getContentValuesInsert()
    {
        ContentValues values = new ContentValues();

        values.put("DeviceID",  DeviceID);
        values.put("Time",  time);
        values.put("BPM",  bpm);

        return values;
    }

    public String getDeviceID()
    {
        return DeviceID;
    }

    public boolean setDeviceID(String value)
    {
        DeviceID = value;
        return true;
    }

    public long getTime()
    {
        return time;
    }

    public boolean setTime(long value)
    {
        time = value;
        return true;
    }

    public int getBpm()
    {
        return bpm;
    }

    public boolean setBpm(int value)
    {
        bpm = value;
        return true;
    }

    public long getSID()
    {
        return sid;
    }

    public boolean setSID(long id)
    {
        sid = id;
        return true;
    }
}
