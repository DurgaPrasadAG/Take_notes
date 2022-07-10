package com.bliszkot.fileapp;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.documentfile.provider.DocumentFile;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

public class MainActivity extends AppCompatActivity {

    String fileName = "file.txt";
    String filePath = "FileStorage";
    File externalFile;
    String myData = "";
    FileOutputStream fos;
    Intent fileIntent;
    EditText fileNameText;
    EditText text;
    Uri contentUri;
    Boolean createFile = false;

    @RequiresApi(api = Build.VERSION_CODES.R)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(this,
                new String[]{
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.MANAGE_EXTERNAL_STORAGE
                }, 1
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                Uri uri = Uri.fromParts("package", this.getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
            }
        }

        FloatingActionButton create = findViewById(R.id.createButton);
        ImageView open = findViewById(R.id.openButton);
        ImageView save = findViewById(R.id.saveButton);
        text = findViewById(R.id.multiLineText);
        fileNameText = findViewById(R.id.fileNameText);

        ActivityResultLauncher<Intent> fileChooserLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {

                        Intent data = result.getData();
                        contentUri = data.getData();

                        Cursor returnCursor =
                                getContentResolver().query(contentUri, null, null, null, null);

                        int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                        returnCursor.moveToFirst();
                        fileNameText.setText(returnCursor.getString(nameIndex));

                        try {
                            InputStream in = getContentResolver().openInputStream(contentUri);

                            BufferedReader r = new BufferedReader(new InputStreamReader(in));
                            StringBuilder total = new StringBuilder();
                            for (String line; (line = r.readLine()) != null; ) {
                                total.append(line).append('\n');
                            }

                            filePath = total.toString();
                        } catch (Exception e) {
                            Log.e(TAG, "onCreate: Error: " + e);
                        }
                        text.setText(filePath);
                        fileNameText.setFocusableInTouchMode(false);
                        fileNameText.setVisibility(View.VISIBLE);
                    }
                });

        fileNameText.setVisibility(View.INVISIBLE);

        open.setOnClickListener(v -> {
            fileIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            fileIntent.setType("text/plain");
            fileChooserLauncher.launch(fileIntent);
        });

        create.setOnClickListener(v -> {
            try {
                createDialogBox().show();
                fileNameText.setVisibility(View.VISIBLE);
                createFile = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        save.setOnClickListener(v -> {
            try {
                File file = null;
                String fileName = fileNameText.getText().toString();
                if (createFile) {
                    file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), fileName);
                    file.createNewFile();
                } else {
                    if (fileName.equals("") || fileName == null) {
                        Toast.makeText(this, "Please create a file", Toast.LENGTH_SHORT).show();
                    } else {
                        DocumentFile docfile = DocumentFile.fromSingleUri(this, contentUri);
                        Uri path = docfile.getUri();

                        String lastPath = path.getLastPathSegment();
                        lastPath = lastPath.replace("primary:", "");
                        file = new File(Environment.getExternalStorageDirectory(), lastPath);
                    }
                }
                if (file != null && file.exists()) {
                    FileOutputStream output = new FileOutputStream(file);
                    byte[] array = text.getText().toString().getBytes();
                    output.write(array);
                    output.close();
                    if (createFile) {
                        Toast.makeText(this, "Created in Documents/", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Updated in Documents/", Toast.LENGTH_LONG).show();
                    }
                }
                createFile = false;
            } catch (Exception e) {
                Log.w("ExternalStorage", "Error writing " + e);
            }
        });
    }

    private Dialog createDialogBox() {
        fileNameText.setFocusableInTouchMode(true);
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        View content = LayoutInflater.from(MainActivity.this).inflate(R.layout.dialog, null, false);

        EditText fileNameCreate = content.findViewById(R.id.file_nameText);

        builder.setTitle("Save (.txt)");
        builder.setView(content);
        builder.setPositiveButton("DONE", (dialog, which) -> {
            String fileName = fileNameCreate.getText().toString() + ".txt";
            fileNameText.setText(fileName);
            text.setText("");
        });
        return builder.create();
    }

}