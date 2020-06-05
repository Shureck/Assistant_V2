package com.shureck.assistentv2;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
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
import android.os.IBinder;
import android.os.Looper;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
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

public class VoiceService extends Service implements RecognitionListener, LocationListener {

    NfcAdapter nfcAdapter;
    NfcVAdapter nfcVAdapter;
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            camManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        }

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationListener = this;

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
    }

    public void speak_tts(String str){
        if(isSpeak_tts == true) {
            tts.speak(str, TextToSpeech.QUEUE_FLUSH, null);
        }
    }

    private String robo_answer(String ask) {
        ask = ask.toLowerCase();
        if(ask.contains("включи")){
            on_flash();
            return "Фонарик включен";
        }
        if(ask.contains("выключи")){
            off_flash();
            return "Фонарик выключен";
        }
        if(ask.contains("where")||(ask.contains("где я"))){
            get_location();
            return "Определение местоположения...";
        }
        else{
            return "Не удалось обнаружить команду";
        }
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

    public void get_location() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, locationListener, null);
    }

    public void speak(final String text){ // make text 'final'

        // ... do not declare tts here


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
/////////////////////////////GPS////////////////////////////////////


    @Override
    public void onLocationChanged(Location loc) {
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

        String s = "Местоположение: " + cityName;

        send_answer(null,s);
        Log.v(LOG_TAG, s);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

//\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\

}


//public class VoiceRecognitionActivity extends Activity implements RecognitionListener {
//    private TextView returnedText;
//    private ToggleButton toggleButton;
//    private ProgressBar progressBar;
//    private SpeechRecognizer speech = null;
//    private Intent recognizerIntent;
//    private String LOG_TAG = "VoiceRecognitionActivity";
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_voice_recognition);
//        returnedText = (TextView) findViewById(R.id.textView1);
//        progressBar = (ProgressBar) findViewById(R.id.progressBar1);
//        toggleButton = (ToggleButton) findViewById(R.id.toggleButton1);
//        progressBar.setVisibility(View.INVISIBLE);
//
//        toggleButton.setOnCheckedChangeListener(new OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                if (isChecked) {
//                    progressBar.setVisibility(View.VISIBLE);
//                    progressBar.setIndeterminate(true);
//                    speech.startListening(recognizerIntent);
//                } else {
//                    progressBar.setIndeterminate(false);
//                    progressBar.setVisibility(View.INVISIBLE);
//                    speech.stopListening();
//                }
//            }
//        });
//    }
//    @Override
//    public void onResume() {
//        super.onResume();
//    }
//    @Override
//    protected void onPause() {
//        super.onPause();
//        if (speech != null) {
//            speech.destroy();
//            Log.i(LOG_TAG, "destroy");
//        }
//    }
//    @Override
//    public void onBeginningOfSpeech() {
//        Log.i(LOG_TAG, "onBeginningOfSpeech");
//        progressBar.setIndeterminate(false);
//        progressBar.setMax(10);
//    }
//    @Override
//    public void onBufferReceived(byte[] buffer) {
//        Log.i(LOG_TAG, "onBufferReceived: " + buffer);
//    }
//    @Override
//    public void onEndOfSpeech() {
//        Log.i(LOG_TAG, "onEndOfSpeech");
//        progressBar.setIndeterminate(true);
//        toggleButton.setChecked(false);
//    }
//    @Override
//    public void onError(int errorCode) {
//        String errorMessage = getErrorText(errorCode);
//        Log.d(LOG_TAG, "FAILED " + errorMessage);
//        returnedText.setText(errorMessage);
//        toggleButton.setChecked(false);
//    }
//    @Override
//    public void onEvent(int arg0, Bundle arg1) {
//        Log.i(LOG_TAG, "onEvent");
//    }
//    @Override
//    public void onPartialResults(Bundle arg0) {
//        Log.i(LOG_TAG, "onPartialResults");
//    }
//    @Override public void onReadyForSpeech(Bundle arg0) {
//        Log.i(LOG_TAG, "onReadyForSpeech");
//    }
//    @Override public void onResults(Bundle results) {
//        Log.i(LOG_TAG, "onResults");
//        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
//        String text = "";
//        for (String result : matches) text += result + "\n";
//        returnedText.setText(text);
//    }
//    @Override public void onRmsChanged(float rmsdB) {
//        Log.i(LOG_TAG, "onRmsChanged: " + rmsdB);
//        progressBar.setProgress((int) rmsdB);
//    }
//    public static String getErrorText(int errorCode) {
//        String message;
//        switch (errorCode) {
//            case SpeechRecognizer.ERROR_AUDIO: message = "Audio recording error";
//            break;
//            case SpeechRecognizer.ERROR_CLIENT: message = "Client side error";
//            break;
//            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS: message = "Insufficient permissions";
//            break;
//            case SpeechRecognizer.ERROR_NETWORK: message = "Network error";
//            break;
//            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT: message = "Network timeout";
//            break;
//            case SpeechRecognizer.ERROR_NO_MATCH: message = "No match";
//            break;
//            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY: message = "RecognitionService busy";
//            break;
//            case SpeechRecognizer.ERROR_SERVER: message = "error from server";
//            break;
//            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT: message = "No speech input";
//            break;
//            default: message = "Didn't understand, please try again.";
//            break;
//        }
//        return message;
//    }
//}
