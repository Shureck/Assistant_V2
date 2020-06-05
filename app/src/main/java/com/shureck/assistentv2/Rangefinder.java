package com.shureck.assistentv2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.nio.charset.StandardCharsets;
import java.sql.SQLOutput;

import static java.nio.charset.StandardCharsets.UTF_16;
import static java.nio.charset.StandardCharsets.UTF_8;

public class Rangefinder extends AppCompatActivity{

    public TextView dist_text, seekText1, seekText2, seekText3, seekText4;
    public SeekBar seekBar1, seekBar2, seekBar3, seekBar4;
    public Button accebt_bt;
    public Spinner spinner;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rangefinder);

        dist_text = (TextView) findViewById(R.id.textView2);

        seekBar1 = (SeekBar) findViewById(R.id.seekBar);
        seekBar2 = (SeekBar) findViewById(R.id.seekBar2);
        seekBar3 = (SeekBar) findViewById(R.id.seekBar3);
        seekBar4 = (SeekBar) findViewById(R.id.seekBar4);

        seekText1 = (TextView) findViewById(R.id.textView4);
        seekText2 = (TextView) findViewById(R.id.textView5);
        seekText3 = (TextView) findViewById(R.id.textView6);
        seekText4 = (TextView) findViewById(R.id.textView7);

        accebt_bt = (Button) findViewById(R.id.button3);

        seekBar1.setOnSeekBarChangeListener(seecListner);
        seekBar2.setOnSeekBarChangeListener(seecListner);
        seekBar3.setOnSeekBarChangeListener(seecListner);
        seekBar4.setOnSeekBarChangeListener(seecListner);

        spinner = (Spinner) findViewById(R.id.spinner);
        ArrayAdapter<?> adapter =
                ArrayAdapter.createFromResource(this, R.array.metrics, R.layout.spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinner.setAdapter(adapter);

        im_init();

        seekText1.setText(String.valueOf(seekBar1.getProgress()));
        seekText2.setText(String.valueOf(seekBar2.getProgress()));
        seekText3.setText(String.valueOf(seekBar3.getProgress()));
        seekText4.setText(String.valueOf(seekBar4.getProgress()));

        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String s = intent.getStringExtra("Result");
                char hesh_sum = 0;
                if(s.charAt(s.length()-1) == '!') {
//                    App.sendLocalBroadcastMessage("Операция2", s+'!');
                    for(int i=0;i<s.length()-2;i++){
                        hesh_sum += s.charAt(i);
                    }
                    hesh_sum = (char) (hesh_sum % 127);
                    if(hesh_sum == s.charAt(s.length()-2)) {
                        if ((s.charAt(0) == (char)102) && (s.charAt(1) == (char)85)) {
                            if(s.charAt(2) == (char)66){
                                dist_text.setText("NaN");
                            }
                            else{
                                dist_text.setText(String.valueOf((int) s.charAt(2)));
                            }
                        }
                        if ((s.charAt(0) == (char)100) && (s.charAt(1) == (char)85)) {
                            seekBar1.setProgress((int) s.charAt(2));
                            seekBar2.setProgress((int) s.charAt(3));
                            seekBar3.setProgress((int) s.charAt(4));
                            seekBar4.setProgress((int) s.charAt(5));

                            seekText1.setText(String.valueOf(seekBar1.getProgress()));
                            seekText2.setText(String.valueOf(seekBar2.getProgress()));
                            seekText3.setText(String.valueOf(seekBar3.getProgress()));
                            seekText4.setText(String.valueOf(seekBar4.getProgress()));
                        }
                    }
                }
            }
        },new IntentFilter("Операция1"));

        accebt_bt.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onClick(View v) {
                String s = new String();
                char data[] = new char[8];
                data[0] = (char) 85;  //0x55
                data[1] = (char) 102;  //0x66
                data[2] = (char) seekBar1.getProgress();
                data[3] = (char) seekBar2.getProgress();
                data[4] = (char) seekBar3.getProgress();
                data[5] = (char) seekBar4.getProgress();
                data[6] = (char) ((85+102+seekBar1.getProgress()+seekBar2.getProgress()+seekBar3.getProgress()+seekBar4.getProgress()) % 127);
                data[7] = (char) 33;

                App.sendLocalBroadcastMessage("Операция2", new String(data));
            }
        });

        char data[] = new char[5];
        data[0] = (char) 85;  //0x55
        data[1] = (char) 100;
        data[2] = (char) 63;
        data[3] = (char) ((85+100+63) % 127);
        data[4] = (char) 33;
        App.sendLocalBroadcastMessage("Операция2", new String(data));
    }

    public static byte[] hexStringToByteArray(String hex) {
        int l = hex.length();
        byte[] data = new byte[l/2];
        for (int i = 0; i < l; i += 2) {
            data[i/2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i+1), 16));
        }
        return data;
    }

    private void im_init(){
        ImageView im_settings = (ImageView) findViewById(R.id.imageView4);
        im_settings.setClickable(true);
        im_settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Rangefinder.this, Settings.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slidein, R.anim.slideout);
            }
        });

        ImageView im_nfc = (ImageView) findViewById(R.id.imageView3);
        im_nfc.setClickable(true);
        im_nfc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Rangefinder.this, Nfc.class);
                startActivity(intent);
                overridePendingTransition(R.anim.in_right, R.anim.out_right);
            }
        });

        ImageView im_main = (ImageView) findViewById(R.id.imageView);
        im_main.setClickable(true);
        im_main.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Rangefinder.this, MainActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slidein, R.anim.slideout);
            }
        });

        ImageView im_gps = (ImageView) findViewById(R.id.imageView2);
        im_gps.setClickable(true);
        im_gps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Rangefinder.this, Gps.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slidein, R.anim.slideout);
            }
        });
    }

    SeekBar.OnSeekBarChangeListener seecListner = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            if(seekBar1.getProgress()>=seekBar1.getMax()-4){
                seekBar1.setProgress(seekBar1.getMax()-4);
            }
            if(seekBar2.getProgress() <= seekBar1.getProgress()+1){
                seekBar2.setProgress(seekBar1.getProgress()+1);
            }
            if(seekBar3.getProgress() <= seekBar2.getProgress()+1){
                seekBar3.setProgress(seekBar2.getProgress()+1);
            }
            if(seekBar4.getProgress() <= seekBar3.getProgress()+1){
                seekBar4.setProgress(seekBar3.getProgress()+1);
            }

            if(seekBar.getId() == R.id.seekBar){
                seekText1.setText(String.valueOf(seekBar1.getProgress()));
            }
            if(seekBar.getId() == R.id.seekBar2){
                seekText2.setText(String.valueOf(seekBar2.getProgress()));
            }
            if(seekBar.getId() == R.id.seekBar3){
                seekText3.setText(String.valueOf(seekBar3.getProgress()));
            }
            if(seekBar.getId() == R.id.seekBar4){
                seekText4.setText(String.valueOf(seekBar4.getProgress()));
            }



        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    };

}
