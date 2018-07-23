package com.example.yannick.camera2test.Sqlite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.opencv.core.Mat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

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
        return fromDBBytes(bytes);
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
            values.put("Keypoints", toDBBytes(feature.keypoints));
            values.put("Descriptor", toDBBytes(feature.descriptor));
            values.put("Mask", toDBBytes(feature.mask));
            database.insert("Feature", null, values);
        }
    }

    public Dictionary<String, List<FeatureData>> getFeaturesByType(String type){
        Dictionary<String, List<FeatureData>> data = new Hashtable<>();

        String sql = "SELECT * FROM Feature WHERE Type = '" + type + "';";
        Cursor cursor = database.rawQuery(sql, null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            FeatureData feature = new FeatureData(type);

        }
        cursor.close();

        return data;
    }

    private Mat fromDBBytes(byte[] data){
        // first 12 bytes are for storing type, width and height of the mat
        int type   = fromByte(data, 0);
        int width  = fromByte(data, 4);
        int heigth = fromByte(data, 8);

        Log.d("SQL", type + ", " + width + ", "+ heigth);

        Mat mat = new Mat(width, heigth, type);
        mat.put(0,0, Arrays.copyOfRange(data, 12, data.length));
        return mat;
    }

    private byte[] toDBBytes(Mat data){
        // first 12 bytes are for storing type, width and height of the mat
        byte[] desc = new byte[12];
        fromInteger(desc, 0, data.type());
        fromInteger(desc, 4, data.rows());
        fromInteger(desc, 8, data.cols());

        int nbytes = (int)(data.total() * data.elemSize());
        byte[] bytes = new byte[ (int)nbytes ];
        data.get(0, 0,bytes);

        // concatenate both arrays
        byte[] total = new byte[nbytes + 12];
        System.arraycopy(desc, 0, total, 0, desc.length);
        System.arraycopy(bytes, 0, total, 12, nbytes);
        return total;
    }

    private int fromByte(byte[] bytes, int start){
        return  (bytes[start    ]<<24)&0xff000000|
                (bytes[start + 1]<<16)&0x00ff0000|
                (bytes[start + 2]<< 8)&0x0000ff00|
                (bytes[start + 3]    )&0x000000ff;
    }

    private void fromInteger(byte[] bytes, int start, int value) {
        bytes[start    ] = (byte)(value >>> 24);
        bytes[start + 1] = (byte)(value >>> 16);
        bytes[start + 2] = (byte)(value >>> 8);
        bytes[start + 3] = (byte)value;
    }
}
