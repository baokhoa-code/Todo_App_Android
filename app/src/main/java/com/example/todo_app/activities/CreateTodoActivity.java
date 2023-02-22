package com.example.todo_app.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.example.todo_app.R;
import com.example.todo_app.database.TodosDatabase;
import com.example.todo_app.entities.Todo;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CreateTodoActivity extends AppCompatActivity {
    private static final String TAG = CreateTodoActivity.class.getSimpleName();

    private EditText inputTodoTitle, inputTodoSubtitle, inputTodoText;
    private TextView textDateTime;
    private View viewSubtitleIndicator;
    private ImageView imageTodo;
    private TextView textWebURL;
    private LinearLayout layoutWebURL;

    private String selectedTodoColor;
    private String selectedImagePath;

    private static final int REQUEST_CODE_STORAGE_PERMISSION = 1;
    private static final int REQUEST_CODE_SELECT_IMAGE = 2;

    private AlertDialog dialogAddURL;
    private AlertDialog dialogDeleteTodo;

    private Todo alreadyAvailableTodo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_todo);

        ImageView imageBack = findViewById(R.id.imageBack);
        imageBack.setOnClickListener(v -> onBackPressed());

        inputTodoTitle = findViewById(R.id.inputTodoTitle);
        inputTodoSubtitle = findViewById(R.id.inputTodoSubtitle);
        inputTodoText = findViewById(R.id.inputTodoText);
        viewSubtitleIndicator = findViewById(R.id.viewSubtitleIndicator);
        imageTodo = findViewById(R.id.imageTodo);
        textWebURL = findViewById(R.id.textWebURL);
        layoutWebURL = findViewById(R.id.layoutWebURL);

        textDateTime = findViewById(R.id.textDateTime);
        textDateTime.setText(new SimpleDateFormat(
                "EEEE, dd MMMM yyyy HH:mm a", Locale.getDefault()).format(new Date().getTime())
        );

        ImageView imageSave = findViewById(R.id.imageSave);
        imageSave.setOnClickListener(v -> saveTodo());

        selectedTodoColor = "#333333";
        selectedImagePath = "";

        if (getIntent().getBooleanExtra("isViewOrUpdate", false)) {
            alreadyAvailableTodo = (Todo) getIntent().getSerializableExtra("todo");
            setViewOrUpdateTodo();
        }

        findViewById(R.id.imageRemoveWebURL).setOnClickListener(v -> {
            textWebURL.setText(null);
            layoutWebURL.setVisibility(View.GONE);
        });

        findViewById(R.id.imageRemoveImage).setOnClickListener(v -> {
            imageTodo.setImageBitmap(null);
            imageTodo.setVisibility(View.GONE);
            findViewById(R.id.imageRemoveImage).setVisibility(View.GONE);
            selectedImagePath = "";
        });

        if (getIntent().getBooleanExtra("isFromQuickActions", false)) {
            String type = getIntent().getStringExtra("quickActionType");
            if (type != null) {
                if (type.equals("image")) {
                    selectedImagePath = getIntent().getStringExtra("imagePath");
                    imageTodo.setImageBitmap(BitmapFactory.decodeFile(selectedImagePath));
                    imageTodo.setVisibility(View.VISIBLE);
                    findViewById(R.id.imageRemoveImage).setVisibility(View.VISIBLE);
                } else if (type.equals("URL")) {
                    textWebURL.setText(getIntent().getStringExtra("URL"));
                    layoutWebURL.setVisibility(View.VISIBLE);
                }
            }
        }

        initMiscellaneous();
        setSubtitleIndicatorColor();
    }

    private void setViewOrUpdateTodo() {
        inputTodoTitle.setText(alreadyAvailableTodo.getTitle());
        inputTodoSubtitle.setText(alreadyAvailableTodo.getSubtitle());
        inputTodoText.setText(alreadyAvailableTodo.getTodoText());
        textDateTime.setText(alreadyAvailableTodo.getDateTime());

        final String imagePathStr = alreadyAvailableTodo.getImagePath();
        if (imagePathStr != null && !imagePathStr.trim().isEmpty()) {
            imageTodo.setImageBitmap(BitmapFactory.decodeFile(imagePathStr));
            imageTodo.setVisibility(View.VISIBLE);
            findViewById(R.id.imageRemoveImage).setVisibility(View.VISIBLE);
            selectedImagePath = imagePathStr;
        }

        final String webLinkStr = alreadyAvailableTodo.getWebLink();
        if (webLinkStr != null && !webLinkStr.trim().isEmpty()) {
            textWebURL.setText(alreadyAvailableTodo.getWebLink());
            layoutWebURL.setVisibility(View.VISIBLE);
        }
    }

    private void saveTodo() {
        final String todoTitle = inputTodoTitle.getText().toString().trim();
        final String todoSubtitle = inputTodoSubtitle.getText().toString().trim();
        final String todoText = inputTodoText.getText().toString().trim();
        final String dateTimeStr = textDateTime.getText().toString().trim();

        if (todoTitle.isEmpty()) {
            Toast.makeText(this, "Todo title can't be empty!", Toast.LENGTH_SHORT).show();
            return;
        } else if (todoSubtitle.isEmpty() && todoText.isEmpty()) {
            Toast.makeText(this, "Todo can't be empty!", Toast.LENGTH_SHORT).show();
            return;
        }

        final Todo todo = new Todo();
        todo.setTitle(todoTitle);
        todo.setSubtitle(todoSubtitle);
        todo.setTodoText(todoText);
        todo.setDateTime(dateTimeStr);
        todo.setColor(selectedTodoColor);
        todo.setImagePath(selectedImagePath);

        if (layoutWebURL.getVisibility() == View.VISIBLE) {
            todo.setWebLink(textWebURL.getText().toString());
        }

        if (alreadyAvailableTodo != null) {
            todo.setId(alreadyAvailableTodo.getId());
        }

        @SuppressLint("StaticFieldLeak")
        class SaveTodoTask extends AsyncTask<Void, Void, Void> {
            @Override
            protected Void doInBackground(Void... voids) {
                TodosDatabase.getTodosDatabase(getApplicationContext()).todoDao().insertTodo(todo);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                Intent intent = new Intent();
                setResult(RESULT_OK, intent);

                finish();
            }
        }

        new SaveTodoTask().execute();
    }

    private void initMiscellaneous() {
        final LinearLayout layoutOtherTypes = findViewById(R.id.layoutOtherTypes);
        final BottomSheetBehavior<LinearLayout> bottomSheetBehavior = BottomSheetBehavior.from(layoutOtherTypes);
        layoutOtherTypes.findViewById(R.id.layoutOtherTypes).setOnClickListener(v -> {
            if (bottomSheetBehavior.getState() != BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            } else {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });

        final ImageView imageColor1 = layoutOtherTypes.findViewById(R.id.imageColor1);
        final ImageView imageColor2 = layoutOtherTypes.findViewById(R.id.imageColor2);
        final ImageView imageColor3 = layoutOtherTypes.findViewById(R.id.imageColor3);
        final ImageView imageColor4 = layoutOtherTypes.findViewById(R.id.imageColor4);
        final ImageView imageColor5 = layoutOtherTypes.findViewById(R.id.imageColor5);

        layoutOtherTypes.findViewById(R.id.viewColor1).setOnClickListener(v -> {
            selectedTodoColor = "#333333";
            imageColor1.setImageResource(R.drawable.ic_done);
            imageColor2.setImageResource(0);
            imageColor3.setImageResource(0);
            imageColor4.setImageResource(0);
            imageColor5.setImageResource(0);
            setSubtitleIndicatorColor();
        });

        layoutOtherTypes.findViewById(R.id.viewColor2).setOnClickListener(v -> {
            selectedTodoColor = "#FDBE3B";
            imageColor1.setImageResource(0);
            imageColor2.setImageResource(R.drawable.ic_done);
            imageColor3.setImageResource(0);
            imageColor4.setImageResource(0);
            imageColor5.setImageResource(0);
            setSubtitleIndicatorColor();
        });

        layoutOtherTypes.findViewById(R.id.viewColor3).setOnClickListener(v -> {
            selectedTodoColor = "#FF4842";
            imageColor1.setImageResource(0);
            imageColor2.setImageResource(0);
            imageColor3.setImageResource(R.drawable.ic_done);
            imageColor4.setImageResource(0);
            imageColor5.setImageResource(0);
            setSubtitleIndicatorColor();
        });

        layoutOtherTypes.findViewById(R.id.viewColor4).setOnClickListener(v -> {
            selectedTodoColor = "#3A52FC";
            imageColor1.setImageResource(0);
            imageColor2.setImageResource(0);
            imageColor3.setImageResource(0);
            imageColor4.setImageResource(R.drawable.ic_done);
            imageColor5.setImageResource(0);
            setSubtitleIndicatorColor();
        });

        layoutOtherTypes.findViewById(R.id.viewColor5).setOnClickListener(v -> {
            selectedTodoColor = "#000000";
            imageColor1.setImageResource(0);
            imageColor2.setImageResource(0);
            imageColor3.setImageResource(0);
            imageColor4.setImageResource(0);
            imageColor5.setImageResource(R.drawable.ic_done);
            setSubtitleIndicatorColor();
        });

        if (alreadyAvailableTodo != null) {
            final String todoColorCode = alreadyAvailableTodo.getColor();
            if (todoColorCode != null && !todoColorCode.trim().isEmpty()) {
                switch (todoColorCode) {
                    case "#FDBE3B":
                        layoutOtherTypes.findViewById(R.id.viewColor2).performClick();
                        break;
                    case "#FF4842":
                        layoutOtherTypes.findViewById(R.id.viewColor3).performClick();
                        break;
                    case "#3A52FC":
                        layoutOtherTypes.findViewById(R.id.viewColor4).performClick();
                        break;
                    case "#000000":
                        layoutOtherTypes.findViewById(R.id.viewColor5).performClick();
                        break;
                }
            }
        }

        layoutOtherTypes.findViewById(R.id.layoutAddImage).setOnClickListener(v -> {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            if (ContextCompat.checkSelfPermission(getApplicationContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(CreateTodoActivity.this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE_STORAGE_PERMISSION);
            } else {
                selectImage();
            }
        });

        layoutOtherTypes.findViewById(R.id.layoutAddUrl).setOnClickListener(v -> {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            showAddURLDialog();
        });

        if (alreadyAvailableTodo != null) {
            layoutOtherTypes.findViewById(R.id.layoutDeleteTodo).setVisibility(View.VISIBLE);
            layoutOtherTypes.findViewById(R.id.layoutDeleteTodo).setOnClickListener(v -> {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                showDeleteTodoDialog();
            });
        }
    }

    private void showDeleteTodoDialog() {
        if (dialogDeleteTodo == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(CreateTodoActivity.this);
            View view = LayoutInflater.from(this).inflate(
                    R.layout.layout_delete_todo,
                    (ViewGroup) findViewById(R.id.layoutDeleteTodoContainer)
            );
            builder.setView(view);
            dialogDeleteTodo = builder.create();
            if (dialogDeleteTodo.getWindow() != null) {
                dialogDeleteTodo.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }
            view.findViewById(R.id.textDeleteTodo).setOnClickListener(v -> {
                @SuppressLint("StaticFieldLeak")
                class DeleteTodoTask extends AsyncTask<Void, Void, Void> {

                    @Override
                    protected Void doInBackground(Void... voids) {
                        TodosDatabase.getTodosDatabase(getApplicationContext()).todoDao().deleteTodo(alreadyAvailableTodo);
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void aVoid) {
                        super.onPostExecute(aVoid);
                        Intent intent = new Intent();
                        intent.putExtra("isTodoDeleted", true);
                        setResult(RESULT_OK, intent);

                        dialogDeleteTodo.dismiss();
                        finish();
                    }
                }

                new DeleteTodoTask().execute();
            });

            view.findViewById(R.id.textCancel).setOnClickListener(v -> dialogDeleteTodo.dismiss());
        }

        dialogDeleteTodo.show();
    }

    private void setSubtitleIndicatorColor() {
        GradientDrawable gradientDrawable = (GradientDrawable) viewSubtitleIndicator.getBackground();
        gradientDrawable.setColor(Color.parseColor(selectedTodoColor));
    }

    @SuppressLint("QueryPermissionsNeeded")
    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, REQUEST_CODE_SELECT_IMAGE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_STORAGE_PERMISSION && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                selectImage();
            } else {
                Toast.makeText(this, "Permission Denied!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SELECT_IMAGE && resultCode == RESULT_OK) {
            if (data != null) {
                Uri selectedImageUri = data.getData();
                if (selectedImageUri != null) {
                    try {
//                        InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);
//                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
//                        imageTodo.setImageBitmap(bitmap);

                        Glide.with(CreateTodoActivity.this)
                                .load(selectedImageUri)
                                .into(imageTodo);

                        imageTodo.setVisibility(View.VISIBLE);
                        findViewById(R.id.imageRemoveImage).setVisibility(View.VISIBLE);

                        selectedImagePath = getPathFromUri(selectedImageUri);
                    } catch (Exception e) {
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    private String getPathFromUri(Uri contentUri) {
        String filePath;
        Cursor cursor = getContentResolver().query(contentUri, null, null, null, null);
        if (cursor == null) {
            filePath = contentUri.getPath();
        } else {
            cursor.moveToFirst();
            int index = cursor.getColumnIndex("_data");
            filePath = cursor.getString(index);
            cursor.close();
        }
        return filePath;
    }

    private void showAddURLDialog() {
        if (dialogAddURL == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(CreateTodoActivity.this);
            View view = LayoutInflater.from(this)
                    .inflate(R.layout.layout_add_url, findViewById(R.id.layoutAddUrlContainer));
            builder.setView(view);

            dialogAddURL = builder.create();
            if (dialogAddURL.getWindow() != null) {
                dialogAddURL.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }

            final EditText inputURL = view.findViewById(R.id.inputURL);
            inputURL.requestFocus();

            view.findViewById(R.id.textAdd).setOnClickListener(v -> {
                final String inputURLStr = inputURL.getText().toString().trim();

                if (inputURLStr.isEmpty()) {
                    Toast.makeText(CreateTodoActivity.this, "Enter URL", Toast.LENGTH_SHORT).show();
                } else if (!Patterns.WEB_URL.matcher(inputURLStr).matches()) {
                    Toast.makeText(CreateTodoActivity.this, "Enter valid URL", Toast.LENGTH_SHORT).show();
                } else {
                    textWebURL.setText(inputURL.getText().toString());
                    layoutWebURL.setVisibility(View.VISIBLE);
                    dialogAddURL.dismiss();
                }
            });

            view.findViewById(R.id.textCancel).setOnClickListener(v -> dialogAddURL.dismiss());
        }
        dialogAddURL.show();
    }
}