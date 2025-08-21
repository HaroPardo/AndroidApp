package com.example.androidapp.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.example.androidapp.models.User;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "DatabaseHelper";
    private static final String DATABASE_NAME = "app_database.db";
    private static final int DATABASE_VERSION = 3; // versión con users, reports, reminders

    // Tabla de usuarios
    public static final String TABLE_USERS = "users";
    public static final String COLUMN_USER_ID = "user_id";
    public static final String COLUMN_USER_NAME = "name";
    public static final String COLUMN_USER_EMAIL = "email";
    public static final String COLUMN_USER_PASSWORD = "password";

    // Tabla de reportes
    public static final String TABLE_REPORTS = "reports";
    public static final String COLUMN_REPORT_ID = "report_id";
    public static final String COLUMN_REPORT_USER_FK = "user_id";
    public static final String COLUMN_PLACE = "place";
    public static final String COLUMN_RATING = "rating";
    public static final String COLUMN_EXPLANATION = "explanation";
    public static final String COLUMN_IMAGES = "images";

    // Tabla de recordatorios
    public static final String TABLE_REMINDERS = "reminders";
    public static final String COLUMN_REMINDER_ID = "reminder_id";
    public static final String COLUMN_REMINDER_USER_FK = "user_id";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_LATITUDE = "latitude";
    public static final String COLUMN_LONGITUDE = "longitude";
    public static final String COLUMN_RADIUS = "radius";
    public static final String COLUMN_IMAGE_PATH = "image_path";
    public static final String COLUMN_CREATED_AT = "created_at";

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
                    + COLUMN_REPORT_USER_FK + " INTEGER NOT NULL,"
                    + COLUMN_PLACE + " TEXT NOT NULL,"
                    + COLUMN_RATING + " INTEGER NOT NULL,"
                    + COLUMN_EXPLANATION + " TEXT NOT NULL,"
                    + COLUMN_IMAGES + " TEXT NOT NULL,"
                    + "FOREIGN KEY(" + COLUMN_REPORT_USER_FK + ") REFERENCES "
                    + TABLE_USERS + "(" + COLUMN_USER_ID + ")"
                    + ");";

    // Crear tabla de recordatorios
    private static final String CREATE_TABLE_REMINDERS =
            "CREATE TABLE " + TABLE_REMINDERS + "("
                    + COLUMN_REMINDER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + COLUMN_REMINDER_USER_FK + " INTEGER NOT NULL,"
                    + COLUMN_TITLE + " TEXT NOT NULL,"
                    + COLUMN_LATITUDE + " REAL NOT NULL,"
                    + COLUMN_LONGITUDE + " REAL NOT NULL,"
                    + COLUMN_RADIUS + " REAL NOT NULL,"
                    + COLUMN_IMAGE_PATH + " TEXT,"
                    + COLUMN_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP,"
                    + "FOREIGN KEY(" + COLUMN_REMINDER_USER_FK + ") REFERENCES "
                    + TABLE_USERS + "(" + COLUMN_USER_ID + ")"
                    + ");";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            db.execSQL(CREATE_TABLE_USERS);
            db.execSQL(CREATE_TABLE_REPORTS);
            db.execSQL(CREATE_TABLE_REMINDERS);
            Log.d(TAG, "Tablas creadas exitosamente");
        } catch (SQLException e) {
            Log.e(TAG, "Error al crear tablas: " + e.getMessage());
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Migraciones progresivas
        if (oldVersion < 2) {
            db.execSQL(CREATE_TABLE_REMINDERS);
        }
        if (oldVersion < 3) {
            db.execSQL(CREATE_TABLE_REPORTS);
        }
        Log.d(TAG, "Base de datos actualizada a la versión " + newVersion);
    }

    // ==== CRUD USUARIOS ====
    public long addUser(String name, String email, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        long id = -1;
        try {
            ContentValues values = new ContentValues();
            values.put(COLUMN_USER_NAME, name);
            values.put(COLUMN_USER_EMAIL, email);
            values.put(COLUMN_USER_PASSWORD, password);

            id = db.insertOrThrow(TABLE_USERS, null, values);
            Log.d(TAG, "Usuario agregado con ID: " + id);
        } catch (SQLException e) {
            Log.e(TAG, "Error al agregar usuario: " + e.getMessage());
        } finally {
            db.close();
        }
        return id;
    }

    public User getUser(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        User user = null;
        Cursor cursor = null;

        try {
            String query = "SELECT * FROM " + TABLE_USERS +
                    " WHERE " + COLUMN_USER_EMAIL + " = ?" +
                    " AND " + COLUMN_USER_PASSWORD + " = ?";

            cursor = db.rawQuery(query, new String[]{email, password});

            if (cursor != null && cursor.moveToFirst()) {
                user = new User(
                        cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_USER_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USER_NAME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USER_EMAIL))
                );
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al obtener usuario: " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
        return user;
    }

    // ==== CRUD REPORTES ====
    public long addReport(int userId, String place, int rating, String explanation, String images) {
        SQLiteDatabase db = this.getWritableDatabase();
        long id = -1;
        try {
            ContentValues values = new ContentValues();
            values.put(COLUMN_REPORT_USER_FK, userId);
            values.put(COLUMN_PLACE, place);
            values.put(COLUMN_RATING, rating);
            values.put(COLUMN_EXPLANATION, explanation);
            values.put(COLUMN_IMAGES, images != null ? images : ""); // ✅ Evitar null

            id = db.insertOrThrow(TABLE_REPORTS, null, values);
        } catch (SQLException e) {
            Log.e(TAG, "Error al agregar reporte: " + e.getMessage());
        } finally {
            db.close();
        }
        return id;
    }

    // ✅ Método para obtener lista de reportes de un usuario
    public Cursor getUserReports(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_REPORTS,
                new String[]{
                        COLUMN_REPORT_ID + " AS _id", // ✅ Añadir alias _id
                        COLUMN_PLACE,
                        COLUMN_RATING
                },
                COLUMN_REPORT_USER_FK + " = ?",
                new String[]{String.valueOf(userId)},
                null, null, COLUMN_REPORT_ID + " DESC");
    }

    // Método para obtener detalles de un reporte específico
    public Cursor getReportDetails(long reportId) {
        SQLiteDatabase db = this.getReadableDatabase();
        try {
            return db.query(TABLE_REPORTS,
                    null, // Todas las columnas
                    COLUMN_REPORT_ID + " = ?",
                    new String[]{String.valueOf(reportId)},
                    null, null, null);
        } catch (Exception e) {
            Log.e(TAG, "Error en getReportDetails: " + e.getMessage());
            return null;
        }
    }


    // ==== CRUD RECORDATORIOS ====
    public long addReminder(int userId, String title, double latitude, double longitude, float radius, String imagePath) {
        SQLiteDatabase db = this.getWritableDatabase();
        long id = -1;
        try {
            ContentValues values = new ContentValues();
            values.put(COLUMN_REMINDER_USER_FK, userId);
            values.put(COLUMN_TITLE, title);
            values.put(COLUMN_LATITUDE, latitude);
            values.put(COLUMN_LONGITUDE, longitude);
            values.put(COLUMN_RADIUS, radius);
            values.put(COLUMN_IMAGE_PATH, imagePath);

            id = db.insertOrThrow(TABLE_REMINDERS, null, values);
            Log.d(TAG, "Recordatorio agregado con ID: " + id);
        } catch (SQLException e) {
            Log.e(TAG, "Error al agregar recordatorio: " + e.getMessage());
        } finally {
            db.close();
        }
        return id;
    }

    public Cursor getUserReminders(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_REMINDERS,
                new String[]{
                        COLUMN_REMINDER_ID + " AS _id", // ✅ Añadir alias requerido
                        COLUMN_TITLE,
                        COLUMN_CREATED_AT
                },
                COLUMN_REMINDER_USER_FK + " = ?",
                new String[]{String.valueOf(userId)},
                null, null,
                COLUMN_CREATED_AT + " DESC");
    }
}
