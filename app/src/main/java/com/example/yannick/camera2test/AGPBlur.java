package com.example.yannick.camera2test;

import android.graphics.Bitmap;
import android.widget.ImageView;
import android.widget.ProgressBar;

import java.util.ArrayList;

public class AGPBlur extends AsyncGraphicsProcessor {
    public AGPBlur(Bitmap bitmap, ProgressBar progressBar, ImageView imageView)
    {
        super(new GraphicsProcessor(GraphicsProcessor.Task.DoNothing), progressBar, imageView);

        ArrayList<GraphicsProcessor> processors = new ArrayList<>();
        processors.add(new GraphicsProcessor((new GData(bitmap)).asMat(), GraphicsProcessor.Task.ResizeImage));
        processors.add(new GraphicsProcessor(GraphicsProcessor.Task.MedianBlur));
        processors.add(new GraphicsProcessor(GraphicsProcessor.Task.ConvertToBitmap));

        task = processors;
    }
}
