package com.example.yannick.camera2test.AGP;

import android.graphics.Bitmap;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.example.yannick.camera2test.GData;
import com.example.yannick.camera2test.GraphicsProcessor;
import com.example.yannick.camera2test.Sqlite.DatabaseManager;

import java.util.ArrayList;

public class AGPOtsu extends AsyncGraphicsProcessor {
    public AGPOtsu(Bitmap bitmap, ProgressBar progressBar, ImageView imageView, DatabaseManager dbm) {
        super(new GraphicsProcessor("DoNothing"), progressBar, imageView, dbm);

        ArrayList<GraphicsProcessor> processors = new ArrayList<>();
        processors.add(new GraphicsProcessor((new GData(bitmap)).asMat(), "ResizeImage"));
        processors.add(new GraphicsProcessor("MedianBlur"));

        processors.add(new GraphicsProcessor("EdgeDetection"));
        processors.add(new GraphicsProcessor("FindContours"));
        processors.add(new GraphicsProcessor("SplitContours"));
        processors.add(new GraphicsProcessor("FilterContours"));

        processors.add(new GraphicsProcessor((new GData(bitmap)).asMat(), "ResizeImage"));
        processors.add(new GraphicsProcessor("GrayScale"));
        processors.add(new GraphicsProcessor("LocalOtsu"));
        processors.add(new GraphicsProcessor("ConvertToBitmap"));

        //processors.add(new GraphicsProcessor(GraphicsProcessor.Task.FindEllipse));
        //processors.add(new GraphicsProcessor((new GData(bitmap)).asMat(), GraphicsProcessor.Task.ResizeImage));
        //processors.add(new GraphicsProcessor(GraphicsProcessor.Task.DrawEllipse));
        //processors.add(new GraphicsProcessor(GraphicsProcessor.Task.DrawContours));
        //processors.add(new GraphicsProcessor(GraphicsProcessor.Task.ConvertToBitmap));

        task = processors;
    }
}
