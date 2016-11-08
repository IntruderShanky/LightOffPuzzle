package com.intrusoft.lightsonpuzzle;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper {

    InnerHelper helper;
    String APP_DATABASE = "LightOn.db";
    public String TABLE_NAME = "score";
    SQLiteDatabase database;

    //old version 1
    //current version 1

    public DatabaseHelper(Context context) {
        helper = new InnerHelper(context, APP_DATABASE, null, 1);
    }

    public DatabaseHelper open() {
        database = helper.getWritableDatabase();
        return this;
    }

    public void close() {
        database.close();
    }

    public long insertData(String table, ContentValues values) {
        return database.insert(table, null, values);
    }

    public Cursor getData(String table) {
        return database.query(table, null, null, null, null, null, null);
    }

    public Cursor getSelect(int level) {
        return database.rawQuery("select * from " + TABLE_NAME + " where level =" + level + ";", null);
    }

    public class InnerHelper extends SQLiteOpenHelper {

        public InnerHelper(Context context, String name, CursorFactory factory,
                           int version) {
            super(context, name, factory, version);
            // TODO Auto-generated constructor stub
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("create table " + TABLE_NAME + "(level integer not null, name String not null, time String not null, steps integer not null, score integer not null);");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int arg1, int arg2) {
            // TODO Auto-generated method stub
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        }

    }
}
