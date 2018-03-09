package org.thosp.yourlocalweather.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Address;
import android.os.Parcel;

import static org.thosp.yourlocalweather.model.WeatherForecastContract.SQL_CREATE_TABLE_WEATHER_FORECAST;
import static org.thosp.yourlocalweather.model.WeatherForecastContract.SQL_DELETE_TABLE_WEATHER_FORECAST;

public class WeatherForecastDbHelper extends SQLiteOpenHelper {

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "WeatherForecast.db";
    private static WeatherForecastDbHelper instance;

    public static WeatherForecastDbHelper getInstance(Context ctx) {
        if (instance == null) {
            instance = new WeatherForecastDbHelper(ctx.getApplicationContext());
        }
        return instance;
    }

    private WeatherForecastDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_TABLE_WEATHER_FORECAST);
    }
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_TABLE_WEATHER_FORECAST);
        onCreate(db);
    }
    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    public void deleteRecordByLocation(Location location) {
        SQLiteDatabase db = getWritableDatabase();
        String selection = WeatherForecastContract.WeatherForecast.COLUMN_NAME_LOCATION_ID + " = ?";
        String[] selectionArgs = {location.getId().toString()};
        db.delete(WeatherForecastContract.WeatherForecast.TABLE_NAME, selection, selectionArgs);
    }

    public void deleteRecordFromTable(Integer recordId) {
        SQLiteDatabase db = getWritableDatabase();
        try {
            String selection = WeatherForecastContract.WeatherForecast._ID + " = ?";
            String[] selectionArgs = {recordId.toString()};
            db.delete(WeatherForecastContract.WeatherForecast.TABLE_NAME, selection, selectionArgs);
        } finally {
            db.close();
        }
    }

    public void saveWeatherForecast(long locationId, long weatherUpdateTime, CompleteWeatherForecast completeWeatherForecast) {
        SQLiteDatabase db = getWritableDatabase();

        WeatherForecastRecord oldWeatherForecast = getWeatherForecast(locationId);

        ContentValues values = new ContentValues();
        values.put(WeatherForecastContract.WeatherForecast.COLUMN_NAME_WEATHER_FORECAST,
                   getCompleteWeatherForecastAsBytes(completeWeatherForecast));
        values.put(WeatherForecastContract.WeatherForecast.COLUMN_NAME_LOCATION_ID, locationId);
        values.put(WeatherForecastContract.WeatherForecast.COLUMN_NAME_LAST_UPDATED_IN_MS, weatherUpdateTime);
        if (oldWeatherForecast == null) {
            db.insert(WeatherForecastContract.WeatherForecast.TABLE_NAME, null, values);
        } else {
            db.update(WeatherForecastContract.WeatherForecast.TABLE_NAME,
                    values,
                    WeatherForecastContract.WeatherForecast.COLUMN_NAME_LOCATION_ID + "=" + locationId,
                    null);
        }
    }

    public WeatherForecastRecord getWeatherForecast(long locationId) {
        SQLiteDatabase db = getReadableDatabase();

        String[] projection = {
                WeatherForecastContract.WeatherForecast.COLUMN_NAME_WEATHER_FORECAST,
                WeatherForecastContract.WeatherForecast.COLUMN_NAME_LAST_UPDATED_IN_MS
        };

        Cursor cursor = db.query(
                WeatherForecastContract.WeatherForecast.TABLE_NAME,
                projection,
                WeatherForecastContract.WeatherForecast.COLUMN_NAME_LOCATION_ID + "=" + locationId,
                null,
                null,
                null,
                null
        );

        if (cursor.moveToNext()) {
            CompleteWeatherForecast completeWeatherForecast = getCompleteWeatherForecastFromBytes(
                    cursor.getBlob(cursor.getColumnIndexOrThrow(WeatherForecastContract.WeatherForecast.COLUMN_NAME_WEATHER_FORECAST)));
            return new WeatherForecastRecord(
                    cursor.getLong(cursor.getColumnIndexOrThrow(WeatherForecastContract.WeatherForecast.COLUMN_NAME_LAST_UPDATED_IN_MS)),
                    completeWeatherForecast);
        } else {
            return null;
        }
    }

    public static CompleteWeatherForecast getCompleteWeatherForecastFromBytes(byte[] addressBytes) {
        final Parcel parcel = Parcel.obtain();
        parcel.unmarshall(addressBytes, 0, addressBytes.length);
        parcel.setDataPosition(0);
        CompleteWeatherForecast completeWeatherForecast = CompleteWeatherForecast.CREATOR.createFromParcel(parcel);
        parcel.recycle();
        return completeWeatherForecast;
    }

    public byte[] getCompleteWeatherForecastAsBytes(CompleteWeatherForecast completeWeatherForecast) {
        final Parcel parcel = Parcel.obtain();
        completeWeatherForecast.writeToParcel(parcel, 0);
        byte[] completeWeatherForecastBytes = parcel.marshall();
        parcel.recycle();
        return completeWeatherForecastBytes;
    }


    public class WeatherForecastRecord {
        long lastUpdatedTime;
        CompleteWeatherForecast completeWeatherForecast;

        public WeatherForecastRecord(long lastUpdatedTime, CompleteWeatherForecast completeWeatherForecast) {
            this.lastUpdatedTime = lastUpdatedTime;
            this.completeWeatherForecast = completeWeatherForecast;
        }

        public long getLastUpdatedTime() {
            return lastUpdatedTime;
        }

        public CompleteWeatherForecast getCompleteWeatherForecast() {
            return completeWeatherForecast;
        }
    }
}