package com.example.androidapp.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.example.androidapp.models.User;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "DatabaseHelper";
    private static final String DATABASE_NAME = "app_database.db";
    private static final int DATABASE_VERSION = 1;

    // Tabla de usuarios
    public static final String TABLE_USERS = "users";
    public static final String COLUMN_USER_ID = "user_id";
    public static final String COLUMN_USER_NAME = "name";
    public static final String COLUMN_USER_EMAIL = "email";
    public static final String COLUMN_USER_PASSWORD = "password";

    // Tabla de reportes
    public static final String TABLE_REPORTS = "reports";
    public static final String COLUMN_REPORT_ID = "report_id";
    public static final String COLUMN_USER_FK = "user_id";
    public static final String COLUMN_PLACE = "place";
    public static final String COLUMN_RATING = "rating";
    public static final String COLUMN_EXPLANATION = "explanation";
    public static final String COLUMN_IMAGES = "images";

    // Crear tabla de usuarios
    private static final String CREATE_TABLE_USERS =
            "CREATE TABLE " + TABLE_USERS + "("
                    + COLUMN_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + COLUMN_USER_NAME + " TEXT NOT NULL,"
                    + COLUMN_USER_EMAIL + " TEXT UNIQUE NOT NULL,"
                    + COLUMN_USER_PASSWORD + " TEXT NOT NULL"
                    + ");";

    // Crear tabla de reportes
    private static final String CREATE_TABLE_REPORTS =
            "CREATE TABLE " + TABLE_REPORTS + "("
                    + COLUMN_REPORT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + COLUMN_USER_FK + " INTEGER NOT NULL,"
                    + COLUMN_PLACE + " TEXT NOT NULL,"
                    + COLUMN_RATING + " INTEGER NOT NULL,"
                    + COLUMN_EXPLANATION + " TEXT NOT NULL,"
                    + COLUMN_IMAGES + " TEXT NOT NULL,"
                    + "FOREIGN KEY(" + COLUMN_USER_FK + ") REFERENCES "
                    + TABLE_USERS + "(" + COLUMN_USER_ID + ")"
                    + ");";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_USERS);
        db.execSQL(CREATE_TABLE_REPORTS);
        Log.d(TAG, "Base de datos creada");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_REPORTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
        Log.d(TAG, "Base de datos actualizada");
    }

    // Operaciones CRUD para usuarios
    public long addUser(String name, String email, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USER_NAME, name);
        values.put(COLUMN_USER_EMAIL, email);
        values.put(COLUMN_USER_PASSWORD, password);

        long id = db.insert(TABLE_USERS, null, values);
        db.close();
        return id;
    }

    public User getUser(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_USERS +
                " WHERE " + COLUMN_USER_EMAIL + " = ?" +
                " AND " + COLUMN_USER_PASSWORD + " = ?";

        Cursor cursor = db.rawQuery(query, new String[]{email, password});

        if (cursor != null && cursor.moveToFirst()) {
            User user = new User(
                    cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_USER_ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USER_NAME)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USER_EMAIL))
            );
            cursor.close();
            return user;
        }
        return null;
    }

    // Operaciones CRUD para reportes
    public long addReport(int userId, String place, int rating, String explanation, String images) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USER_FK, userId);
        values.put(COLUMN_PLACE, place);
        values.put(COLUMN_RATING, rating);
        values.put(COLUMN_EXPLANATION, explanation);
        values.put(COLUMN_IMAGES, images);

        long id = db.insert(TABLE_REPORTS, null, values);
        db.close();
        return id;
    }
}
