package com.holux.iot.mqttdemo;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.holux.iot.mqttdemo.db.ItemDevice;
import com.holux.iot.mqttdemo.db.ItemHR;
import com.holux.iot.mqttdemo.db.ItemPos;
import com.holux.iot.mqttdemo.db.LocalDBHelper;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import static android.content.ContentValues.TAG;

public class MainActivity extends Activity {
    static final public String DB_NAME = "HLX-IoT";
    static final public String SP_NAME = "HLX-IoT";
    static final private String KEY_ICON = "icon";
    static final private String KEY_MSG = "msg";
    static final private String KEY_CLIENT_ID = "client_id";

    static final private int MSG_TYPE_COMMENT = 1;
    static final private int MSG_TYPE_SEND = 2;
    static final private int MSG_TYPE_DOWNLOAD = 3;
    static final private int MSG_TYPE_POS = 4;
    static final private int MSG_TYPE_HR = 5;

    private Button buttonConnect, buttonSubscribe, buttonDeviceID;
    private ImageButton buttonPublish;
    private EditText editMessage;
    private ListView listMsg ;
    private ImageView imageSOS;
    private List<Map<String,Object>> listItem;
    private SimpleAdapter listAdapter ;
    SharedPreferences prefSetting;
    private ObjectAnimator animSosAlarm;

    MqttAndroidClient clientMQTT = null;
    String clientId = null;
    String deviceId = null;
    String deviceName = null;

    Handler mainHandler;

    boolean isAutoSend = false;
    int countAutoSend = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences prefSetting = getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        clientId = prefSetting.getString(KEY_CLIENT_ID, null);
        if(clientId == null) {
            clientId = MqttClient.generateClientId();
            prefSetting.edit().putString(KEY_CLIENT_ID, clientId).apply();
        }

        mainHandler = new Handler();

        findViews();
        addListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(!isConnected())
            connectBroker();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(isConnected())
            disconnectBroker();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            AlertDialog.Builder dlgBuilder = new AlertDialog.Builder(MainActivity.this);
            dlgBuilder.setMessage(R.string.exit_app);
            dlgBuilder.setIcon(android.R.drawable.ic_dialog_alert);
            dlgBuilder.setCancelable(false);
            dlgBuilder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                    finish();
                    System.exit(0);
                }
            });

            dlgBuilder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });

            dlgBuilder.show();

            return true;
        }
        else
            return super.onKeyDown(keyCode, event);
    }

    private void findViews() {
        buttonConnect = findViewById(R.id.buttonConnect);
        buttonPublish = findViewById(R.id.buttonPublish);
        buttonSubscribe = findViewById(R.id.buttonSubscribe);
        buttonDeviceID = findViewById(R.id.buttonDeviceID);
        editMessage = findViewById(R.id.editMessage);
        listMsg = findViewById( R.id.listComingMsg );
        listItem = new ArrayList<Map<String,Object>>();
        listAdapter = new SimpleAdapter(this, listItem, R.layout.message_row,
                new String[] {KEY_ICON, KEY_MSG},
                new int[] {R.id.iconMessage, R.id.textMessage}) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);

                ImageView icon = view.findViewById(R.id.iconMessage);
                TextView msg = view.findViewById(R.id.textMessage);

                String szMsg = msg.getText().toString();

                if(szMsg.startsWith("+"))
                    icon.setImageResource(R.drawable.send_icon);
                else if(szMsg.startsWith("-"))
                    icon.setImageResource(R.drawable.download_icon);
                else
                    icon.setImageResource(R.drawable.comments_icon);

                return super.getView(position, convertView, parent);
            }
        };

        listMsg.setAdapter(listAdapter);

        imageSOS = findViewById(R.id.imageSOS);
        animSosAlarm = ObjectAnimator.ofFloat(imageSOS, "alpha", 0.4f, 1f);
        animSosAlarm.setDuration(600);
        animSosAlarm.setRepeatCount(5);
        animSosAlarm.setRepeatMode(ObjectAnimator.REVERSE);
        animSosAlarm.setInterpolator(new LinearInterpolator());
    }

    private void addListeners() {
        buttonConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!isConnected())
                    connectBroker();
                else
                    disconnectBroker();
            }
        });

        buttonPublish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isConnected()) {
                    if(deviceId != null) {
                        String szMsg = editMessage.getText().toString();
                        switch(szMsg) {
                            case "$Start":
                                if(!isAutoSend) {
                                    isAutoSend = true;
                                    countAutoSend = 0;
                                    mainHandler.post(taskAutoSend);
                                }
                                break;
                            case "$Stop":
                                isAutoSend = false;
                                break;
                            default:
                                publish("HLX-IoT/dev/" + deviceId, "$002," + szMsg);
                                showMessage(MSG_TYPE_SEND, "To " + deviceId + ": " + szMsg);
                        }
                    }
                    else {
                        showMessage("No Device Selected!");
                    }
                }
                else {
                    showMessage("No Connection!");
                }

                editMessage.clearFocus();
            }
        });

        buttonSubscribe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isConnected())
                    subscribe("HLX-IoT/pub", 1);
                else
                    showMessage("No Connection!");
            }
        });

        buttonDeviceID.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //get devices registered
                LocalDBHelper dbHelper = new LocalDBHelper(MainActivity.this, DB_NAME);
                final String[] deviceIDs = dbHelper.queryAllDeviceID();
                dbHelper.close();
                if(deviceIDs != null && deviceIDs.length > 0) {
                    AlertDialog.Builder b = new AlertDialog.Builder(MainActivity.this);
                    b.setTitle("Device ID");
                    b.setItems(deviceIDs, new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();

                            deviceId = deviceIDs[which];
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    buttonDeviceID.setText(deviceId);
                                }
                            });
                        }

                    });

                    b.show();
                }
                else {
                    showMessage(MSG_TYPE_COMMENT, "No registered device!");
                }
            }
        });

        editMessage.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if(b) {
                    // set keypad position
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if(imm != null) {
                        imm.showSoftInput(view, 0);
                    }
                }
                else {
                    // hide soft keypad
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                    }
                }
            }
        });

        listMsg.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                ImageView icon = view.findViewById(R.id.iconMessage);
                TextView msg = view.findViewById(R.id.textMessage);

                Map<String, Object> item = (Map<String, Object>)adapterView.getItemAtPosition(i);
                if(item.get(KEY_ICON).equals(R.drawable.icon_info_shoe)) {
                    String szMsg = (String)item.get(KEY_MSG);
                    String [] columns = szMsg.split(":");
                    if(columns.length > 1) {
                        String [] pos = columns[1].split(",");
                        Intent it = new Intent(MainActivity.this, MapsActivity.class);
                        it.putExtra(MapsActivity.EXTRA_DEVICE_NAME, columns[0]);
                        it.putExtra(MapsActivity.EXTRA_LAT, Double.valueOf(pos[0]));
                        it.putExtra(MapsActivity.EXTRA_LON, Double.valueOf(pos[1]));
                        startActivity(it);
                    }
                }
                else if(item.get(KEY_ICON).equals(R.drawable.icon_info_pulse)) {
                    String szMsg = (String)item.get(KEY_MSG);
                    String [] columns = szMsg.split(":");
                    if(columns.length > 1) {
                        Intent it = new Intent(MainActivity.this, HrActivity.class);
                        it.putExtra(HrActivity.EXTRA_DEVICE_ID, columns[0]);
                        startActivity(it);
                    }
                }
            }
        });
    }

    private void connectBroker() {
        if(isConnected()) {
            //showMessage("Connected already!");
            return;
        }

        showMessage(MSG_TYPE_COMMENT, "Connecting...");

        if(clientMQTT == null) {
            clientMQTT = new MqttAndroidClient(this.getApplicationContext(), "tcp://iot.eclipse.org:1883", clientId);
            clientMQTT.setCallback(new MqttCallbackExtended() {

                @Override
                public void connectComplete(boolean reconnect, String serverURI) {
                    subscribe("HLX-IoT/pub", 0);
                    if(reconnect)
                        showMessage(MSG_TYPE_COMMENT, "Reconnect OK!");
                }

                @Override
                public void connectionLost(Throwable cause) {
                    showMessage(MSG_TYPE_COMMENT, "Lost Connection!");
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    if(message.isRetained())    //skip retained message
                        return;

                    if (message.toString().startsWith("$000,")) {
                        String deviceID = message.toString().substring(5);
                        showMessage(MSG_TYPE_DOWNLOAD, "Registration: " + deviceID);
                        registerDevice(deviceID);
                    }
                    else if (message.toString().startsWith("$001,")) {
                        String szMsg = message.toString().substring(5);
                        String [] columns = szMsg.split(",");
                        showMessage(MSG_TYPE_DOWNLOAD, columns[0] + ": " + columns[1]);
                    }
                    else if (message.toString().startsWith("$002,")) {
                        String szMsg = message.toString().substring(5);
                        String [] columns = szMsg.split(",");
                        showMessage(MSG_TYPE_DOWNLOAD, columns[0] + ": " + columns[1]);
                    }
                    else if (message.toString().startsWith("$003,")) {
                        String szMsg = message.toString().substring(5);
                        String [] columns = szMsg.split(",");
                        if(columns.length == 2 && isDeviceRegistered(columns[0])) {
                            showMessage(MSG_TYPE_HR, columns[0] + ": " + columns[1] + " bpm");
                            logHeartRate(columns[0], Integer.valueOf(columns[1]));
                            Intent intent = new Intent();
                            intent.setAction(HrActivity.ACTION_UPDATE);
                            sendBroadcast(intent);
                        }
                    }
                    else if (message.toString().startsWith("$004,")) {
                        String szMsg = message.toString().substring(5);
                        String [] columns = szMsg.split(",");
                        if(columns.length > 2 && isDeviceRegistered(columns[0])) {
                            showMessage(MSG_TYPE_POS, columns[0] + ": " + columns[1] + "," + columns[2]);
                            if(columns.length == 4) {
                                SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.US);
                                formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
                                Date date = formatter.parse(columns[3]);
                                logPosition(columns[0], Double.valueOf(columns[1]), Double.valueOf(columns[2]), date);
                            }
                            else
                                logPosition(columns[0], Double.valueOf(columns[1]), Double.valueOf(columns[2]));

                            showSosAlarm();
                        }
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {

                }
            });
        }

        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setCleanSession(true);
        mqttConnectOptions.setConnectionTimeout(30);
        mqttConnectOptions.setKeepAliveInterval(60);
        mqttConnectOptions.setMqttVersion(4);
        mqttConnectOptions.setAutomaticReconnect(true);
        //mqttConnectOptions.setUserName("fet/fet");
        //mqttConnectOptions.setPassword("fet@1234".toCharArray());
        try {
            IMqttToken token = clientMQTT.connect(mqttConnectOptions);
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // We are connected
                    showMessage(MSG_TYPE_COMMENT, "Connect OK!");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    // Something went wrong e.g. connection timeout or firewall problems
                    showMessage(MSG_TYPE_COMMENT, "Connect Failed!");
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void disconnectBroker() {
        if(clientMQTT != null) {
            try {
                IMqttToken mqttToken = clientMQTT.disconnect();
                mqttToken.setActionCallback(new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken iMqttToken) {
                        showMessage("Disconnect OK");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                buttonConnect.setText("Connect");
                            }
                        });
                    }

                    @Override
                    public void onFailure(IMqttToken iMqttToken, Throwable throwable) {
                        showMessage("Disconnect Failed");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                buttonConnect.setText("Connect");
                            }
                        });
                    }
                });
            } catch (MqttException e) {
                Log.e(TAG, "Disconnect Exception", e);
            }
        }
    }

    private void publish(String topic, String msg) {
        byte[] encodedPayload = new byte[0];
        try {
            encodedPayload = msg.getBytes("UTF-8");
            MqttMessage message = new MqttMessage(encodedPayload);
            clientMQTT.publish(topic, message);
        } catch (UnsupportedEncodingException | MqttException e) {
            e.printStackTrace();
        }


    }

    private void subscribe(String topic, int qos) {
        try {
            IMqttToken subToken = clientMQTT.subscribe(topic, qos);
            subToken.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // The message was published
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken,
                                      Throwable exception) {
                    // The subscription could not be performed, maybe the user was not
                    // authorized to subscribe on the specified topic e.g. using wildcards

                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void showMessage(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showMessage(final int type, final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Map<String, Object> item = new HashMap<String, Object>();

                item.put(KEY_MSG, msg);

                switch (type) {
                    case MSG_TYPE_COMMENT:
                        item.put(KEY_ICON, R.drawable.comments_icon);
                        break;
                    case MSG_TYPE_SEND:
                        item.put(KEY_ICON, R.drawable.send_icon);
                        break;
                    case MSG_TYPE_DOWNLOAD:
                        item.put(KEY_ICON, R.drawable.download_icon);
                        break;
                    case MSG_TYPE_POS:
                        item.put(KEY_ICON, R.drawable.icon_info_shoe);
                        break;
                    case MSG_TYPE_HR:
                        item.put(KEY_ICON, R.drawable.icon_info_pulse);
                        break;
                }

                listItem.add(0, item);
                listAdapter.notifyDataSetChanged();
            }
        });
    }

    private boolean isConnected() {
        return (clientMQTT != null && clientMQTT.isConnected());
    }

    private void registerDevice(String id) {
        LocalDBHelper dbHelper = new LocalDBHelper(MainActivity.this, DB_NAME);
        ItemDevice item = new ItemDevice();
        item.setDeviceID(id);
        dbHelper.insert(item);
        dbHelper.close();
    }

    private void logHeartRate(String id, int bpm) {
        long time = Calendar.getInstance().getTimeInMillis();
        ItemHR item = new ItemHR();
        item.setDeviceID(id);
        item.setTime(time);
        item.setBpm(bpm);
        LocalDBHelper dbHelper = new LocalDBHelper(MainActivity.this, DB_NAME);
        dbHelper.insert(item);
        dbHelper.close();
    }

    private void logPosition(String id, double lat, double lon) {
        long time = Calendar.getInstance().getTimeInMillis();
        ItemPos item = new ItemPos();
        item.setDeviceID(id);
        item.setTime(time);
        item.setLat(lat);
        item.setLon(lon);
        LocalDBHelper dbHelper = new LocalDBHelper(MainActivity.this, DB_NAME);
        dbHelper.insert(item);
        dbHelper.close();
    }

    private void logPosition(String id, double lat, double lon, Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        long time = calendar.getTimeInMillis();
        ItemPos item = new ItemPos();
        item.setDeviceID(id);
        item.setTime(time);
        item.setLat(lat);
        item.setLon(lon);
        LocalDBHelper dbHelper = new LocalDBHelper(MainActivity.this, DB_NAME);
        dbHelper.insert(item);
        dbHelper.close();
    }

    private boolean isDeviceRegistered(String id) {
        LocalDBHelper dbHelper = new LocalDBHelper(MainActivity.this, DB_NAME);
        boolean result = dbHelper.isDeviceIdExist(id);
        dbHelper.close();
        return result;
    }

    private Runnable taskAutoSend = new Runnable() {
        @Override
        public void run() {
            if(isAutoSend) {
                String szMsg = "" + countAutoSend++;
                publish("HLX-IoT/dev/" + deviceId, "$002," + szMsg);
                showMessage(MSG_TYPE_SEND, "To " + deviceId + ": " + szMsg);
                mainHandler.postDelayed(this, 60000);
            }
        }
    };

    private void showSosAlarm() {
        if(imageSOS.getVisibility() == View.INVISIBLE) {
            imageSOS.setVisibility(View.VISIBLE);
            animSosAlarm.start();
            mainHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    imageSOS.setVisibility(View.INVISIBLE);
                    imageSOS.setAlpha(1f);
                }
            }, 3000);
        }
    }
}
