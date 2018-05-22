package com.example.yannick.camera2test;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.util.Log;

import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;

import java.util.ArrayList;

public class Contour implements Comparable {
    MatOfPoint data;
    double[] size;
    double area;

    public Contour(MatOfPoint data)
    {
        this.data = data;

        // convert and find the dimension in space
        Point[] points = data.toArray();
        size = new double[4];
        size[0] = points[0].x; size[2] = points[0].y;

        for (Point p : points) {
            if(p.x < size[0]) { size[0] = p.x; }
            if(p.x > size[1]) { size[1] = p.x; }
            if(p.y < size[2]) { size[2] = p.y; }
            if(p.y > size[3]) { size[3] = p.y; }
        }
        area = (size[1] - size[0]) * (size[3] - size[2]);
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
