package com.mahdi20.NfcTag;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.Charset;

public class WriteActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "SmartBookWriter";
    NfcAdapter mNfcAdapter;
    PendingIntent mNfcPendingIntent;
    IntentFilter[] mReadTagFilters;
    IntentFilter[] mWriteTagFilters;
    AlertDialog mWriteTagDialog;
    private boolean mWriteMode = false;
    private EditText edtName, edtId;
    Button btnSave;
    AlertDialog.Builder builder;
    AlertDialog.Builder alertDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write);

        popupMenu();


        edtName = (EditText) findViewById(R.id.edtName);
        edtId = (EditText) findViewById(R.id.edtId);
        btnSave = (Button) findViewById(R.id.btnSave);
        btnSave.setOnClickListener(this);

        checkNFCSupport();
        initNFCListener();
    }

    private void checkNFCSupport() {
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

        if (mNfcAdapter == null) {
            Toast.makeText(this, "your device not supported nfc", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkNfcEnabled();
        Log.d(TAG, "onResume: " + getIntent());
        mNfcAdapter.enableForegroundDispatch(this, mNfcPendingIntent,
                mReadTagFilters, null);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: " + getIntent());
        mNfcAdapter.disableForegroundDispatch(this);
    }


    private void initNFCListener() {

        mNfcPendingIntent = PendingIntent.getActivity(this, 0, new Intent
                (this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        IntentFilter ndefDetected = new IntentFilter(
                NfcAdapter.ACTION_NDEF_DISCOVERED);
        try {
            ndefDetected.addDataType("application/root.gast.playground.nfc");
        } catch (IntentFilter.MalformedMimeTypeException e) {
            throw new RuntimeException("Could not add MIME type.", e);
        }

        IntentFilter tagDetected = new IntentFilter(
                NfcAdapter.ACTION_TAG_DISCOVERED);

        mWriteTagFilters = new IntentFilter[]{tagDetected};
    }


    private NdefMessage createNdefFromJson() {

        String name = edtName.getText().toString();
        String id = edtId.getText().toString();


        JSONObject jsonObjectData = new JSONObject();
        try {


            jsonObjectData.put("name", name);
            jsonObjectData.put("id", id);


        } catch (JSONException e) {
            Log.e(TAG, "Could not create JSON: ", e);
        }
        String mimeType = "application/root.gast.playground.nfc";
        byte[] mimeBytes = mimeType.getBytes(Charset.forName("UTF-8"));
        String data = jsonObjectData.toString();
        byte[] dataBytes = data.getBytes(Charset.forName("UTF-8"));
        byte[] id2 = new byte[0];
        NdefRecord record = new NdefRecord(NdefRecord.TNF_MIME_MEDIA, mimeBytes, id2, dataBytes);
        NdefMessage m = new NdefMessage(new NdefRecord[]{record});
        return m;
    }

    private void enableTagWriteMode() {
        mWriteMode = true;
        mNfcAdapter.enableForegroundDispatch(this, mNfcPendingIntent,
                mWriteTagFilters, null);
    }


    @Override
    protected void onNewIntent(Intent intent) {
        if (intent.getAction().equals(NfcAdapter.ACTION_TAG_DISCOVERED)) {
            Tag detectedTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            writeTag(createNdefFromJson(), detectedTag);
            mWriteTagDialog.cancel();
        }
    }


    boolean writeTag(NdefMessage message, Tag tag) {
        int size = message.toByteArray().length;

        try {
            Ndef ndef = Ndef.get(tag);
            if (ndef != null) {
                ndef.connect();

                if (!ndef.isWritable()) {
                    Toast.makeText(this, "this tag is read only", Toast.LENGTH_LONG).show();
                    return false;
                }
                if (ndef.getMaxSize() < size) {
                    Toast.makeText(
                            this, "The amount of information is high",
                            Toast.LENGTH_LONG).show();
                    return false;
                }

                ndef.writeNdefMessage(message);
                Toast.makeText(this, "Successfully saved", Toast.LENGTH_LONG).show();
                edtName.setText("");
                edtId.setText("");


                return true;
            } else {
                NdefFormatable format = NdefFormatable.get(tag);
                if (format != null) {
                    try {
                        format.connect();
                        format.format(message);
                        Toast.makeText(
                                this,
                                "The tag was successfully updated",
                                Toast.LENGTH_LONG).show();
                        return true;
                    } catch (IOException e) {
                        Toast.makeText(
                                this,
                                "Can not save!",
                                Toast.LENGTH_LONG).show();
                        return false;
                    }
                } else {
                    Toast.makeText(
                            this,
                            "this tag not supported NDEF",
                            Toast.LENGTH_LONG).show();
                    return false;
                }
            }
        } catch (Exception e) {
            Toast.makeText(this,
                    "error",
                    Toast.LENGTH_LONG).show();
        }

        return false;
    }


    private void checkNfcEnabled() {
        Boolean nfcEnabled = mNfcAdapter.isEnabled();
        if (!nfcEnabled) {
            alertDialog = new AlertDialog.Builder(this);
            alertDialog.setTitle("NFC is disable");
            alertDialog.setMessage("enable the nfc");
            alertDialog.setCancelable(false);
            alertDialog.setPositiveButton("Update Settings",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,
                                            int id) {
                            startActivity(new Intent(
                                    android.provider.Settings.ACTION_WIRELESS_SETTINGS));
                        }
                    }).create().show();
            alertDialog.create().cancel();
            alertDialog.create().dismiss();
        }
    }

    @Override
    public void onClick(View view) {
        enableTagWriteMode();

        builder = new AlertDialog.Builder(
                this)
                .setTitle("Waiting for writing ...")
                .setMessage("Please approach the tag")
                .setCancelable(true)
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int id) {
                                dialog.cancel();
                            }
                        })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        builder.create().dismiss();
                    }
                });
        mWriteTagDialog = builder.create();
        mWriteTagDialog.show();
    }

    private void popupMenu() {

        ImageView img = (ImageView) findViewById(R.id.img);
        img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popup = new PopupMenu(WriteActivity.this, v);
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

