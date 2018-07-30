package com.example.yannick.camera2test.AGP;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.example.yannick.camera2test.GData;
import com.example.yannick.camera2test.GraphicsProcessor;
import com.example.yannick.camera2test.Sqlite.DatabaseManager;
import com.example.yannick.camera2test.TensorFlow.ImageClassifierProcessor;

import java.util.ArrayList;

public class AGPTensorFlow extends AsyncGraphicsProcessor {
    public AGPTensorFlow(Bitmap bitmap, ProgressBar progressBar, ImageView imageView, Activity activity) {
        super(new GraphicsProcessor("DoNothing"), progressBar, imageView, activity);

        ArrayList<GraphicsProcessor> processors = new ArrayList<>();

        processors.add(new GraphicsProcessor((new GData(bitmap)).asMat(),"DoNothing"));
        ImageClassifierProcessor p = new ImageClassifierProcessor("test");
        processors.add(p);
        processors.add(new GraphicsProcessor("ConvertToBitmap"));

        task = processors;
    }
}
