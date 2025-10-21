package com.example.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.InputStream;
import java.nio.FloatBuffer;
import java.util.Collections;
import java.util.Objects;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;

public class MainActivity extends AppCompatActivity {
    // Request codes
    private static final int REQUEST_CAMERA = 101;
    private static final int REQUEST_GALLERY = 102;
    private static final int REQUEST_LOCATION_PERMISSION = 1;
    private static final int REQUEST_CAMERA_PERMISSION = 100;
    private static final int IMG_SIZE = 224;

    // UI Views
    private ImageView imageView;
    private TextView tvPrediction;
    private EditText etComplaint;

    // --- Add Firebase Auth and Listener ---
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    // Helper classes and ONNX Runtime
    private ComplainActivity complaintManager;
    private LocationManagerHelper locationHelper;
    private OrtEnvironment env;
    private OrtSession session;

    // Model Labels
    private final String[] CLASS_NAMES = {"Cardboard", "Glass", "Metal", "Paper", "Plastic", "Trash"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // --- Initialize Firebase Auth ---
        mAuth = FirebaseAuth.getInstance();

        if (!OpenCVLoader.initDebug()) {
            Toast.makeText(this, "OpenCV initialization failed!", Toast.LENGTH_LONG).show();
        }

        // Initialize UI components
        imageView = findViewById(R.id.imageView);
        tvPrediction = findViewById(R.id.tvResult);
        etComplaint = findViewById(R.id.etComplaint);
        Button btnCamera = findViewById(R.id.btnCamera);
        Button btnGallery = findViewById(R.id.btnGallery);
        Button btnSubmit = findViewById(R.id.btnSubmitComplaint);
        Button btnGetLocation = findViewById(R.id.btnGetLocation);
        Button btnDashboard = findViewById(R.id.btnDashboard);
        Button btnStatus = findViewById(R.id.btnStatus);
        Button btnProfile = findViewById(R.id.btnProfile);

        // Initialize helper classes
        complaintManager = new ComplainActivity(this);
        locationHelper = new LocationManagerHelper(this);

        try {
            env = OrtEnvironment.getEnvironment();
            session = env.createSession(getAssets().open("model.onnx").readAllBytes(), new OrtSession.SessionOptions());
        } catch (Exception e) {
            Toast.makeText(this, "Failed to load ONNX model!", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }

        requestLocationPermission();

        // --- Create the AuthStateListener ---
        mAuthListener = firebaseAuth -> {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            if (user == null) {
                // User is signed out. For safety, you could disable submission.
                // Or simply rely on the check inside saveComplaint().
                // This is a good place to handle logic if the user gets signed out while on this screen.
                Toast.makeText(MainActivity.this, "User session not found.", Toast.LENGTH_SHORT).show();
            }
            // No action needed if user is signed in, the button click will handle it.
        };


        // --- Set OnClick Listeners ---
        btnCamera.setOnClickListener(v -> checkCameraPermissionAndOpen());
        btnGallery.setOnClickListener(v -> openGallery());
        btnSubmit.setOnClickListener(v -> submitComplaint());
        btnGetLocation.setOnClickListener(v -> openLocationInMaps());

        btnDashboard.setOnClickListener(v -> startActivity(new Intent(this, DashboardActivity.class)));
        btnStatus.setOnClickListener(v -> startActivity(new Intent(this, StatusActivity.class)));
        btnProfile.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
    }

    private void submitComplaint() {
        // --- Added check for login status before submission ---
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please wait, session is verifying...", Toast.LENGTH_SHORT).show();
            return;
        }

        String complaintText = etComplaint.getText().toString().trim();
        String prediction = tvPrediction.getText().toString();

        if (TextUtils.isEmpty(complaintText) || imageView.getDrawable() == null) {
            Toast.makeText(this, "Please capture an image and write a complaint.", Toast.LENGTH_SHORT).show();
            return;
        }

        double lat = locationHelper.getLatitude();
        double lon = locationHelper.getLongitude();
        String locationInfo = (lat != 0.0 && lon != 0.0) ? "Location: " + lat + ", " + lon : "Location: Not available";
        String combinedInfo = prediction + " | " + locationInfo;

        complaintManager.saveComplaint(complaintText, combinedInfo);

        etComplaint.setText("");
        tvPrediction.setText("Prediction will appear here");
        imageView.setImageResource(android.R.color.transparent);
    }

    // --- ATTACH AND DETACH THE LISTENER ---
    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    // The rest of your MainActivity methods remain unchanged...
    // onActivityResult, preprocess, runModel, flatten, permission methods, onDestroy, etc.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) return;

        Bitmap bitmap = null;
        try {
            if (requestCode == REQUEST_CAMERA) {
                bitmap = (Bitmap) Objects.requireNonNull(data.getExtras()).get("data");
            } else if (requestCode == REQUEST_GALLERY) {
                Uri imageUri = data.getData();
                if (imageUri != null) {
                    InputStream is = getContentResolver().openInputStream(imageUri);
                    bitmap = BitmapFactory.decodeStream(is);
                }
            }

            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
                Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, IMG_SIZE, IMG_SIZE, true);
                float[][][][] preprocessedInput = preprocess(resizedBitmap);
                runModel(preprocessedInput);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show();
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
        if (session == null || env == null) {
            Toast.makeText(this, "Model is not loaded!", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            OnnxTensor tensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(flatten(input)),
                    new long[]{1, 3, IMG_SIZE, IMG_SIZE});
            OrtSession.Result result = session.run(Collections.singletonMap("input", tensor));
            float[] scores = ((float[][]) result.get(0).getValue())[0];
            int maxIdx = 0;
            for (int i = 1; i < scores.length; i++) {
                if (scores[i] > scores[maxIdx]) maxIdx = i;
            }
            tvPrediction.setText("Prediction: " + CLASS_NAMES[maxIdx]);
        } catch (Exception e) {
            e.printStackTrace();
            tvPrediction.setText("Prediction: Error");
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

    private void openLocationInMaps() {
        locationHelper.requestLocationUpdates();
        double lat = locationHelper.getLatitude();
        double lon = locationHelper.getLongitude();
        if (lat != 0.0 && lon != 0.0) {
            String uri = "geo:" + lat + "," + lon + "?q=" + lat + "," + lon + "(Reported Location)";
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            mapIntent.setPackage("com.google.android.apps.maps");
            if (mapIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(mapIntent);
            } else {
                Toast.makeText(this, "Google Maps is not installed.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Fetching location... please wait and try again.", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            openCamera();
        }
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, REQUEST_CAMERA);
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_GALLERY);
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION
            }, REQUEST_LOCATION_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Camera permission is required.", Toast.LENGTH_SHORT).show();
            }
        }
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                locationHelper.requestLocationUpdates();
            } else {
                Toast.makeText(this, "Location permission is required for full functionality.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (session != null) {
                session.close();
            }
            if (env != null) {
                env.close();
            }
        } catch (OrtException e) {
            e.printStackTrace();
        }
    }
}
