package com.example.yannick.camera2test.Sqlite;

import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;

public class FeatureData {
    public String type;
    public MatOfKeyPoint keypoints;
    public Mat descriptor;
    public Mat mask;

    public FeatureData(String type){
        this.type = type;
    }

    public FeatureData(String type, MatOfKeyPoint keypoints, Mat descriptor, Mat mask){
        this.type = type;
        this.keypoints = keypoints;
        this.descriptor = descriptor;
        this.mask = mask;
    }
}
