package com.holux.iot.mqttdemo.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LocalDBHelper  extends SQLiteOpenHelper {
    public LocalDBHelper(Context context, String name) {
        super(context, name, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // TODO Auto-generated method stub

        String CREATE_DEVICE_TABLE = "CREATE TABLE device ( " +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +        //serial id
                "DeviceID TEXT UNIQUE ON CONFLICT IGNORE)";       //Device ID

        String CREATE_HR_TABLE = "CREATE TABLE hr ( " +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +        //serial id
                "Time INTEGER NOT NULL, " +                            //time in seconds from 1970/1/1,00:00:00 UTC
                "BPM INTEGER NOT NULL, " +                           //heart rate bpm
                "DeviceID TEXT NOT NULL)";                            //Device ID

        String CREATE_POS_TABLE = "CREATE TABLE pos ( " +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +        //serial id
                "Time INTEGER NOT NULL, " +                     //time in seconds from 1970/1/1,00:00:00 UTC
                "Lat REAL NOT NULL, " +                     //latitude
                "Lon REAL NOT NULL, " +                     //longitude
                "DeviceID TEXT NOT NULL)";                            //Device ID

        db.execSQL(CREATE_DEVICE_TABLE);
        db.execSQL(CREATE_HR_TABLE);
        db.execSQL(CREATE_POS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVer, int newVer) {
        // TODO Auto-generated method stub
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS device");
        db.execSQL("DROP TABLE IF EXISTS hr");
        db.execSQL("DROP TABLE IF EXISTS pos");

        // create fresh table
        this.onCreate(db);
    }

    public void clearAll()
    {
        SQLiteDatabase db = this.getWritableDatabase();

        db.execSQL("DROP TABLE IF EXISTS device");
        db.execSQL("DROP TABLE IF EXISTS hr");
        db.execSQL("DROP TABLE IF EXISTS pos");

        // create fresh table
        this.onCreate(db);

        db.close();
    }

    public long insert(ItemDevice item)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        long id = db.insert("device", null, item.getContentValuesInsert());
        db.close();

        return id;
    }

    public long insert(ItemHR item)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        long id = db.insert("hr", null, item.getContentValuesInsert());
        db.close();

        return id;
    }

    public long insert(ItemPos item)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        long id = db.insert("pos", null, item.getContentValuesInsert());
        db.close();

        return id;
    }

    public String [] queryAllDeviceID()
    {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM device", null);
        String [] deviceIDs = null;

        if(cursor.getCount() > 0) {
            deviceIDs = new String[cursor.getCount()];
            int count = 0;
            if(cursor.moveToFirst()) {
                do {
                    try {
                        deviceIDs[count++] = cursor.getString(1);
                    }
                    catch(Exception e) {

                    }
                } while(cursor.moveToNext());
            }
        }

        cursor.close();

        db.close();

        return deviceIDs;
    }

    public boolean isDeviceIdExist(String id) {
        boolean result;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM device WHERE DeviceID = '" + id + "'", null);

        if(cursor.getCount() > 0) {
            result = true;
        }
        else {
            result = false;
        }

        return result;
    }

    public List<ItemHR> queryHR(String query)
    {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(query, null);

        List<ItemHR> list = new ArrayList<ItemHR>();

        if(cursor.moveToFirst())
        {
            do {
                ItemHR item = new ItemHR();

                try {
                    item.setSID(Integer.parseInt(cursor.getString(0)));
                    item.setTime(cursor.getLong(1));
                    item.setBpm(cursor.getInt(2));
                    item.setDeviceID(cursor.getString(3));
                }
                catch(Exception e) {

                }
                list.add(item);
            } while(cursor.moveToNext());
        }

        cursor.close();

        db.close();

        return list;
    }

    public List<ItemHR> queryHrByDevicePeriod(String id, long start_time, long end_time)
    {
        String query = "SELECT * FROM hr WHERE DeviceID = '" +
                id + "' AND Time BETWEEN " +
                String.format(Locale.US, "%d AND %d ", start_time, end_time) +
                "ORDER BY Time ASC";

        return queryHR(query);
    }

    public List<ItemHR> queryLastHrByDevice(String id, int num)
    {
        String query = "SELECT * FROM hr WHERE DeviceID = '" +
                id + "' " +
                String.format(Locale.US, "ORDER BY Time DESC LIMIT %d", num);

        return queryHR(query);
    }

    public List<ItemPos> queryPos(String query)
    {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(query, null);

        List<ItemPos> list = new ArrayList<ItemPos>();

        if(cursor.moveToFirst())
        {
            do {
                ItemPos item = new ItemPos();

                try {
                    item.setSID(Integer.parseInt(cursor.getString(0)));
                    item.setTime(cursor.getLong(1));
                    item.setLat(cursor.getDouble(2));
                    item.setLon(cursor.getDouble(3));
                    item.setDeviceID(cursor.getString(4));
                }
                catch(Exception e) {

                }
                list.add(item);
            } while(cursor.moveToNext());
        }

        cursor.close();

        db.close();

        return list;
    }

    public List<ItemPos> queryPositionByDevicePeriod(String id, long start_time, long end_time)
    {
        String query = "SELECT * FROM pos WHERE DeviceID = '" +
                id + "' AND Time BETWEEN " +
                String.format(Locale.US, "%d AND %d ", start_time, end_time) +
                "ORDER BY Time ASC";

        return queryPos(query);
    }

    public List<ItemPos> queryLastPositionByDevice(String id, int num)
    {
        String query = "SELECT * FROM pos WHERE DeviceID = '" +
                id + "' " +
                String.format(Locale.US, "ORDER BY Time DESC LIMIT %d", num);

        return queryPos(query);
    }
}
