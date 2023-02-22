package com.example.todo_app.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.todo_app.dao.TodoDao;
import com.example.todo_app.entities.Todo;

@Database(entities = Todo.class, version = 1, exportSchema = false)
public abstract class TodosDatabase extends RoomDatabase {

    private static TodosDatabase todosDatabase;

    public static synchronized TodosDatabase getTodosDatabase(Context context) {
        if (todosDatabase == null) {
            todosDatabase = Room.databaseBuilder(
                    context,
                    TodosDatabase.class,
                    "todos_db"
            ).build();
        }
        return todosDatabase;
    }

    public abstract TodoDao todoDao();
}
