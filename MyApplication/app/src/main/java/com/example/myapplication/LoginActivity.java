package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log; // Import Log for better debugging
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername;
    private EditText etPassword;
    private Button btnLogin, btnSignUp;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private boolean isNavigating = false;
    private static final String TAG = "LoginActivity"; // A tag for logging errors

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnSignUp = findViewById(R.id.btnSignUp);

        mAuthListener = firebaseAuth -> {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            if (user != null && !isNavigating) {
                isNavigating = true;
                Toast.makeText(LoginActivity.this, "Login Successful!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
                finishAffinity();
            }
        };

        btnLogin.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
                Toast.makeText(LoginActivity.this, "Username and password cannot be empty.", Toast.LENGTH_SHORT).show();
                return;
            }
            loginWithUsername(username, password);
        });

        btnSignUp.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
    }

    private void loginWithUsername(final String username, final String password) {
        Query query = mDatabase.child("users").orderByChild("username").equalTo(username);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String email = "";
                    for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                        email = userSnapshot.child("email").getValue(String.class);
                    }

                    if (TextUtils.isEmpty(email)) {
                        Toast.makeText(LoginActivity.this, "Error: User found but email is missing.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    mAuth.signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener(task -> {
                                if (!task.isSuccessful()) {
                                    // Give a more detailed error message if possible
                                    String error = task.getException() != null ? task.getException().getMessage() : "Incorrect password.";
                                    Log.e(TAG, "Authentication failed: " + error);
                                    Toast.makeText(LoginActivity.this, "Authentication failed: " + error, Toast.LENGTH_LONG).show();
                                }
                                // On success, the mAuthListener will handle it.
                            });
                } else {
                    Toast.makeText(LoginActivity.this, "Authentication failed: User not found.", Toast.LENGTH_SHORT).show();
                }
            }

            // =================================================================
            // ========= THIS IS THE FIX TO PREVENT THE APP FROM CRASHING ======
            // =================================================================
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Add a check to make sure databaseError is not null before using it.
                // This makes the code safe and prevents the crash.
                String errorMessage = "Database query was cancelled.";
                if (databaseError != null && databaseError.getMessage() != null) {
                    errorMessage = databaseError.getMessage();
                }
                Log.e(TAG, "Database Error: " + errorMessage);
                Toast.makeText(LoginActivity.this, "Database error: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
            // =================================================================
            // ======================= END OF FIX ==============================
            // =================================================================
        });
    }

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
}
