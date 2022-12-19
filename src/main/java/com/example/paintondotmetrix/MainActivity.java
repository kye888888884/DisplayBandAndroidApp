package com.example.paintondotmetrix;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

@SuppressLint("MissingPermission")
public class MainActivity extends AppCompatActivity {
    public static Context context_main;

    private final static int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter btAdapter;
    private Set<BluetoothDevice> devices;
    private BluetoothDevice btDevice;
    private BluetoothSocket btSocket = null;
    private OutputStream outputStream = null;
    private InputStream inputStream = null;
    private Thread workerThread = null;
    private byte[] readBuffer;
    private int readBufferPosition;
    String[] array = {"0"};

    public boolean onBT = false;
    public byte[] sendByte = new byte[10];

    TextView textStatus;
    Button btnConnect, btnDraw;

    ArrayAdapter<String> btArrayAdapter;
    ArrayList<String> deviceAddressArray;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context_main = this;
        setContentView(R.layout.activity_main);

        // Get permission
        String[] permission_list = {
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.INTERNET,
        };

        ActivityCompat.requestPermissions(MainActivity.this, permission_list, 1);

        // variables
        textStatus = (TextView) findViewById(R.id.text_status);
        btnConnect = (Button) findViewById(R.id.btn_connect);
        btnDraw = (Button) findViewById(R.id.btn_draw);

        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!onBT) { // Connect
                    // Enable bluetooth
                    btAdapter = BluetoothAdapter.getDefaultAdapter();
                    if (btAdapter == null) {
                        Toast.makeText(getApplicationContext(), "Your device don't support bluetooth.", Toast.LENGTH_SHORT).show();
                    } else {
                        if (!btAdapter.isEnabled()) {
                            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                        }
                        selectBluetoothDevice();
                    }
                } else { // Disconnect
                    try {
                        BTSend.interrupt();
                        btSocket.close();
                        inputStream.close();
                        outputStream.close();
                        onBT = false;
                        btnConnect.setText("Connect");
                        textStatus.setText("");
                    } catch (Exception ignored) {}
                }
            }
        });
        btnDraw.setOnClickListener((v) -> {
                Intent intent = new Intent(getApplicationContext(), PaintActivity.class);
                startActivity(intent);
        });
    }

    int pairedDeviceCount;
    public void selectBluetoothDevice() {
        // Search paired bluetooth devices
        devices = btAdapter.getBondedDevices();
        // Get number of paired bluetooth devices
        pairedDeviceCount = devices.size();

        if (pairedDeviceCount == 0) {
            Toast.makeText(getApplicationContext(), "Please do pairing with any bluetooth device in setting.", Toast.LENGTH_SHORT).show();
        }
        else {
            // Create message box to select a device
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("List of paired bluetooth devices");

            // Get name and address of devices
            List<String> list = new ArrayList<>();
            for (BluetoothDevice btDevice: devices) {
                list.add(btDevice.getName());
            }
            list.add("Cancel");

            // Convert List to CharSequence
            final CharSequence[] charSequences = list.toArray(new CharSequence[list.size()]);
            list.toArray(new CharSequence[list.size()]);

            // Connect event listener to each items
            builder.setItems(charSequences, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    connectDevice(charSequences[i].toString());
                    textStatus.setText(charSequences[i].toString());
                }
            });

            // Don't close window when pressed back button
            builder.setCancelable(false);

            // Create dialog
            AlertDialog alertDialog = builder.create();
            alertDialog.show();
        }

    }

    public void connectDevice(String deviceName) {
        // Search paired bluetooth devices
        for (BluetoothDevice tempDevice: devices) {
            if (deviceName.equals(tempDevice.getName())) {
                btDevice = tempDevice;
                break;
            }
        }
        Toast.makeText(getApplicationContext(), "Successfully connected with " + btDevice.getName() + "!", Toast.LENGTH_SHORT).show();

        // Create UUID
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

        // Create socket to communicate with bluetooth device via Rfcomm channel
        try {
            btSocket = btDevice.createRfcommSocketToServiceRecord(uuid);
            btSocket.connect();

            outputStream = btSocket.getOutputStream();
            inputStream = btSocket.getInputStream();
            receiveData();

            onBT = true;
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void receiveData() {
        final Handler handler = new Handler();

        // Create buffer to receive data
        readBufferPosition = 0;
        readBuffer = new byte[1024];

        // Create thread to receive data
        workerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(!Thread.currentThread().isInterrupted()) {
                    try {
                        // Check receiving data
                        int byteAvailable = inputStream.available();
                        // When received data
                        if (byteAvailable > 0) {
                            // Read from input stream in bytes
                            byte[] bytes = new byte[byteAvailable];
                            inputStream.read(bytes);
                            // Read byte by byte from input stream bytes
                            for (int i = 0; i < byteAvailable; i++) {
                                byte tempByte = bytes[i];
                                // Receive based on newline character (one line)
                                if (tempByte == '\n') {
                                    // Copy readBuffer to encodedBytes
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    // Convert encoded byte array to string
                                    final String text = new String(encodedBytes, "UTF-8");
                                    readBufferPosition = 0;
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            // Receive data

                                        }
                                    });
                                }
                                // If it is not newline
                                else {
                                    readBuffer[readBufferPosition++] = tempByte;
                                }
                            }
                        }
                        btnConnect.setText("Disconnect");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                try {
                    // Receive every seconds
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        workerThread.start();
    }

    public void dotmetrix(boolean[][] paintArray) {
        try {
            sendData(paintArray);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    Thread BTSend = new Thread(new Runnable() {
        @Override
        public void run() {
            try {
                outputStream.write(sendByte);
            } catch (Exception e) {}
        }
    });

    public void sendData(boolean[][] paintArray) throws IOException {
        System.out.println(Arrays.deepToString(paintArray));
        byte[] bytes = new byte[10];
        bytes[0] = (byte) 0xA5;
        bytes[1] = (byte) 0x5a;
        for (int i = 0; i < 8; i++) {
            byte B = (byte) 0;
            for (int j = 0; j < 8; j++) {
                if (paintArray[i][j]) {
                    byte b = (byte) (1 << j);
                    B += b;
                }
            }
            bytes[i + 2] = B;
        }
        sendByte = bytes;
        BTSend.run();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Don't forget to unregister
        if (btSocket != null) {
            try {
                btSocket.close();
                inputStream.close();
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}