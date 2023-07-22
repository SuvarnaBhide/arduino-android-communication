package com.example.arduinoledcontroller;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class ConvertFirestoreDataToJSON extends AppCompatActivity {

    private static final String TAG = "ConvertFirestoreDataToJSON";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final CollectionReference cmdsRef = db.collection("BluetoothCommunication");

    public void downloadExcel(View v, Context context){

        //String csv = (Environment.getExternalStorageDirectory().getAbsolutePath() + "/ArduinoCommands.csv");
        // Get the external files directory
        File downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if(downloadsDir != null) {
            File file = new File(downloadsDir, "ArduinoCommands.csv");
            try {

                if (!file.exists()) {
                    boolean isFileCreated = file.createNewFile();
                    if (!isFileCreated) {
                        Log.d(TAG, "Failed to create the file");
                        return;
                    }
                }

                CSVWriter writer = new CSVWriter(new FileWriter(file, true));

                //List<String[]> data = new ArrayList<String[]>();
                String[] columnNames = {"Command entered", "Command Description"};

                writer.writeNext(columnNames);

                cmdsRef.get().addOnSuccessListener(queryDocumentSnapshots -> {

                    for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {

                        ArduinoCommand arduinoCommand = documentSnapshot.toObject(ArduinoCommand.class);
                        arduinoCommand.setDocumentId(documentSnapshot.getId());

                        String command = arduinoCommand.getCommand();
                        String description = arduinoCommand.getCommandDescription();

                        String[] data = {command, description};
                        writer.writeNext(data);

                    }
                    try {
                        writer.close();
                        Log.d(TAG, "doneeeee");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
            Toast.makeText(context, "File successfully created!", Toast.LENGTH_LONG).show();
        }
    }


}
