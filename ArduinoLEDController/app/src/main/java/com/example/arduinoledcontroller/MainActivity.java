package com.example.arduinoledcontroller;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "SendToFireStore";
    public static Handler handler;
    public static BluetoothSocket mmSocket;
    public static ConnectedThread connectedThread;
    public static CreateConnectThread createConnectThread;

    // The following variables used in bluetooth handler to identify message status
    private final static int CONNECTION_STATUS = 1;
    private final static int MESSAGE_READ = 2;

    private TextView textViewData;
    private View downloadButtonView;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final CollectionReference cmdsRef = db.collection("BluetoothCommunication");
    //private DocumentReference cmdRef = db.document("BluetoothCommunication/LED OnOff Commands");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Instantiate UI
        final TextView bluetoothStatus = findViewById(R.id.textBluetoothStatus);
        Button buttonConnect = findViewById(R.id.buttonConnect);
        Button buttonDisconnect = findViewById(R.id.buttonDisconnect);
        final TextView ledStatus = findViewById(R.id.textLedStatus);
        Button buttonOn = findViewById(R.id.buttonOn);
        Button buttonOff = findViewById(R.id.buttonOff);
        //Button buttonBlink = findViewById(R.id.buttonBlink);
        textViewData = findViewById(R.id.text_view_data);

        // Code for the "Connect" button
        buttonConnect.setOnClickListener(view -> {
            // This is the code to move to another screen
            Intent intent = new Intent(MainActivity.this, SelectDeviceActivity.class);
            startActivity(intent);
        });

        // Get Device Address from SelectDeviceActivity.java to create connection
        String deviceAddress = getIntent().getStringExtra("deviceAddress");
        if (deviceAddress != null) {
            bluetoothStatus.setText("Connecting...");
            /*
            This is the most important piece of code.
            When "deviceAddress" is found, the code will call the create connection thread
            to create bluetooth connection to the selected device using the device Address
             */
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            createConnectThread = new CreateConnectThread(bluetoothAdapter, deviceAddress);
            createConnectThread.start();
        }

        /*
        Second most important piece of Code.
        This handler is used to update the UI whenever a Thread produces a new output
        and passes through the values to Main Thread
         */
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    // If the updates come from the Thread to Create Connection
                    case CONNECTION_STATUS:
                        switch (msg.arg1) {
                            case 1:
                                bluetoothStatus.setText("Bluetooth Connected");
                                break;
                            case -1:
                                bluetoothStatus.setText("Connection Failed");
                                break;
                        }
                        break;

                    // If the updates come from the Thread for Data Exchange
                    case MESSAGE_READ:
                        String statusText = msg.obj.toString().replace("/n", "");
                        ledStatus.setText(statusText);
                        break;
                }
            }
        };

        // Code for the disconnect button
        buttonDisconnect.setOnClickListener(view -> {
            if (createConnectThread != null) {
                createConnectThread.cancel();
                bluetoothStatus.setText("Bluetooth is Disconnected");
            }
        });

        // Code to turn ON LED
        buttonOn.setOnClickListener(view -> {
            String androidCmd = "1";
            ArduinoCommand arduinoCommand = new ArduinoCommand(androidCmd, "LED is turned ON");
            cmdsRef.add(arduinoCommand);
            connectedThread.write(androidCmd);
        });

        // Code to turn OFF LED
        buttonOff.setOnClickListener(view -> {
            String androidCmd = "0";

            ArduinoCommand arduinoCommand = new ArduinoCommand(androidCmd, "LED is turned OFF");
            cmdsRef.add(arduinoCommand);
            connectedThread.write(androidCmd);
        });

        // Code to make the LED blinking
        /*buttonBlink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String androidCmd = "d";
                connectedThread.write(androidCmd);
            }
        });*/

        View downloadButton = findViewById(R.id.downloadButton);
        downloadButton.setOnClickListener(v -> {
            downloadButtonView = v;
            downloadExcel(v);
        });
    }

    /*@Override
    protected void onStart() {
        super.onStart();
        cmdsRef.addSnapshotListener(this, new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException error) {

                if(error != null) {
                    return;
                }

                String data = "";

                for(QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots){

                    ArduinoCommand arduinoCommand = documentSnapshot.toObject(ArduinoCommand.class);
                    arduinoCommand.setDocumentId(documentSnapshot.getId());

                    String command = arduinoCommand.getCommand();
                    String description = arduinoCommand.getCommandDescription();

                    data += command + " - " + description + "\n\n";
                }

                textViewData.setText(data);
            }
        });
    }*/

    public void downloadExcel(View v){

        //Check if the required permissions are granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    1);
        } else{
            Log.d(TAG, "Is permission granted ? = " + (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED));
            ConvertFirestoreDataToJSON convertFirestoreDataToJSON = new ConvertFirestoreDataToJSON();
            convertFirestoreDataToJSON.downloadExcel(v, MainActivity.this);
        }
        Toast.makeText(this, "File successfully created!", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Toast.makeText(this, "Permission denied. Cannot save file.", Toast.LENGTH_SHORT).show();
            }
            else {
                downloadExcel(downloadButtonView);
            }
        }
    }

    public void loadCommands(View v){
        cmdsRef.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    String data = "";

                    for(QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots){

                        ArduinoCommand arduinoCommand = documentSnapshot.toObject(ArduinoCommand.class);
                        arduinoCommand.setDocumentId(documentSnapshot.getId());

                        String command = arduinoCommand.getCommand();
                        String description = arduinoCommand.getCommandDescription();

                        data += command + " - " + description + "\n\n";
                    }

                    textViewData.setText(data);
                });
    }

    /* ============================ Thread to Create Connection ================================= */
    public class CreateConnectThread extends Thread {

        public CreateConnectThread(BluetoothAdapter bluetoothAdapter, String address) {
            // Opening connection socket with the Arduino board
            BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(address);
            BluetoothSocket tmp = null;
            if (ActivityCompat.checkSelfPermission(MainActivity.this,
                    Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Log.d("SuzieTag","BLUETOOTH_CONNECT not granted so far !!!");
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                        2);
                //return;
            }
            UUID uuid = bluetoothDevice.getUuids()[0].getUuid();
            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                tmp = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(uuid);
            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        @RequiresApi(api = Build.VERSION_CODES.S)
        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Log.d("SuzieTag","BLUETOOTH_CONNECT not granted so far !!!");
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                        2);
                //return;
            }
            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                Log.d("SuzieTag","BLUETOOTH_SCAN not granted so far !!!");
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.BLUETOOTH_SCAN},
                        2);
                //return;
            }
            bluetoothAdapter.cancelDiscovery();
            try {
                // Connect to the Arduino board through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
                handler.obtainMessage(CONNECTION_STATUS, 1, -1).sendToTarget();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    mmSocket.close();
                    handler.obtainMessage(CONNECTION_STATUS, -1, -1).sendToTarget();
                } catch (IOException closeException) { }
                return;
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            // Calling for the Thread for Data Exchange (see below)
            connectedThread = new ConnectedThread(mmSocket);
            connectedThread.run();
        }

        // Closes the client socket and causes the thread to finish.
        // Disconnect from Arduino board
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

    /* =============================== Thread for Data Exchange ================================= */
    public static class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        // Getting Input and Output Stream when connected to Arduino Board
        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        // Read message from Arduino device and send it to handler in the Main Thread
        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes = 0; // bytes returned from read()
            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    buffer[bytes] = (byte) mmInStream.read();
                    String arduinoMsg;

                    // Parsing the incoming data stream
                    if (buffer[bytes] == '\n'){
                        arduinoMsg = new String(buffer,0,bytes);
                        Log.e("Arduino Message",arduinoMsg);
                        handler.obtainMessage(MESSAGE_READ,arduinoMsg).sendToTarget();
                        bytes = 0;
                    } else {
                        bytes++;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }

        // Send command to Arduino Board
        // This method must be called from Main Thread
        public void write(String input) {
            byte[] bytes = input.getBytes(); //converts entered String into bytes
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) { }
        }
    }
}