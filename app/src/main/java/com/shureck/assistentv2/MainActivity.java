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
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    NfcAdapter nfcAdapter;
    public AlertDialog dialog;
    public RecyclerView recyclerView;
    public Button msButton;
    public DataAdapter ad_answer;
    public EditText editText;
    public List<RowType> phones = new ArrayList<>();
    public Gson gson = new Gson();
    public Date currentDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_test);

        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        msButton = (Button) findViewById(R.id.button);
        editText = (EditText) findViewById(R.id.EditText);
        msButton.setOnClickListener(addMessage);

        currentDate = new Date();

        startService(new Intent(MainActivity.this, VoiceService.class));
        //setVolumeControlStream(AudioManager.STREAM_MUSIC);

        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String s = intent.getStringExtra("Result");
                if(s.equals("stop_rec")){
                    msButton.setAlpha(1.0f);
                }
                else {
                    Type type = new TypeToken<String[]>() {
                    }.getType();
                    String[] serv_answ = gson.fromJson(s, type);
                    if (s.length() > 0) {
                        DateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                        String timeText = timeFormat.format(currentDate);

                        if (serv_answ[0] != null) {
                            phones.add(new Messages(serv_answ[0], timeText, R.mipmap.ic_launcher, true));
                        }
                        if (serv_answ[1] != null) {
                            phones.add(new Answers(serv_answ[1], timeText, R.drawable.micro_sh, true));
                        }
                        ad_answer = new DataAdapter(MainActivity.this, phones);
                        recyclerView.setAdapter(ad_answer);
                        editText.setText("");
                        recyclerView.scrollToPosition(phones.size() - 1);
                    }
                }
            }
        },new IntentFilter("rec_res"));
        im_init();

        setInitialData();
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
//            Animation animation = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.blink);
//            msButton.startAnimation(animation);

            msButton.setAlpha(0.4f);

            if(editText.getText().toString().length()>0) {
                App.sendLocalBroadcastMessage("VS_call","No_Speech"+editText.getText().toString());
//                phones.add(new Messages(editText.getText().toString(), "18:50", R.mipmap.ic_launcher, true));
//                phones.add(new Answers(robo_answer(editText.getText().toString()), "18:50", R.drawable.micro_sh, true));
//                ad_answer = new DataAdapter(MainActivity.this, phones);
//                recyclerView.setAdapter(ad_answer);
//                editText.setText("");
//                recyclerView.scrollToPosition(phones.size()-1);
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
                overridePendingTransition(R.anim.slidein, R.anim.slideout);
            }
        });

        ImageView im_nfc = (ImageView) findViewById(R.id.imageView3);
        im_nfc.setClickable(true);
        im_nfc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, Nfc.class);
                startActivity(intent);
                overridePendingTransition(R.anim.in_right, R.anim.out_right);
            }
        });

        ImageView im_range = (ImageView) findViewById(R.id.imageView5);
        im_range.setClickable(true);
        im_range.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, Rangefinder.class);
                startActivity(intent);
                overridePendingTransition(R.anim.in_right, R.anim.out_right);
            }
        });

        ImageView im_gps = (ImageView) findViewById(R.id.imageView2);
        im_gps.setClickable(true);
        im_gps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, Gps.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slidein, R.anim.slideout);
            }
        });
    }

    private void setInitialData(){
        DateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String timeText = timeFormat.format(currentDate);

        phones.add(new Answers ("Здравствуйте, доступны следующие команды: \n\n" +
                "1) Включи/выключи фонарик \n" +
                "2) Где я? - определит местоположение", timeText, R.drawable.micro_sh, true));
        ad_answer = new DataAdapter(MainActivity.this, phones);
        recyclerView.setAdapter(ad_answer);
    }
}
