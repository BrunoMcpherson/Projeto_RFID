package com.example.aplicacao_rfid;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

public class Localizacao extends AppCompatActivity {

    //CONSTANTES
    private static final int REQUEST_PERMISSIONS_CODE = 101;
    private static final String TAG = "NfcLocationApp";

    //COMPONENTES UI
    private TextView tvLatitude, tvLongitude, tvStatus;
    private Button btnSave;

    //SERVIÇOS
    private NfcAdapter nfcAdapter;
    private FusedLocationProviderClient fusedLocationClient;

    //VARIAVEIS
    private double currentLatitude = 0;
    private double currentLongitude = 0;
    private String currentTagId = "";
    private boolean locationCaptured = false;

    //CICLO DE VIDA
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_localizacao);

        // Inicializa views
        tvLatitude = findViewById(R.id.tvLatitude);
        tvLongitude = findViewById(R.id.tvLongitude);
        tvStatus = findViewById(R.id.tvStatus);
        btnSave = findViewById(R.id.btnSave);

        // Configura serviços
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (nfcAdapter != null) {
            setupNfc();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (nfcAdapter != null) {
            nfcAdapter.disableForegroundDispatch(this);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            if (tag != null) {
                handleNfcTag(tag);
                vibrateDevice(); // Chamada segura
            }
        }
    }

    //METODOS NFC
    private void setupNfc() {
        try {
            Intent intent = new Intent(this, getClass())
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }

            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, flags);

            // Habilita detecção de tags NFC
            nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);

        } catch (Exception e) {
            tvStatus.setText("Erro na configuração NFC");
            Log.e("NFC_ERROR", "Configuração NFC falhou", e);
        }
    }

    private void handleNfcIntent(Intent intent) {
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            if (tag != null) {
                handleNfcTag(tag);
                vibrateDevice();
            }
        }
    }

    private void handleNfcTag(Tag tag) {
        runOnUiThread(() -> {
            try {
                currentTagId = bytesToHex(tag.getId());
                tvStatus.setText("Tag detectada: " + currentTagId);

                // Verifica permissões antes de obter localização
                if (checkLocationPermission()) {
                    getCurrentLocation();
                }

            } catch (Exception e) {
                Log.e(TAG, "Erro no processamento da tag NFC", e);
                tvStatus.setText("Erro na leitura da tag");
            }
        });
    }

    //METODOS DE LOCALIZACAO
    private void getCurrentLocation() {
        if (checkLocationPermission()) {
            tvStatus.setText("Obtendo localização...");

            LocationRequest locationRequest = new LocationRequest.Builder(
                    Priority.PRIORITY_HIGH_ACCURACY, 5000)
                    .setMinUpdateIntervalMillis(3000)
                    .build();

            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            updateLocationUI(location);
                        } else {
                            requestNewLocation(locationRequest);
                        }
                    });
        }
    }

    private void requestNewLocation(LocationRequest locationRequest) {
        LocationCallback locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null) {
                    updateLocationUI(locationResult.getLastLocation());
                    fusedLocationClient.removeLocationUpdates(this);
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
        );
    }

    //PERMISSOES

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_PERMISSIONS_CODE) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                tvStatus.setText("Permissão de localização negada");
            }
        }
    }

    private boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSIONS_CODE);
            return false;
        }
        return true;
    }

    //METODOS UI
    private void updateLocationUI(Location location) {
        currentLatitude = location.getLatitude();
        currentLongitude = location.getLongitude();
        locationCaptured = true;

        runOnUiThread(() -> {
            tvLatitude.setText(String.format("Latitude: %.6f", currentLatitude));
            tvLongitude.setText(String.format("Longitude: %.6f", currentLongitude));
            tvStatus.setText("Localização obtida!");
            btnSave.setEnabled(true);
        });
    }
    private void configureSaveButton() {
        btnSave.setOnClickListener(v -> {
            if (locationCaptured && !currentTagId.isEmpty()) {
                saveData();
            } else {
                Toast.makeText(this, "Leia uma tag NFC primeiro", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void saveData() {
        // Implemente sua lógica de salvamento aqui
        Toast.makeText(this, "Dados salvos com sucesso!", Toast.LENGTH_SHORT).show();

        // Reset para nova leitura
        currentTagId = "";
        locationCaptured = false;
        btnSave.setEnabled(false);
        tvStatus.setText("Aproxime uma nova tag NFC");
    }

    //METODOS DE UTILIZACAO
    private void vibrateDevice() {
        // 1. Verifica se o dispositivo tem hardware de vibração
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vibrator == null || !vibrator.hasVibrator()) {
            Log.w(TAG, "Dispositivo não possui vibrador");
            return;
        }

        // 2. Verifica explicitamente a permissão
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.VIBRATE)
                != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Permissão VIBRATE não concedida");
            return;
        }

        // 3. Executa com tratamento de exceção
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(200,
                        VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                // Método depreciado mas necessário para versões antigas
                vibrator.vibrate(200);
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Falha de segurança ao vibrar", e);
            Toast.makeText(this, "Erro: permissão de vibração negada", Toast.LENGTH_SHORT).show();
        }
    }
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}//FIM