package com.shureck.assistentv2;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
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

import java.util.ArrayList;
import java.util.Locale;

import static android.app.Activity.RESULT_OK;

public class VoiceService extends Service {

    final String LOG_TAG = "myLogs";
    public SpeechRecognizer speech = null;
    public Intent recognizerIntent;
    public TextToSpeech tts;
    public AudioManager myAudioManager;
    public static final int VOICE_RECOGNITION_REQUEST_CODE = 1234;


    public void onCreate() {
        super.onCreate();
        Log.d(LOG_TAG, "onCreate");
        myAudioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    Locale locale = new Locale("ru");
                    int result = tts.setLanguage(locale);
                    tts.setPitch(1.3f); //1.3f
                    tts.setSpeechRate(0.7f);
                } else if (status == TextToSpeech.ERROR) {
                    Toast.makeText(VoiceService.this, "Всё плохо", Toast.LENGTH_LONG).show();

                }
            }
        });
        tts.speak("Ураааа", TextToSpeech.QUEUE_FLUSH, null);
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(LOG_TAG, "onStartCommand");
        bspeesh();
        speech.startListening(recognizerIntent);
        return super.onStartCommand(intent, flags, startId);
    }

    public void onDestroy() {
        super.onDestroy();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        speech.destroy();
        //speak_on();
        Log.d(LOG_TAG, "onDestroy");
    }

    public IBinder onBind(Intent intent) {
        Log.d(LOG_TAG, "onBind");
        return null;
    }

    public void bspeesh() {
        speech = SpeechRecognizer.createSpeechRecognizer(VoiceService.this); //Создание объекта распознавателя речи
        speech.setRecognitionListener(thiss); //Установить обработчик событий распознавания
        // Передача параметров
        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,
                this.getPackageName());
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 4);
    }

    RecognitionListener thiss = new RecognitionListener() {
        @Override
        public void onBeginningOfSpeech() {
        }

        @Override
        public void onRmsChanged(float rmsdB) {

        }

        @Override
        public void onBufferReceived(byte[] buffer) {
        }

        @Override
        public void onEndOfSpeech() {
        }

        @Override
        public void onError(int errorCode) {
            //speak_off();  //Выключить звук в случае любой ошибки
            speech.destroy();
            bspeesh();
            speech.startListening(recognizerIntent);
        }

        @Override
        public void onEvent(int arg0, Bundle arg1) {

        }

        @Override
        public void onPartialResults(Bundle arg0) {

        }

        @Override
        public void onReadyForSpeech(Bundle arg0) {

        }

        @Override
        public void onResults(Bundle results) {  // Результаты распознавания
            ArrayList data = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            String sp = data.get(0).toString();


            int r0 = sp.compareTo("Тест");

            if (r0 == 0) {
                //speak_on(); // Включаем динамики
                tts.speak("Ээээээ тебе ^ @", TextToSpeech.QUEUE_FLUSH, null);
            } else {
                //speak_off(); // Если фразы и команды не описаны, выполняется распознавание речи, вывод результата
                // в виде строки при выключенных динамиках
                speech.stopListening(); //Прекратить слушать речь
                speech.destroy();       // Уничтожить объект SpeechRecognizer
                bspeesh();
                speech.startListening(recognizerIntent);
            }
        }
    };

    public void speak_off() // Метод для выключение внешних динамиков планшета
    {
        myAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
    }
    public void speak_on() // Метод включения внешних динамиков планшета
    {
        myAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 20, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
    }
}
