package com.example.scolarai;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.FileViewHolder> {

    public interface OnFileClickListener {
        void onFileClick(File file);
    }

    private final ArrayList<Uri> files;
    private final Context context;
    private final OnFileClickListener clickListener;

    public FileAdapter(Context context, ArrayList<Uri> files, OnFileClickListener clickListener) {
        this.context = context;
        this.files = files;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_file, parent, false);
        return new FileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
        Uri fileUri = files.get(position);
        File file = new File(fileUri.getPath());
        String fileName = file.getName();
        holder.fileName.setText(fileName);

        // Set icon based on file type
        if (fileName.endsWith(".txt")) holder.fileIcon.setImageResource(android.R.drawable.ic_menu_edit);
        else if (fileName.endsWith(".pdf")) holder.fileIcon.setImageResource(android.R.drawable.ic_dialog_info);
        else if (fileName.endsWith(".ppt") || fileName.endsWith(".pptx"))
            holder.fileIcon.setImageResource(android.R.drawable.ic_menu_slideshow);
        else if (fileName.endsWith(".doc") || fileName.endsWith(".docx"))
            holder.fileIcon.setImageResource(android.R.drawable.ic_menu_edit);
        else holder.fileIcon.setImageResource(android.R.drawable.ic_menu_help);

        // Click file: either open or select for attachment
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onFileClick(file); // return selected file to MainActivity
            } else {
                openFile(file);
            }
        });

        // Overflow menu
        holder.fileMenu.setOnClickListener(v -> showPopupMenu(file, position));
    }

    private void showPopupMenu(File file, int position) {
        PopupMenu menu = new PopupMenu(context, files.get(position) != null ? null : null);
        menu.getMenu().add("Rename");
        menu.getMenu().add("Delete");
        menu.getMenu().add("Edit Text (if .txt)");

        menu.setOnMenuItemClickListener(item -> {
            String action = item.getTitle().toString();
            if (action.equals("Rename")) renameFile(file, position);
            else if (action.equals("Delete")) deleteFile(file, position);
            else if (action.equals("Edit Text (if .txt)")) {
                if (file.getName().endsWith(".txt")) editTextFile(file);
                else Toast.makeText(context, "Only text files can be edited", Toast.LENGTH_SHORT).show();
            }
            return true;
        });
        menu.show();
    }

    private void openFile(File file) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri contentUri = FileProvider.getUriForFile(
                    context,
                    context.getApplicationContext().getPackageName() + ".provider",
                    file
            );
            intent.setDataAndType(contentUri, getMimeType(file.getName()));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, "Cannot open file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String getMimeType(String fileName) {
        if (fileName.endsWith(".pdf")) return "application/pdf";
        else if (fileName.endsWith(".txt")) return "text/plain";
        else if (fileName.endsWith(".doc") || fileName.endsWith(".docx")) return "application/msword";
        else if (fileName.endsWith(".ppt") || fileName.endsWith(".pptx")) return "application/vnd.ms-powerpoint";
        else return "*/*";
    }

    private void renameFile(File file, int position) {
        EditText input = new EditText(context);
        input.setText(file.getName());
        input.setSingleLine(true);

        new AlertDialog.Builder(context)
                .setTitle("Rename File")
                .setView(input)
                .setPositiveButton("Rename", (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        File newFile = new File(file.getParent(), newName);
                        if (file.renameTo(newFile)) {
                            files.set(position, Uri.fromFile(newFile));
                            notifyItemChanged(position);
                            Toast.makeText(context, "Renamed to " + newName, Toast.LENGTH_SHORT).show();
                        } else Toast.makeText(context, "Rename failed", Toast.LENGTH_SHORT).show();
                    } else Toast.makeText(context, "Filename cannot be empty", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteFile(File file, int position) {
        new AlertDialog.Builder(context)
                .setTitle("Delete File")
                .setMessage("Are you sure you want to delete this file?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (file.delete()) {
                        files.remove(position);
                        notifyItemRemoved(position);
                        Toast.makeText(context, "File deleted", Toast.LENGTH_SHORT).show();
                    } else Toast.makeText(context, "Delete failed", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void editTextFile(File file) {
        try {
            FileInputStream fis = new FileInputStream(file);
            byte[] data = new byte[(int) file.length()];
            fis.read(data);
            fis.close();
            String content = new String(data);

            EditText editText = new EditText(context);
            editText.setText(content);
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
            editText.setMinLines(5);

            new AlertDialog.Builder(context)
                    .setTitle("Edit Text File")
                    .setView(editText)
                    .setPositiveButton("Save", (dialog, which) -> saveTextFile(file, editText.getText().toString()))
                    .setNegativeButton("Cancel", null)
                    .show();

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, "Error opening file", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveTextFile(File file, String content) {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(content.getBytes());
            Toast.makeText(context, "File saved successfully", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, "Save failed", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    static class FileViewHolder extends RecyclerView.ViewHolder {
        ImageView fileIcon, fileMenu;
        TextView fileName;

        public FileViewHolder(@NonNull View itemView) {
            super(itemView);
            fileIcon = itemView.findViewById(R.id.file_icon);
            fileMenu = itemView.findViewById(R.id.file_menu);
            fileName = itemView.findViewById(R.id.file_name);
        }
    }
}
