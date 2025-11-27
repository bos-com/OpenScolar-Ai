package com.example.scolarai;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

public class CloudActivity extends AppCompatActivity {

    Button buttonUpload;
    RecyclerView recyclerFiles;
    ArrayList<Uri> uploadedFiles = new ArrayList<>();
    FileAdapter fileAdapter;
    private File cloudFolder;
    private boolean selectMode = false; // if true, clicking a file will return it to MainActivity

    private ActivityResultLauncher<Intent> filePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cloud);

        // 1️⃣ Determine if this is selection mode
        selectMode = getIntent().getBooleanExtra("selectMode", false);

        // 2️⃣ Create local "ScholarCloud" folder if it doesn't exist
        cloudFolder = new File(getExternalFilesDir(null), "ScholarCloud");
        if (!cloudFolder.exists()) {
            cloudFolder.mkdirs();
        }

        // 3️⃣ Initialize UI elements
        buttonUpload = findViewById(R.id.button_upload);
        recyclerFiles = findViewById(R.id.recycler_files);

        // 4️⃣ Setup RecyclerView
        recyclerFiles.setLayoutManager(new LinearLayoutManager(this));
        fileAdapter = new FileAdapter(this, uploadedFiles, fileUri -> {
            if (selectMode) {
                // Return file to MainActivity
                Intent result = new Intent();
                result.putExtra("filePath", new File(fileUri.getPath()).getAbsolutePath());
                setResult(RESULT_OK, result);
                finish();
            } else {
                // Normal behavior: edit/open/delete
                Toast.makeText(this, "Clicked: " + new File(fileUri.getPath()).getName(), Toast.LENGTH_SHORT).show();
            }
        });
        recyclerFiles.setAdapter(fileAdapter);

        // 5️⃣ Load existing files from local cloud folder
        loadLocalCloudFiles();

        // 6️⃣ File picker launcher
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri fileUri = result.getData().getData();
                        if (fileUri != null) {
                            saveFileToLocalStorage(fileUri);
                        }
                    }
                }
        );

        // 7️⃣ Upload button
        buttonUpload.setOnClickListener(v -> openFilePicker());
    }

    // Open file picker
    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*"); // allow any file type
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        filePickerLauncher.launch(intent);
    }

    // Save file to local storage
    private void saveFileToLocalStorage(Uri fileUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(fileUri);
            if (inputStream == null) {
                Toast.makeText(this, "Cannot open file stream", Toast.LENGTH_SHORT).show();
                return;
            }

            String fileName = getFileName(fileUri);
            if (fileName == null) {
                fileName = "unknown_file";
            }

            // Check if file already exists
            File destFile = new File(cloudFolder, fileName);
            int counter = 1;
            String baseName = fileName;
            String extension = "";

            int dotIndex = fileName.lastIndexOf('.');
            if (dotIndex > 0) {
                baseName = fileName.substring(0, dotIndex);
                extension = fileName.substring(dotIndex);
            }

            while (destFile.exists()) {
                fileName = baseName + " (" + counter + ")" + extension;
                destFile = new File(cloudFolder, fileName);
                counter++;
            }

            OutputStream outputStream = new FileOutputStream(destFile);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            inputStream.close();
            outputStream.close();

            Toast.makeText(this, "File saved: " + fileName, Toast.LENGTH_SHORT).show();

            // Add to RecyclerView
            uploadedFiles.add(Uri.fromFile(destFile));
            fileAdapter.notifyItemInserted(uploadedFiles.size() - 1);
            recyclerFiles.scrollToPosition(uploadedFiles.size() - 1);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error saving file: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // Load existing files from local cloud folder
    private void loadLocalCloudFiles() {
        if (cloudFolder.exists()) {
            File[] files = cloudFolder.listFiles();
            if (files != null) {
                uploadedFiles.clear(); // Clear existing files first
                for (File f : files) {
                    if (f.isFile()) {
                        uploadedFiles.add(Uri.fromFile(f));
                    }
                }
                fileAdapter.notifyDataSetChanged();
            }
        }
    }

    // Helper method to get file name from URI
    private String getFileName(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment();
        }
        return result;
    }
}
