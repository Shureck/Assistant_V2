package com.shureck.assistentv2;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

public class Settings extends AppCompatActivity {

    private static final String TAG = "bluetooth2";
    private ConnectedThread mConnectedThread;
    //Handler h;
    public SharedPreferences appSharedPrefs;
    public SharedPreferences.Editor prefsEditor;
    final String SAVED_TEXT = "saved_text";
    final String SAVED_ADR = "saved_adr";

    private static final int REQUEST_ENABLE_BT = 1;
    final int RECIEVE_MESSAGE = 1;        // Статус для Handler
    private BluetoothAdapter btAdapter = null;
    private BluetoothAdapter bluetoothAdapter = null;
    private BluetoothSocket btSocket = null;
    private StringBuilder sb = new StringBuilder();
    public ListView countriesList;
    public ArrayList<String> mArrayAdapter;
    public ArrayList<BluetoothDevice> tmpBtChecker = new ArrayList<>();
    public Switch mSwitch;


    public Button btn;
    public Button btn2;
    public EditText editText;
    public EditText postEdit;
    public EditText radiusEdit;

    public AlertDialog dialog;
    public LinearLayout set_menu, set_voice;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);

        im_init();

        appSharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        prefsEditor = appSharedPrefs.edit();
        boolean isSp = appSharedPrefs.getBoolean("isSpeak", false);

        btn = (Button) findViewById(R.id.btn_phone);
        btn2 = (Button) findViewById(R.id.btn_accept);
        editText = (EditText) findViewById(R.id.editTextPhone1);
        postEdit = (EditText) findViewById(R.id.postAdr);
        radiusEdit = (EditText) findViewById(R.id.editRadius);

        set_menu = (LinearLayout) findViewById(R.id.list_bt);
        set_voice = (LinearLayout) findViewById(R.id.lin_ask);
        btAdapter = BluetoothAdapter.getDefaultAdapter();
//        mArrayAdapter.add("Hello, Shureck!");
//        mArrayAdapter.notifyDataSetChanged();
//        countriesList.setAdapter(mArrayAdapter);
        mArrayAdapter = new ArrayList<>();
        mSwitch = (Switch) findViewById(R.id.isSpeak);
        mSwitch.setChecked(isSp);

        String ss = loadText(SAVED_TEXT);
        if(ss != ""){
            editText.setHint(ss);
        }
        ss = loadText(SAVED_ADR);
        if(ss != ""){
            postEdit.setHint(ss.substring(0,ss.indexOf('|')));
            radiusEdit.setHint(ss.substring(ss.indexOf('|')+1,ss.length()));
        }

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String s = editText.getText().toString();
                String reg = "^((8|\\+7)[\\- ]?)?(\\(?\\d{3}\\)?[\\- ]?)?[\\d\\- ]{7,10}$";
                if (s != null && Pattern.matches(reg,s)){
                    editText.setText("");
                    saveText(SAVED_TEXT,s);
                    editText.setHint(loadText(SAVED_TEXT));
                    App.sendLocalBroadcastMessage("Номер",s);
                }
                else{
                    editText.setText("");
                    editText.setHint("Неверный формат");
                }
            }
        });

        btn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String sA = postEdit.getText().toString();
                String sR = radiusEdit.getText().toString();
                saveText(SAVED_ADR,sA+"|"+sR);
                App.sendLocalBroadcastMessage("Address",sA+"|"+sR);
                String ss = loadText(SAVED_ADR);
                if(ss != ""){
                    postEdit.setHint(ss.substring(0,ss.indexOf('|')));
                    radiusEdit.setHint(ss.substring(ss.indexOf('|')+1,ss.length()));
                }
            }
        });

        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String s = intent.getStringExtra("Result");
                Toast.makeText(Settings.this, "Адрес "+ s + " установлен", Toast.LENGTH_LONG).show();
            }
        }, new IntentFilter("Address_back"));

        mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    System.out.println("++++++++++++++++++++++++++++++++++++");
                    prefsEditor.putBoolean("isSpeak", true);
                    prefsEditor.commit();
                    App.sendLocalBroadcastMessage("VS_call","isSpeak"+true);
                } else {
                    System.out.println("------------------------------------");
                    prefsEditor.putBoolean("isSpeak", false);
                    prefsEditor.commit();
                    App.sendLocalBroadcastMessage("VS_call","isSpeak"+false);
                }
            }
        });

        set_voice.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View v) {

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

    void saveText(String tag, String text) {
        prefsEditor.putString(tag, text);
        prefsEditor.commit();
    }

    String loadText(String tag) {
        String savedText = appSharedPrefs.getString(tag, "");
        return savedText;
    }

    private void im_init(){
        ImageView im_main = (ImageView) findViewById(R.id.imageView);
        im_main.setClickable(true);
        im_main.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Settings.this, MainActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.in_right, R.anim.out_right);
            }
        });

        ImageView im_nfc = (ImageView) findViewById(R.id.imageView3);
        im_nfc.setClickable(true);
        im_nfc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Settings.this, Nfc.class);
                startActivity(intent);
                overridePendingTransition(R.anim.in_right, R.anim.out_right);
            }
        });

        ImageView im_range = (ImageView) findViewById(R.id.imageView5);
        im_range.setClickable(true);
        im_range.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Settings.this, Rangefinder.class);
                startActivity(intent);
                overridePendingTransition(R.anim.in_right, R.anim.out_right);
            }
        });

        ImageView im_gps = (ImageView) findViewById(R.id.imageView2);
        im_gps.setClickable(true);
        im_gps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Settings.this, Gps.class);
                startActivity(intent);
                overridePendingTransition(R.anim.in_right, R.anim.out_right);
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
