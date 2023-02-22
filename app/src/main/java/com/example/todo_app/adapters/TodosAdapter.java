package com.example.todo_app.adapters;


import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.makeramen.roundedimageview.RoundedImageView;
import com.example.todo_app.R;
import com.example.todo_app.entities.Todo;
import com.example.todo_app.listeners.TodosListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class TodosAdapter extends RecyclerView.Adapter<TodosAdapter.TodoViewHolder> {

    private List<Todo> todos;
    private final List<Todo> todosSource;
    private final TodosListener todosListener;

    private Timer timer;

    public TodosAdapter(List<Todo> todos, TodosListener todosListener) {
        this.todos = todos;
        this.todosListener = todosListener;
        todosSource = todos;
    }

    @NonNull
    @Override
    public TodoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new TodoViewHolder(
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_container_todo, parent, false)
        );
    }

    @Override
    public void onBindViewHolder(@NonNull TodoViewHolder holder, int position) {
        holder.setTodo(todos.get(position));
        holder.layoutTodo.setOnClickListener(v ->
                todosListener.onTodoClicked(todos.get(position), position));
    }

    @Override
    public int getItemCount() {
        return todos.size();
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    static class TodoViewHolder extends RecyclerView.ViewHolder {

        TextView textTitle, textSubtitle, textDateTime;
        LinearLayout layoutTodo;
        RoundedImageView imageTodo;

        TodoViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.textTitle);
            textSubtitle = itemView.findViewById(R.id.textSubtitle);
            textDateTime = itemView.findViewById(R.id.textDateTime);
            layoutTodo = itemView.findViewById(R.id.layoutTodo);
            imageTodo = itemView.findViewById(R.id.imageTodo);
        }

        void setTodo(Todo todo) {
            textTitle.setText(todo.getTitle());
            if (todo.getSubtitle().trim().isEmpty()) {
                textSubtitle.setVisibility(View.GONE);
            } else {
                textSubtitle.setText(todo.getSubtitle());
            }
            textDateTime.setText(todo.getDateTime());

            GradientDrawable gradientDrawable = (GradientDrawable) layoutTodo.getBackground();
            if (todo.getColor() != null) {
                gradientDrawable.setColor(Color.parseColor(todo.getColor()));
            } else {
                gradientDrawable.setColor(Color.parseColor("#333333"));
            }

            if (todo.getImagePath() != null) {
                imageTodo.setImageBitmap(BitmapFactory.decodeFile(todo.getImagePath()));
                imageTodo.setVisibility(View.VISIBLE);
            } else {
                imageTodo.setVisibility(View.GONE);
            }
        }
    }

    public void searchTodos(final String searchKeyword) {
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (searchKeyword.trim().isEmpty()) {
                    todos = todosSource;
                } else {
                    ArrayList<Todo> temp = new ArrayList<>();
                    for (Todo todo : todosSource) {
                        if (todo.getTitle().toLowerCase().contains(searchKeyword.toLowerCase()) ||
                                todo.getSubtitle().toLowerCase().contains(searchKeyword.toLowerCase()) ||
                                todo.getTodoText().toLowerCase().contains(searchKeyword.toLowerCase())) {
                            temp.add(todo);
                        }
                    }
                    todos = temp;
                }

                new Handler(Looper.getMainLooper()).post(() -> notifyDataSetChanged());
            }
        }, 500);
    }

    public void cancelTimer() {
        if (timer != null) {
            timer.cancel();
        }
    }
}
