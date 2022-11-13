package com.example.vexrobotcontroller;

import android.app.AlertDialog;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import com.example.vexrobotcontroller.RobotControl.RobotControl;

import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.vexrobotcontroller.databinding.ActivityMainBinding;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;

    private boolean motionButtonDown;
    private boolean clawClose;
    private boolean clawOpen;
    private boolean armLow;
    private boolean armRise;
    private boolean ligthsON;
    private float lastX, lastY, lastZ;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private float mAccel; // acceleration apart from gravity
    private float mAccelCurrent; // current acceleration including gravity
    private float mAccelLast; // last acceleration including gravity
    private String warningMessage;

    InetAddress globalLocal;
    // main loop here
    Handler handler = new Handler();

    public SensorEventListener mSensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            lastX = event.values[0];
            lastY = event.values[1];
            lastZ = event.values[2];

            if (motionButtonDown == true) {
                myRobotControl.rightX = convertAccelValues(lastX);
                myRobotControl.rightY = convertAccelValues(lastY);
                myRobotControl.leftY = convertAccelValues(lastZ);
                if (lastX < 0) // get direction of tilt on X axis
                    myRobotControl.btnA = 1;
                else
                    myRobotControl.btnA = 0;

                if (lastY < 0) // get direction of tilt on Y axis
                    myRobotControl.btnB = 1;
                else
                    myRobotControl.btnB = 0;
                //myRobotControl.calcCRC16();

            }
            else {
                myRobotControl.rightX = 0;
                myRobotControl.rightY = 0;
                //myRobotControl.calcCRC16();
            }
            //send_TcpIPmessage(myRobotControl.createMessage(),local);

        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }
    };

    public RobotControl myRobotControl = new RobotControl();

    public InetAddress udpport(String ip_address) {

        String messageStr = "Hello Android!";
        int server_port = 22211;
        String ipAddressString;
        InetAddress remote;

        InetAddress local; //= InetAddress.getAllByName("127.0.0.0");

        try {
            WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            int ipAddress = wifiInfo.getIpAddress();
            ipAddressString = String.format("%d.%d.%d.%d",
                    (ipAddress & 0xff),
                    (ipAddress >> 8 & 0xff),
                    (ipAddress >> 16 & 0xff),
                    (ipAddress >> 24 & 0xff));

            remote = InetAddress.getByName("192.168.1.22");
            //remote = InetAddress.getByName("10.0.0.1");

            local = InetAddress.getLocalHost();
            Button robotPresent = (Button) findViewById(R.id.RobotPresent);
            robotPresent.setBackgroundColor(Color.parseColor("#00FF00"));
            robotPresent.setText("Robot Found");
            return remote;
        }
        catch(Exception ex)
        {
            Toast.makeText(MainActivity.this, "Could Not Find Robot", Toast.LENGTH_LONG).show();
            TextView warningMessage = (TextView)findViewById(R.id.textview_first);
            warningMessage.setText("Could Not Find Robot");
            Button robotPresent = (Button) findViewById(R.id.RobotPresent);
            robotPresent.setBackgroundColor(Color.parseColor("#FF0000"));
            robotPresent.setText("Could Not Find Robot");
        }

        return null;
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(mSensorListener, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);


        //setContentView(R.layout.activity_main);

        final InetAddress local = udpport("192.168.1.22");
        globalLocal = local;
        final Button button = (Button) findViewById(R.id.test_button);
        final Button openClaw = (Button) findViewById(R.id.clawOpen);
        final Button closeClaw = (Button) findViewById(R.id.clawClose);
        final Button armRaise = (Button) findViewById(R.id.raiseArm);
        final Button armLower = (Button) findViewById(R.id.lowerArm);
        final Button robotPresent = (Button) findViewById(R.id.RobotPresent);
        final Switch ligthson = (Switch) findViewById(R.id.lightsOn);
        final AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(this);

        ligthson.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                if ((ligthson.getText()).equals("Lights ON")) {
                    ligthsON = true;
                    myRobotControl.btnStart = 1;
                }
                else { // lights off
                    ligthsON = false;
                    myRobotControl.btnStart = 0;
                }
            }
        }
        );


        armLower.setOnTouchListener(new View.OnTouchListener() {
            @Override

            public boolean onTouch(View v, MotionEvent event) {

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // PRESSED
                        armLow = true;
                        return true; // if you want to handle the touch event
                    case MotionEvent.ACTION_UP:
                        // RELEASED
                        armLow = false;
                        return true; // if you want to handle the touch event

                }
                return false;
            }
        });

        armRaise.setOnTouchListener(new View.OnTouchListener() {
            @Override

            public boolean onTouch(View v, MotionEvent event) {

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // PRESSED
                        armRise = true;
                        return true; // if you want to handle the touch event
                    case MotionEvent.ACTION_UP:
                        // RELEASED
                        armRise = false;
                        return true; // if you want to handle the touch event

                }
                return false;
            }
        });

        closeClaw.setOnTouchListener(new View.OnTouchListener() {
            @Override

            public boolean onTouch(View v, MotionEvent event) {

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // PRESSED
                        clawClose = true;
                        return true; // if you want to handle the touch event
                    case MotionEvent.ACTION_UP:
                        // RELEASED
                        clawClose = false;
                        return true; // if you want to handle the touch event

                }
                return false;
            }
        });

        openClaw.setOnTouchListener(new View.OnTouchListener() {
            @Override

            public boolean onTouch(View v, MotionEvent event) {

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // PRESSED
                        clawOpen = true;
                        return true; // if you want to handle the touch event
                    case MotionEvent.ACTION_UP:
                        // RELEASED
                        clawOpen = false;
                        return true; // if you want to handle the touch event

                }
                return false;
            }
        });

        button.setOnTouchListener(new View.OnTouchListener() {
            @Override

            public boolean onTouch(View v, MotionEvent event) {

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // PRESSED
                        motionButtonDown = true;
                        return true; // if you want to handle the touch event
                    case MotionEvent.ACTION_UP:
                        // RELEASED
                        motionButtonDown = false;
                        return true; // if you want to handle the touch event

                }
                return false;
            }
        });

        // post periodic task to execute
        handler.post(periodicTasks);

    }

    public void send_TcpIPmessage(byte[] message, InetAddress local){
        TextView warningMessage = (TextView)findViewById(R.id.textview_first);
        TextView robotRunTime = (TextView) findViewById(R.id.robotRunTime);
        TextView robotPresent = (Button) findViewById(R.id.RobotPresent);

        DatagramPacket p = new DatagramPacket(message, 29, local, 22211);
        try { // sending and receiving packets to/from Robot
            DatagramSocket s = new DatagramSocket();
            s.send(p);
            s.receive(p);
            int seconds;

            if (message[4] < 0)
                seconds = -message[4];
            else
                seconds = message[4];

            seconds = message[3] << 8 | seconds;

            robotRunTime.setText("Connection Time = " + seconds + " " + (message[3]<<8 | message[4]));
            warningMessage.setText("Last Command Succeeded");
        }
        catch (Exception ex) {

            warningMessage.setText("Last Command Failed");
            warningMessage.setTextColor(Color.parseColor("#FF0000"));

        }
    }

    public byte convertAccelValues(float xyz){
        // 45 degree maximum
        float maxAccel = (float) 4.9;
        float convertedVal = xyz/maxAccel * 127;

        // absolute value
        if  (convertedVal < 0)
            convertedVal = -1 * convertedVal;

        if (convertedVal > (float) 127)
            return 127;
        else if (convertedVal < (float) -128)
            return -128;
        else
            return (byte) convertedVal;
    }

    private Runnable periodicTasks = new Runnable() {
        @Override
        public void run() {
            // Do something here on the main thread
            if (clawClose == true)
                myRobotControl.btnX = 1;
            else
                myRobotControl.btnX = 0;

            if (clawOpen == true)
                myRobotControl.btnY = 1;
            else
                myRobotControl.btnY = 0;

            if (armRise == true)
                myRobotControl.dPadUp = 1;
            else
                myRobotControl.dPadUp = 0;

            if (armLow == true)
                myRobotControl.dPadDown = 1;
            else
                myRobotControl.dPadDown = 0;

            myRobotControl.calcCRC16();
            send_TcpIPmessage(myRobotControl.createMessage(),globalLocal);
            // Repeat this runnable code again every 2 seconds
            handler.postDelayed(periodicTasks, 100);
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

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }
}