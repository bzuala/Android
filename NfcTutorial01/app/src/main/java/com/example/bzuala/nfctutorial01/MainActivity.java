package com.example.bzuala.nfctutorial01;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    NfcAdapter nfcAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter != null && nfcAdapter.isEnabled()) {
        } else {
            finish();
        }
    }


    private void enableForegroundDispatchSystem() {
        Intent intent = new Intent(this, MainActivity.class).addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        IntentFilter[] intentFilter = new IntentFilter[]{};
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFilter, null);
    }

    private void disableForegroundDispatchSystem() {
        nfcAdapter.disableForegroundDispatch(this);
    }

    private void formatTag(Tag tag, NdefMessage message) {
        try {
            NdefFormatable ndefFormatable = NdefFormatable.get(tag);
            if (ndefFormatable == null) {
                Toast.makeText(this, "Tag is not NDEF formatted", Toast.LENGTH_SHORT).show();
                return;
            }
            ndefFormatable.connect();
            ndefFormatable.format(message);
            ndefFormatable.close();

        } catch (Exception e) {
            Log.e("formatTag", e.getMessage());
        }
    }

    private void writeNdefMessage(Tag tag, NdefMessage ndefMessage) {
        try {
            if (tag == null) {
                Toast.makeText(this, "Tag object cann't be null", Toast.LENGTH_SHORT).show();
                return;
            }

            Ndef ndef = Ndef.get(tag);
            if (ndef == null) {
                //format tag with the ndef format and writes the message
                formatTag(tag, ndefMessage);
            } else {
                ndef.connect();
                if (!ndef.isWritable()) {
                    Toast.makeText(this, "Tag is not writeable", Toast.LENGTH_SHORT).show();
                    ndef.close();
                    return;
                }
                //ndef.writeNdefMessage(ndefMessage);
                ndef.close();

                Toast.makeText(this, "Tag written !", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e("formatTag", e.getMessage());
        }
    }

    private NdefMessage createNdefMessage(String content) {
        NdefRecord ndefRecord = createTextRecord(content);
        NdefMessage ndefMessage = new NdefMessage(new NdefRecord[]{ndefRecord});
        return ndefMessage;
    }

    private NdefRecord createTextRecord(String content) {
        try {
            byte[] language;
            language = Locale.getDefault().getLanguage().getBytes("UTF-8");

            final byte[] text = content.getBytes("UTF-8");
            final int languageSize = language.length;
            final int textLength = text.length;
            final ByteArrayOutputStream payload = new ByteArrayOutputStream(1 + languageSize + textLength);

            payload.write((byte) (languageSize & 0x1F));
            payload.write(language, 0, languageSize);
            payload.write(text, 0, textLength);

            return new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], payload.toByteArray());

        } catch (UnsupportedEncodingException e) {
            Log.e("createTextRecord", e.getMessage());
        }
        return null;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        //Toast.makeText(this, "NFC Intent Received", Toast.LENGTH_LONG).show();
        super.onNewIntent(intent);
        String[] cardTypeInfo = getTagInfo(intent);
        final EditText textInfo = (EditText) findViewById(R.id.infoText);
        String infoText = null;
        for (int i = 0; i < cardTypeInfo.length; i++) {
            infoText += cardTypeInfo[i];
        }
        textInfo.setText("Hello World");
        //if (intent.hasExtra(NfcAdapter.EXTRA_TAG)) {
        //    Toast.makeText(this, "NFCIntent", Toast.LENGTH_SHORT).show();
        //    Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        //    NdefMessage ndefMessage = createNdefMessage("My Content");
        //    writeNdefMessage(tag,ndefMessage);
        //}
    }

    @Override
    protected void onResume() {
        //Intent intent = new Intent(this, MainActivity.class);
        //intent.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);

        //PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        //IntentFilter[] intentFilter = new IntentFilter[]{};

        //nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFilter, null);

        super.onResume();
        enableForegroundDispatchSystem();

    }

    @Override
    protected void onPause() {
        super.onPause();
        disableForegroundDispatchSystem();
    }

    private String[] getTagInfo(Intent intent) {
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        String prefix = "android.nfc.tech.";
        String[] info = new String[2];

        // UID
        byte[] uid = tag.getId();
        info[0] = "UID In Hex: " + Utils.convertByteArrayToHexString(uid) + "\n" +
                "UID In Dec: " + Utils.convertByteArrayToDecimal(uid) + "\n\n";

        // Tech List
        String[] techList = tag.getTechList();
        String techListConcat = "Technologies: ";
        for (int i = 0; i < techList.length; i++) {
            techListConcat += techList[i].substring(prefix.length()) + ",";
        }
        info[0] += techListConcat.substring(0, techListConcat.length() - 1) + "\n\n";

        // Mifare Classic/UltraLight Info
        info[0] += "Card Type: ";
        String type = "Unknown";
        for (int i = 0; i < techList.length; i++) {
            if (techList[i].equals(MifareClassic.class.getName())) {
                info[1] = "Mifare Classic";
                MifareClassic mifareClassicTag = MifareClassic.get(tag);

                // Type Info
                switch (mifareClassicTag.getType()) {
                    case MifareClassic.TYPE_CLASSIC:
                        type = "Classic";
                        break;
                    case MifareClassic.TYPE_PLUS:
                        type = "Plus";
                        break;
                    case MifareClassic.TYPE_PRO:
                        type = "Pro";
                        break;
                }
                info[0] += "Mifare " + type + "\n";

                // Size Info
                info[0] += "Size: " + mifareClassicTag.getSize() + " bytes \n" +
                        "Sector Count: " + mifareClassicTag.getSectorCount() + "\n" +
                        "Block Count: " + mifareClassicTag.getBlockCount() + "\n";
            } else if (techList[i].equals(MifareUltralight.class.getName())) {
                info[1] = "Mifare UltraLight";
                MifareUltralight mifareUlTag = MifareUltralight.get(tag);

                // Type Info
                switch (mifareUlTag.getType()) {
                    case MifareUltralight.TYPE_ULTRALIGHT:
                        type = "Ultralight";
                        break;
                    case MifareUltralight.TYPE_ULTRALIGHT_C:
                        type = "Ultralight C";
                        break;
                }
                info[0] += "Mifare " + type + "\n";
            } else if (techList[i].equals(IsoDep.class.getName())) {
                info[1] = "IsoDep";
                IsoDep isoDepTag = IsoDep.get(tag);
                info[0] += "IsoDep \n";
            } else if (techList[i].equals(Ndef.class.getName())) {
                Ndef ndefTag = Ndef.get(tag);
                info[0] += "Is Writable: " + ndefTag.isWritable() + "\n" +
                        "Can Make ReadOnly: " + ndefTag.canMakeReadOnly() + "\n";
            } else if (techList[i].equals(NdefFormatable.class.getName())) {
                NdefFormatable ndefFormatableTag = NdefFormatable.get(tag);
            }
        }

        return info;
    }

}
