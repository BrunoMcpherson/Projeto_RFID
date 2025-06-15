package com.example.aplicacao_rfid;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

public class MainActivity extends AppCompatActivity {

    //CONSTANTES
    private static final int REQUEST_LOCATION_PERMISSION = 1;
    private static final String TAG = "MainActivity_NFC";

    // VARIAVEIS DE INSTANCIA
    private Button id_objeto, id_etiqueta, id_instrucao;
    private FusedLocationProviderClient fusedLocationClient;
    private NfcAdapter nfcAdapter;
    private PendingIntent nfcPendingIntent;
    private boolean nfcSetupAttempted = false;
    private boolean nfcProcessing = false;

    //CICLO DE VIDA
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        id_objeto = findViewById(R.id.id_objeto);
        id_etiqueta = findViewById(R.id.id_etiqueta);
        id_instrucao = findViewById(R.id.id_instrucao);

        // Inicializa o cliente de localização
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Configuração do NFC
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter == null) {
            Toast.makeText(this, "Seu dispositivo não suporta NFC", Toast.LENGTH_LONG).show();
        } else if (!nfcAdapter.isEnabled()) {
            showNfcSettingsDialog();
        } else {
            setupNfc();
        }

        id_etiqueta.setOnClickListener(v -> {
            checkLocationPermission();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!nfcSetupAttempted) {
            checkNfcSupport();
        } else {
            // Verifica se o NFC foi desativado enquanto o app estava em background
            if (nfcAdapter != null && !nfcAdapter.isEnabled()) {
                showEnableNfcDialog();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            if (nfcAdapter != null) {
                nfcAdapter.disableForegroundDispatch(this);
            }
        } catch (Exception e) {
            Log.w(TAG, "Erro ao desabilitar NFC", e);
        }
    }

    //METODOS UI
    private void showNfcSettingsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("NFC Desativado")
                .setMessage("Por favor, ative o NFC para usar esta funcionalidade")
                .setPositiveButton("Configurações", (dialog, which) -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        startActivity(new Intent(Settings.ACTION_NFC_SETTINGS));
                    } else {
                        startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
                    }
                })
                .setNegativeButton("Continuar", (dialog, which) -> {
                    Toast.makeText(this, "Algumas funcionalidades estarão limitadas", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void showNfcErrorDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle("NFC Indisponível")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .setCancelable(false)
                .show();
    }

    private void showEnableNfcDialog() {
        new AlertDialog.Builder(this)
                .setTitle("NFC Desativado")
                .setMessage("Para usar o RFID, ative o NFC nas configurações")
                .setPositiveButton("Abrir Configurações", (d, w) -> {
                    startActivity(new Intent(Settings.ACTION_NFC_SETTINGS));
                })
                .setNegativeButton("Continuar sem NFC", (d, w) -> {
                    Toast.makeText(this, "Funcionalidades limitadas sem NFC", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void showNfcErrorDialog(String message, boolean finishActivity) {
        new AlertDialog.Builder(this)
                .setTitle("NFC Indisponível")
                .setMessage(message)
                .setPositiveButton("OK", (d, w) -> {
                    if (finishActivity) {
                        finish();
                    }
                })
                .setCancelable(false)
                .show();
    }

    private void showGpsDisabledDialog() {
        new AlertDialog.Builder(this)
                .setTitle("GPS desativado")
                .setMessage("Ative o GPS para maior precisão")
                .setPositiveButton("Configurações", (d, w) ->
                        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)))
                .setNegativeButton("Continuar", null)
                .show();
    }

    //METODOS NFC
    private void checkNfcSupport() {
        if (nfcProcessing) return; // Evita múltiplas chamadas simultâneas
        nfcProcessing = true;

        try {
            nfcAdapter = NfcAdapter.getDefaultAdapter(this);

            if (nfcAdapter == null) {
                if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_NFC)) {
                    Log.w(TAG, "NFC reportado mas adaptador nulo - possivel bug do dispositivo");
                    showNfcErrorDialog("Erro no hardware NFC", false);
                } else {
                    showNfcErrorDialog("NFC não disponível", true);
                }
                return;
            }

            if (!nfcAdapter.isEnabled()) {
                showEnableNfcDialog();
                return;
            }

            if (!nfcSetupAttempted) {
                setupNfc();
                nfcSetupAttempted = true;
            }

        } catch (SecurityException se) {
            Log.e(TAG, "Erro de permissão NFC", se);
            showNfcErrorDialog("Permissão NFC negada", false);
        } catch (Exception e) {
            Log.e(TAG, "Erro inesperado NFC", e);
            showNfcErrorDialog("Falha no NFC", false);
        } finally {
            nfcProcessing = false;
        }
    }

    private void setupNfc() {
        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                // 1. Configura PendingIntent
                Intent nfcIntent = new Intent(this, getClass())
                        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

                int flags = PendingIntent.FLAG_UPDATE_CURRENT;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    flags |= PendingIntent.FLAG_IMMUTABLE;
                }

                nfcPendingIntent = PendingIntent.getActivity(
                        this, 0, nfcIntent, flags);

                // 2. Configura filters para tipos de tags específicos
                IntentFilter[] intentFilters = new IntentFilter[] {
                        new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED),
                        new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)
                };

                // 3. Habilita foreground dispatch
                nfcAdapter.enableForegroundDispatch(
                        this,
                        nfcPendingIntent,
                        intentFilters,
                        null);

                Log.i(TAG, "NFC configurado com sucesso");
                Toast.makeText(this, "NFC configurado", Toast.LENGTH_SHORT).show();

            } catch (Exception e) {
                Log.e(TAG, "Falha na configuração NFC", e);
                Toast.makeText(this, "Falha na configuração NFC", Toast.LENGTH_SHORT).show();
            }
        });
    }

    //METODOS DE LOCALIZACAO
    //Checar permissão de GPS
    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            if (checkGpsEnabled()) {
                navigateToLocalizacao();
            }
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
        }
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                REQUEST_LOCATION_PERMISSION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (checkGpsEnabled()) {
                    navigateToLocalizacao();
                }
            } else {
                Toast.makeText(this, "Permissão de localização negada", Toast.LENGTH_SHORT).show();
            }
        }
    }

    //Verificação GPS
    private boolean checkGpsEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            Toast.makeText(this, "Serviço de localização indisponível", Toast.LENGTH_SHORT).show();
            return false;
        }

        boolean gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (!gpsEnabled) {
            showGpsDisabledDialog();
        }
        return gpsEnabled;
    }

    //METODOS AUXILIARES
    private void navigateToLocalizacao() {
        if (nfcAdapter != null) {
            nfcAdapter.disableForegroundDispatch(this);
        }
        startActivity(new Intent(this, Localizacao.class));
    }

    //METODOS DE DEBUG
    private void logNfcInfo() {
        Log.d(TAG, "NFC Feature: " + getPackageManager().hasSystemFeature(PackageManager.FEATURE_NFC));
        Log.d(TAG, "NFC Adapter: " + (nfcAdapter != null));
        if (nfcAdapter != null) {
            Log.d(TAG, "NFC Enabled: " + nfcAdapter.isEnabled());
        }
    }

}// FIM DA CLASSE