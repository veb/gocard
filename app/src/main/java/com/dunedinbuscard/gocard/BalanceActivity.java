package com.dunedinbuscard.gocard;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.NfcA;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.text.NumberFormat;

public class BalanceActivity extends Activity {

    TextView label;
    TextView label2;
    TextView label3;
    TextView label4;
    TextView balance_low;
    ImageView balance_low_sign;
    IntentFilter[] filters;
    String[][] techs;
    PendingIntent pendingIntent;
    NfcAdapter adapter;

    public static String TAG = "Balance";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_balance);
        label = (TextView) findViewById(R.id.balance);
        label2 = (TextView) findViewById(R.id.you_have);
        label3 = (TextView) findViewById(R.id.on_your_card);
        label4 = (TextView) findViewById(R.id.card_id);
        balance_low = (TextView) findViewById(R.id.balance_low);
        balance_low_sign = (ImageView) findViewById(R.id.balance_low_sign);
        pendingIntent = PendingIntent.getActivity(
                this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        IntentFilter mifare = new IntentFilter((NfcAdapter.ACTION_TECH_DISCOVERED));
        filters = new IntentFilter[]{mifare};
        techs = new String[][]{new String[]{NfcA.class.getName()}};
        adapter = NfcAdapter.getDefaultAdapter(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        adapter.disableForegroundDispatch(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        adapter.enableForegroundDispatch(this, pendingIntent, filters, techs);
        if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(getIntent().getAction())) {
            Tag tag = getIntent().getParcelableExtra(NfcAdapter.EXTRA_TAG);
            DisplayBalance(tag);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void onNewIntent(Intent intent) {
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        DisplayBalance(tag);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu my_menu) {
        MenuInflater menuInflater = new MenuInflater(this);
        menuInflater.inflate(R.menu.balance, my_menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_about:
                Intent intent = new Intent(this, AboutActivity.class);
                startActivity(intent);
                break;
            default:
                break;
        }

        return true;
    }

    /**
     * Displays the balance of a GoCard
     *
     * @param tag
     */
    public void DisplayBalance(Tag tag) {
        MifareClassic mfc = MifareClassic.get(tag);
        byte[] data;
        boolean auth = false;
        byte[] balance = new byte[]{};
        NumberFormat fmt = NumberFormat.getCurrencyInstance();

        try {
            // Connect if not already connected
            if (!mfc.isConnected()) {
                mfc.connect();
            }
            // Authenticate the first sector. If it fails it's not a GoCard
            auth = mfc.authenticateSectorWithKeyA(0, GoCard.BIT_AUTH_KEY);
            if (auth) {
                GoCard item = new GoCard();

                // Check the flag to see where the balance is
                String cardBalanceBit = bytesToHex(mfc.readBlock(1));
                char balanceStorage = cardBalanceBit.charAt(10);

                if (balanceStorage == '1') {
                    // Current balance at offset 168
                    mfc.authenticateSectorWithKeyA(5, GoCard.BALANCE_168_AUTH_KEY);
                    data = mfc.readBlock(22);
                    balance = new byte[]{data[8], data[9]};
                } else {
                    // Current balance at offset 68
                    mfc.authenticateSectorWithKeyA(1, GoCard.BALANCE_068_AUTH_KEY);
                    data = mfc.readBlock(6);
                    balance = new byte[]{data[8], data[9]};
                }

                double currentBalance = GoCard.ProcessGoCardAmountHex(balance);
                label.setText(fmt.format(currentBalance));
                item.currentBalance = currentBalance;

                if(currentBalance < 5) {
                    balance_low.setVisibility(View.VISIBLE);
                    balance_low_sign.setVisibility(View.VISIBLE);
                } else {
                    balance_low.setVisibility(View.INVISIBLE);
                    balance_low_sign.setVisibility(View.INVISIBLE);
                }

                // Read the card ID, reauthenticating if need be
                mfc.authenticateSectorWithKeyA(1, GoCard.BALANCE_068_AUTH_KEY);
                String cardIDRow = bytesToHex(mfc.readBlock(4));
                label4.setText(String.format("Card ID: %s", cardIDRow.substring(10, cardIDRow.length() - 16).replaceFirst("^0+(?!$)", "")));
                item.printedID = cardIDRow.substring(10, cardIDRow.length() - 16);

                label2.setVisibility(View.VISIBLE);
                label3.setVisibility(View.VISIBLE);
                label4.setVisibility(View.VISIBLE);
            } else {
                Toast.makeText(getApplicationContext(), "Unable to read your GoCard.", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), "Unable to read your GoCard.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error: " + e.getLocalizedMessage());
        }
    }

    /**
     * Converts a byte array to hex string
     *
     * @param in
     * @return Hex string
     */
    public static String bytesToHex(byte[] in) {
        final StringBuilder builder = new StringBuilder();
        for (byte b : in) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }
}
