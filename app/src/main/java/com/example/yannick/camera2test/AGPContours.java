package com.example.yannick.camera2test;

import android.graphics.Bitmap;
import android.widget.ImageView;
import android.widget.ProgressBar;

import java.util.ArrayList;

public class AGPContours extends AsyncGraphicsProcessor {
    public AGPContours(Bitmap bitmap, ProgressBar progressBar, ImageView imageView)
    {
        super(new GraphicsProcessor(GraphicsProcessor.Task.DoNothing), progressBar, imageView);

        ArrayList<GraphicsProcessor> processors = new ArrayList<>();
        processors.add(new GraphicsProcessor((new GData(bitmap)).asMat(), GraphicsProcessor.Task.ResizeImage));
        //processors.add(new GraphicsProcessor(GraphicsProcessor.Task.GrayScale));
        processors.add(new GraphicsProcessor(GraphicsProcessor.Task.MedianBlur));
        processors.add(new GraphicsProcessor(GraphicsProcessor.Task.EdgeDetection));
        processors.add(new GraphicsProcessor(GraphicsProcessor.Task.FindContours));
        processors.add(new GraphicsProcessor(GraphicsProcessor.Task.SplitContours));
        processors.add(new GraphicsProcessor(GraphicsProcessor.Task.FilterContours));
        processors.add(new GraphicsProcessor(GraphicsProcessor.Task.DrawContours));
        processors.add(new GraphicsProcessor(GraphicsProcessor.Task.ConvertToBitmap));

        task = processors;
    }
}
