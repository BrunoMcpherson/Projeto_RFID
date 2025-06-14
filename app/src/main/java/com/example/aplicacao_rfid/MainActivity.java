package com.example.aplicacao_rfid;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {
    private Button id_objeto, id_etiqueta, id_instrucao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        id_objeto = findViewById(R.id.id_objeto);
        id_etiqueta = findViewById(R.id.id_etiqueta);
        id_instrucao = findViewById(R.id.id_instrucao);

    }
}