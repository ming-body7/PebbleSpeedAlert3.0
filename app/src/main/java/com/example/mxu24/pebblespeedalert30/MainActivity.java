package com.example.mxu24.pebblespeedalert30;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.PebbleKit.PebbleAckReceiver;
import com.getpebble.android.kit.PebbleKit.PebbleNackReceiver;
import com.getpebble.android.kit.util.PebbleDictionary;
import com.openxc.VehicleManager;
import com.openxc.measurements.Measurement;
import com.openxc.measurements.VehicleSpeed;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public class MainActivity extends ActionBarActivity {

    //openxc

    private VehicleManager mVehicleManager;
    private TextView mSpeedView;
    private int speedLimit = 100;
    //Log TAG
    private static final String TAG = "Pebble";

    private boolean connected = false;
    private boolean overSpeedTag = false;
    private int overSpeedCount = 1000;
    private int notOverSpeedCount = 0;
    //private final static UUID PEBBLE_APP_UUID = UUID.fromString("5570367d-0195-4d39-9bae-c369346005e5");
    private final static UUID PEBBLE_APP_UUID = UUID.fromString("10bc1ace-c87b-478a-8d64-a4cd3e4ca528");
    //UI relate
    private TextView introduction_text;
    private TextView message_text;
    private TextView speed_limit;
    private TextView show_limit;
    private Button set_speed_limit;
    private Button push_notification;
    private Button send_message;
    private Button open_app;
    private Button close_app;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        introduction_text = (TextView)findViewById(R.id.introduce);
        message_text = (TextView)findViewById(R.id.messageText);
        push_notification = (Button)findViewById(R.id.push_notification);
        send_message = (Button)findViewById(R.id.send_message);
        open_app = (Button)findViewById(R.id.openApp);
        close_app = (Button)findViewById(R.id.closeApp);
        set_speed_limit = (Button)findViewById(R.id.setSpeedLimit);
        mSpeedView = (TextView) findViewById(R.id.speedView);
        speed_limit = (TextView)findViewById(R.id.speedLimit);
        show_limit = (TextView)findViewById(R.id.showSpeedLimit);
        show_limit.setText("Limit:"+speedLimit);
        initView();
    }

    @Override
    public void onPause() {
        super.onPause();
        // When the activity goes into the background or exits, we want to make
        // sure to unbind from the service to avoid leaking memory
        if(mVehicleManager != null) {
            Log.i(TAG, "Unbinding from Vehicle Manager");
            // Remember to remove your listeners, in typical Android
            // fashion.
            mVehicleManager.removeListener(VehicleSpeed.class,
                    mSpeedListener);
            unbindService(mConnection);
            mVehicleManager = null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // When the activity starts up or returns from the background,
        // re-connect to the VehicleManager so we can receive updates.
        if(mVehicleManager == null) {
            Intent intent = new Intent(this, VehicleManager.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }

    }

    VehicleSpeed.Listener mSpeedListener = new VehicleSpeed.Listener() {
        @Override
        public void receive(Measurement measurement) {
            // When we receive a new EngineSpeed value from the car, we want to
            // update the UI to display the new value. First we cast the generic
            // Measurement back to the type we know it to be, an EngineSpeed.
            final VehicleSpeed speed = (VehicleSpeed) measurement;

            if(speed.getValue().doubleValue()>speedLimit){
                overSpeedCount++;
                //sendMessage("OverSpeed!");
                if(overSpeedCount>500){
                    sendAlertToPebble("OverSpeed!");
                    overSpeedCount = 0;
                    notOverSpeedCount = 0;
                }
            }
            else{
                notOverSpeedCount++;
                if(notOverSpeedCount>200){
                    overSpeedCount = 0;
                    notOverSpeedCount = 0;
                }
            }
            // In order to modify the UI, we have to make sure the code is
            // running on the "UI thread" - Google around for this, it's an
            // important concept in Android.
            MainActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    // Finally, we've got a new value and we're running on the
                    // UI thread - we set the text of the EngineSpeed view to
                    // the latest value
                    mSpeedView.setText(speed.getValue().toString());

                }
            });
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initView(){

        set_speed_limit.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                speedLimit = Integer.parseInt(speed_limit.getText().toString());
                show_limit.setText("Limit:"+speedLimit);

            }
        });
        push_notification.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                sendAlertToPebble(message_text.getText().toString());
                //message_text.setText("Input Here");
            }
        });

        send_message.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                sendMessage(message_text.getText().toString());

            }
        });
        open_app.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                openMyApp();

            }
        });

        close_app.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                closeMyApp();

            }
        });
        boolean connected = PebbleKit.isWatchConnected(getApplicationContext());
        Log.i(getLocalClassName(), "Pebble is " + (connected ? "connected" : "not connected"));
    }


    public void sendAlertToPebble(String alert) {
        final Intent i = new Intent("com.getpebble.action.SEND_NOTIFICATION");

        final Map data = new HashMap();
        data.put("title", "Test Message");
        data.put("body", alert);
        final JSONObject jsonData = new JSONObject(data);
        final String notificationData = new JSONArray().put(jsonData).toString();

        i.putExtra("messageType", "PEBBLE_ALERT");
        i.putExtra("sender", "MyAndroidApp");
        i.putExtra("notificationData", notificationData);

        Log.d(TAG, "About to send a modal alert to Pebble: " + notificationData);
        sendBroadcast(i);
    }

    public void detectingConnection(){
        PebbleKit.registerPebbleConnectedReceiver(getApplicationContext(), new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                Log.i(getLocalClassName(), "Pebble connected!");
            }

        });

        PebbleKit.registerPebbleDisconnectedReceiver(getApplicationContext(), new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                Log.i(getLocalClassName(), "Pebble disconnected!");
            }

        });
    }

    public void openMyApp(){
        //open my app identified by UUID
        PebbleKit.startAppOnPebble(getApplicationContext(), PEBBLE_APP_UUID);
    }

    public void closeMyApp(){
        // Closing my app
        PebbleKit.closeAppOnPebble(getApplicationContext(), PEBBLE_APP_UUID);
    }

    public void checkCompatibilityWithAppMessage(){

        //firmware 2.0+ is support
        if (PebbleKit.areAppMessagesSupported(getApplicationContext())) {
            Log.i(getLocalClassName(), "App Message is supported!");
        } else {
            Log.i(getLocalClassName(), "App Message is not supported");
        }
    }

    public void sendMessage(){
        PebbleDictionary data = new PebbleDictionary();

        // Add a key of 0, and a uint8_t (byte) of value 42.
        data.addUint8(0, (byte) 42);

        // Add a key of 1, and a string value.
        data.addString(1, "A string");

        PebbleKit.sendDataToPebble(getApplicationContext(), PEBBLE_APP_UUID, data);
    }

    public void sendMessage(String str){
        PebbleDictionary data = new PebbleDictionary();

        // Add a key of 0, and a uint8_t (byte) of value 42.
        data.addUint8(0, (byte) 42);

        //add string
        data.addString(1, str);

        PebbleKit.sendDataToPebble(getApplicationContext(), PEBBLE_APP_UUID, data);
    }
    public void registerMessage(){
        PebbleKit.registerReceivedAckHandler(getApplicationContext(), new PebbleAckReceiver(PEBBLE_APP_UUID) {

            @Override
            public void receiveAck(Context context, int transactionId) {
                Log.i(getLocalClassName(), "Received ack for transaction " + transactionId);
            }

        });

        PebbleKit.registerReceivedNackHandler(getApplicationContext(), new PebbleNackReceiver(PEBBLE_APP_UUID) {

            @Override
            public void receiveNack(Context context, int transactionId) {
                Log.i(getLocalClassName(), "Received nack for transaction " + transactionId);
            }

        });

        PebbleKit.registerReceivedDataHandler(this, new PebbleKit.PebbleDataReceiver(PEBBLE_APP_UUID) {

            @Override
            public void receiveData(final Context context, final int transactionId, final PebbleDictionary data) {
                Log.i(getLocalClassName(), "Received value=" + data.getUnsignedIntegerAsLong(0) + " for key: 0");

                PebbleKit.sendAckToPebble(getApplicationContext(), transactionId);
            }

        });
    }


    private ServiceConnection mConnection = new ServiceConnection() {
        // Called when the connection with the VehicleManager service is
        // established, i.e. bound.
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            Log.i(TAG, "Bound to VehicleManager");
            // When the VehicleManager starts up, we store a reference to it
            // here in "mVehicleManager" so we can call functions on it
            // elsewhere in our code.
            mVehicleManager = ((VehicleManager.VehicleBinder) service)
                    .getService();

            // We want to receive updates whenever the EngineSpeed changes. We
            // have an EngineSpeed.Listener (see above, mSpeedListener) and here
            // we request that the VehicleManager call its receive() method
            // whenever the EngineSpeed changes
            mVehicleManager.addListener(VehicleSpeed.class, mSpeedListener);
        }

        // Called when the connection with the service disconnects unexpectedly
        public void onServiceDisconnected(ComponentName className) {
            Log.w(TAG, "VehicleManager Service  disconnected unexpectedly");
            mVehicleManager = null;
        }
    };
}


