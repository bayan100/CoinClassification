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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Contour implements Comparable {
    MatOfPoint data;
    Point[] points;
    MatOfPoint convexContour;

    Point center;
    double area;
    int centerLinePoint;

    Contour(MatOfPoint data) {
        this.data = data;

        // convert and find the dimension in space
        points = data.toArray();
        double[] size = new double[4];
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

        // calculate area with standard value of 5 basepoints
        //calculateArea(5);
    }

    void calculateArea(int nBasePoints){
        // calculate the area under the contour with accurate trapeze shape
        // reset area
        area = 0;

        // base line from both endpoints
        double sy = points[points.length - 1].y - points[0].y;
        double sx = points[points.length - 1].x - points[0].x;
        double m = sy / sx;
        double m_ = m + 1 / m;
        double c = points[0].y - m * points[0].x;

        double side = 0;
        double h0 = 0;
        double xj = 0, yj = 0;
        double xj_ = 0, yj_ = 0;
        double dx = (points.length / nBasePoints);

        for (int j = 1; j < nBasePoints; j++) {
            int k = (int)(j * dx);
            double xi = points[k].x, yi = points[k].y;

            // get the coordinates on the baseline
            double x_ = (yi + xi / m - c) / m_;
            double y_ = x_ * m + c;

            // in case the points are parallel vertical or horizontal
            if(sx == 0) {
                x_ = points[0].x;
                y_ = points[0].y + (sy / nBasePoints) * j;
            }
            if(sy == 0) {
                y_ = points[0].y;
                x_ = points[0].x + (sx / nBasePoints) * j;
            }

            // compute on which side of the line the contour point lies
            double side_ = Math.signum((xi - points[0].x) * sy - (yi - points[0].y) * sx);

            // if not on the same side:
            if(side != side_ && side != 0) {
                // calculate the zeropoint between the two
                double m0 = (yj - yi) / (xj - xi);
                double x0 = (yi - m0 * xi - c) / (m - m0);
                double y0 = x0 * m0 + (yi - m0 * xi) + c;

                // now add the area from xj_ to x0 and x0 to x_
                double d0 = Math.sqrt((x0 - xj_) * (x0 - xj_) + (y0 - yj_) * (y0 - yj_));
                area += side * d0 * h0 / 2;

                double h = Math.sqrt((y_ - yi) * (y_ - yi) + (x_ - xi) * (x_ - xi));
                d0 = Math.sqrt((x0 - x_) * (x0 - x_) + (y0 - y_) * (y0 - y_));
                area += side_ * d0 * h / 2;
            }
            else {
                // calculate length from point n curve to base point
                double h = Math.sqrt((y_ - yi) * (y_ - yi) + (x_ - xi) * (x_ - xi));
                double da = (h + h0) / 2 * dx;

                h0 = h;
                // add area according to side, that way a curve has more than a wobbly line
                area += da * side_;
            }

            side = side_;
            xj = xi;
            yj = yi;
            xj_ = x_;
            yj_ = y_;
        }

        // save the accurate area
        if(!Double.isNaN(area))
            this.area = Math.abs(area);
        else
            this.area = -1;
    }

    void calculateCenterPoint(){
        double xmean = 0, ymean = 0;
        for (int i = 0; i < points.length; i++) {
            xmean += points[i].x;
            ymean += points[i].y;
        }
        center = new Point(xmean / points.length, ymean / points.length);
    }

    void calculateClosure(){

        // base line from both endpoints
        double sy = points[points.length - 1].y - points[0].y;
        double sx = points[points.length - 1].x - points[0].x;
        double m = sy / sx;
        double c = points[0].y - m * points[0].x;

        // get the coordinates on the baseline
        double x_ = (center.y + center.x / m - c) / (m + 1 / m);
        double y_ = x_ * m + c;

        // line through centerpoint
        double m_ = -1 / m;
        double c_ = center.y - m_ * center.x;

        // find meeting point between centerline and contour
        for (int i = 0; i < points.length; i++) {
            double y = m_ * points[i].x + c_;

            // find a close point
            if(Math.abs(y - points[i].y) < 2){
                centerLinePoint = i;

                // now calculate the distance between center point and both other points
                double contourPoint = dist(points[centerLinePoint], center);
                double basePoint = dist(new Point(x_, y_), center);
                break;
            }
        }
    }

    void appendPoints(List<Point> p){
        MatOfPoint matOfPoint = new MatOfPoint();

        ArrayList<Point> tmp = new ArrayList<>();
        Collections.addAll(tmp, points);
        tmp.addAll(p);

        matOfPoint.fromList(tmp);
        data = matOfPoint;

        points = tmp.toArray(new Point[tmp.size()]);
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
        int direction = (Math.abs(m) < 1 ? 2 : 0);
        direction += (direction == 2 ? ((points[points.length - n].x <= points[points.length - 1].x) ? 0 : 1)
                : ((points[points.length - n].y <= points[points.length - 1].y) ? 0 : 1));
        direction++; // + UNDEFINED

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

        for (int i = 0; i < points.length; i++) {
            if(i == 0 || i == points.length - 1)
                colors[matWidth * (int)points[i].y + (int)points[i].x] = Color.WHITE;
            else if(i == centerLinePoint)
                colors[matWidth * (int)points[i].y + (int)points[i].x] = Color.YELLOW;
            else
                colors[matWidth * (int)points[i].y + (int)points[i].x] = color;
        }

        if(center != null)
            for (int i = 0; i < 5; i++) {
                colors[matWidth * (int) center.y + (int) center.x + i - 2] = color;
                colors[matWidth * ((int) center.y + i - 2) + (int) center.x] = color;
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
