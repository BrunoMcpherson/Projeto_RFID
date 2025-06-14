package com.example.aplicacao_rfid;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Parcelable;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import java.nio.charset.StandardCharsets;

public class Localizacao extends MainActivity {
    private NfcAdapter nfcAdapter;
    private FusedLocationProviderClient fusedLocationClient;
    private TextView lastLocationText;
    private SharedPreferences sharedPreferences;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        lastLocationText = findViewById(R.id.lastLocationText);
        sharedPreferences = getSharedPreferences("RFIDLocations", MODE_PRIVATE);

        // Verifica se o dispositivo suporta NFC
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter == null) {
            Toast.makeText(this, "Dispositivo não suporta NFC", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Configura cliente de localização
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Verifica permissões de localização
        checkLocationPermission();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Habilita a detecção de NFC em foreground
        if (nfcAdapter != null) {
            nfcAdapter.enableForegroundDispatch(this,
                    PendingIntent.getActivity(this, 0,
                            new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0),
                    null, null);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Desabilita a detecção de NFC quando o app está em background
        if (nfcAdapter != null) {
            nfcAdapter.disableForegroundDispatch(this);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Quando uma tag NFC é detectada
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            processNfcIntent(intent);
        }
    }

    private void processNfcIntent(Intent intent) {
        Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        if (rawMsgs != null) {
            NdefMessage[] msgs = new NdefMessage[rawMsgs.length];
            for (int i = 0; i < rawMsgs.length; i++) {
                msgs[i] = (NdefMessage) rawMsgs[i];
            }

            // Extrai o ID da tag RFID
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            String tagId = bytesToHexString(tag.getId());

            // Obtém a localização atual
            getCurrentLocation(tagId);
        }
    }

    private void getCurrentLocation(final String tagId) {
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                // Armazena a localização no SharedPreferences
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putString(tagId + "_lat", String.valueOf(location.getLatitude()));
                                editor.putString(tagId + "_lng", String.valueOf(location.getLongitude()));
                                editor.putLong(tagId + "_time", System.currentTimeMillis());
                                editor.apply();

                                // Exibe a última localização
                                displayLastLocation(tagId);

                                Toast.makeText(Localizacao.this,
                                        "Localização registrada para o objeto " + tagId,
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(Localizacao.this,
                                        "Não foi possível obter a localização",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
    }

    private void displayLastLocation(String tagId) {
        String lat = sharedPreferences.getString(tagId + "_lat", null);
        String lng = sharedPreferences.getString(tagId + "_lng", null);
        long time = sharedPreferences.getLong(tagId + "_time", 0);

        if (lat != null && lng != null) {
            String locationText = "Última localização do objeto " + tagId + ":\n" +
                    "Latitude: " + lat + "\n" +
                    "Longitude: " + lng + "\n" +
                    "Registrado em: " + new java.util.Date(time).toString();

            lastLocationText.setText(locationText);
        } else {
            lastLocationText.setText("Nenhuma localização registrada para este objeto");
        }
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    // Converte bytes do ID da tag para string hexadecimal
    private String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder("0x");
        if (src == null || src.length <= 0) {
            return null;
        }

        char[] buffer = new char[2];
        for (byte b : src) {
            buffer[0] = Character.forDigit((b >>> 4) & 0x0F, 16);
            buffer[1] = Character.forDigit(b & 0x0F, 16);
            stringBuilder.append(buffer);
        }

        return stringBuilder.toString();
    }
}