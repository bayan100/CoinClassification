package com.example.yannick.camera2test;

import android.graphics.Bitmap;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.example.yannick.camera2test.Sqlite.DatabaseManager;

import org.opencv.xfeatures2d.SIFT;

import java.util.ArrayList;

public class AGPSIFT extends AsyncGraphicsProcessor {
    public AGPSIFT(Bitmap bitmap, ProgressBar progressBar, ImageView imageView, DatabaseManager dbm) {
        super(new GraphicsProcessor("DoNothing"), progressBar, imageView, dbm);

        ArrayList<GraphicsProcessor> processors = new ArrayList<>();
        processors.add(new GraphicsProcessor((new GData(bitmap)).asMat(), "ResizeImage"));
        processors.add(new GraphicsProcessor("MedianBlur"));
        processors.add(new GraphicsProcessor("EdgeDetection"));
        processors.add(new GraphicsProcessor("FindContours"));
        processors.add(new GraphicsProcessor("SplitContours"));
        processors.add(new GraphicsProcessor("FilterContours"));
        processors.add(new GraphicsProcessor("FindEllipse"));

        processors.add(new GraphicsProcessor((new GData(bitmap)).asMat(), "ResizeImage"));
        processors.add(new GraphicsProcessor("GrayScale"));
        /*SIFTProcessor p = new SIFTProcessor("GenerateSIFT");
        p.addAdditionaData("images", new String[]{
                "Germany_0.jpg",
                "Germany_1.jpg",
                "Germany_2.jpg"});
        processors.add(p);*/
        processors.add(new SIFTProcessor("SIFT"));
        processors.add(new GraphicsProcessor("ConvertToBitmap"));

        task = processors;
    }
}
