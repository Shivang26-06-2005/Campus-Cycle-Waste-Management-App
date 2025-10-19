package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

public class DashboardActivity extends AppCompatActivity {

    private TextView tvComplaintCount;
    private Button btnStatus, btnProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        tvComplaintCount = findViewById(R.id.tvComplaintCount);
        btnStatus = findViewById(R.id.btnStatus);
        btnProfile = findViewById(R.id.btnProfile);

        // Load number of complaints
        ComplainActivity complaintManager = new ComplainActivity(this);
        List<String> complaints = complaintManager.getAllComplaints();
        tvComplaintCount.setText("Total Complaints: " + complaints.size());

        btnStatus.setOnClickListener(v ->
                startActivity(new Intent(DashboardActivity.this, StatusActivity.class))
        );

        btnProfile.setOnClickListener(v ->
                startActivity(new Intent(DashboardActivity.this, ProfileActivity.class))
        );
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

