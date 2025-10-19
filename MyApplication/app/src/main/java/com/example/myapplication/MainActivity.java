package com.example.myapplication;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;

import java.io.InputStream;
import java.nio.FloatBuffer;
import java.util.Collections;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA = 100;
    private static final int REQUEST_GALLERY = 101;
    private static final int IMG_SIZE = 224;

    private ImageView imgPreview;
    private TextView tvResult;
    private EditText etComplaint;
    private Button btnSubmitComplaint, btnGetLocation, btnDashboard, btnStatus, btnProfile;
    private OrtSession session;
    private OrtEnvironment env;

    private ComplainActivity complaintManager;
    private LocationManagerHelper locationHelper;

    private final String[] CLASS_NAMES = {"cardboard", "glass", "metal", "paper", "plastic", "trash"};

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Init OpenCV
        if (!OpenCVLoader.initDebug()) {
            Toast.makeText(this, "OpenCV failed to load", Toast.LENGTH_SHORT).show();
        }

        // Initialize UI elements
        imgPreview = findViewById(R.id.imageView);
        tvResult = findViewById(R.id.tvResult);
        etComplaint = findViewById(R.id.etComplaint);
        btnSubmitComplaint = findViewById(R.id.btnSubmitComplaint);
        Button btnCamera = findViewById(R.id.btnCamera);
        Button btnGallery = findViewById(R.id.btnGallery);
        btnGetLocation = findViewById(R.id.btnGetLocation);
        btnDashboard = findViewById(R.id.btnDashboard);
        btnStatus = findViewById(R.id.btnStatus);
        btnProfile = findViewById(R.id.btnProfile);

        // Initialize managers
        complaintManager = new ComplainActivity(this);
        locationHelper = new LocationManagerHelper(this);

        // Ask location permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            }, 1);
        }

        // Load ONNX model
        try {
            env = OrtEnvironment.getEnvironment();
            session = env.createSession(getAssets().open("model.onnx").readAllBytes(), new OrtSession.SessionOptions());
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "ONNX load failed", Toast.LENGTH_SHORT).show();
        }

        // Camera button
        btnCamera.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA);
            } else {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(intent, REQUEST_CAMERA);
            }
        });

        // Gallery button
        btnGallery.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, REQUEST_GALLERY);
        });

        // Complaint submission
        btnSubmitComplaint.setOnClickListener(v -> {
            String complaint = etComplaint.getText().toString().trim();
            if (!complaint.isEmpty()) {
                double lat = locationHelper.getLatitude();
                double lon = locationHelper.getLongitude();
                String fullComplaint = complaint;

                if (lat != 0.0 && lon != 0.0) {
                    fullComplaint += " | Location: " + lat + ", " + lon;
                } else {
                    fullComplaint += " | Location: Not available";
                }

                complaintManager.saveComplaint(fullComplaint);
                Toast.makeText(MainActivity.this, "Complaint submitted!", Toast.LENGTH_SHORT).show();
                etComplaint.setText("");
            } else {
                Toast.makeText(MainActivity.this, "Please enter a complaint.", Toast.LENGTH_SHORT).show();
            }
        });

        // Location button — open maps
        btnGetLocation.setOnClickListener(v -> {
            locationHelper.requestLocationUpdates();
            double lat = locationHelper.getLatitude();
            double lon = locationHelper.getLongitude();

            if (lat != 0.0 && lon != 0.0) {
                String uri = "geo:" + lat + "," + lon + "?q=" + lat + "," + lon + "(Reported Location)";
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                mapIntent.setPackage("com.google.android.apps.maps");
                startActivity(mapIntent);
            } else {
                Toast.makeText(this, "Fetching location... please wait a few seconds.", Toast.LENGTH_SHORT).show();
            }
        });

        // Navigation buttons
        btnDashboard.setOnClickListener(v -> {
            Intent i = new Intent(MainActivity.this, DashboardActivity.class);
            startActivity(i);
        });

        btnStatus.setOnClickListener(v -> {
            Intent i = new Intent(MainActivity.this, StatusActivity.class);
            startActivity(i);
        });

        btnProfile.setOnClickListener(v -> {
            Intent i = new Intent(MainActivity.this, ProfileActivity.class);
            startActivity(i);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) return;

        Bitmap bitmap = null;
        try {
            if (requestCode == REQUEST_CAMERA) {
                bitmap = (Bitmap) data.getExtras().get("data");
            } else if (requestCode == REQUEST_GALLERY) {
                Uri imageUri = data.getData();
                InputStream is = getContentResolver().openInputStream(imageUri);
                bitmap = BitmapFactory.decodeStream(is);
            }

            if (bitmap != null) {
                imgPreview.setImageBitmap(bitmap);
                Bitmap resized = Bitmap.createScaledBitmap(bitmap, IMG_SIZE, IMG_SIZE, true);
                float[][][][] input = preprocess(resized);
                runModel(input);

                // Save image info for dashboard
                String prediction = tvResult.getText().toString();
                String imageInfo = "Image: " + System.currentTimeMillis() + " | Prediction: " + prediction;
                complaintManager.saveImageInfo(imageInfo);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private float[][][][] preprocess(Bitmap bitmap) {
        Mat mat = new Mat();
        Utils.bitmapToMat(bitmap, mat);
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGBA2RGB);

        float[][][][] input = new float[1][3][IMG_SIZE][IMG_SIZE];
        for (int y = 0; y < IMG_SIZE; y++) {
            for (int x = 0; x < IMG_SIZE; x++) {
                double[] pixel = mat.get(y, x);
                input[0][0][y][x] = (float) ((pixel[0] / 255.0 - 0.485) / 0.229);
                input[0][1][y][x] = (float) ((pixel[1] / 255.0 - 0.456) / 0.224);
                input[0][2][y][x] = (float) ((pixel[2] / 255.0 - 0.406) / 0.225);
            }
        }
        return input;
    }

    private void runModel(float[][][][] input) {
        try {
            OnnxTensor tensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(flatten(input)),
                    new long[]{1, 3, IMG_SIZE, IMG_SIZE});
            OrtSession.Result result = session.run(Collections.singletonMap("input", tensor));
            float[] scores = ((float[][]) result.get(0).getValue())[0];
            int maxIdx = 0;
            for (int i = 1; i < scores.length; i++) {
                if (scores[i] > scores[maxIdx]) maxIdx = i;
            }
            tvResult.setText("Prediction: " + CLASS_NAMES[maxIdx]);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private float[] flatten(float[][][][] input) {
        float[] flat = new float[3 * IMG_SIZE * IMG_SIZE];
        int idx = 0;
        for (int c = 0; c < 3; c++)
            for (int y = 0; y < IMG_SIZE; y++)
                for (int x = 0; x < IMG_SIZE; x++)
                    flat[idx++] = input[0][c][y][x];
        return flat;
    }
}
