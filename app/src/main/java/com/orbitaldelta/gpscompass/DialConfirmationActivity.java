package com.orbitaldelta.gpscompass;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

public class DialConfirmationActivity extends Activity {
    static final String EXTRA_NUMBER = "number";
    static final String EXTRA_LABEL = "label";
    private boolean finished;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String number = getIntent().getStringExtra(EXTRA_NUMBER);
        String label = getIntent().getStringExtra(EXTRA_LABEL);
        if (number == null || label == null) {
            finish();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(label)
                .setMessage("Abrir o marcador telefónico com " + number + "? A chamada não será iniciada automaticamente.")
                .setNegativeButton("Cancelar", (dialog, which) -> finish())
                .setPositiveButton("Abrir marcador", (dialog, which) -> openDialer(number))
                .setOnCancelListener(dialog -> finish())
                .show();
    }

    private void openDialer(String number) {
        Intent dialIntent = new Intent(WidgetActionSpec.ACTION_DIAL, Uri.parse(WidgetActionSpec.dialUri(number)));
        try {
            startActivity(dialIntent);
            finished = true;
            finish();
        } catch (ActivityNotFoundException | SecurityException e) {
            showNoDialerWarning();
        }
    }

    private void showNoDialerWarning() {
        new AlertDialog.Builder(this)
                .setTitle("Marcador telefónico indisponível")
                .setMessage("Não existe uma aplicação compatível para abrir o marcador. Pode copiar as coordenadas no botão do widget.")
                .setNegativeButton("Fechar", (dialog, which) -> finish())
                .setPositiveButton("Copiar coordenadas", (dialog, which) -> copyCoordinates())
                .setOnCancelListener(dialog -> finish())
                .show();
    }

    private void copyCoordinates() {
        String text = CoordinateText.fromPreferences(this);
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (clipboard != null) clipboard.setPrimaryClip(ClipData.newPlainText("Coordenadas GPS", text));
        Toast.makeText(this, "Coordenadas copiadas", Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public void onBackPressed() {
        if (!finished) finish();
        super.onBackPressed();
    }
}
