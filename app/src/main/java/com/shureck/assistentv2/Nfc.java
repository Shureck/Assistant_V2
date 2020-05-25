package com.shureck.assistentv2;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Application;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.NfcV;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class Nfc_struct{
    String uid, name, descrip;

    Nfc_struct(String uid,String name,String descrip){
        this.uid = uid;
        this.name = name;
        this.descrip = descrip;
    }

    public String getUid() {
        return uid;
    }

    public String getName() {
        return name;
    }

    public String getDescrip() {
        return descrip;
    }
}

class FireMissilesDialogFragment extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(inflater.inflate(R.layout.dialog_settings, null))
                // Add action buttons
                .setPositiveButton("Signin", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // sign in the user ...
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                    }
                });
        return builder.create();
    }
}

public class Nfc extends AppCompatActivity{

    NfcAdapter nfcAdapter;
    NfcVAdapter nfcVAdapter;
    public ArrayList<Nfc_struct> nfc_list = new ArrayList<>();
    public EditText name_t, descr_t;
    public Button del_dt, add_bt;
    public RecyclerView nfc_list_v;
    public SharedPreferences appSharedPrefs;
    public SharedPreferences.Editor prefsEditor;
    public Gson gson = new Gson();
    public DialogFragment dialogFragment;


    RecyclerView.LayoutManager RecyclerViewLayoutManager;
    View ChildView ;
    int RecyclerViewItemPosition ;

    public String jsonText = "{\"descrip\":\"здрасьте\",\"name\":\"лол\",\"uid\":\"FFFF322\"}";
    public DataAdapter ad_answer;
    public List<RowType> phones = new ArrayList<>();

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.nfc);

        im_init();

        appSharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        prefsEditor = appSharedPrefs.edit();

        name_t = (EditText) findViewById(R.id.editText);
        descr_t = (EditText) findViewById(R.id.editText3);

        del_dt = (Button) findViewById(R.id.button2);
        add_bt = (Button) findViewById(R.id.button5);

        nfc_list_v = (RecyclerView) findViewById(R.id.rec_v_nfc);

        NfcManager manager = (NfcManager) getSystemService(Context.NFC_SERVICE);
        nfcAdapter= manager.getDefaultAdapter();

        String savedText = appSharedPrefs.getString("MyObject", "");
        Type type = new TypeToken<ArrayList<Nfc_struct>>(){}.getType();
        nfc_list = gson.fromJson(savedText, type);
        if((nfc_list != null)&&(nfc_list.size() > 0)) {
            nfcVAdapter = new NfcVAdapter(Nfc.this, nfc_list);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    nfc_list_v.setAdapter(nfcVAdapter);
                }
            });
        }

        nfc_list_v.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {

            GestureDetector gestureDetector = new GestureDetector(Nfc.this, new GestureDetector.SimpleOnGestureListener() {
                @Override public boolean onSingleTapUp(MotionEvent motionEvent) {
                    return true;
                }

            });

            @Override
            public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {

                ChildView = rv.findChildViewUnder(e.getX(), e.getY());

                if(ChildView != null && gestureDetector.onTouchEvent(e)) {

                    RecyclerViewItemPosition = nfc_list_v.getChildAdapterPosition(ChildView);

                    build_dialog(RecyclerViewItemPosition);

                    Toast.makeText(Nfc.this, nfc_list.get(RecyclerViewItemPosition).uid, Toast.LENGTH_LONG).show();
                }

                return false;
            }

            @Override
            public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {

            }

            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

            }
        });
        add_bt.setOnClickListener(add_bt_onClick);
    }

    @Override protected void onNewIntent(Intent intent) {
        if(NfcAdapter.ACTION_TAG_DISCOVERED.equals(getIntent().getAction())) {
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            Log.d("NFC_LOL", "tag ID = " + tag.getId().toString());
        }
        super.onNewIntent(intent);
    }

    View.OnClickListener add_bt_onClick = new View.OnClickListener() {
        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        public void onClick(View v) {
            if(!name_t.getText().toString().equals("")){
                nfcAdapter.enableReaderMode(Nfc.this, new NfcAdapter.ReaderCallback() {
                    @Override
                    public void onTagDiscovered(Tag tag) {
                        System.out.println("$$$$$$$$$$$$$$$$ money $$$$$$$$$$$$$$$$ " + bin2hex(tag.getId()));
                        boolean check = false;
                        for(int i=0; i<nfc_list.size();i++){
                            if(nfc_list.get(i).uid.equals(bin2hex(tag.getId()))){
                                check = true;
                                break;
                            }
                        }
                        if(check == false){
                            nfc_list.add(new Nfc_struct(bin2hex(tag.getId()), name_t.getText().toString(), descr_t.getText().toString()));
                            nfcVAdapter = new NfcVAdapter(Nfc.this, nfc_list);

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    nfc_list_v.setAdapter(nfcVAdapter);
                                }
                            });

                            String json = gson.toJson(nfc_list);
                            Log.d("TAG","json = " + json);
                            prefsEditor.putString("MyObject", json);
                            prefsEditor.commit();

                            name_t.setText("");
                            descr_t.setText("");

                        }
                        nfcAdapter.disableReaderMode(Nfc.this);
                    }
                }, NfcAdapter.FLAG_READER_NFC_A, null);
            }
        }
    };

    static String bin2hex(byte[] data) {
        return String.format("%0" + (data.length * 2) + "X", new BigInteger(1,data));
    }

    public void build_dialog(final int pos) {
        final AlertDialog.Builder builder = new AlertDialog.Builder( Nfc.this);
        // Get the layout inflater
        final LayoutInflater inflater = Nfc.this.getLayoutInflater();

        final View view = inflater.inflate(R.layout.dialog_settings, null);

        EditText user = (EditText) view.findViewById(R.id.username);
        EditText pass = (EditText) view.findViewById(R.id.password);
        TextView uid_t = (TextView) view.findViewById(R.id.textView13);

        uid_t.setText(nfc_list.get(pos).uid);
        user.setText(nfc_list.get(pos).name);
        pass.setText(nfc_list.get(pos).descrip);

        builder.setView(view)
                // Add action buttons
                .setPositiveButton("Signin", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        Dialog dialogView = (Dialog) dialog;
                        EditText user = (EditText) view.findViewById(R.id.username);
                        EditText pass = (EditText) view.findViewById(R.id.password);

                        nfc_list.get(pos).name = user.getText().toString();
                        nfc_list.get(pos).descrip = pass.getText().toString();

                        System.out.println("!!!!!!!! "+ user.getText()+" @@@@@@@@@@@@@@@@@@@ "+ pass.getText()+" "+id);

                        String json = gson.toJson(nfc_list);
                        Log.d("TAG","json = " + json);
                        prefsEditor.putString("MyObject", json);
                        prefsEditor.commit();

                        nfcVAdapter = new NfcVAdapter(Nfc.this, nfc_list);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                nfc_list_v.setAdapter(nfcVAdapter);
                            }
                        });

                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                    }
                })
                .setNeutralButton("Delite", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        nfc_list.remove(pos);

                        String json = gson.toJson(nfc_list);
                        Log.d("TAG","json = " + json);
                        prefsEditor.putString("MyObject", json);
                        prefsEditor.commit();

                        nfcVAdapter = new NfcVAdapter(Nfc.this, nfc_list);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                nfc_list_v.setAdapter(nfcVAdapter);
                            }
                        });
                    }
                });
        builder.show();
    }

    private void im_init(){
        ImageView im_settings = (ImageView) findViewById(R.id.imageView4);
        im_settings.setClickable(true);
        im_settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Nfc.this, Settings.class);
                startActivity(intent);
            }
        });

        ImageView im_main = (ImageView) findViewById(R.id.imageView);
        im_main.setClickable(true);
        im_main.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Nfc.this, MainActivity.class);
                startActivity(intent);
            }
        });

        ImageView im_range = (ImageView) findViewById(R.id.imageView5);
        im_range.setClickable(true);
        im_range.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Nfc.this, Rangefinder.class);
                startActivity(intent);
            }
        });

        ImageView im_gps = (ImageView) findViewById(R.id.imageView2);
        im_gps.setClickable(true);
        im_gps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Nfc.this, Gps.class);
                startActivity(intent);
            }
        });
    }
}
