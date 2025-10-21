package com.example.myapplication;

import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ComplaintManager handles saving and retrieving user complaints from Firebase Realtime Database.
 */
public class ComplainActivity {

    private final DatabaseReference databaseReference;
    private final FirebaseAuth firebaseAuth;
    private final Context context;

    public ComplainActivity(Context context) {
        this.context = context;
        // Get a reference to the root of your Firebase Realtime Database
        this.databaseReference = FirebaseDatabase.getInstance().getReference();
        this.firebaseAuth = FirebaseAuth.getInstance();
    }

    /**
     * Represents a single complaint record.
     */
    public static class Complaint {
        public String userId;
        public String username;
        public String complaintText;
        public String imageInfo;
        public long timestamp;

        public Complaint() {
            // Default constructor required for calls to DataSnapshot.getValue(Complaint.class)
        }

        public Complaint(String userId, String username, String complaintText, String imageInfo) {
            this.userId = userId;
            this.username = username;
            this.complaintText = complaintText;
            this.imageInfo = imageInfo;
            this.timestamp = System.currentTimeMillis();
        }

        @Override
        public String toString() {
            // This format will be shown in the ListView in StatusActivity
            return "User: " + username + "\nComplaint: " + complaintText + "\nPrediction: " + imageInfo;
        }
    }

    /**
     * Save a complaint with its associated image info to the Firebase Realtime Database.
     * @param complaintText The text of the complaint.
     * @param imageInfo The prediction or info related to the image.
     */
    public void saveComplaint(String complaintText, String imageInfo) {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(context, "You must be logged in to file a complaint.", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();
        String userEmail = currentUser.getEmail();
        String username = (userEmail != null && userEmail.contains("@")) ? userEmail.split("@")[0] : "Anonymous";

        // Create a new Complaint object
        Complaint complaint = new Complaint(userId, username, complaintText, imageInfo);

        // Push to "complaints" node to generate a unique ID for each complaint
        databaseReference.child("complaints").push().setValue(complaint)
                .addOnSuccessListener(aVoid -> Toast.makeText(context, "Complaint submitted successfully!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(context, "Failed to submit complaint.", Toast.LENGTH_SHORT).show());
    }

    /**
     * Retrieve all complaints from Firebase for the StatusActivity.
     * This method is now asynchronous. You need a callback to get the data.
     */
    public void getAllComplaints(FirebaseDataListener<List<Complaint>> listener) {
        databaseReference.child("complaints").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Complaint> complaints = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Complaint complaint = snapshot.getValue(Complaint.class);
                    if (complaint != null) {
                        complaints.add(complaint);
                    }
                }
                listener.onDataReceived(complaints);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                listener.onError(databaseError);
                Toast.makeText(context, "Failed to load complaints.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Callback interface for asynchronous data fetching from Firebase.
     */
    public interface FirebaseDataListener<T> {
        void onDataReceived(T data);
        void onError(DatabaseError databaseError);
    }
}
