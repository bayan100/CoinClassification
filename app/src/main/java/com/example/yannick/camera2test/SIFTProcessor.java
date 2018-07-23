package com.example.yannick.camera2test;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.DMatch;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.*;
import org.opencv.imgproc.Imgproc;

import org.opencv.xfeatures2d.*;

import java.util.ArrayList;
import java.util.List;

public class SIFTProcessor {
    private Mat data;
    private RotatedRect ellipse;

    public SIFTProcessor(Mat source, RotatedRect ellipse){
        this.data = source;
        this.ellipse = ellipse;
    }

    public Mat run2(){

        // generate mask form found ellipse
        Mat mask = Mat.zeros(data.size(), data.type());
        Imgproc.ellipse(mask, ellipse, new Scalar(255,255,255), -1);

        MatOfKeyPoint keypoints = new MatOfKeyPoint();
        Mat descriptors = new Mat();

        SIFT sift = SIFT.create(0, 3, 0.04, 10, 1.6);
        sift.detectAndCompute(data, mask, keypoints, descriptors);



        Features2d.drawKeypoints(data, keypoints, data);
        return data;
    }

    public Mat run(){

        // generate mask form found ellipse
        Mat mask = Mat.zeros(data.size(), data.type());
        Imgproc.ellipse(mask, ellipse, new Scalar(255,255,255), -1);

        //Core.bitwise_and(data, mask, data);

        MatOfKeyPoint keypoints = new MatOfKeyPoint();
        Mat descriptors = new Mat();

        //SIFT detector = SIFT.create(0, 3, 0.04, 30, 1.2);
        SURF detector = SURF.create();
        detector.detectAndCompute(data, mask, keypoints, descriptors);

        // Resource
        String testpath = "/sdcard/Pictures/Testpictures/trainset/Germany_2euro.jpg";
        Bitmap bitmap = BitmapFactory.decodeFile(testpath);

        Mat data2 = new Mat();
        Bitmap bmp32 = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(bmp32, data2);
        Imgproc.cvtColor(data2, data2, Imgproc.COLOR_RGB2GRAY);

        Mat mask2 = Mat.zeros(data2.size(), data2.type());
        Imgproc.ellipse(mask2, new RotatedRect(new Point(data2.size().width / 2, data2.size().height / 2), data2.size(), 0), new Scalar(255,255,255), -1);

        MatOfKeyPoint keypoints2 = new MatOfKeyPoint();
        Mat descriptors2 = new Mat();
        detector.detectAndCompute(data2, mask2, keypoints2, descriptors2);

        // Matching
        //data = matchBFMatcher(descriptors, descriptors2, data, data2, keypoints, keypoints2);
        data = matchFLANNMatcher(descriptors, descriptors2, data, data2, keypoints, keypoints2);

        return data;
    }

    private Mat matchFLANNMatcher(Mat descriptors, Mat descriptors2, Mat data, Mat data2, MatOfKeyPoint keypoints, MatOfKeyPoint keypoints2){

        FlannBasedMatcher matcher = FlannBasedMatcher.create();
        List<MatOfDMatch> knnMatches = new ArrayList<>();
        //matcher.knnMatch(descriptors, descriptors2, knnMatches, 2);
        MatOfDMatch match = new MatOfDMatch();

        matcher.match(descriptors, descriptors2, match);

        //-- Filter matches using the Lowe's ratio test
       /* float ratioThresh = 0.87f;
        List<DMatch> listOfGoodMatches = new ArrayList<>();
        for (int i = 0; i < knnMatches.size(); i++) {
            if (knnMatches.get(i).rows() > 1) {
                DMatch[] matches = knnMatches.get(i).toArray();
                if (matches[0].distance < ratioThresh * matches[1].distance) {
                    listOfGoodMatches.add(matches[0]);
                }
            }
        }
        MatOfDMatch goodMatches = new MatOfDMatch();
        goodMatches.fromList(listOfGoodMatches);
        //-- Draw matches
        Mat imgMatches = new Mat();
        Features2d.drawMatches(data, keypoints, data2, keypoints2, goodMatches, imgMatches, Scalar.all(-1),
                Scalar.all(-1), new MatOfByte(), Features2d.NOT_DRAW_SINGLE_POINTS);*/

        Mat imgMatches = new Mat();
        Features2d.drawMatches(data, keypoints, data2, keypoints2, match, imgMatches, Scalar.all(-1),
                Scalar.all(-1), new MatOfByte(), Features2d.NOT_DRAW_SINGLE_POINTS);

        return imgMatches;
    }

    private Mat matchBFMatcher(Mat descriptors, Mat descriptors2, Mat data, Mat data2, MatOfKeyPoint keypoints, MatOfKeyPoint keypoints2){
        BFMatcher matcher = BFMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING, false);
        List<MatOfDMatch> knnMatches = new ArrayList<>();
        matcher.knnMatch(descriptors, descriptors2, knnMatches, 2);

        //-- Filter matches using the Lowe's ratio test
        float ratioThresh = 0.7f;
        List<DMatch> listOfGoodMatches = new ArrayList<>();
        for (int i = 0; i < knnMatches.size(); i++) {
            if (knnMatches.get(i).rows() > 1) {
                DMatch[] matches = knnMatches.get(i).toArray();
                if (matches[0].distance < ratioThresh * matches[1].distance) {
                    listOfGoodMatches.add(matches[0]);
                }
            }
        }
        MatOfDMatch goodMatches = new MatOfDMatch();
        goodMatches.fromList(listOfGoodMatches);
        //-- Draw matches
        Mat imgMatches = new Mat();
        Features2d.drawMatches(data, keypoints, data2, keypoints2, goodMatches, imgMatches, Scalar.all(-1),
                Scalar.all(-1), new MatOfByte(), Features2d.NOT_DRAW_SINGLE_POINTS);

        return imgMatches;
    }
}
