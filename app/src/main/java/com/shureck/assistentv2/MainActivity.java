package com.shureck.assistentv2;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.nfc.tech.NfcF;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    NfcAdapter nfcAdapter;
    public AlertDialog dialog;
    public RecyclerView recyclerView;
    public Button msButton;
    public DataAdapter ad_answer;
    public EditText editText;
    public List<RowType> phones = new ArrayList<>();
    public Gson gson = new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_test);

        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        msButton = (Button) findViewById(R.id.button);
        editText = (EditText) findViewById(R.id.EditText);
        msButton.setOnClickListener(addMessage);

        startService(new Intent(MainActivity.this, VoiceService.class));
        //setVolumeControlStream(AudioManager.STREAM_MUSIC);

        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String s = intent.getStringExtra("Result");
                Type type = new TypeToken<String[]>() {
                }.getType();
                String[] serv_answ = gson.fromJson(s, type);
                if(s.length()>0) {
                    phones.add(new Messages(serv_answ[0], "18:50", R.mipmap.ic_launcher, true));
                    phones.add(new Answers(serv_answ[1], "18:50", R.drawable.micro_sh, true));
                    ad_answer = new DataAdapter(MainActivity.this, phones);
                    recyclerView.setAdapter(ad_answer);
                    editText.setText("");
                    recyclerView.scrollToPosition(phones.size()-1);
                }
            }
        },new IntentFilter("rec_res"));
        im_init();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(new Intent(MainActivity.this, VoiceService.class));
        stopService(new Intent(MainActivity.this, Bt_Service.class));
    }

    View.OnClickListener addMessage = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(editText.getText().toString().length()>0) {
                phones.add(new Messages(editText.getText().toString(), "18:50", R.mipmap.ic_launcher, true));
                phones.add(new Answers(robo_answer(editText.getText().toString()), "18:50", R.drawable.micro_sh, true));
                ad_answer = new DataAdapter(MainActivity.this, phones);
                recyclerView.setAdapter(ad_answer);
                editText.setText("");
                recyclerView.scrollToPosition(phones.size()-1);
            }
            else{
                App.sendLocalBroadcastMessage("VS_call","start_rec");
            }
        }
    };

    private String robo_answer(String ask){
        return "Извините?";
    }

    private void im_init(){
        ImageView im_settings = (ImageView) findViewById(R.id.imageView4);
        im_settings.setClickable(true);
        im_settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, Settings.class);
                startActivity(intent);
            }
        });

        ImageView im_nfc = (ImageView) findViewById(R.id.imageView3);
        im_nfc.setClickable(true);
        im_nfc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, Nfc.class);
                startActivity(intent);
            }
        });

        ImageView im_range = (ImageView) findViewById(R.id.imageView5);
        im_range.setClickable(true);
        im_range.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, Rangefinder.class);
                startActivity(intent);
            }
        });

        ImageView im_gps = (ImageView) findViewById(R.id.imageView2);
        im_gps.setClickable(true);
        im_gps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, Gps.class);
                startActivity(intent);
            }
        });
    }


    private void setInitialData(){
        phones.add(new Messages ("Включи дальномер", "18:50", R.mipmap.ic_launcher, true));
        phones.add(new Answers ("Дальномер включен", "18:50", R.drawable.micro_sh, false));
        phones.add(new Messages ("Где я?", "18:51", R.mipmap.ic_launcher, true));
        phones.add(new Answers ("пр-т Вернадского 78", "18:51", R.drawable.micro_sh,false));
        phones.add(new Answers ("Что ты вообще тут забыл?", "18:51", R.drawable.micro_sh,false));
        phones.add(new Answers ("Иди домой, мальчик", "18:51", R.drawable.micro_sh,false));
    }
}
