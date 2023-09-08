package com.practica.deberscannerqr;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.WriterException;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import java.util.EnumMap;
import java.util.Map;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int CAMERA_PERMISSION_REQUEST = 100;
    private static final int GALLERY_REQUEST = 101;

    private Button btnScanFromCamera;
    private Button btnScanFromGallery;
    private EditText etQRCode;
    private ImageView ivQRCode;

    private boolean isScanningFromCamera = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Establece el título en la Toolbar
        getSupportActionBar().setTitle("Escáner de Código QR");

        btnScanFromCamera = findViewById(R.id.botonScanCamara);
        btnScanFromGallery = findViewById(R.id.botonScanGaleria);
        etQRCode = findViewById(R.id.resultCodigoQR);
        ivQRCode = findViewById(R.id.ImageCodigoQR);

        btnScanFromCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkCameraPermission()) {
                    isScanningFromCamera = true;
                    ivQRCode.setVisibility(View.GONE);
                    startCameraScanner();
                }
            }
        });

        btnScanFromGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isScanningFromCamera = false;
                ivQRCode.setVisibility(View.VISIBLE);
                // Abrir la galería para seleccionar una imagen
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, GALLERY_REQUEST);
            }
        });
    }

    private boolean checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
            return false;
        }
    }

    private void startCameraScanner() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        integrator.setOrientationLocked(true); // Bloquea la orientación en vertical
        integrator.setPrompt("Escanea un código QR");
        integrator.setBeepEnabled(true);
        integrator.initiateScan();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (isScanningFromCamera) {
                    startCameraScanner();
                }
            } else {
                Toast.makeText(this, "No se han concedido los permisos para la cámara", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GALLERY_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            Bitmap bitmap = null;
            try {
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (bitmap != null) {
                ivQRCode.setImageBitmap(bitmap);

                String qrCodeText = scanQRCode(bitmap);
                if (qrCodeText != null) {
                    etQRCode.setText(qrCodeText);
                } else {
                    etQRCode.setText("No se pudo detectar un código QR");
                }
            }
        } else if (requestCode == IntentIntegrator.REQUEST_CODE && resultCode == RESULT_OK) {
            // Cuando se escanea un código QR desde la cámara
            IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
            if (result != null) {
                String qrCodeText = result.getContents();
                if (qrCodeText != null) {
                    etQRCode.setText(qrCodeText); // Actualiza el EditText con el resultado

                    // Muestra el código QR en el ImageView
                    try {
                        Bitmap qrCodeBitmap = generateQRCode(qrCodeText); // Llama a un método para generar el QR (más detalles a continuación)
                        ivQRCode.setVisibility(View.VISIBLE); // Asegura que el ImageView sea visible
                        ivQRCode.setImageBitmap(qrCodeBitmap);
                    } catch (WriterException e) {
                        e.printStackTrace();
                    }
                } else {
                    etQRCode.setText("No se pudo detectar un código QR");
                }
            }
        }
    }

    private Bitmap generateQRCode(String text) throws WriterException {
        // Configurar la generación del código QR
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);

        BitMatrix bitMatrix = new MultiFormatWriter().encode(
                text, BarcodeFormat.QR_CODE, 512, 512, hints);

        int width = bitMatrix.getWidth();
        int height = bitMatrix.getHeight();
        int[] pixels = new int[width * height];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixels[y * width + x] = bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF;
            }
        }

        Bitmap qrCodeBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        qrCodeBitmap.setPixels(pixels, 0, width, 0, 0, width, height);

        return qrCodeBitmap;
    }



@Nullable
    private String scanQRCode(Bitmap bitmap) {
        try {
            int[] intArray = new int[bitmap.getWidth() * bitmap.getHeight()];
            bitmap.getPixels(intArray, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
            RGBLuminanceSource source = new RGBLuminanceSource(bitmap.getWidth(), bitmap.getHeight(), intArray);
            HybridBinarizer binarizer = new HybridBinarizer(source);
            BinaryBitmap binaryBitmap = new BinaryBitmap(binarizer);

            Result result = new QRCodeReader().decode(binaryBitmap);
            return result.getText();
        } catch (Exception e) {
            Log.e(TAG, "Error al escanear el código QR", e);
            return null;
        }
    }
}