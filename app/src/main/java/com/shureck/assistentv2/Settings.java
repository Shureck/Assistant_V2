package com.shureck.assistentv2;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class Settings extends AppCompatActivity {

    private static final String TAG = "bluetooth2";
    private ConnectedThread mConnectedThread;
    //Handler h;

    private static final int REQUEST_ENABLE_BT = 1;
    final int RECIEVE_MESSAGE = 1;        // Статус для Handler
    private BluetoothAdapter btAdapter = null;
    private BluetoothAdapter bluetoothAdapter = null;
    private BluetoothSocket btSocket = null;
    private StringBuilder sb = new StringBuilder();
    public ListView countriesList;
    public ArrayList<String> mArrayAdapter;
    public ArrayList<BluetoothDevice> tmpBtChecker = new ArrayList<>();


    public AlertDialog dialog;
    public LinearLayout set_menu, set_voice;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);

        im_init();
        set_menu = (LinearLayout) findViewById(R.id.list_bt);
        set_voice = (LinearLayout) findViewById(R.id.lin_ask);
        btAdapter = BluetoothAdapter.getDefaultAdapter();
//        mArrayAdapter.add("Hello, Shureck!");
//        mArrayAdapter.notifyDataSetChanged();
//        countriesList.setAdapter(mArrayAdapter);
        mArrayAdapter = new ArrayList<>();

        /*
        h = new Handler() {
            public void handleMessage(android.os.Message msg) {
                switch (msg.what) {
                    case RECIEVE_MESSAGE:                                                   // если приняли сообщение в Handler
                        byte[] readBuf = (byte[]) msg.obj;
                        String strIncom = new String(readBuf, 0, msg.arg1);
                        sb.append(strIncom);                                                // формируем строку
                        int endOfLineIndex = sb.indexOf("\r\n");                            // определяем символы конца строки
                        if (endOfLineIndex > 0) {                                            // если встречаем конец строки,
                            String sbprint = sb.substring(0, endOfLineIndex);               // то извлекаем строку
                            sb.delete(0, sb.length());                                      // и очищаем sb
                            Toast.makeText(Settings.this,"Ответ от Arduino: " + sbprint, Toast.LENGTH_LONG).show();             // обновляем TextView
                        }
                        //Log.d(TAG, "...Строка:"+ sb.toString() +  "Байт:" + msg.arg1 + "...");
                        break;
                }
            };
        };
        */


        set_voice.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View v) {

                 mConnectedThread.write("Lol 1");
                 mConnectedThread.write(String.valueOf(new char[]{'0','1','!'}));

             }
        });

        set_menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mArrayAdapter.clear();

                Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
                for(BluetoothDevice bt : pairedDevices) {
                    if (!tmpBtChecker.contains(bt)) {
                        tmpBtChecker.add(bt);
                    }
                    mArrayAdapter.add(bt.getName()+"\n"+bt.getAddress());
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(Settings.this);
                builder.setTitle("Выберети устройство")
                        .setItems(mArrayAdapter.toArray(new String[0]), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                //bt_connect(which);
                                Intent intent = new Intent(Settings.this, Bt_Service.class).putExtra("address", tmpBtChecker.get(which).getAddress());
                                startService(intent);

                            }
                        })
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // Закрываем окно
                                dialog.cancel();
                            }
                        });
                builder.create();
                builder.show();
            }
        });

    }


    public void bt_connect(int position){

        Log.d(TAG, "...onResume - попытка соединения...");

        // Set up a pointer to the remote node using it's address.
        BluetoothDevice device = btAdapter.getRemoteDevice(tmpBtChecker.get(position).getAddress());

        // Two things are needed to make a connection:
        //   A MAC address, which we got above.
        //   A Service ID or UUID.  In this case we are using the
        //     UUID for SPP.
        try {
            btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
        } catch (IOException e) {
            //errorExit("Fatal Error", "In onResume() and socket create failed: " + e.getMessage() + ".");
        }

        // Discovery is resource intensive.  Make sure it isn't going on
        // when you attempt to connect and pass your message.
        btAdapter.cancelDiscovery();

        //while (!btSocket.isConnected()) {
        // Establish the connection.  This will block until it connects.
        Log.d(TAG, "...Соединяемся...");
        try {
            btSocket.connect();
            Log.d(TAG, "...Соединение установлено и готово к передачи данных...");
        } catch (IOException e) {
            try {
                btSocket.close();
            } catch (IOException e2) {
                //errorExit("Fatal Error", "In onResume() and unable to close socket during connection failure" + e2.getMessage() + ".");
            }
        }
        //}
        // Create a data stream so we can talk to server.
        Log.d(TAG, "...Создание Socket...");

        mConnectedThread = new ConnectedThread(btSocket);
        mConnectedThread.start();

        Log.d(TAG, "...In onCreate... last " + btSocket.isConnected() + " " + mConnectedThread.isAlive());

        if(btSocket.isConnected()) {
            Toast.makeText(Settings.this, "Соединение установлено", Toast.LENGTH_LONG).show();
        }
        else{
            Toast.makeText(Settings.this, "Ошибка подключения", Toast.LENGTH_LONG).show();
        }
    }


    private void im_init(){
        ImageView im_main = (ImageView) findViewById(R.id.imageView);
        im_main.setClickable(true);
        im_main.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Settings.this, MainActivity.class);
                startActivity(intent);
            }
        });

        ImageView im_nfc = (ImageView) findViewById(R.id.imageView3);
        im_nfc.setClickable(true);
        im_nfc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Settings.this, Nfc.class);
                startActivity(intent);
            }
        });

        ImageView im_range = (ImageView) findViewById(R.id.imageView5);
        im_range.setClickable(true);
        im_range.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Settings.this, Rangefinder.class);
                startActivity(intent);
            }
        });

        ImageView im_gps = (ImageView) findViewById(R.id.imageView2);
        im_gps.setClickable(true);
        im_gps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Settings.this, Gps.class);
                startActivity(intent);
            }
        });
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[256];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);        // Получаем кол-во байт и само собщение в байтовый массив "buffer"
                    //h.obtainMessage(RECIEVE_MESSAGE, bytes, -1, buffer).sendToTarget();     // Отправляем в очередь сообщений Handler
                } catch (IOException e) {
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(String message) {
            Log.d(TAG, "...Данные для отправки: " + message + "...");
            byte[] msgBuffer = message.getBytes();
            try {
                mmOutStream.write(msgBuffer);
            } catch (IOException e) {
                Log.d(TAG, "...Ошибка отправки данных: " + e.getMessage() + "...");
            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }
}
