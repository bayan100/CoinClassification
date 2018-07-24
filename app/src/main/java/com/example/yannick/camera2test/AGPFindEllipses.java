package com.example.yannick.camera2test;

import android.graphics.Bitmap;
import android.util.Log;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.example.yannick.camera2test.Sqlite.DatabaseManager;

import java.util.ArrayList;

public class AGPFindEllipses extends AsyncGraphicsProcessor {
    public AGPFindEllipses(Bitmap bitmap, ProgressBar progressBar, ImageView imageView, DatabaseManager dbm)
    {
        super(new GraphicsProcessor("DoNothing"), progressBar, imageView, dbm);

        ArrayList<GraphicsProcessor> processors = new ArrayList<>();
        processors.add(new GraphicsProcessor((new GData(bitmap)).asMat(), "ResizeImage"));
        processors.add(new GraphicsProcessor("MedianBlur"));processors.add(new GraphicsProcessor("MedianBlur"));
        processors.add(new GraphicsProcessor("EdgeDetection"));
        processors.add(new GraphicsProcessor("FindContours"));
        processors.add(new GraphicsProcessor("SplitContours"));
        processors.add(new GraphicsProcessor("FilterContours"));
        processors.add(new GraphicsProcessor("FindEllipse"));
        processors.add(new GraphicsProcessor((new GData(bitmap)).asMat(), "ResizeImage"));
        processors.add(new GraphicsProcessor("DrawEllipse"));
        processors.add(new GraphicsProcessor("ConvertToBitmap"));
        /*processors.add(new GraphicsProcessor(GraphicsProcessor.Task.MedianBlur));
        processors.add(new GraphicsProcessor(GraphicsProcessor.Task.EdgeDetection));
        processors.add(new GraphicsProcessor(GraphicsProcessor.Task.FindContours));
        processors.add(new GraphicsProcessor(GraphicsProcessor.Task.SplitContours));
        processors.add(new GraphicsProcessor(GraphicsProcessor.Task.DrawContours));
        processors.add(new GraphicsProcessor(GraphicsProcessor.Task.ConvertToBitmap));*/

        task = processors;
    }
}
