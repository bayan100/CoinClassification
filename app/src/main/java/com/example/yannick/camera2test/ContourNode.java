package com.example.yannick.camera2test;

import android.support.annotation.NonNull;
import android.util.Log;

import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;

import java.util.ArrayList;
import java.util.List;

class ContourNode implements Comparable
{
    int x, y;
    List<ContourNode> nodes;

    public ContourNode(int x, int y){
        this.x = x;
        this.y = y;

        nodes = new ArrayList<>();
    }

    boolean connected(ContourNode node) {
        return nodes.contains(node);
    }

    @Override
    public int compareTo(@NonNull Object o) {
        if(((ContourNode)o).y == y && ((ContourNode)o).x == x)
            return 0;
        else
            return -1;
    }
}

class ContourMap
{
    // [y][x]
    List<List<ContourNode>> data = new ArrayList<>();

    void insert(int x, int y)
    {
        // insert first element
        if(data.size() == 0){
            List<ContourNode> item = new ArrayList<>();
            item.add(new ContourNode(x, y));
            data.add(item);
            return;
        }

        // binary search in y
        int a = 0, b = data.size() - 1;
        int i = (b - a) / 2;
        while (true){
            ContourNode c = data.get(i).get(0);

            if(c.y == y)
            {
                List<ContourNode> list = data.get(i);

                // check if node does not already exist
                int[] xIndex = getIndex(x, list);
                if(xIndex[0] == 0)
                {
                    // add the node
                    ContourNode node = new ContourNode(x, y);
                    list.add(xIndex[1], node);

                    // accumulate its neighbors
                    addNeighbours(node, i);
                    return;
                }
                return;
            }
            else if(c.y < y)
                a = i;
            else
                b = i;

            i = (b - a) / 2 + a;

            // When reached a new y-spot
            if(a == i || b == i)
            {
                // new List with single Element
                List<ContourNode> item = new ArrayList<>();
                ContourNode node = new ContourNode(x, y);
                item.add(node);
                if(i == 0 && y < c.y)
                    i = -1;
                data.add(i + 1, item);

                // accumulate its neighbors
                addNeighbours(node, i + 1);
                return;
            }
        }
    }

    private void addNeighbours(ContourNode item, int yIndex)
    {
        // iterate 1 from left to right
        for (int i = (yIndex > 0) ? -1 : 0; i < 2 && i + yIndex < data.size(); i++) {
            // search for neighbors
            for (int j = -1; j < 2; j++) {
                if(i != 0 || j != 0) {
                    List<ContourNode> list = data.get(yIndex + i);

                    // check if gaps in between make the spacing larger than 1
                    if(Math.abs(list.get(0).y - item.y) < 2) {
                        // search for node with the requested x-value
                        int[] ci = getIndex(item.x + j, list);
                        if (ci[0] == 1) {
                            // linking on both sides
                            ContourNode c = list.get(ci[1]);
                            item.nodes.add(c);
                            c.nodes.add(item);
                        }
                    }
                }
            }
        }
    }

    private int[] getIndex(int x, List<ContourNode> list)
    {
        // binary search in y
        if(list.size() > 1) {
            int a = 0, b = list.size();
            int i = (b - a) / 2, oldi = -1;
            while (true) {
                ContourNode c = list.get(i);

                if (c.x == x)
                    return new int[]{1, i};
                else if (c.x < x)
                    a = i;
                else
                    b = i;

                oldi = i;
                i = (b - a) / 2 + a;

                // When reached a new x-spot
                if (i == oldi)
                    // no element with this particular x-Koordinate
                    return new int[]{0, i + 1};
            }
        }
        if(list.get(0).x > x)
            return new int[]{0, 0};
        else if(list.get(0).x < x)
            return new int[]{0, 1};
        return new int[]{1,0};
    }

    List<ContourNode> getSplitpoints()
    {
        List<ContourNode> result = new ArrayList<>();

        // iterate every contournode
        for (int i = 0; i < data.size(); i++) {
            List<ContourNode> list = data.get(i);
            for (int j = 0; j < list.size(); j++) {
                // if it has at least 3 neighbors
                List<ContourNode> nodes = list.get(j).nodes;
                if(nodes.size() > 2){
                    // check if at least 3 of those nodes are remote
                    int remoteCount = 0;
                    for (int k = 0; k < nodes.size(); k++)
                        for (int l = k; l < nodes.size(); l++) {
                            if(nodes.get(k).connected(nodes.get(l)))
                                remoteCount++;
                        }

                    // remotepairs?
                    if(remoteCount > 2)
                        // add to output
                        result.add(list.get(j));
                }
            }
        }

        return result;
    }

    void log()
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < data.size(); i++) {
            for (int j = 0; j < data.get(i).size(); j++) {
                ContourNode c = data.get(i).get(j);
                sb.append("(" + c.x + ", " + c.y + ", ");
                for (int k = 0; k < c.nodes.size(); k++) {
                    sb.append("[" + c.nodes.get(k).x + "," + c.nodes.get(k).y + "],");
                }
                sb.append("), ");
            }
            sb.append("\n");
        }
        Log.d("SPLITC", sb.toString());
    }

    static ContourMap fromContour(MatOfPoint data)
    {
        ContourMap map = new ContourMap();
        Point[] points = data.toArray();
        for (int i = 0; i < points.length; i++) {
            map.insert((int)points[i].x, (int)points[i].y);
        }
        return map;
    }
}
