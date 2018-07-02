package com.example.yannick.camera2test;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.util.Log;

import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;

public class Contour implements Comparable {
    MatOfPoint data;
    Point[] points;
    MatOfPoint convexContour;
    double[] size;
    double area;

    Contour(MatOfPoint data) {
        this.data = data;

        // convert and find the dimension in space
        points = data.toArray();
        size = new double[4];
        size[0] = points[0].x;
        size[2] = points[0].y;

        for (Point p : points) {
            if (p.x < size[0]) {
                size[0] = p.x;
            }
            if (p.x > size[1]) {
                size[1] = p.x;
            }
            if (p.y < size[2]) {
                size[2] = p.y;
            }
            if (p.y > size[3]) {
                size[3] = p.y;
            }
        }
        area = (size[1] - size[0]) * (size[3] - size[2]);

        //analyseContour();
    }

    int[] getEndDirection(){
        // simple linear regression on the last n points
        int n = 5;

        // find x- and y-mean
        double xMean = 0, yMean = 0;
        for (int i = points.length - n; i < points.length; i++) {
            xMean += points[i].x;
            yMean += points[i].y;
        }
        xMean /= n;
        yMean /= n;

        double bx2 = 0, bxy = 0;
        for (int i = points.length - n; i < points.length; i++) {
            bxy += (points[i].x - xMean) * (points[i].y - yMean);
            bx2 += (points[i].x - xMean) * (points[i].x - xMean);
        }
        double m = bxy / bx2;

        // convert to direction
        int direction = (Math.abs(m) < 0.5 ? 2 : 0) + (m < 0 ? 1 : 0);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < points.length; i++) {
            if(i % 10 == 0)
                sb.append("\n");
            sb.append("(" );
            sb.append(points[i].x);
            sb.append(",");
            sb.append(points[i].y);
            sb.append("), ");
        }
        Log.d("OTSU", sb.toString());

        return new int[] {(int)points[points.length - 1].x, (int)points[points.length - 1].y, direction};
    }

    private void analyseContour()
    {
        // start by getting the convex hull with some parameters
        calcConvexHull();

        // Check whether Contour is mostly a straight line
        calcLineConfidence();

        // Split contour?
    }

    private void calcConvexHull()
    {
        /*
        // get the convex hull from the contour
        MatOfInt hull = new MatOfInt();
        Imgproc.convexHull(data, hull);

        // convert to points
        int[] p = hull.toArray();
        Point[] q = data.toArray();
        Point[] points = new Point[p.length];

        for (int j = 0; j < p.length; j++) {
            points[j] = q[p[j]];
        }

        // if contour is not closed, rearrange array to have the endpoints at arraystart and -end
        // find the max distance from one point to another and compare to the average distance
        double maxDistance = 0, avDistance = 0;
        int maxIndex = -1;
        for (int i = 0; i < points.length; i++) {
            double distance = dist(points[i], points[(i + 1) % points.length]);
            if(distance > maxDistance) {
                maxDistance = distance;
                maxIndex = i;
            }
            avDistance += distance;
        }
        avDistance = avDistance / points.length;

        // compare to average distance
        // if atleast 3 times average distance then we found an open end
        if(maxDistance >= avDistance * 3){
            closedCurve = false;

            // shift the points if necessary
            if(maxIndex != points.length - 1){
                Point[] tmp = new Point[points.length];
                for (int i = 0; i < points.length; i++) {
                    tmp[(i - maxIndex - 1 + points.length) % points.length] = points[i];
                }
                points = tmp;
            }
        }
        else
            closedCurve = true;

        // convert to matOfPoint to draw
        convexContour = new MatOfPoint();
        convexContour.fromArray(points);
        */
    }

    private double slopeTolerance = 0.9;
    private void calcLineConfidence()
    {
        // start with the results from the convex hull
        /*Point[] points = convexContour.toArray();

        // if the contour is not closed it might be a line
        if(!closedCurve) {
            // global line parameter
            double mTotal = 0;
            int confidenceCounter = 0;
            for (int i = 0; i < points.length - 1; i++) {
                Point p1 = points[i], p2 = points[i + 1];
                double m = (p2.y - p1.y) / (p2.x - p1.x);

                // check if slope is within tolerance close to the global slope
                if(m > (mTotal / (i + 1)) * slopeTolerance && m < (mTotal / (i + 1)) * (2 - slopeTolerance))
                    confidenceCounter++;
                mTotal += m;
            }

            // set the confidence as number of pixels on the line divided by total pixels
            isLineConfidence = confidenceCounter / (double)points.length;
        }*/
    }

    private double dist(Point p1, Point p2) {
        return Math.sqrt((p1.x - p2.x) * (p1.x - p2.x) + (p1.y - p2.y) * (p1.y - p2.y));
    }

    public void drawMultiColored(Bitmap material)
    {
        int matWidth = material.getWidth();
        int matHeight = material.getHeight();

        int[] colors = new int[matWidth * matHeight];
        material.getPixels(colors, 0, matWidth, 0, 0, matWidth, matHeight);

        Point[] points = data.toArray();
        for (int i = 0; i < points.length; i++) {
            //if(i < points.length / 2)
            //    continue;
            colors[matWidth * (int)points[i].y + (int)points[i].x] = Color.rgb((int)((points.length - i) * (255f / points.length)), (int)(i * (255f / points.length)), 0);
        }

        material.setPixels(colors, 0, matWidth, 0,0, matWidth, matHeight);
    }

    public void draw(Bitmap material, int color)
    {
        int matWidth = material.getWidth();
        int matHeight = material.getHeight();

        int[] colors = new int[matWidth * matHeight];
        material.getPixels(colors, 0, matWidth, 0, 0, matWidth, matHeight);

        Point[] points = data.toArray();
        for (int i = 0; i < points.length; i++) {
            colors[matWidth * (int)points[i].y + (int)points[i].x] = color;
        }

        material.setPixels(colors, 0, matWidth, 0,0, matWidth, matHeight);
    }

    @Override
    public int compareTo(@NonNull Object o) {
        return Double.compare(area, ((Contour)o).area) * -1;
    }


    public static ArrayList<Contour> create(ArrayList<MatOfPoint> contours)
    {
        ArrayList<Contour> result = new ArrayList<>(contours.size());
        for (MatOfPoint m: contours) {
            result.add(new Contour(m));
        }
        return result;
    }
}
