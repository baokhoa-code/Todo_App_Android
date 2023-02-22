package com.example.todo_app.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.example.todo_app.R;
import com.example.todo_app.adapters.TodosAdapter;
import com.example.todo_app.database.TodosDatabase;
import com.example.todo_app.entities.Todo;
import com.example.todo_app.listeners.TodosListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements TodosListener {
    private static final String TAG = MainActivity.class.getSimpleName();

    public static final int REQUEST_CODE_ADD_TODO = 1;
    public static final int REQUEST_CODE_UPDATE_TODO = 2;
    public static final int REQUEST_CODE_SHOW_TODOS = 3;
    public static final int REQUEST_CODE_SELECT_IMAGE = 4;
    public static final int REQUEST_CODE_STORAGE_PERMISSION = 5;

    private RecyclerView todosRecyclerView;
    private List<Todo> todoList;
    private TodosAdapter todosAdapter;

    private int todoClickedPosition = -1;

    private AlertDialog dialogAddURL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageView imageAddTodoMain = findViewById(R.id.imageAddTodoMain);
        imageAddTodoMain.setOnClickListener(v -> startActivityForResult(
                new Intent(getApplicationContext(), CreateTodoActivity.class), REQUEST_CODE_ADD_TODO)
        );

        todosRecyclerView = findViewById(R.id.todosRecyclerView);
        todosRecyclerView.setLayoutManager(
                new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));

        todoList = new ArrayList<>();
        todosAdapter = new TodosAdapter(todoList, this);
        todosRecyclerView.setAdapter(todosAdapter);

        getTodos(REQUEST_CODE_SHOW_TODOS, false);

        EditText inputSearch = findViewById(R.id.inputSearch);
        inputSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                todosAdapter.cancelTimer();
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (todoList.size() != 0) {
                    todosAdapter.searchTodos(s.toString());
                }
            }
        });

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

    @Override
    public void onTodoClicked(Todo todo, int position) {
        todoClickedPosition = position;
        Intent intent = new Intent(getApplicationContext(), CreateTodoActivity.class);
        intent.putExtra("isViewOrUpdate", true);
        intent.putExtra("todo", todo);
        startActivityForResult(intent, REQUEST_CODE_UPDATE_TODO);
    }

    private void getTodos(final int requestCode, final boolean isTodoDeleted) {

        @SuppressLint("StaticFieldLeak")
        class GetTodoTask extends AsyncTask<Void, Void, List<Todo>> {

            @Override
            protected List<Todo> doInBackground(Void... voids) {
                return TodosDatabase.getTodosDatabase(getApplicationContext())
                        .todoDao().getAllTodos();
            }

            @Override
            protected void onPostExecute(List<Todo> todos) {
                super.onPostExecute(todos);
                if (requestCode == REQUEST_CODE_SHOW_TODOS) {
                    todoList.addAll(todos);
                    todosAdapter.notifyDataSetChanged();
                } else if (requestCode == REQUEST_CODE_ADD_TODO) {
                    todoList.add(0, todos.get(0));
                    todosAdapter.notifyItemInserted(0);
                    todosRecyclerView.smoothScrollToPosition(0);
                } else if (requestCode == REQUEST_CODE_UPDATE_TODO) {
                    todoList.remove(todoClickedPosition);
                    if (isTodoDeleted) {
                        todosAdapter.notifyItemRemoved(todoClickedPosition);
                    } else {
                        todoList.add(todoClickedPosition, todos.get(todoClickedPosition));
                        todosAdapter.notifyItemChanged(todoClickedPosition);
                    }
                }
            }
        }

        new GetTodoTask().execute();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_ADD_TODO && resultCode == RESULT_OK) {
            getTodos(REQUEST_CODE_ADD_TODO, false);
        } else if (requestCode == REQUEST_CODE_UPDATE_TODO && resultCode == RESULT_OK) {
            if (data != null) {
                getTodos(REQUEST_CODE_UPDATE_TODO, data.getBooleanExtra("isTodoDeleted", false));
            }
        } else if (requestCode == REQUEST_CODE_SELECT_IMAGE && resultCode == RESULT_OK) {
            if (data != null) {
                Uri selectedImageUri = data.getData();
                if (selectedImageUri != null) {
                    try {
                        String selectedImagePath = getPathFromUri(selectedImageUri);
                        Intent intent = new Intent(getApplicationContext(), CreateTodoActivity.class);
                        intent.putExtra("isFromQuickActions", true);
                        intent.putExtra("quickActionType", "image");
                        intent.putExtra("imagePath", selectedImagePath);
                        startActivityForResult(intent, REQUEST_CODE_ADD_TODO);
                    } catch (Exception e) {
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    private void showAddURLDialog() {
        if (dialogAddURL == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
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
                    Toast.makeText(MainActivity.this, "Enter URL", Toast.LENGTH_SHORT).show();
                } else if (!Patterns.WEB_URL.matcher(inputURLStr).matches()) {
                    Toast.makeText(MainActivity.this, "Enter valid URL", Toast.LENGTH_SHORT).show();
                } else {
                    dialogAddURL.dismiss();
                    Intent intent = new Intent(getApplicationContext(), CreateTodoActivity.class);
                    intent.putExtra("isFromQuickActions", true);
                    intent.putExtra("quickActionType", "URL");
                    intent.putExtra("URL", inputURLStr);
                    startActivityForResult(intent, REQUEST_CODE_ADD_TODO);

                }
            });

            view.findViewById(R.id.textCancel).setOnClickListener(v -> dialogAddURL.dismiss());
        }
        dialogAddURL.show();
    }
}