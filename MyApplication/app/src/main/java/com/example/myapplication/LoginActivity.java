package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;

public class LoginActivity extends AppCompatActivity {

    private static final String FILE_NAME = "users.txt";
    private EditText etUsername;
    private Button btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etUsername = findViewById(R.id.etUsername);
        btnLogin = findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            if (username.isEmpty()) {
                Toast.makeText(this, "Enter a username", Toast.LENGTH_SHORT).show();
                return;
            }

            if (userExists(username)) {
                Toast.makeText(this, "Welcome back " + username, Toast.LENGTH_SHORT).show();
            } else {
                createUser(username);
                Toast.makeText(this, "User created: " + username, Toast.LENGTH_SHORT).show();
            }

            // Go to MainActivity
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish(); // close login activity
        });
    }

    private boolean userExists(String username) {
        File file = new File(getFilesDir(), FILE_NAME);
        if (!file.exists()) return false;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.equals(username)) return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void createUser(String username) {
        try (FileOutputStream fos = openFileOutput(FILE_NAME, Context.MODE_APPEND)) {
            fos.write((username + "\n").getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
