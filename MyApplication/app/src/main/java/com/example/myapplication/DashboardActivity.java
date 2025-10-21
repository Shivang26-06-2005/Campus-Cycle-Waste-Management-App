package com.example.myapplication;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseError;
import java.util.ArrayList;
import java.util.List;

public class DashboardActivity extends AppCompatActivity {

    private ArrayAdapter<String> adapter;
    private final List<String> complaintDisplayList = new ArrayList<>();
    private ComplainActivity complaintManager;

    // --- Add Firebase Auth and a Listener ---
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_status);

        // --- Initialize Firebase Auth ---
        mAuth = FirebaseAuth.getInstance();

        complaintManager = new ComplainActivity(this);
        ListView listView = findViewById(R.id.listComplaints);
        Button btnBack = findViewById(R.id.btnBack);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, complaintDisplayList);
        listView.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // --- Create the AuthStateListener ---
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in, NOW it is safe to load data.
                    loadComplaintsFromFirebase();
                } else {
                    // User is signed out. Clear the list and show a message.
                    complaintDisplayList.clear();
                    adapter.notifyDataSetChanged();
                    Toast.makeText(DashboardActivity.this, "Please log in to view complaints.", Toast.LENGTH_SHORT).show();
                }
            }
        };
    }

    private void loadComplaintsFromFirebase() {
        complaintManager.getAllComplaints(new ComplainActivity.FirebaseDataListener<List<ComplainActivity.Complaint>>() {
            @Override
            public void onDataReceived(List<ComplainActivity.Complaint> complaints) {
                if (complaints.isEmpty()) {
                    Toast.makeText(DashboardActivity.this, "No complaints have been submitted yet.", Toast.LENGTH_SHORT).show();
                }
                complaintDisplayList.clear();
                for (ComplainActivity.Complaint complaint : complaints) {
                    complaintDisplayList.add(complaint.toString());
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onError(DatabaseError databaseError) {
                Toast.makeText(DashboardActivity.this, "Failed to load complaints.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // --- Attach and detach the listener during the activity lifecycle ---
    @Override
    public void onStart() {
        super.onStart();
        // Attach the listener when the activity starts
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        // Detach the listener when the activity stops
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
