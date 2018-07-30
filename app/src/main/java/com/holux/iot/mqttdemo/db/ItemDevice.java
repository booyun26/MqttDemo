package com.holux.iot.mqttdemo.db;

import android.content.ContentValues;

public class ItemDevice {
    private String DeviceID;
    private long sid;

    public ContentValues getContentValuesInsert()
    {
        ContentValues values = new ContentValues();

        values.put("DeviceID",  DeviceID);

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
