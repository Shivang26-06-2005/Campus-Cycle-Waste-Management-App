package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class RoleSelectionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_role_selection);

        Button btnUser = findViewById(R.id.btnRoleUser);
        Button btnAdmin = findViewById(R.id.btnRoleAdmin);

        btnUser.setOnClickListener(v -> {
            navigateToLogin("User");
        });

        btnAdmin.setOnClickListener(v -> {
            navigateToLogin("Admin");
        });
    }

    private void navigateToLogin(String role) {
        Intent intent = new Intent(RoleSelectionActivity.this, LoginActivity.class);
        // Pass the selected role to the LoginActivity
        intent.putExtra("ROLE", role);
        startActivity(intent);
    }
}
