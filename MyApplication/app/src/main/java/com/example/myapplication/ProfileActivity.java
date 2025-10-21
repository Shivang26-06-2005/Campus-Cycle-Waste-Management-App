package com.example.myapplication;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
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

public class ProfileActivity extends AppCompatActivity {

    private TextView tvProfile;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener; // Add listener
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        tvProfile = findViewById(R.id.tvProfileInfo);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        Button btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // Create the AuthStateListener
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in, load the profile.
                    loadUserProfile(user);
                } else {
                    // User is signed out.
                    tvProfile.setText("Please log in to view your profile.");
                }
            }
        };
    }

    private void loadUserProfile(FirebaseUser currentUser) { // Pass the user in
        String userId = currentUser.getUid();
        String userEmail = currentUser.getEmail();
        String username = "User";

        if (userEmail != null && userEmail.contains("@")) {
            username = userEmail.split("@")[0];
        }

        String finalUsername = username;
        tvProfile.setText("User: " + finalUsername + "\nTotal Reports: Fetching...");

        Query userComplaintsQuery = mDatabase.child("complaints").orderByChild("userId").equalTo(userId);
        userComplaintsQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                long reportCount = dataSnapshot.getChildrenCount();
                tvProfile.setText("User: " + finalUsername + "\nTotal Reports: " + reportCount);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                tvProfile.setText("User: " + finalUsername + "\nTotal Reports: Error");
            }
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

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
