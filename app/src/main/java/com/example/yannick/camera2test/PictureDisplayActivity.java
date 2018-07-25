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

import com.example.yannick.camera2test.Sqlite.DatabaseManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class PictureDisplayActivity extends AppCompatActivity {

    private ImageView imageView;
    private ProgressBar progressBar;

    private DatabaseManager dbm;

    private Bitmap bitmap;

    private Button button_blur, button_canny, button_contours, button_ellipses, button_save, button_otsu, button_sift;

    static {
        if (!OpenCVLoader.initDebug()) {
            Log.e("OpenCV", "Cannot load OpenCV library");
        }
    }

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

                    (new AGPFindEllipses(bitmap, progressBar, imageView, dbm)).execute();

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

        // load database
        dbm = new DatabaseManager(getApplicationContext());
        dbm.open();

        // Init the debug Buttons
        button_blur = findViewById(R.id.button_blur);
        button_blur.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { (new AGPBlur(bitmap, progressBar, imageView, dbm)).execute(); }
        });
        button_canny = findViewById(R.id.button_canny);
        button_canny.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { (new AGPCannyEdge(bitmap, progressBar, imageView, dbm)).execute(); }
        });
        button_contours = findViewById(R.id.button_contours);
        button_contours.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { (new AGPContours(bitmap, progressBar, imageView, dbm)).execute(); }
        });
        button_ellipses = findViewById(R.id.button_ellipses);
        button_ellipses.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { (new AGPFindEllipses(bitmap, progressBar, imageView, dbm)).execute(); }
        });
        button_save = findViewById(R.id.button_save);
        button_save.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Date dNow = new Date( );
                SimpleDateFormat ft = new SimpleDateFormat("yyyy.MM.dd_hh:mm:ss");
                String testpath = "/sdcard/Pictures/Testpictures/Example" + ft.format(dNow) + ".jpg";

                FileOutputStream out = null;
                try {
                    out = new FileOutputStream(testpath);
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance
                    // PNG is a lossless format, the compression factor (100) is ignored
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (out != null) {
                            out.close();
                            button_save.setEnabled(false);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }
        });
        button_otsu = findViewById(R.id.button_otsu);
        button_otsu.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { (new AGPOtsu(bitmap, progressBar, imageView, dbm)).execute(); }
        });
        button_sift = findViewById(R.id.button_sift);
        button_sift.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { (new AGPSIFT(bitmap, progressBar, imageView, dbm)).execute(); }
        });


        // Load the photo
        Intent intent = getIntent();
        String filepath = intent.getStringExtra("File");

        try {
            //String testpath = "/sdcard/Pictures/Testpictures/otsutest.jpg";
            String testpath = "/sdcard/Pictures/Testpictures/testset/ex00.jpg";
            //String testpath = "/sdcard/Pictures/Testpictures/trainset/Germany_0.jpg";
            bitmap = BitmapFactory.decodeFile(testpath);

            //bitmap = BitmapFactory.decodeStream(this.openFileInput(filepath));
            //bitmap = bitmap.copy( Bitmap.Config.ARGB_8888 , true);

            Log.d("SUCCESS", "Loaded: " + filepath);
            imageView.setImageBitmap(bitmap);
        } catch (Exception e) {
            Log.d("ERROR", "Couldn't load: " + filepath);
            e.printStackTrace();
        }
    }

    public void onResume() {
        super.onResume();

        button_save.setEnabled(true);
        if (!OpenCVLoader.initDebug()) {
            Log.d("OpenCV", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
        } else {
            Log.d("OpenCV", "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // close database connection
        dbm.close();
    }
}
