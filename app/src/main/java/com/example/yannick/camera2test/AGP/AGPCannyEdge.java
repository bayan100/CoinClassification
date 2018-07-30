package com.example.yannick.camera2test.AGP;

import android.graphics.Bitmap;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.example.yannick.camera2test.GData;
import com.example.yannick.camera2test.GraphicsProcessor;
import com.example.yannick.camera2test.Sqlite.DatabaseManager;

import java.util.ArrayList;

public class AGPCannyEdge extends AsyncGraphicsProcessor {
    public AGPCannyEdge(Bitmap bitmap, ProgressBar progressBar, ImageView imageView, DatabaseManager dbm)
    {
        super(new GraphicsProcessor("DoNothing"), progressBar, imageView, dbm);

        ArrayList<GraphicsProcessor> processors = new ArrayList<>();
        processors.add(new GraphicsProcessor((new GData(bitmap)).asMat(), "ResizeImage"));
        processors.add(new GraphicsProcessor("MedianBlur"));
        processors.add(new GraphicsProcessor("EdgeDetection"));
        processors.add(new GraphicsProcessor("ConvertToBitmap"));

        task = processors;
    }
}
