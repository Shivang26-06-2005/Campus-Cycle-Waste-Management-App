package com.example.myapplication;

import android.content.Context;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.io.BufferedReader;
import java.io.FileReader;

/**
 * ComplaintManager handles saving and retrieving user complaints and image info.
 * Data is stored in private internal files.
 */
public class ComplainActivity {

    private static final String FILE_COMPLAINTS = "complaints.txt";
    private static final String FILE_IMAGES = "images.txt";

    private Context context;

    public ComplainActivity(Context context) {
        this.context = context;
    }

    /** Save a complaint to the internal file */
    public void saveComplaint(String complaint) {
        try (FileOutputStream fos = context.openFileOutput(FILE_COMPLAINTS, Context.MODE_APPEND)) {
            fos.write((complaint + "\n").getBytes());
            Toast.makeText(context, "Complaint saved!", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, "Failed to save complaint", Toast.LENGTH_SHORT).show();
        }
    }

    /** Save image info (like prediction) to internal file */
    public void saveImageInfo(String info) {
        try (FileOutputStream fos = context.openFileOutput(FILE_IMAGES, Context.MODE_APPEND)) {
            fos.write((info + "\n").getBytes());
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, "Failed to save image info", Toast.LENGTH_SHORT).show();
        }
    }

    /** Retrieve all complaints from the internal file */
    public List<String> getAllComplaints() {
        List<String> complaints = new ArrayList<>();
        File file = new File(context.getFilesDir(), FILE_COMPLAINTS);
        if (!file.exists()) return complaints;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                complaints.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return complaints;
    }

    /** Retrieve all saved image info */
    public List<String> getAllImages() {
        List<String> images = new ArrayList<>();
        File file = new File(context.getFilesDir(), FILE_IMAGES);
        if (!file.exists()) return images;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                images.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return images;
    }

    /** Clear all complaints */
    public void clearComplaints() {
        File file = new File(context.getFilesDir(), FILE_COMPLAINTS);
        if (file.exists()) {
            boolean deleted = file.delete();
            if (deleted) {
                Toast.makeText(context, "All complaints cleared", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /** Clear all saved image info */
    public void clearImages() {
        File file = new File(context.getFilesDir(), FILE_IMAGES);
        if (file.exists()) {
            boolean deleted = file.delete();
            if (deleted) {
                Toast.makeText(context, "All image info cleared", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
