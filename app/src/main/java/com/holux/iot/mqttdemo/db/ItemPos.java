package com.holux.iot.mqttdemo.db;

import android.content.ContentValues;

public class ItemPos {
    private String DeviceID;
    private long time;
    private double lat, lon;
    private long sid;

    public ContentValues getContentValuesInsert()
    {
        ContentValues values = new ContentValues();

        values.put("DeviceID",  DeviceID);
        values.put("Time",  time);
        values.put("Lat",  lat);
        values.put("Lon",  lon);

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

    public double getLat()
    {
        return lat;
    }

    public boolean setLat(double value)
    {
        lat = value;
        return true;
    }

    public double getLon()
    {
        return lon;
    }

    public boolean setLon(double value)
    {
        lon = value;
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
