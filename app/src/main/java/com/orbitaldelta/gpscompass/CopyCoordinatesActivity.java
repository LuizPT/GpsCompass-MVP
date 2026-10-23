package com.orbitaldelta.gpscompass;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.os.Bundle;
import android.widget.Toast;

public class CopyCoordinatesActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (clipboard != null) clipboard.setPrimaryClip(ClipData.newPlainText("Coordenadas GPS", CoordinateText.fromPreferences(this)));
        Toast.makeText(this, "Coordenadas GPS copiadas", Toast.LENGTH_SHORT).show();
        finish();
    }
}
