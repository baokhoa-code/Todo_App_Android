package com.example.todo_app.listeners;

import com.example.todo_app.entities.Todo;

public interface TodosListener {
    void onTodoClicked(Todo note, int position);
}
