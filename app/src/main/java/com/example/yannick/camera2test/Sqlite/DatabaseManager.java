package com.example.yannick.camera2test.Sqlite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

public class DatabaseManager {

    SQLiteDatabase database;
    DatabaseHelper helper;

    public DatabaseManager(Context context){
        helper = new DatabaseHelper(context);
    }

    public void open(){
        try {
            database = helper.getWritableDatabase();
            Log.d("SQL", "p: " + database.getPath() + " v: " + database.getVersion());
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public void close(){
        try {
            helper.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    /*
    public void putTest(Mat test){
        ContentValues values = new ContentValues();
        values.put("Type", "TestMat");
        values.put("Coin_id", 0);
        values.put("Keypoints", new byte[]{(byte)100});
        Log.d("SQL", "put1");
        values.put("Descriptor", toDBBytes(test));
        values.put("Mask", new byte[]{(byte)100});
        database.insert("Feature", null, values);
    }

    public Mat getTest(){
        String sql = "SELECT Descriptor FROM Feature WHERE " +
                "Type = 'TestMat';";
        Cursor cursor = database.rawQuery(sql, null);
        cursor.moveToFirst();
        byte[] bytes = cursor.getBlob(0);
        cursor.close();

        database.delete("Feature", "Type = ?", new String[]{"TestMat"});

        return fromDBBytes(bytes);
    }*/

    public Map<String, CoinData[]> getCoins(){
        Map<String, CoinData[]> data = new HashMap<>();

        String sql = "SELECT Name, Value FROM Coin, Country WHERE Coin.Country_id = Country.id;";
        Cursor cursor = database.rawQuery(sql, null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            String country = cursor.getString(0);
            int value = cursor.getInt(1);

            if(!data.containsKey(country)){
                data.put(country, new CoinData[3]);
            }
            data.get(country)[value] = new CoinData(value, country);
            cursor.moveToNext();
        }
        cursor.close();
        return data;
    }

    public void putFeatures(List<FeatureData> featureData, CoinData coin){
        // find the id corresponding to the coin
        String sql = "SELECT id FROM Coin, Country WHERE " +
                "Coin.Country_id = Country.id AND " +
                "County.Name = '" + coin.country + "' AND " +
                "Coin.Value = " + coin.value + ";";
        Cursor cursor = database.rawQuery(sql, null);
        cursor.moveToFirst();
        int coinId = cursor.getInt(0);
        cursor.close();

        // put the feature data into the database
        for (FeatureData feature: featureData) {
            ContentValues values = new ContentValues();
            values.put("Type", feature.type);
            values.put("Coin_id", coinId);
            values.put("Keypoints", MatSerializer.matToBytes(feature.keypoints));
            values.put("Descriptor", MatSerializer.matToBytes(feature.descriptor));
            values.put("Mask", MatSerializer.matToBytes(feature.mask));
            database.insert("Feature", null, values);
        }
    }

    public void putFeature(FeatureData feature, CoinData coin) {
        // find the id corresponding to the coin
        String sql = "SELECT Coin.id FROM Coin, Country WHERE " +
                "Coin.Country_id = Country.id AND " +
                "Country.Name = '" + coin.country + "' AND " +
                "Coin.Value = " + coin.value + ";";
        Cursor cursor = database.rawQuery(sql, null);
        cursor.moveToFirst();
        int coinId = cursor.getInt(0);
        cursor.close();

        // put the feature data into the database
        ContentValues values = new ContentValues();
        values.put("Type", feature.type);
        values.put("Coin_id", coinId);
        values.put("Keypoints", MatSerializer.matToBytes(feature.keypoints));
        values.put("Descriptor", MatSerializer.matToBytes(feature.descriptor));
        values.put("Mask", MatSerializer.matToBytes(feature.mask));
        database.insert("Feature", null, values);

    }

    public Map<CoinData, FeatureData> getFeaturesByType(String type){
        Map<CoinData, FeatureData> data = new HashMap<>();

        String sql = "SELECT Country.Name, Coin.Value, Feature.Keypoints, Feature.Descriptor, Feature.Mask " +
                "FROM Feature, Coin, Country WHERE " +
                "Feature.Coin_id = Coin.id AND " +
                "Coin.Country_id = Country.id AND " +
                "Type = '" + type + "';";
        Cursor cursor = database.rawQuery(sql, null);
        cursor.moveToFirst();
        int c = 0;
        while (!cursor.isAfterLast() &&  c < 10) {

            //Log.d("SQL", "pos: " + cursor.getPosition());

            //Log.d("SQL", cursor.getColumnNames()[0] + ": " + cursor.getColumnIndex(cursor.getColumnNames()[0]));

            FeatureData feature = new FeatureData(type);
            feature.keypoints = new MatOfKeyPoint(MatSerializer.matFromBytes(cursor.getBlob(2)));
            feature.descriptor = MatSerializer.matFromBytes(cursor.getBlob(3));
            feature.mask = MatSerializer.matFromBytes(cursor.getBlob(4));
            CoinData coinData = new CoinData(cursor.getInt(1), cursor.getString(0));

            data.put(coinData, feature);
            c++;
            cursor.moveToNext();
        }
        cursor.close();

        return data;
    }

    public void putCoin(CoinData coinData){
        // find the id corresponding to the coin
        String sql = "SELECT Country.id FROM Country WHERE " +
                "Country.Name = '" + coinData.country + "';";
        Cursor cursor = database.rawQuery(sql, null);
        cursor.moveToFirst();
        int countryId = cursor.getInt(0);
        cursor.close();

        // put the feature data into the database
        ContentValues values = new ContentValues();
        values.put("Country_id", countryId);
        values.put("Value", coinData.value);
        database.insert("Coin", null, values);
    }

    public List<String> getCountrys(){
        List<String> data = new ArrayList<>();

        String sql = "SELECT Name FROM Country ;";
        Cursor cursor = database.rawQuery(sql, null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            data.add(cursor.getString(0));
            cursor.moveToNext();
        }
        cursor.close();

        return data;
    }
    public void putCountry(String country){
        ContentValues values = new ContentValues();
        values.put("Name", country);
        database.insert("Country", null, values);
    }
}
