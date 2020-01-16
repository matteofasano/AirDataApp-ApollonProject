package com.example.airdataapp;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;

public class DBHelper extends SQLiteOpenHelper {

    static final String KEY_SOURCE = "source";
    static final String KEY_PM1 = "pm1";
    static final String KEY_PM25 = "pm25";
    static final String KEY_PM10 = "pm10";
    static final String KEY_TEMPERATURE = "temperature";
    static final String KEY_HUMIDITY = "relative_humidity";
    static final String KEY_LATITUDE = "latitude";
    static final String KEY_LONGITUDE = "longitude";
    static final String KEY_DATE = "date";
    static final String KEY_USER = "userID";

    public DBHelper(Context context) {
        super(context, "ApollonDB" , null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // TODO Auto-generated method stub
        db.execSQL("CREATE TABLE IF NOT EXISTS \"misurazione_aria\" (" +
                "\"id\" INTEGER PRIMARY KEY AUTOINCREMENT," +
                "\"source\" TEXT NOT NULL ," +
                "\"pm1\" DOUBLE NOT NULL ," +
                "\"pm25\" DOUBLE NOT NULL ," +
                "\"pm10\" DOUBLE NOT NULL ," +
                "\"temperature\" DOUBLE NOT NULL ," +
                "\"relative_humidity\" DOUBLE NOT NULL ," +
                "\"latitude\" DOUBLE NOT NULL ," +
                "\"longitude\" DOUBLE NOT NULL ," +
                "\"date\" TEXT NOT NULL ," +
                "\"userID\" TEXT NOT NULL);");

        System.out.println("SONO IN DBHELPER ONCREATE");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO Auto-generated method stub
    }

    public void inserisci_misurazione(String dispositivo, Double pm1_value, Double pm25_value, Double pm10_value, Double temperature_value, Double humidity_value, Double latitude_value, Double longitude_value, String date, String userID)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_SOURCE, dispositivo);
        initialValues.put(KEY_PM1, pm1_value);
        initialValues.put(KEY_PM25, pm25_value);
        initialValues.put(KEY_PM10, pm10_value);
        initialValues.put(KEY_TEMPERATURE, temperature_value);
        initialValues.put(KEY_HUMIDITY, humidity_value);
        initialValues.put(KEY_LATITUDE, latitude_value);
        initialValues.put(KEY_LONGITUDE, longitude_value);
        initialValues.put(KEY_DATE, date);
        initialValues.put(KEY_USER, userID);
        try
        {
            db.insert("misurazione_aria", null,initialValues);
        }
        catch (SQLiteException sqle)
        {
            System.out.println(sqle);        }
    }

    // sort all user in
    public Cursor getDataForCurrentUser(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res =  db.rawQuery( "select * from misurazione_aria where userID="+"\"" + email + "\"  ORDER BY id DESC LIMIT 12", null );
        return res;

    }

    public int getCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res =  db.rawQuery( "select COUNT (*) from misurazione_aria", null );
        res.moveToFirst();
        int count = res.getInt(0);
        return count;
    }

    public ArrayList<String> getAllMeasurement() {
        ArrayList<String> array_list = new ArrayList<String>();

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res =  db.rawQuery( "select * from misurazione_aria", null );
        res.moveToFirst();

        while(res.isAfterLast() == false){
            array_list.add(res.getString(0));
            array_list.add(res.getString(1));
            array_list.add(res.getString(2));
            array_list.add(res.getString(3));
            array_list.add(res.getString(4));
            array_list.add(res.getString(5));
            array_list.add(res.getString(6));
            array_list.add(res.getString(7));
            System.out.println("IN QUERYYYYYYYYY: "+res.getString(7));
            array_list.add(res.getString(8));
            array_list.add(res.getString(9));
            array_list.add(res.getString(10));
            res.moveToNext();
        }
        return array_list;
    }


}