package com.example.yannick.camera2test;

import android.support.annotation.NonNull;

public class Ellipse implements Comparable {
    int x, y;
    double a, b;
    int votes;
    int originalWidth, originalHeight;

    public Ellipse(int x, int y, double a, double b)
    {
        this.x = x;
        this.y = y;
        this.a = a;
        this.b = b;
    }

    public static Ellipse from3Points(int x1, int y1, int x2, int y2, int x3, int y3) {
        int x0 = (x1 + x2) / 2;
        int y0 = (y1 + y2) / 2;

        double a = len(x1, y1, x2, y2) / 2;

        // length minor axis
        double b = Math.sqrt(
                ((y3 - y0) * (y3 - y0) * a * a) /
                        (a * a - (x3 - x0) * (x3 - x0)));

        return new Ellipse(x0, y0, a, b);
    }

    private static double len(int x1, int y1, int x0, int y0)
    {
        return Math.sqrt((x1 - x0) * (x1 - x0) + (y1 - y0) * (y1 - y0));
    }

    @Override
    public String toString() {
        return "x0=(" + x + ", " + y + "), a = " + a + ", b = " + b + "(votes: " + votes + ")";
    }

    @Override
    public int compareTo(@NonNull Object o) {
        Ellipse item = (Ellipse)o;
        if (item.votes < votes)
            return -1;
        else if(item.votes > votes)
            return 1;
        return 0;
    }

    public boolean equals(Ellipse e){
        return x == e.x && y == e.y && a == e.a && b == e.b;
    }

    public void scale(int targetWidth, int targetHeight)
    {
        double sx = targetWidth / (double)originalWidth;
        double sy = targetHeight / (double)originalHeight;

        a *= sx;
        b *= sy;
        x = (int)Math.round(x * sx);
        y = (int)Math.round(y * sy);
    }

}
