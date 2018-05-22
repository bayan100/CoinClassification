package com.example.yannick.camera2test;

import android.graphics.Bitmap;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

public class GData {
    public enum Type
    {
        UNDEF,
        MAT,
        BITMAP,
        BOOLEANMAP
    }

    public Type type = Type.UNDEF;

    private Mat material;
    private Bitmap bitmap;
    private Boolean[][] booleanmap;

    public Mat getMat() { return material; }
    public Bitmap getBitmap() { return bitmap; }
    public Boolean[][] getBooleanmap() { return booleanmap; }

    public void setMat(Mat data) { material = data; type = Type.MAT; bitmap = null; booleanmap = null; }
    public void setBitmap(Bitmap data) { bitmap = data; type = Type.BITMAP; material = null; booleanmap = null; }
    public void setBooleanmap(Boolean[][] data) { booleanmap = data; type = Type.BOOLEANMAP; bitmap = null; material = null; }

    public GData(Bitmap data) {
        bitmap = data;
        type = Type.BITMAP;
    }

    public GData(Mat data) {
        material = data;
        type = Type.MAT;
    }

    public GData(Boolean[][] data){
        booleanmap = data;
        type = Type.BOOLEANMAP;
    }

    public GData asMat()
    {
        if (type == Type.BITMAP) {
            Mat image = new Mat();
            Bitmap bmp32 = bitmap.copy(Bitmap.Config.ARGB_8888, true);
            Utils.bitmapToMat(bmp32, image);
            material = image;
            bitmap = null;

            type = Type.MAT;
        }
        return this;
    }

    public GData asBitmap()
    {
        if (type == Type.MAT) {
            Bitmap bmp32 = Bitmap.createBitmap(material.width(), material.height(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(material, bmp32);
            bitmap = bmp32;
            material = null;

            type = Type.BITMAP;
        }
        return this;
    }
}
