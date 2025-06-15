package com.example.aplicacao_rfid;

import android.content.Intent;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class NfcHandlerActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Passa a intent para a Localizacao activity
        Intent intent = new Intent(this, Localizacao.class);
        intent.putExtras(getIntent().getExtras());
        startActivity(intent);
        finish(); // Fecha esta activity
    }
}