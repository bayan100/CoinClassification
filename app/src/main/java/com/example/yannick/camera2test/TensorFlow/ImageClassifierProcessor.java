package com.example.yannick.camera2test.TensorFlow;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.util.Log;
import com.example.yannick.camera2test.GraphicsProcessor;
import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public class ImageClassifierProcessor extends GraphicsProcessor {
    private final String MODEL_PATH = "optimized_graph.lite";
    private final String LABEL_PATH = "labels.txt";

    private Interpreter tflite;
    private List<String> labelList;
    private float[][] labelProbArray, filterLabelProbArray;

    private ByteBuffer imgData;
    private int[] intValues;

    public ImageClassifierProcessor(String task){
        super(task);
    }

    public static void initParameters()
    {
        parameter.put("dimBatchSize", 1f);
        parameter.put("dimPixelSize", 3f);

        parameter.put("tensorImageWidth", 224f);
        parameter.put("tensorImageHeight", 224f);

        parameter.put("tensorImageMean", 128f);
        parameter.put("tensorImageSTD", 128f);

        parameter.put("filterStages", 3f);
        parameter.put("filterFactor", 0.4f);
    }

    @Override
    public Status execute() {
        // start Timer
        long starttime = System.nanoTime();

        switch (task) {

        }

        // timer stop
        Log.d("TIMER", "Task " + task + " completed in: " + ((System.nanoTime() - starttime) / 1000000) + " ms");
        return Status.PASSED;
    }


    private void loadInterpreter() throws IOException {
        // load the TensorFlow Interpreter (kinda like Session in python)
        tflite = new Interpreter(loadModelFile(activity));

        labelList = loadLabelList(activity);
        // the image as ByteBuffer
        imgData = ByteBuffer.allocateDirect((int)(4 *
                parameter.get("dimBatchSize") *
                parameter.get("tensorImageWidth") *
                parameter.get("tensorImageHeight") *
                parameter.get("dimPixelSize")));
        imgData.order(ByteOrder.nativeOrder());
        labelProbArray = new float[1][labelList.size()];
        filterLabelProbArray = new float[Math.round(parameter.get("filterStages"))][labelList.size()];
    }

    private void classifyImage(Bitmap bitmap){
        // prepare the image
        convertBitmapToByteBuffer(bitmap);

        // classify it!
        tflite.run(imgData, labelProbArray);

        // smooth the results
        applyFilter();
    }


    private MappedByteBuffer loadModelFile(Activity activity) throws IOException {
        // Memory-map the model file in Assets
        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd(MODEL_PATH);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private List<String> loadLabelList(Activity activity) throws IOException {
        // read the labels list from the assets
        List<String> labelList = new ArrayList<String>();
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(activity.getAssets().open(LABEL_PATH)));
        String line;
        while ((line = reader.readLine()) != null) {
            labelList.add(line);
        }
        reader.close();
        return labelList;
    }

    private void convertBitmapToByteBuffer(Bitmap bitmap) {
        int imageWidth = Math.round(parameter.get("dimImageWidth"));
        int imageHeight = Math.round(parameter.get("dimImageHeight"));
        float imageMean = parameter.get("tensorImageMean");
        float imageSTD = parameter.get("tensorImageSTD");

        intValues = new int[imageWidth * imageHeight];
        imgData.rewind();
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        // Convert the image to floating point.
        int pixel = 0;
        for (int i = 0; i < imageWidth; ++i) {
            for (int j = 0; j < imageHeight; ++j) {
                final int val = intValues[pixel++];
                imgData.putFloat((((val >> 16) & 0xFF)-imageMean)/imageSTD);
                imgData.putFloat((((val >> 8) & 0xFF)-imageMean)/imageSTD);
                imgData.putFloat((((val) & 0xFF)-imageMean)/imageSTD);
            }
        }
    }

    private void applyFilter(){
        int num_labels =  labelList.size();
        int filterStages = Math.round(parameter.get("filterStages"));
        float filterFactor = parameter.get("filterFactor");

        // Low pass filter `labelProbArray` into the first stage of the filter.
        for(int j = 0; j < num_labels; ++j){
            filterLabelProbArray[0][j] += filterFactor * (labelProbArray[0][j] - filterLabelProbArray[0][j]);
        }
        // Low pass filter each stage into the next.
        for (int i = 1; i < filterStages; ++i){
            for(int j = 0; j < num_labels; ++j){
                filterLabelProbArray[i][j] += filterFactor * (filterLabelProbArray[i - 1][j] - filterLabelProbArray[i][j]);

            }
        }

        // Copy the last stage filter output back to `labelProbArray`.
        for(int j = 0; j < num_labels; ++j){
            labelProbArray[0][j] = filterLabelProbArray[filterStages - 1][j];
        }
    }
}
