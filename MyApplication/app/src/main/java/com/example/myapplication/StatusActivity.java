package com.example.myapplication;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseError;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class StatusActivity extends AppCompatActivity {

    private ArrayAdapter<String> adapter;
    private List<String> complaintStrings = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_status);

        ListView listView = findViewById(R.id.listComplaints);
        // The adapter is initialized with an empty list first.
        // It will be populated once data is fetched from Firebase.
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, complaintStrings);
        listView.setAdapter(adapter);

        // Fetch data from Firebase
        ComplainActivity complaintManager = new ComplainActivity(this);
        complaintManager.getAllComplaints(new ComplainActivity.FirebaseDataListener<List<ComplainActivity.Complaint>>() {
            @Override
            public void onDataReceived(List<ComplainActivity.Complaint> complaints) {
                // When data is received, update the adapter
                complaintStrings.clear();
                // We use the custom toString() method from the Complaint class
                for(ComplainActivity.Complaint complaint : complaints) {
                    complaintStrings.add(complaint.toString());
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onError(DatabaseError databaseError) {
                Toast.makeText(StatusActivity.this, "Error fetching complaints: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Back button in layout
        Button btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
