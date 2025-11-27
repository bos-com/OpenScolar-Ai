package com.example.scolarai;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.navigation.NavigationView;

import java.io.File;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    DrawerLayout drawerLayout;
    NavigationView navigationView;
    Toolbar toolbar;

    RecyclerView chatRecycler;
    ChatAdapter chatAdapter;
    ArrayList<ChatMessage> chatMessages = new ArrayList<>();

    ImageView buttonTools, buttonSend;
    EditText inputMessage;

    ArrayList<String> selectedTools = new ArrayList<>();
    ArrayList<File> attachedFiles = new ArrayList<>();

    private static final int REQUEST_SELECT_CLOUD_FILE = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Apply saved night mode preference
        if (AppSettings.isDarkMode(this)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        setContentView(R.layout.activity_main);

        // Toolbar + Drawer
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.open, R.string.close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_profile) {
                startActivity(new Intent(MainActivity.this, CloudActivity.class));
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            } else if (id == R.id.nav_light_mode) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                AppSettings.setDarkMode(this, false);
                Toast.makeText(this, "Light Mode Activated", Toast.LENGTH_SHORT).show();
            } else if (id == R.id.nav_dark_mode) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                AppSettings.setDarkMode(this, true);
                Toast.makeText(this, "Dark Mode Activated", Toast.LENGTH_SHORT).show();
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        // Chat UI
        chatRecycler = findViewById(R.id.chat_recycler);
        chatRecycler.setLayoutManager(new LinearLayoutManager(this));
        chatAdapter = new ChatAdapter(chatMessages);
        chatRecycler.setAdapter(chatAdapter);

        buttonTools = findViewById(R.id.button_tools);
        buttonSend = findViewById(R.id.button_send);
        inputMessage = findViewById(R.id.input_message);

        buttonTools.setOnClickListener(v -> showToolPopup());
        buttonSend.setOnClickListener(v -> sendMessage());
    }

    // Tools popup menu
    private void showToolPopup() {
        PopupMenu popupMenu = new PopupMenu(this, buttonTools);
        popupMenu.getMenu().add("Cloud");
        popupMenu.getMenu().add("Math");
        popupMenu.getMenu().add("Presentation");
        popupMenu.getMenu().add("Documents");
        popupMenu.getMenu().add("PDF");
        popupMenu.getMenu().add("PPTX");
        popupMenu.getMenu().add("Web");

        popupMenu.setOnMenuItemClickListener(item -> {
            String tool = item.getTitle().toString();
            if (tool.equals("Cloud")) {
                openCloudForSelection();
            } else {
                if (!selectedTools.contains(tool)) {
                    selectedTools.add(tool);
                    Toast.makeText(this, tool + " selected", Toast.LENGTH_SHORT).show();
                }
            }
            return true;
        });

        popupMenu.show();
    }

    // Open CloudActivity for selecting file
    private void openCloudForSelection() {
        Intent intent = new Intent(MainActivity.this, CloudActivity.class);
        intent.putExtra("selectMode", true);
        startActivityForResult(intent, REQUEST_SELECT_CLOUD_FILE);
    }

    // Handle selected cloud file
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_SELECT_CLOUD_FILE && resultCode == RESULT_OK && data != null) {
            String filePath = data.getStringExtra("filePath");
            if (filePath != null) {
                File file = new File(filePath);
                if (file.exists()) {
                    attachedFiles.add(file);
                    ChatMessage fileMessage = new ChatMessage(
                            "[Cloud Attachment] " + file.getName(),
                            true, true // isUser=true, isFile=true
                    );
                    chatMessages.add(fileMessage);
                    chatAdapter.notifyItemInserted(chatMessages.size() - 1);
                    chatRecycler.scrollToPosition(chatMessages.size() - 1);
                    Toast.makeText(this, "File attached: " + file.getName(), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void sendMessage() {
        String text = inputMessage.getText().toString().trim();
        if (text.isEmpty()) return;

        // Show user's message
        ChatMessage userMessage = new ChatMessage(text, true, false);
        chatMessages.add(userMessage);
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        chatRecycler.scrollToPosition(chatMessages.size() - 1);

        inputMessage.setText("");

        // Send to Gemini
        GeminiClient.sendPrompt(text, new GeminiClient.Callback() {
            @Override
            public void onResponse(String response) {
                ChatMessage botMessage = new ChatMessage(response, false, false);
                chatMessages.add(botMessage);
                chatAdapter.notifyItemInserted(chatMessages.size() - 1);
                chatRecycler.scrollToPosition(chatMessages.size() - 1);
            }

            @Override
            public void onError(String error) {
                ChatMessage errorMessage = new ChatMessage("Error: " + error, false, false);
                chatMessages.add(errorMessage);
                chatAdapter.notifyItemInserted(chatMessages.size() - 1);
                chatRecycler.scrollToPosition(chatMessages.size() - 1);
            }
        });
    }

}
