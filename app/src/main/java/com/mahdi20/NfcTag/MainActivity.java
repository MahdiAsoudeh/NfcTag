package com.mahdi20.NfcTag;

import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    NfcAdapter mNfcAdapter;
    boolean nfcEnabled;
    PendingIntent mNfcPendingIntent;
    IntentFilter[] mReadTagFilters;
    private static final String TAG = "nfcinventory_simple";
    AlertDialog.Builder alertDialog;
    private boolean mWriteMode = false;
    private TextView txtName, txtId, txtAlert;
    private FloatingActionButton fab;
    private ProgressBar pg1;
    private ConstraintLayout cl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        popupMenu();

        fab = (FloatingActionButton) findViewById(R.id.fab);
        txtName = (TextView) findViewById(R.id.txtName);
        txtId = (TextView) findViewById(R.id.txtId);
        txtAlert = (TextView) findViewById(R.id.txtAlert);
        pg1 = (ProgressBar) findViewById(R.id.pg1);
        cl = (ConstraintLayout) findViewById(R.id.cl);

        checkNFCSupport();
        initNFCListener();


        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                MainActivity.this.startActivity(new Intent(MainActivity.this, WriteActivity.class));

            }
        });


    }

    private void checkNFCSupport() {
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

        if (mNfcAdapter == null) {
            Toast.makeText(this, "not supported!", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
    }

    public void doAction(Intent intent) {
        NdefMessage[] msgs = getNdefMessagesFromIntent(intent);
        confirmDisplayedContentOverwrite(msgs[0]);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        mWriteMode = false;
        if (!mWriteMode) {
            if (intent.getAction().equals(NfcAdapter.ACTION_NDEF_DISCOVERED)) {
                doAction(intent);
            } else if (intent.getAction().equals(NfcAdapter.ACTION_TAG_DISCOVERED)) {
                Toast.makeText(this, "tag is emtpy", Toast.LENGTH_LONG).show();
            }
        }


    }

    private void enableTagReadMode() {
        mWriteMode = false;
        mNfcAdapter.enableForegroundDispatch(this, mNfcPendingIntent, mReadTagFilters, null);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: " + getIntent());
        mNfcAdapter.disableForegroundDispatch(this);
        pg1.setVisibility(View.VISIBLE);


    }

    NdefMessage[] getNdefMessagesFromIntent(Intent intent) {
        NdefMessage[] msgs = null;
        String action = intent.getAction();
        if (action.equals(NfcAdapter.ACTION_TAG_DISCOVERED) || action.equals(NfcAdapter.ACTION_NDEF_DISCOVERED)) {
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            if (rawMsgs != null) {
                msgs = new NdefMessage[rawMsgs.length];
                for (int i = 0; i < rawMsgs.length; i++) {
                    msgs[i] = (NdefMessage) rawMsgs[i];
                }
            } else {
                byte[] empty = new byte[]{};
                NdefRecord record = new NdefRecord(NdefRecord.TNF_UNKNOWN, empty, empty, empty);
                NdefMessage msg = new NdefMessage(new NdefRecord[]{record});
                msgs = new NdefMessage[]{msg};
            }
        } else {
            Log.e(TAG, "Unknown intent.");
            finish();
        }
        return msgs;
    }

    private void confirmDisplayedContentOverwrite(final NdefMessage msg) {
        String payload = new String(msg.getRecords()[0].getPayload());
        setTextFieldValues(payload);
        Toast.makeText(MainActivity.this, "data received", Toast.LENGTH_SHORT).show();
        pg1.setVisibility(View.INVISIBLE);


    }


    private void setTextFieldValues(String jsonString) {

        JSONObject inventory = null;
        try {
            inventory = new JSONObject(jsonString);
            String id = inventory.getString("id");
            String name = inventory.getString("name");

            txtName.setText("name: " + name);
            txtId.setText("id: " + id);
            txtAlert.setVisibility(View.INVISIBLE);
            txtAlert.setVisibility(View.INVISIBLE);
            pg1.setVisibility(View.INVISIBLE);


        } catch (JSONException e) {
        }


    }

    private void checkNfcEnabled() {
        nfcEnabled = mNfcAdapter.isEnabled();

        if (!nfcEnabled) {
            Log.e("dar if", nfcEnabled + "");
            alertDialog = new AlertDialog.Builder(this);
            alertDialog.setTitle("NFC is disable");
            alertDialog.setMessage("enable the NFC");
            alertDialog.setCancelable(false);
            alertDialog.setPositiveButton("Update Settings", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.dismiss();
                    startActivity(new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS));
                }
            }).create().show();
            alertDialog.create().cancel();
            alertDialog.create().dismiss();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        checkNfcEnabled();
        Log.d(TAG, "onResume: " + getIntent());
        if (!mWriteMode) {
            if (getIntent().getAction() != null) {
                if (getIntent().getAction().equals(NfcAdapter.ACTION_NDEF_DISCOVERED)) {
                    doAction(getIntent());

                }
            }
        }


        mNfcAdapter.enableForegroundDispatch(this, mNfcPendingIntent, mReadTagFilters, null);

    }

    private void initNFCListener() {

        mNfcPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        IntentFilter ndefDetected = new IntentFilter(
                NfcAdapter.ACTION_NDEF_DISCOVERED);
        try {
            ndefDetected.addDataType("application/root.gast.playground.nfc");
        } catch (IntentFilter.MalformedMimeTypeException e) {
            throw new RuntimeException("Could not add MIME type.", e);
        }
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        mReadTagFilters = new IntentFilter[]{ndefDetected, tagDetected};
    }

    private void popupMenu() {

        ImageView img = (ImageView) findViewById(R.id.img);
        img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popup = new PopupMenu(MainActivity.this, v);
                popup.getMenuInflater().inflate(R.menu.menu, popup.getMenu());
                popup.show();
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item2) {
                        switch (item2.getItemId()) {
                            case R.id.action1:

                                String url = "http://t.me/MahdiAsodeh";
                                Intent i = new Intent(Intent.ACTION_VIEW);
                                i.setData(Uri.parse(url));
                                startActivity(i);

                                break;
                            default:
                                break;
                        }
                        return true;
                    }
                });
            }

        });


    }


}
