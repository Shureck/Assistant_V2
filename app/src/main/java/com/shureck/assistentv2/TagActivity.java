package com.shureck.assistentv2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.math.BigInteger;
import java.util.ArrayList;

public class TagActivity extends AppCompatActivity {

    public SharedPreferences appSharedPrefs;
    public Gson gson = new Gson();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d("myLogs", "Yep!");
        resolveIntent(getIntent());
        finish();
    }

    void resolveIntent(Intent intent)
    {
        // Parse the intent
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action))
        {
            byte[] tagId = intent.getByteArrayExtra(NfcAdapter.EXTRA_ID);
            Tag myTag = (Tag) intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                Log.d("myLogs", "Y!"+ bin2hex(myTag.getId()));
            App.sendLocalBroadcastMessage("nfc_tag", bin2hex(myTag.getId()));

            appSharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            ArrayList<Nfc_struct> nfc_list = new ArrayList<>();
            String savedT = appSharedPrefs.getString("MyObject", "");
            Type type = new TypeToken<ArrayList<Nfc_struct>>() {
            }.getType();
            nfc_list = gson.fromJson(savedT, type);

            if (nfc_list != null) {
                for (int i = 0; i < nfc_list.size(); i++) {
                    if (nfc_list.get(i).uid.equals(bin2hex(myTag.getId()))) {
                        App.sendLocalBroadcastMessage("nfc_rec", nfc_list.get(i).name + ". " + nfc_list.get(i).descrip);
                        break;
                    }
                }
            }
        }
        else
        {
            Log.e("maLogs", "Unknown intent " + intent);
            finish();
            return;
        }
    }

    static String bin2hex(byte[] data) {
        return String.format("%0" + (data.length * 2) + "X", new BigInteger(1,data));
    }
}
