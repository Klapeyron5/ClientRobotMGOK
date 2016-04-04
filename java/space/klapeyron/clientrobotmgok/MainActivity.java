package space.klapeyron.clientrobotmgok;

import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Set;


public class MainActivity extends Activity {
    public static final String TAG = "TAG";

    public String clientState;
    public static final String CLIENT_NO_CONNECTION = "No connection with robot.";
    public static final String CLIENT_SEARCHING = "Searching devices...";
    public static final String CLIENT_FINISHED_SEARCHING = "Finished searching devices.";
    public static final String CLIENT_CONNECTING = "Connecting to the robot.";
    public static final String CLIENT_ROBOT_READY = "The robot is ready.";
    public static final String CLIENT_ROBOT_EXECUTING_TASK = "Executing task.";
    public static final String CLIENT_ROBOT_REACHED_TARGET_NO_CONNECTION = "Robot reached the target. No connection with robot.";


    private String activityState;
    private static final String ACTIVITY_STATE_MAIN_XML = "activity_main.xml";
    private static final String ACTIVITY_STATE_INTERACTIVE_MAP = "interactive map";

    private static final int REQUEST_ENABLE_BT = 0; //>=0 for run onActivityResult from startActivityForResult
    private static final String UUID = "e91521df-92b9-47bf-96d5-c52ee838f6f6";
    private static final String ROBOT_SERVER_NAME = "RobotServerMGOK";
    private static final String ROBOT_SERVER_ADRESS = "78:24:AF:64:3E:54";

    BluetoothAdapter bluetoothAdapter; //локальный БТ адаптер
    private Set<BluetoothDevice> pairedDevices; //спаренные девайсы
    private BluetoothDevice selectedServer; //сервер, выбранный из списка найденных устройств
    private BluetoothSocket serverSocket; //канал соединения с сервером
    private ArrayList<BluetoothDevice> discoveredDevices; //обнаруженные устройства
    private ArrayAdapter<String> btDevicesAdapterList; //to show

    MainActivity link = this;
    InteractiveMapView interactiveMapView;

    TextView textViewLog;
    EditText editTextX;
    EditText editTextY;

    public int currentX;
    public int currentY;
    public String absolutePath = null;

    //***BLE***
    ArrayList<String> beacons = new ArrayList<String>();
    ArrayAdapter<String> adapter;

    private Handler scanHandler = new Handler();
    private int scan_interval_ms = 500;
    private boolean isScanning = false;

    //Данные выборки для сглаживания значений мощности сигнала и расстояния
    int[][] rssiData = new int[10][2];
    double[][] distanceData = new double[10][2];
    double d = 0;
    int f = 0; // флажок

    //Доступные маячки
    ArrayList<String> MAC = new ArrayList<>();
    ArrayList<Double> distance = new ArrayList<>();
    //***BLE***(end)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //TODO
        discoveredDevices = new ArrayList<BluetoothDevice>();
        btDevicesAdapterList = new ArrayAdapter<String>(this,R.layout.support_simple_spinner_dropdown_item);
        initConstructor();

        Log.i(TAG, "OnCreate");
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        //***BLE***
        scanHandler.post(scanRunnable);
        //***BLE***(end)

        registerReceiver(discoveryStartedReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND)); // Don't forget to unregister during onDestroy
        registerReceiver(discoveryFinishedReceiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
    }

    //***BLE***
    private Runnable scanRunnable = new Runnable()
    {
        @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
        @Override
        public void run() {
            if (isScanning)
            {
                if (bluetoothAdapter != null)
                {
                    bluetoothAdapter.stopLeScan(leScanCallback);
                }
            }
            else
            {
                if (bluetoothAdapter != null)
                {
                    bluetoothAdapter.startLeScan(leScanCallback);
                }
            }

            isScanning = !isScanning;

            scanHandler.postDelayed(this, scan_interval_ms);
        }
    };

    public BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback()
    {
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord)
        {
            int startByte = 2;
            boolean patternFound = false;
            while (startByte <= 5)
            {
                if (    ((int) scanRecord[startByte + 2] & 0xff) == 0x02 &&
                        ((int) scanRecord[startByte + 3] & 0xff) == 0x15)
                {
                    patternFound = true;
                    break;
                }
                startByte++;
            }

            if (patternFound)
            {
                byte[] uuidBytes = new byte[16];
                System.arraycopy(scanRecord, startByte + 4, uuidBytes, 0, 16);

                byte txPower = scanRecord[29];

                int i = 0;

                String macBuf = device.toString();
                f = 0;
                if (MAC.size() != 0) {
                    for (i = 0; i < MAC.size(); i++) {
                        if (macBuf.equals(MAC.get(i))) {
                            optimizeRssiData(i,rssi);
                            d = distance(Integer.parseInt(Byte.toString(txPower)) - rssiData[i][0]);
                            optimizeDistanceData(i);
                            distanceData[i][0] = Math.round(distanceData[i][0]*100.00)/100.00;
                            distance.set(i, distanceData[i][0]);
                            f = 1;
                        }
                    }

                    if (f == 0) {
                        optimizeRssiData(i,rssi);
                        d = distance(Integer.parseInt(Byte.toString(txPower)) - rssiData[i][0]);
                        optimizeDistanceData(i);
                        distanceData[i][0] = Math.round(distanceData[i][0]*100.00)/100.00;
                        MAC.add(macBuf);
                        distance.add(distanceData[i][0]);
                    }
                }
                else {
                    MAC.add(macBuf);
                    distance.add(distanceData[0][0]);
                }

                //Запись в beacons
                if (MAC.size() != 0) {
                    beacons.clear();
                    for (i = 0; i < MAC.size(); i++) {
                        beacons.add("MAC: " + MAC.get(i) + "      distance:  " + distance.get(i).toString() + "m");
                    }
                }
            }
        }
    };
    double distance(int power){
        if (power < 5) return (0.0399 * power + 0.7951);
        else return (Math.pow(10,((power - 1.6) / 20)));
    }

    void optimizeRssiData(int i, int rssi){
        rssiData[i][1] = rssiData[i][0];
        rssiData[i][0] = rssi;

        while ((rssiData[i][0] - rssiData[i][1]) > 2)
            rssiData[i][0] = (rssiData[i][0] + rssiData[i][1])/2;
    }

    void optimizeDistanceData(int i){
        distanceData[i][1] = distanceData[i][0];
        distanceData[i][0] = d;

        if (distanceData[i][0] < 0) distanceData[i][0] = 0;
        while ((distanceData[i][0] - distanceData[i][1]) > 1.5)
            distanceData[i][0] = (distanceData[i][0] + distanceData[i][1])/2;
    }

    //***BLE***(end)

    private  void initConstructor() {
        currentX = -1;
        currentY = -1;
        setContentView(R.layout.activity_main);
        activityState = ACTIVITY_STATE_MAIN_XML;

        Button buttonStopRobot = (Button) findViewById(R.id.buttonStopRobot);
        buttonStopRobot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "нажал на стоп");
                if (clientState == CLIENT_ROBOT_READY) {
                    setClientState(CLIENT_NO_CONNECTION);
                    sendMessage("stop",0,0,"stop");
                } else
                    if (clientState == CLIENT_ROBOT_EXECUTING_TASK) {
                        setClientState(CLIENT_ROBOT_REACHED_TARGET_NO_CONNECTION);
                        sendMessage("stop",0,0,"stop");
                    }
            }
        });

        Button buttonDiscoverDevices = (Button) findViewById(R.id.buttonDiscoverDevices);
        buttonDiscoverDevices.setOnClickListener(new View.OnClickListener() {
            @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
            @Override
            public void onClick(View v) {
                Log.i(TAG, "нажал на поиск БТ");
                //***BLE***
                if (isScanning == true) bluetoothAdapter.stopLeScan(leScanCallback);
                //***BLE***(end)
                if (clientState == CLIENT_SEARCHING)
                    bluetoothAdapter.cancelDiscovery();
                setClientState(CLIENT_SEARCHING);
                btDevicesAdapterList.clear();
                bluetoothAdapter.startDiscovery();
            }
        });

        Button buttonMap = (Button) findViewById(R.id.buttonMap);
        buttonMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                interactiveMapView = new InteractiveMapView(link);
                setContentView(interactiveMapView);
                activityState = ACTIVITY_STATE_INTERACTIVE_MAP;
            }
        });

        textViewLog = (TextView) findViewById(R.id.textViewLog);
        setClientState(clientState);
        editTextX = (EditText) findViewById(R.id.editTextX);
        editTextY = (EditText) findViewById(R.id.editTextY);
        editTextX.setText(Integer.toString(currentX));
        editTextY.setText(Integer.toString(currentY));
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //replaces the default 'Back' button action
        if(keyCode == KeyEvent.KEYCODE_BACK)
        {
            switch(activityState) {
                case ACTIVITY_STATE_MAIN_XML:
                    break;
                case ACTIVITY_STATE_INTERACTIVE_MAP:
                    initConstructor();
                    break;
            }
        }
        return false;
    }

    public void sendTaskXYMessage(View v) {
        Log.i(TAG, "нажал на отправку сообщения");
        int X = (Integer.parseInt(editTextX.getText().toString()));
        int Y = (Integer.parseInt(editTextY.getText().toString()));
        currentX = X;
        currentY = Y;
        sendMessage("task",X,Y,"ride to me");
    }

    public void setClientState(String str) {
        clientState = str;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textViewLog.setText(clientState);
            }
        });
    }





    //ресиверы для организации поиска БТ девайсов
    private final BroadcastReceiver discoveryStartedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i("TAG", "ищем");
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                Log.i("TAG","нашел");
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                discoveredDevices.add(device);
                // Add the name and address to an array adapter to show in a ListView
                btDevicesAdapterList.add(device.getName() + "\n" + device.getAddress());

                //TODO
                if ((device.getName().equals(ROBOT_SERVER_NAME))&&(device.getAddress().equals(ROBOT_SERVER_ADRESS))) {
                    if (clientState != CLIENT_CONNECTING) {
                        selectedServer = device;
                        Log.i(TAG, "SelectedServer: " + selectedServer.getName());
                        bluetoothAdapter.cancelDiscovery();
                        connectMethod();
                    }
                }
            }
        }
    };
    private final BroadcastReceiver discoveryFinishedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i("TAG", "конец поиска");
            if (clientState == CLIENT_SEARCHING)
                setClientState(CLIENT_FINISHED_SEARCHING);
        }
    };

    private void connectMethod() {
        setClientState(CLIENT_CONNECTING);
        try {
            if ((selectedServer.getName().equals(ROBOT_SERVER_NAME))&&(selectedServer.getAddress().equals(ROBOT_SERVER_ADRESS))) {
                serverSocket = selectedServer.createRfcommSocketToServiceRecord(java.util.UUID.fromString(UUID));
                serverSocket.connect(); //send connection request to server, and server send back the confirmation, what server is ready
                ReadMessage readMessage = new ReadMessage();
                readMessage.start();
            } else {
                setClientState(CLIENT_NO_CONNECTION);
            }
        } catch (IOException e) {
            setClientState(CLIENT_NO_CONNECTION);
        }
    }

    /**
     * Set the task for robot by sending message to the server.
     * @param key "task": set task for robot
     *            "stop": cancel robot executing task
     * @param X target robot X coordinate
     * @param Y target robot Y coordinate
     * @param comment comment for robot target*/
    public void sendMessage(String key, int X, int Y, String comment) {
        String str = new String("/"+key+"/"+Integer.toString(X)+"/"+Integer.toString(Y)+"/");
        byte[] b = str.getBytes();
        try {
            (serverSocket.getOutputStream()).write(b);
            Log.i(TAG,new String(b));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class ReadMessage extends Thread {
        @Override
        public void run() {
            readMessage();
        }
    }

    private void readMessage() {
        InputStream inputStream = null;
        try {
            inputStream = serverSocket.getInputStream();
        } catch (IOException e) {}

        byte[] buffer = new byte[1024];  // buffer store for the stream
        int bytes; // bytes returned from read()
        try {
            bytes = inputStream.read(buffer);
        } catch (IOException e) {}
        String str = null;
        try {
            try {
                str = new String(buffer, "UTF-8");
            } catch (UnsupportedEncodingException e) {
            }
            Log.i(TAG, "reading:  " + str);
            String[] a = str.split("/");
            Log.i(TAG, "!" + a[0] + "!" + a[1] + "!" + a[2] + "!" + a[3]);
            String key = a[1];
            String X = a[2];
            String Y = a[3];
            int fX = Integer.parseInt(X.toString());
            int fY = Integer.parseInt(Y.toString());


            if (key.equals("ready")) {
                setClientState(CLIENT_ROBOT_READY);
                absolutePath = null;
            }
            if (key.equals("path")) {
                currentX = fX;
                currentY = fY;
                interactiveMapView.startX = fX;
                interactiveMapView.startY = fY;
                Log.i(TAG, "PATH: " + a[4]);
                absolutePath = a[4];
                if (activityState == ACTIVITY_STATE_INTERACTIVE_MAP)
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            interactiveMapView.drawConstructor(currentX, currentY);
                        }
                    });
            }
            if (key.equals("currentXY")) {
                currentX = fX;
                currentY = fY;
                //TODO draw on map
                if (activityState == ACTIVITY_STATE_INTERACTIVE_MAP)
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            interactiveMapView.drawConstructor(currentX, currentY);
                        }
                    });
                setClientState(CLIENT_ROBOT_EXECUTING_TASK);
                for(int i=4;i<a.length;i++) {
                    if (a[i].equals("target")) {
                        try {
                            if (serverSocket.isConnected())
                                serverSocket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        setClientState(CLIENT_ROBOT_REACHED_TARGET_NO_CONNECTION);
                    }
                }
            }
            //если робот не закончил движение
            if (!key.equals("target")) {
                ReadMessage readMessage = new ReadMessage();
                readMessage.start();
            }
            if (key.equals("target")) {
                try {
                    if (serverSocket.isConnected())
                        serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                setClientState(CLIENT_ROBOT_REACHED_TARGET_NO_CONNECTION);
            }
        } catch (ArrayIndexOutOfBoundsException e) {} catch (IndexOutOfBoundsException e) {}
    }
}
