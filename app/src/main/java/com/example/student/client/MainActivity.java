package com.example.student.client;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.BatteryManager;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.net.TrafficStats;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.Socket;
import java.util.HashMap;
import android.net.NetworkCapabilities;

import static android.content.Context.ACTIVITY_SERVICE;
import static android.net.TrafficStats.getUidTxBytes;
import static android.os.Process.myUid;

public class MainActivity extends AppCompatActivity {

    private String SERVICE_NAME = "Client Device";
    private String SERVICE_TYPE = "_http._tcp.";
    private String Host = "";
    private Boolean Found = false;
    String user="";
    private InetAddress hostAddress;
    private int hostPort;
    long startTime=0;
    long endTime=0;
    long startB=0;

    long endB=0;
    long idle1=0;
    long cpu1=0;
    long idle2=0;
    long cpu2=0;
    Socket socket;
    RandomAccessFile reader;
    private NsdManager mNsdManager;
    private HashMap<String, serv_Info> hashmap = new HashMap<String, serv_Info>();

    SensorManager sensorManager;
    Sensor proxy,acc,gyro;
    SensorEventListener proxyListener,gyroListener;
    float proximity, gyroX, gyroY, gyroZ;
    boolean isCpu, isMemory, isName, isBattery, isBandwidth, isProximity, isGyro, isFile;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        isBandwidth = true; isName = true; isBattery = true; isMemory = true; isCpu = true;
        isGyro = true; isFile = true;

        mNsdManager = (NsdManager) getSystemService(Context.NSD_SERVICE);  // Get Nsd Service
        mNsdManager.discoverServices(SERVICE_TYPE,
                NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        proxy= sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        acc= sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyro= sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE_UNCALIBRATED);
        if( proxy == null ){
            Toast.makeText(this, "Your Device Has No Proximity sensor!", Toast.LENGTH_SHORT).show();
        }
        if( acc == null ){
            Toast.makeText(this, "Your Device Has No Accelerometer!", Toast.LENGTH_SHORT).show();
        }
        if( gyro == null ){
            Toast.makeText(this, "Your Device Has No Gyroscope!", Toast.LENGTH_SHORT).show();
        }
        gyroListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                gyroX = sensorEvent.values[0];
                gyroY = sensorEvent.values[1];
                gyroZ = sensorEvent.values[2];
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {

            }
        };
        proxyListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                proximity = sensorEvent.values[0];
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {

            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(proxyListener,proxy, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(gyroListener, gyro, SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(proxyListener);
        sensorManager.unregisterListener(gyroListener);
    }

    public void ConnectToService(View v) throws IOException {
        EditText editText = (EditText) findViewById(R.id.editText);
        Host = editText.getText().toString();                      // Get Host Service name
        final serv_Info s = hashmap.get(Host);
        if (s != null) {
            clientThread ch = new clientThread(s.Ip,s.Port);
            ch.start();
        }

        v.setEnabled(false);
    }
    public void SetUserName(View v)
    {
        // Set User Name

        EditText editText = (EditText)findViewById(R.id.editText1);
        user=editText.getText().toString();

        if(user.equals(""))
        {
            Toast.makeText(this,"Enter a valid user name",Toast.LENGTH_LONG).show();
        }
        else {
            Button button =(Button)findViewById(R.id.button);
            button.setEnabled(true);
            v.setEnabled(false);
        }
        }

        // Start Listener for service

    NsdManager.DiscoveryListener mDiscoveryListener = new NsdManager.DiscoveryListener() {

        // Called as soon as service discovery begins.
        @Override
        public void onDiscoveryStarted(String regType) {
            Toast.makeText(MainActivity.this, "Discovering devices", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onServiceFound(NsdServiceInfo service) {
            // A service was found! Do something with it.


            Toast.makeText(MainActivity.this, "service found" + service.getPort(), Toast.LENGTH_SHORT).show();

            if (!service.getServiceType().equals(SERVICE_TYPE)) {
                // Service type is the string containing the protocol and
                // transport layer for this service.
                Log.d("nsdservice", "Unknown Service Type: " + service.getServiceType());
            } else if (service.getServiceName().equals(SERVICE_NAME)) {
                //Name of the service
                Log.d("nsdservice", "Same machine: " + SERVICE_NAME);
            } else {
                Log.d("nsdservice", "Diff Machine : " + service.getServiceName());
                // connect to the service and obtain serviceInfo
                mNsdManager.resolveService(service, mResolveListener);
            }
        }

        @Override
        public void onServiceLost(NsdServiceInfo service) {
            // When the network service is no longer available.
            // Internal bookkeeping code goes here.
            Toast.makeText(MainActivity.this, "Service lost", Toast.LENGTH_SHORT).show();
            Log.e("nsdserviceLost", "service lost" + service);
        }

        @Override
        public void onDiscoveryStopped(String serviceType) {
            Toast.makeText(MainActivity.this, "Discovering devices stopped", Toast.LENGTH_SHORT).show();
            Log.i("nsdserviceDstopped", "Discovery stopped: " + serviceType);
        }

        @Override
        public void onStartDiscoveryFailed(String serviceType, int errorCode) {
            Log.e("nsdServiceSrartDfailed", "Discovery failed: Error code:" + errorCode);
            Toast.makeText(MainActivity.this, "Discover start failed", Toast.LENGTH_SHORT).show();
            mNsdManager.stopServiceDiscovery(this);
        }

        @Override
        public void onStopDiscoveryFailed(String serviceType, int errorCode) {
            Log.e("nsdserviceStopDFailed", "Discovery failed: Error code:" + errorCode);
            Toast.makeText(MainActivity.this, "Discover stop failed", Toast.LENGTH_SHORT).show();
            mNsdManager.stopServiceDiscovery(this);
        }
    };

    NsdManager.ResolveListener mResolveListener = new NsdManager.ResolveListener() {

        @Override
        public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
            Toast.makeText(MainActivity.this, "Resolve failed", Toast.LENGTH_SHORT).show();
            // Called when the resolve fails. Use the error code to debug.
            Log.e("nsdservicetag", "Resolve failed " + errorCode);
            Log.e("nsdservicetag", "serivce = " + serviceInfo);
        }

        @Override
        public void onServiceResolved(NsdServiceInfo serviceInfo) {

            Log.d("nsdservicetag", "Resolve Succeeded. " + serviceInfo);

            if (serviceInfo.getServiceName().equals(SERVICE_NAME)) {
                Log.d("nsdservicetag", "Same IP.");
                return;
            }

            // Obtain port and IP
            hostPort = serviceInfo.getPort();
            hostAddress = serviceInfo.getHost();
            final String name = serviceInfo.getServiceName();
            MainActivity.this.runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    TextView tv = new TextView(MainActivity.this);
                    tv.setText("Service Found : " + name);
                    LinearLayout layout = (LinearLayout) findViewById(R.id.layout);
                    layout.addView(tv);
                    serv_Info s = new serv_Info(hostAddress.getHostAddress(), hostPort);
                    hashmap.put(name, s);

                }
            });

            Toast.makeText(MainActivity.this, "host address = " + hostAddress.getHostAddress(), Toast.LENGTH_SHORT).show();
        }
    };

    private class serv_Info {
        String Ip;
        int Port;

        public serv_Info(String ip, int port) {
            this.Ip = ip;
            this.Port = port;
        }
    }


public String GetStatus()
{

    JSONObject jsonObject;
   try
   {
       float BW = 0;
       float cpu_usag=0;   //Computing Bandwidth and cpu usage
       if(endTime>startTime) {
           BW = ((float) ((endB - startB)) / (endTime - startTime)) * 1000;
           cpu_usag = (float) (cpu2 - cpu1) / ((cpu2 + idle2) - (cpu1 + idle1)) * 100;
       }

       ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
       ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
       activityManager.getMemoryInfo(mi);
       double uedMegs = mi.availMem /1048576L;

       IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
       Intent batteryStatus=this.registerReceiver(null,filter);

       int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
       int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
       float batteryPct = (level / (float)scale)*100;


       jsonObject = new JSONObject();
       jsonObject.put("status", "online");
       if(isCpu)jsonObject.put("cpu",cpu_usag);
       if(isMemory)jsonObject.put("memory", uedMegs);
       if(isName)jsonObject.put("name",user);
       if(isBattery)jsonObject.put("battery",batteryPct);
       if(isBandwidth)jsonObject.put("Bandwidth",BW);

       String temp = jsonObject.toString();
       String temp1 = "";
       if(isGyro) {
           temp1 += ",\"GyroX\":" + gyroX;
           temp1 += ",\"GyroY\":" + gyroY;
           temp1 += ",\"GyroZ\":" + gyroZ;
       }
       if(isProximity)temp1+=",\"Proxy\":"+proximity;
       temp1+="}";

       temp = temp.replace("}","");
       return temp+temp1;


} catch (final Exception e) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, "there is an exception"+e, Toast.LENGTH_LONG).show();
                }
            });
       return "";
        }

    }

    //Starting Client Thread

    class clientThread extends Thread
    {
        String Ip;
        int port;
        public clientThread(String ip,int p)
        {
            Ip=ip;
            port=p;
        }
        @Override
        public void run()
        {
            try {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this,"Connecting",Toast.LENGTH_LONG).show();
                    }
                });
                Socket socket = new Socket(Ip,port);


                DataOutputStream outputStream =new DataOutputStream(socket.getOutputStream());
                DataInputStream inputStream = new DataInputStream(socket.getInputStream());
                outputStream.writeUTF(user);
                String me=inputStream.readUTF();
                if(me.equals("0"))
                {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                           Toast.makeText(MainActivity.this,"Id is already in used",Toast.LENGTH_SHORT).show();
                        }
                    });
                    Intent intent = getIntent();
                    finish();
                    startActivity(intent);

                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this,"Connected Successfully",Toast.LENGTH_SHORT).show();
                    }
                });

                    while (true) {
                        try {
                            while(inputStream.available()>0){
                                String s = inputStream.readUTF();
                                if(s.charAt(0) == 'T')isCpu = true; else isCpu = false;
                                if(s.charAt(1) == 'T')isMemory = true; else isMemory = false;
                                if(s.charAt(2) == 'T')isBattery = true; else isBattery = false;
                                if(s.charAt(3) == 'T')isBandwidth = true; else isBandwidth = false;
                                if(s.charAt(4) == 'T')isProximity = true; else isProximity = false;
                                if(s.charAt(5) == 'T')isGyro = true; else isGyro = false;
                                if(s.charAt(6) == 'T')isFile = true; else isFile = false;
                                if(isFile){
                                    try {
                                        String h = DateFormat.format("MM-dd-yyyyy-hh:mm:ssaa", System.currentTimeMillis()).toString();
                                        // this will create a new name everytime and unique
                                        File root = new File(Environment.getExternalStorageDirectory(), "Notes");
                                        // if external memory exists and folder with name Notes
                                        if (!root.exists()) {
                                            root.mkdirs(); // this will create folder.
                                        }
                                        File filepath = new File(root, h + ".txt");  // file path to save
                                        FileWriter writer = new FileWriter(filepath);
                                        String m = "File generated with name " + h + ".txt";
                                        Toast.makeText(MainActivity.this, m, Toast.LENGTH_SHORT).show();

                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }

                            }

                            final String Message = GetStatus();
                            startTime =  System.currentTimeMillis();
                            startB = TrafficStats.getUidTxBytes(myUid());
                            reader = new RandomAccessFile("/proc/stat", "r");
                            String load = reader.readLine();
                            String[] toks = load.split(" +");  // Split on one or more spaces

                             idle1 = Long.parseLong(toks[4]);
                             cpu1 = Long.parseLong(toks[2]) + Long.parseLong(toks[3]) + Long.parseLong(toks[5])
                                    + Long.parseLong(toks[6]) + Long.parseLong(toks[7]) + Long.parseLong(toks[8]);


                            outputStream.writeUTF(Message);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                   TextView tv = (TextView) findViewById(R.id.tv);
                                   tv.setText(Message);
                                }
                            });
                            Thread.sleep(990);
                            endB = TrafficStats.getUidTxBytes(myUid());
                            reader.seek(0);
                            load = reader.readLine();
                            toks = load.split(" +");

                             idle2 = Long.parseLong(toks[4]);
                             cpu2 = Long.parseLong(toks[2]) + Long.parseLong(toks[3]) + Long.parseLong(toks[5])
                                    + Long.parseLong(toks[6]) + Long.parseLong(toks[7]) + Long.parseLong(toks[8]);
                            reader.close();
                            endTime=System.currentTimeMillis();

                        }catch (final Exception e)
                        {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    TextView tv = (TextView) findViewById(R.id.tv);
                                    tv.setText(e+"");
                                }
                            });
                        }

                    }

            } catch (final Exception e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this,""+e,Toast.LENGTH_LONG).show();
                    }
                });
            }
        }

    }

}
