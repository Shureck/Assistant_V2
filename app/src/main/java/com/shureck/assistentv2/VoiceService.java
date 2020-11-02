package com.shureck.assistentv2;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.nfc.Tag;
import android.nfc.tech.NfcF;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.lang.reflect.Type;
import java.security.Policy;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;


import static android.app.Activity.RESULT_OK;

public class VoiceService extends Service implements RecognitionListener {

    NfcAdapter nfcAdapter;
    NfcVAdapter nfcVAdapter;

    final String SAVED_TEXT = "saved_text";
    final String SAVED_ADR = "saved_adr";

    final String LOG_TAG = "LOG_TAG";
    public SpeechRecognizer speech = null;
    public Intent recognizerIntent;
    public TextToSpeech tts;
    public LocationManager locationManager;
    public LocationListener locationListener;
    public AudioManager myAudioManager;
    public boolean ttsEnabled, isSpeak = false;
    public static final int VOICE_RECOGNITION_REQUEST_CODE = 1234;
    public Gson gson = new Gson();
    public CameraManager camManager;
    private Camera camera;
    Camera.Parameters parameters;
    public boolean isSpeak_tts;
    String telephonNum;
    Geocoder geocoder;
    LatLng address;
    double radius = 500;
    private Handler mHandler = new Handler();
    public static Location moment_loc;

    public SharedPreferences appSharedPrefs;
    public SharedPreferences.Editor prefsEditor;
    public Date currentDate;


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void onCreate() {
        super.onCreate();
        Log.d(LOG_TAG, "onCreate");
        myAudioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);

        appSharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        prefsEditor = appSharedPrefs.edit();
        isSpeak_tts = appSharedPrefs.getBoolean("isSpeak", false);

        currentDate = new Date();
        camera = Camera.open();

        geocoder = new Geocoder(this);


        //saveText(SAVED_ADR, "Люберцы, Юбилейная 24|500");
        telephonNum = loadText(SAVED_TEXT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            camManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        }

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        PackageManager packManager = getPackageManager();
        List<ResolveInfo> intActivities = packManager.queryIntentActivities(new
                Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0);
        if (intActivities.size() != 0) {
            speech = SpeechRecognizer.createSpeechRecognizer(this);
            speech.setRecognitionListener(this);
            recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ru");
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, this.getPackageName());
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
            //recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 60000);    //Количество времени, которое должно пройти после того, как мы перестанем слышать речь, чтобы рассмотреть ввод полный.
            //recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, true);    //Минимальная длина высказывания.
            //recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3000);    //Количество времени, которое должно пройти после того, как мы перестанем слышать речь, чтобы рассмотреть ввод возможно, полный

        } else {
            Toast.makeText(this, "Oops - Speech recognition not supported!", Toast.LENGTH_LONG).show();
        }

        String ss = loadText(SAVED_ADR);
        if(!ss.equals("") && ss != null){
            try {
                address = new LatLng(geocoder.getFromLocationName(ss.substring(0,ss.indexOf('|')),1).get(0).getLatitude(),geocoder.getFromLocationName(ss.substring(0,ss.indexOf('|')),1).get(0).getLongitude());
                ss = ss.substring(ss.indexOf('|')+1,ss.length());
                if(!ss.equals("") && ss != null) {
                    radius = Double.parseDouble(ss.substring(ss.indexOf('|') + 1, ss.length()));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    //Locale[] loc = Locale.getAvailableLocales();
                    Locale locale = new Locale("ru");
                    int result = tts.setLanguage(locale);
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("TTS", "Language not supported");
                    } else {

                    }
                } else {
                    Log.e("TTS", "Failed");
                }
            }
        });

        mHandler.removeCallbacks(badTimeUpdater);
        mHandler.postDelayed(badTimeUpdater, 100);


        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String s = intent.getStringExtra("Result");
                if (s.equals("start_rec")) {
                    speech.startListening(recognizerIntent);
                }
                if (s.contains("No_Speech")){
                    s = s.substring(9, s.length());
                    send_answer(s, robo_answer(s));
                    App.sendLocalBroadcastMessage("rec_res", "stop_rec");
                }
                if (s.contains("isSpeak")){
                    s = s.substring(7, s.length());
                    isSpeak_tts = Boolean.valueOf(s);
                }


            }
        }, new IntentFilter("VS_call"));

        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String s = intent.getStringExtra("Result");
                speak_tts(s);
            }
        }, new IntentFilter("nfc_rec"));

        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String s = intent.getStringExtra("Result");
                telephonNum = s;
            }
        }, new IntentFilter("Номер"));

        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String s = intent.getStringExtra("Result");
                try {
                    Address add = geocoder.getFromLocationName(s.substring(0,s.indexOf('|')),1).get(0);
                    address = new LatLng(add.getLatitude(),add.getLongitude());
                    radius = Double.parseDouble(s.substring(s.indexOf('|')+1,s.length()));
                    App.sendLocalBroadcastMessage("Address_back",add.getAddressLine(0));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, new IntentFilter("Address"));


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
                    System.out.println("wwwwwwww" + s);
                    if(s.length()>1) {
                        if (hesh_sum == s.charAt(s.length() - 2)) {
                            System.out.println("ssssss" + s);
                            if ((s.charAt(0) == (char) 102) && (s.charAt(1) == (char) 85)) {
                                System.out.println("LLLL " + s);
                                if (s.charAt(2) == (char) 67) {
                                    String sent = "android.telephony.SmsManager.STATUS_ON_ICC_SENT";
                                    PendingIntent piSent = PendingIntent.getBroadcast(VoiceService.this, 0,new Intent(sent), 0);
                                    if(telephonNum != "") {
                                        sendSMS(telephonNum, "Я рядом с " + get_location());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },new IntentFilter("Операция1"));
    }

    public void speak_tts(String str){
        if(isSpeak_tts == true) {
            tts.speak(str, TextToSpeech.QUEUE_FLUSH, null);
        }
    }

    private Runnable badTimeUpdater = new Runnable() {
        @Override
        public void run() {
            moment_loc = getLastKnownLocation();
            if(calcDist(address.latitude,address.longitude,moment_loc.getLatitude(),moment_loc.getLongitude())[0] > radius){
                Toast.makeText(VoiceService.this,"Вы покинули радиус!!!",Toast.LENGTH_LONG).show();
            }
            mHandler.postDelayed(this, 5*60*1000);
        }
    };

    private String robo_answer(String ask) {
        ask = ask.toLowerCase();
        ask = ask.trim();
        if(ask.contains("включи")){
            on_flash();
            return "Фонарик включен";
        }
        if(ask.contains("выключи")){
            off_flash();
            return "Фонарик выключен";
        }
        if(ask.contains("where")||(ask.contains("где я"))){
            return "Местоположение: " + get_location();
        }
        if(ask.contains("what")||(ask.contains("что это?"))){
            return "В разработке";
        }
        if(ask.contains("sos")||ask.contains("сос")){
            String sent = "android.telephony.SmsManager.STATUS_ON_ICC_SENT";
            PendingIntent piSent = PendingIntent.getBroadcast(VoiceService.this, 0,new Intent(sent), 0);
            sendSMS("89377973353","Я рядом с "+get_location());
            return "Сообщение отправлено";
        }
        else{
            return "Не удалось обнаружить команду";
        }
    }

    private void sendSMS(String phoneNumber, String message) {
        String SENT = "SMS_SENT";
        String DELIVERED = "SMS_DELIVERED";
        PendingIntent sentPI = PendingIntent.getBroadcast(this, 0, new Intent( SENT), 0);
        PendingIntent deliveredPI = PendingIntent.getBroadcast(this, 0, new Intent(DELIVERED), 0); // ---when the SMS has been sent---
        registerReceiver(new BroadcastReceiver() {
            @Override public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        ContentValues values = new ContentValues();
                        getContentResolver().insert( Uri.parse("content://sms/sent"), values);
                        Toast.makeText(getBaseContext(), "SMS sent", Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        Toast.makeText(getBaseContext(), "Generic failure", Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        Toast.makeText(getBaseContext(), "No service", Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        Toast.makeText(getBaseContext(), "Null PDU", Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        Toast.makeText(getBaseContext(), "Radio off", Toast.LENGTH_SHORT).show();
                        break;
                    }
                }
            }, new IntentFilter(SENT)); // ---when the SMS has been delivered---
            registerReceiver(new BroadcastReceiver() {
                @Override public void onReceive(Context arg0, Intent arg1) {
                    switch (getResultCode()) {
                        case Activity.RESULT_OK:
                            Toast.makeText(getBaseContext(), "SMS delivered", Toast.LENGTH_SHORT).show();
                            break;
                        case Activity.RESULT_CANCELED:
                            Toast.makeText(getBaseContext(), "SMS not delivered", Toast.LENGTH_SHORT).show();
                            break;
                        }
                    }
                }, new IntentFilter(DELIVERED));
            SmsManager sms = SmsManager.getDefault();
            SmsManager smsManager = SmsManager.getDefault();
            ArrayList<String> parts = smsManager.divideMessage(message);
            sms.sendMultipartTextMessage(phoneNumber, null, parts, null, null);
        }

    void resolveIntent(Intent intent) {
        // Parse the intent
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)) {
            // When a tag is discovered we send it to the service to be save. We
            // include a PendingIntent for the service to call back onto. This
            // will cause this activity to be restarted with onNewIntent(). At
            // that time we read it from the database and view it.
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            NdefMessage[] msgs;
            if (rawMsgs != null) {
                msgs = new NdefMessage[rawMsgs.length];
                for (int i = 0; i < rawMsgs.length; i++) {
                    msgs[i] = (NdefMessage) rawMsgs[i];
                }
            } else {
                // Unknown tag type
                byte[] empty = new byte[]{};
                NdefRecord record = new NdefRecord(NdefRecord.TNF_UNKNOWN, empty, empty, empty);
                NdefMessage msg = new NdefMessage(new NdefRecord[]{record});
                msgs = new NdefMessage[]{msg};
            }
        } else {
            Log.e("ViewTag", "Unknown intent " + intent);
            return;
        }
    }


    public void on_flash(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (camera != null) {
                    parameters = camera.getParameters();
                    if (parameters != null) {
                        List supportedFlashModes = parameters.getSupportedFlashModes();

                        if (supportedFlashModes.contains(Camera.Parameters.FLASH_MODE_TORCH)) {
                            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                        } else if (supportedFlashModes.contains(Camera.Parameters.FLASH_MODE_ON)) {
                            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
                        } else camera = null;

                        if (camera != null) {
                            camera.setParameters((Camera.Parameters) parameters);
                            camera.startPreview();
                            try {
                                camera.setPreviewTexture(new SurfaceTexture(0));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }).start();
    }

    public void off_flash(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (camera != null) {
                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                    camera.setParameters(parameters);
                    camera.stopPreview();
                }
            }
        }).start();
    }

    public String get_location() {
        Location loc = getLastKnownLocation();

        String longitude = "Longitude: " + loc.getLongitude();
        Log.v(LOG_TAG, longitude);
        String latitude = "Latitude: " + loc.getLatitude();
        Log.v(LOG_TAG, latitude);

        /*------- To get city name from coordinates -------- */
        String cityName = null;
        Geocoder gcd = new Geocoder(getBaseContext(), Locale.getDefault());
        List<Address> addresses;
        try {
            addresses = gcd.getFromLocation(loc.getLatitude(),
                    loc.getLongitude(), 1);
            if (addresses.size() > 0) {
                System.out.println(addresses.get(0).getLocality());
                cityName = addresses.get(0).getAddressLine(0);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
//        String s = longitude + "\n" + latitude + "\n\nМестоположение: "
//                + cityName;

        String s = cityName;

        Log.v(LOG_TAG, s);
        return s;
    }

    void saveText(String tag, String text) {
        prefsEditor.putString(tag, text);
        prefsEditor.commit();
    }

    String loadText(String tag) {
        String savedText = appSharedPrefs.getString(tag, "");
        return savedText;
    }


    private Location getLastKnownLocation() {
        locationManager = (LocationManager) getApplicationContext().getSystemService(LOCATION_SERVICE);
        List<String> providers = locationManager.getProviders(true);
        Location bestLocation = null;
        for (String provider : providers) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return null;
            }
            Location l = locationManager.getLastKnownLocation(provider);
            if (l == null) {
                continue;
            }
            if (bestLocation == null || l.getAccuracy() < bestLocation.getAccuracy()) {
                // Found best last known location: %s", l);
                bestLocation = l;
            }
        }
        return bestLocation;
    }

    public void speak(final String text){ // make text 'final'

        // ... do not declare tts here


    }

    public static double[] calcDist(double llat1, double llong1, double llat2, double llong2){
        //Math.PI;
        int rad = 6372795;
        double lat1 = llat1*Math.PI/180;
        double lat2 = llat2*Math.PI/180;
        double long1 = llong1*Math.PI/180;
        double long2 = llong2*Math.PI/180;

        double cl1 = Math.cos(lat1);
        double cl2 = Math.cos(lat2);
        double sl1 = Math.sin(lat1);
        double sl2 = Math.sin(lat2);
        double delta = long2 - long1;
        double cdelta = Math.cos(delta);
        double sdelta = Math.sin(delta);

        double y = Math.sqrt(Math.pow(cl2*sdelta,2)+Math.pow(cl1*sl2-sl1*cl2*cdelta,2));
        double x = sl1*sl2+cl1*cl2*cdelta;
        double ad = Math.atan2(y,x);
        double dist = ad*rad;

        x = (cl1*sl2) - (sl1*cl2*cdelta);
        y = sdelta*cl2;
        double z = Math.toDegrees(Math.atan(-y/x));

        if (x < 0){z = z+180;}

        double z2 = (z+180.) % 360. - 180;
        z2 -= Math.toRadians(z2);
        double anglerad2 = z2 - ((2*Math.PI)*Math.floor((z2/(2*Math.PI))));
        double angledeg = (anglerad2*180.)/Math.PI;

        System.out.println("!!!!!!!!!!!!!!! "+ dist + " m " + angledeg + " dig");

        double mass[] = {dist,angledeg,-1};
        return mass;
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(LOG_TAG, "onStartCommand");
        //speak("Тебе ^ @");
        //bspeesh();
        return super.onStartCommand(intent, flags, startId);
    }

    public void onDestroy() {
        super.onDestroy();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        if(speech != null) {
            speech.destroy();
        }
        releaseCamera();
        //speak_on();
        Log.d(LOG_TAG, "onDestroy");
    }

    public IBinder onBind(Intent intent) {
        Log.d(LOG_TAG, "onBind");
        return null;
    }
    public void speak_off() // Метод для выключение внешних динамиков планшета
    {
        myAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
    }
    public void speak_on() // Метод включения внешних динамиков планшета
    {
        myAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 20, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
    }

    @Override
    public void onBeginningOfSpeech() {
        Log.d(LOG_TAG, "onBeginningOfSpeech");
    }

    @Override
    public void onRmsChanged(float rmsdB) {

    }

    @Override
    public void onBufferReceived(byte[] buffer) {

    }

    @Override
    public void onEndOfSpeech() {
        App.sendLocalBroadcastMessage("rec_res", "stop_rec");
        Log.d(LOG_TAG, "onEndOfSpeech");
        isSpeak = false;
    }

    @Override
    public void onError(int error) {

    }

    public void send_answer(String ask, String answ){
        speak_tts(answ);
        String[] text = new String[2];
        text[0] = ask;
        text[1] = answ;
        String json = gson.toJson(text);
        App.sendLocalBroadcastMessage("rec_res", json);
    }

    @Override
    public void onResults(Bundle results) {
        Log.d(LOG_TAG, "onResults");
        if(isSpeak == false) {
            ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            send_answer(matches.get(0),robo_answer(matches.get(0)));

            isSpeak = true;
        }
    }

    @Override
    public void onEvent(int arg0, Bundle arg1) {
        Log.d(LOG_TAG, "onEvent");
    }
    @Override
    public void onPartialResults(Bundle arg0) {
        Log.d(LOG_TAG, "onPartialResults" + arg0.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).get(0));
    }
    @Override public void onReadyForSpeech(Bundle arg0) {
        Log.d(LOG_TAG, "onReadyForSpeech");
    }

    private void releaseCamera() {
        if (camera != null) {
            camera.release();
            camera = null;
        }
    }
}
