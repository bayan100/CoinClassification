package com.example.yannick.camera2test;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.util.Log;

import com.example.yannick.camera2test.Sqlite.CoinData;
import com.example.yannick.camera2test.Sqlite.FeatureData;
import com.example.yannick.camera2test.Sqlite.MatSerializer;

import org.opencv.android.Utils;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.DMatch;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.*;
import org.opencv.imgproc.Imgproc;

import org.opencv.xfeatures2d.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.opencv.core.Core.BORDER_CONSTANT;

public class SIFTProcessor extends GraphicsProcessor {
    //private Mat data;
    private RotatedRect ellipse;

    private int knn = 2;
    private float loweThreshold = 0.75f;

    public SIFTProcessor(String task){
        super(task);
    }

    @Override
    public Status execute() {
        // start Timer
        long starttime = System.nanoTime();

        switch (task){
            case "SIFT":
                // generate features for he input
                List<RotatedRect> ellipses = (List<RotatedRect>)additionalData.get("ellipses");
                Mat mat = data.getMat();

                float scale = mat.height() / (float)mat.width();
                float width = parameter.get("Rwidth") > 0 ? parameter.get("Rwidth") : parameter.get("Rheight") / scale;
                float height = parameter.get("Rheight") > 0 ? parameter.get("Rheight") : parameter.get("Rwidth") * scale;

                // scale ellipse
                RotatedRect rect = ellipses.get(0);
                rect = new RotatedRect(new Point(rect.center.x * (mat.width() / width), rect.center.y * (mat.height() / height)),
                        new Size(rect.size.width * (mat.width() / width), rect.size.height / (mat.height() / height)), rect.angle);

                ellipse = rect;
                /*FeatureData input = generateSIFTFeatures(mat, rect);

                // match
                matchAgainstCoins(input);*/
                Mat m = run();
                data.setMat(m);
                break;

            case "GenerateSIFT":
                String[] files = (String[])additionalData.get("images");
                generateSIFTBunch(files);
                break;
        }

        // timer stop
        Log.d("TIMER", "Task " + task + " completed in: " + ((System.nanoTime() - starttime) / 1000000) + " ms");
        return Status.PASSED;
    }

    private Mat loadGrayImage(String name){
        String testpath = "/sdcard/Pictures/Testpictures/trainset/" + name;
        Bitmap bitmap = BitmapFactory.decodeFile(testpath);

        Mat data = new Mat();
        Bitmap bmp32 = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(bmp32, data);
        Imgproc.cvtColor(data, data, Imgproc.COLOR_RGB2GRAY);
        return data;
    }
    
    private void generateSIFTBunch(String[] files){
        // gather the countrys
        List<String> countrys = dbm.getCountrys();
        Map<String, CoinData[]> coins = dbm.getCoins();

        // generate SIFT-Features for all given Coin-Files
        for (int i = 0; i < files.length; i++) {
            Mat file = loadGrayImage(files[i]);
            FeatureData feature = generateSIFTFeatures(file);

            // read the Country and Coin-type from the filename
            Log.d("SIFT", files[i]);
            String[] tmp = files[i].split("_");
            String country = tmp[0];
            int value = Integer.parseInt(tmp[1].substring(0, tmp[1].indexOf(".")));

            Log.d("SIFT","size: keypoints: " + feature.keypoints.size() + ", desc: " + feature.descriptor.size() + ", mask: " + feature.mask.size());
            Log.d("SIFT", "bytes: keypoints: " + MatSerializer.matToBytes(feature.keypoints).length + ", desc: " + MatSerializer.matToBytes(feature.descriptor).length + ", mask: " + MatSerializer.matToBytes(feature.mask).length);
            // if country not already there, add it
            if(!countrys.contains(country)){
                countrys.add(country);
                coins.put(country, new CoinData[3]);
                dbm.putCountry(country);
            }
            // if coin not there add as well
            if(coins.get(country) == null || coins.get(country)[value] == null){
                dbm.putCoin(new CoinData(value, country));
            }

            // save to database
            dbm.putFeature(feature, new CoinData(value, country));
        }
    }

    private FeatureData generateSIFTFeatures(Mat data, RotatedRect ellipse){
        Mat mask = Mat.zeros(data.size(), data.type());
        Imgproc.ellipse(mask, ellipse, new Scalar(255,255,255), -1);

        MatOfKeyPoint keypoints = new MatOfKeyPoint();
        Mat descriptors = new Mat();
        SIFT detector = SIFT.create(256, 3, 0.04, 10, 1.6);
        detector.detectAndCompute(data, mask, keypoints, descriptors);

        return new FeatureData("SIFT", keypoints, descriptors, mask);
    }

    private FeatureData generateSIFTFeatures(Mat data){
        return generateSIFTFeatures(data, new RotatedRect(new Point(data.size().width / 2, data.size().height / 2), data.size(), 0));
    }

    private void matchAgainstCoins(FeatureData input){
        // load all the features from the database
        Map<CoinData, FeatureData> features = dbm.getFeaturesByType("SIFT");

        // choose matcher
        //DescriptorMatcher matcher = BFMatcher.create(DescriptorMatcher.BRUTEFORCE_L1);
        //DescriptorMatcher matcher = FlannBasedMatcher.create();
        DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.FLANNBASED);

        // iterate the features and match the input against hte coins
        for (CoinData cd : features.keySet()){
            double score = match(input, features.get(cd), matcher);
            Log.d("MATCH", "Country: " + cd.country + ", value: " + cd.value + " -> score: " + score);
        }
    }

    static int c = 0;
    private double match(FeatureData input, FeatureData feature, DescriptorMatcher matcher){
        MatOfDMatch match = new MatOfDMatch();
        matcher.match(input.descriptor, feature.descriptor, match);

        double maxDist = 0; double minDist = Double.MAX_VALUE;
        // Quick calculation of max and min distances between keypoints
        List<DMatch> matches = match.toList();
        Log.d("MATCH", "size: " + matches.size());
        for( int i = 0; i < matches.size(); i++ )
        {
            double dist = matches.get(i).distance;
            if( dist < minDist ) minDist = dist;
            if( dist > maxDist ) maxDist = dist;
        }
        Log.d("MATCH", "min: " + minDist + " max: "+ maxDist);

        // get only "good" matches (whose distance is less than 2 * minDist)
        int found = 0;
        for( int i = 0; i < matches.size(); i++ ){
            if(matches.get(i).distance <= 2 * minDist)
                found++;
        }

        //MatOfPoint2f[] points = convertKeypoints(input.keypoints, feature.keypoints, match);
        //Mat homography = Calib3d.findHomography(points[0], points[1], Calib3d.RANSAC, 1);

        // Resource
        if(c == 2) {
            String testpath = "/sdcard/Pictures/Testpictures/trainset/Germany_0.jpg";
            Bitmap bitmap = BitmapFactory.decodeFile(testpath);

            Mat data2 = new Mat();
            Bitmap bmp32 = bitmap.copy(Bitmap.Config.ARGB_8888, true);
            Utils.bitmapToMat(bmp32, data2);
            Imgproc.cvtColor(data2, data2, Imgproc.COLOR_RGB2GRAY);

            Mat imgMatches = new Mat();

            Log.d("MATCH", "drawMatches");
            Features2d.drawMatches(data.getMat(), input.keypoints, data2, feature.keypoints, match, imgMatches, Scalar.all(-1),
                    Scalar.all(-1), new MatOfByte(), Features2d.NOT_DRAW_SINGLE_POINTS);

            data.setMat(imgMatches);
        }
        c++;

        // return the ratio between "good" and bad matches
        return (double)found / matches.size();
    }

    private MatOfPoint2f[] convertKeypoints(MatOfKeyPoint input, MatOfKeyPoint feature, MatOfDMatch match){
        List<Point> p1 = new ArrayList<>(), p2 = new ArrayList<>();

        List<DMatch> dmatches =  match.toList();
        List<KeyPoint> kpointsIn = input.toList();
        List<KeyPoint> kpointsFe = feature.toList();
        for (int i = 0; i < dmatches.size(); i++) {
            KeyPoint k1 = kpointsIn.get(dmatches.get(i).imgIdx);
            KeyPoint k2 = kpointsFe.get(dmatches.get(i).trainIdx);

            p1.add(k1.pt);
            p2.add(k2.pt);
        }

        MatOfPoint2f res1 = new MatOfPoint2f(), res2 = new MatOfPoint2f();
        res1.fromList(p1);
        res2.fromList(p2);
        return new MatOfPoint2f[]{res1, res2};
    }

    private double matchKnn(FeatureData input, FeatureData feature, DescriptorMatcher matcher, boolean thresholding){

        List<MatOfDMatch> knnMatches = new ArrayList<>();
        matcher.knnMatch(input.descriptor, feature.descriptor, knnMatches, knn);

        if(thresholding) {
            int found = 0;
            //-- Filter matches using the Lowe's ratio test
            float ratioThresh = loweThreshold;
            //List<DMatch> listOfGoodMatches = new ArrayList<>();
            for (int i = 0; i < knnMatches.size(); i++) {
                if (knnMatches.get(i).rows() > 1) {
                    DMatch[] matches = knnMatches.get(i).toArray();
                    if (matches[0].distance < ratioThresh * matches[1].distance) {
                        //listOfGoodMatches.add(matches[0]);
                        found++;
                    }
                }
            }

        }
        return 0;
    }

    public Mat run2(){

        Mat d = data.getMat();

        // generate mask form found ellipse
        Mat mask = Mat.zeros(d.size(), d.type());
        Imgproc.ellipse(mask, ellipse, new Scalar(255,255,255), -1);

        MatOfKeyPoint keypoints = new MatOfKeyPoint();
        Mat descriptors = new Mat();

        SIFT sift = SIFT.create(0, 3, 0.04, 10, 1.6);
        sift.detectAndCompute(d, mask, keypoints, descriptors);



        Features2d.drawKeypoints(d, keypoints, d);
        return d;
    }

    public Mat run(){

        Mat d = data.getMat();

        // generate mask form found ellipse
        Mat mask = Mat.zeros(d.size(), d.type());
        Imgproc.ellipse(mask, ellipse, new Scalar(255,255,255), -1);


        int len = Math.max(d.cols(), d.rows());
        Point pt = new Point(len/2., len/2.);
        Mat r = Imgproc.getRotationMatrix2D(pt, 40, 1.0);

        //Imgproc.warpAffine(d, d, r, new Size(len, len), Imgproc.INTER_LINEAR, BORDER_CONSTANT, new Scalar(255,255,255));


        //Core.bitwise_and(data, mask, data);

        MatOfKeyPoint keypoints = new MatOfKeyPoint();
        Mat descriptors = new Mat();

        //SIFT detector = SIFT.create(0, 3, 0.04, 30, 1.2);
        //SIFT detector = SIFT.create(256, 3, 0.04, 10, 1.6);
        Feature2D detector = BRISK.create();
        detector.detectAndCompute(d, mask, keypoints, descriptors);

        Log.d("SIFT", "key: " + keypoints.size() + ", desc: " + descriptors.size());

        // Resource
        String testpath = "/sdcard/Pictures/Testpictures/trainset/Germany_0.jpg";
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
        d = matchBFMatcher(descriptors, descriptors2, d, data2, keypoints, keypoints2);
        //d = matchFLANNMatcher(descriptors, descriptors2, d, data2, keypoints, keypoints2);

        return d;
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
        BFMatcher matcher = BFMatcher.create(DescriptorMatcher.BRUTEFORCE, false);
        List<MatOfDMatch> knnMatches = new ArrayList<>();
        matcher.knnMatch(descriptors, descriptors2, knnMatches, 2);

        Log.d("MATCH", "len: " + knnMatches.size());

        //-- Filter matches using the Lowe's ratio test
        float ratioThresh = 1.7f;
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

        Log.d("MATCH", "len: " + goodMatches.size());
        //-- Draw matches
        Mat imgMatches = new Mat();
        Features2d.drawMatches(data, keypoints, data2, keypoints2, goodMatches, imgMatches, Scalar.all(-1),
                Scalar.all(-1), new MatOfByte(), Features2d.NOT_DRAW_SINGLE_POINTS);

        return imgMatches;
    }
}
