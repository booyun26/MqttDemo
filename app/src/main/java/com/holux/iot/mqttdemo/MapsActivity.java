package com.holux.iot.mqttdemo;

import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.holux.iot.mqttdemo.db.ItemPos;
import com.holux.iot.mqttdemo.db.LocalDBHelper;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    static final public String EXTRA_DEVICE_NAME = "extra_device_name";
    static final public String EXTRA_LAT = "extra_lat";
    static final public String EXTRA_LON = "extra_lon";

    private GoogleMap mMap;
    private LatLng mCurPos;
    private String mDeviceName;
    private List<ItemPos> listPos = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        Intent it = getIntent();
        Bundle bundle = it.getExtras();

        if(bundle != null) {
            if (bundle.containsKey(EXTRA_DEVICE_NAME)) {
                mDeviceName = bundle.getString(EXTRA_DEVICE_NAME);
                LocalDBHelper dbHelper = new LocalDBHelper(MapsActivity.this, MainActivity.DB_NAME);
                listPos = dbHelper.queryLastPositionByDevice(mDeviceName, 10);
                dbHelper.close();
            } else {
                mDeviceName = "Demo";
            }

            if (bundle.containsKey(EXTRA_LAT) && bundle.containsKey(EXTRA_LON)) {
                double lat = bundle.getDouble(EXTRA_LAT);
                double lon = bundle.getDouble(EXTRA_LON);

                mCurPos = new LatLng(lat, lon);
            } else {
                if(listPos != null && !listPos.isEmpty()) {
                    mCurPos = new LatLng(listPos.get(0).getLat(), listPos.get(0).getLon());
                }
            }
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        if(listPos != null && !listPos.isEmpty()) {
            for(int i=0; i<listPos.size(); i++) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(listPos.get(i).getTime());
                String szTime = new SimpleDateFormat("yyyy/MM/dd,HH:mm:ss", Locale.US).format(calendar.getTime());
                LatLng pos = new LatLng(listPos.get(i).getLat(), listPos.get(i).getLon());
                if(pos.equals(mCurPos)) {
                    mMap.addMarker(new MarkerOptions()
                            .position(pos).title(mDeviceName + "@" + szTime))
                            .showInfoWindow();
                }
                else {
                    mMap.addMarker(new MarkerOptions()
                            .position(new LatLng(listPos.get(i).getLat(), listPos.get(i).getLon())).title(mDeviceName + "@" + szTime));
                }
            }
        }

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mCurPos, 14));
    }
}
