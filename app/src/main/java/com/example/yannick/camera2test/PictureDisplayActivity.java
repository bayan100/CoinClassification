package com.example.yannick.camera2test;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class PictureDisplayActivity extends AppCompatActivity {

    private ImageView imageView;
    private ProgressBar progressBar;

    private Bitmap bitmap;

    private Button button_blur, button_canny, button_contours, button_ellipses;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i("OpenCV", "OpenCV loaded successfully");

                    // Load image if necessary
                    if (bitmap == null) {
                        Log.d("Q", "NECESSARY!");
                        Intent intent = getIntent();
                        String filepath = intent.getStringExtra("File");
                        try {
                            bitmap = BitmapFactory.decodeStream(this.mAppContext.openFileInput(filepath));
                        } catch (Exception e) {
                            Log.d("ERROR", "Couldn't load: " + filepath);
                        }
                    }

                    // start spinner
                    progressBar.setVisibility(progressBar.VISIBLE);

                    // Do manipulation
                    GraphicsProcessor.initParameters();
                    //GraphicsProcessor.parameter.put("MBksize", 7f);
                    //GraphicsProcessor.parameter.put("EDthreshold1", 30f);
                    //GraphicsProcessor.parameter.put("EDthreshold2", 100f);

                    (new AGPFindEllipses(bitmap, progressBar, imageView)).execute();

                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picture_display);

        imageView = findViewById(R.id.image_view);
        progressBar = findViewById(R.id.pBar);

        // Init the debug Buttons
        button_blur = findViewById(R.id.button_blur);
        button_blur.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { (new AGPBlur(bitmap, progressBar, imageView)).execute(); }
        });
        button_canny = findViewById(R.id.button_canny);
        button_canny.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { (new AGPCannyEdge(bitmap, progressBar, imageView)).execute(); }
        });
        button_contours = findViewById(R.id.button_contours);
        button_contours.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { (new AGPContours(bitmap, progressBar, imageView)).execute(); }
        });
        button_ellipses = findViewById(R.id.button_ellipses);
        button_ellipses.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { (new AGPFindEllipses(bitmap, progressBar, imageView)).execute(); }
        });

        // Load the photo
        Intent intent = getIntent();
        String filepath = intent.getStringExtra("File");

        try {
            //String testpath = "/sdcard/Pictures/Testpictures/2Euro2.jpg";
            //bitmap = BitmapFactory.decodeFile(testpath);

            //Log.d("BITMAP", (bitmap == null) + "");

            bitmap = BitmapFactory.decodeStream(this.openFileInput(filepath));
            bitmap = bitmap.copy( Bitmap.Config.ARGB_8888 , true);

            Log.d("SUCCESS", "Loaded: " + filepath);
            imageView.setImageBitmap(bitmap);
        } catch (Exception e) {
            Log.d("ERROR", "Couldn't load: " + filepath);
        }
    }

    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d("OpenCV", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d("OpenCV", "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }
}
