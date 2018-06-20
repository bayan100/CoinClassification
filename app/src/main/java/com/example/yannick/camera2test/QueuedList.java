package com.example.yannick.camera2test;

import android.support.annotation.NonNull;
import android.util.Log;

import org.opencv.core.Point;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QueuedList {
    private List<List<Point>> data = new ArrayList<>();
    private List<QueuedListNode> operations = new ArrayList<>();

    // hold the reference to
    private List<List<Point>> toMerge0 = new ArrayList<>();
    private List<List<Point>> toMerge1 = new ArrayList<>();

    void add(int index){
        operations.add(new QueuedListNode(QueuedListNode.Operation.ADD, index));
    }

    void insert(int index, @NonNull List<Point> item){
        operations.add(new QueuedListNode(QueuedListNode.Operation.INSERT, index, item));
    }

    void remove(int index){
        operations.add(new QueuedListNode(QueuedListNode.Operation.REMOVE, index));
    }

    void merge(int upperIndex){
        operations.add(new QueuedListNode(QueuedListNode.Operation.MERGE, upperIndex));
    }

    void split(int lowerIndex){
        operations.add(new QueuedListNode(QueuedListNode.Operation.SPLIT, lowerIndex));
    }

    List<Point> get(int index){
        return data.get(index);
    }

    int size(){
        return data.size();
    }

    void clear(){
        data.clear();
    }

    void commit(){
        // now execute every queued operation
        // keep an eye on removals and additions
        int counter = 0;
        for (int i = 0; i < operations.size(); i++) {
            QueuedListNode node = operations.get(i);
            //Log.d("SPLIT", "    " + i + ". op: " + node.operation);
            switch (node.operation){
                case ADD: data.add(node.index, new ArrayList<Point>());
                    counter++;
                    break;
                case INSERT: data.get(node.index).addAll(node.additionalData);
                    break;
                case REMOVE: data.remove(node.index + counter);
                    counter--;
                    break;
                case MERGE:
                    // invert the lower contour and append to upper
                    Collections.reverse(data.get(node.index + counter - 1));
                    data.get(node.index + counter).addAll(data.get(node.index - 1 + counter));

                    // remove the lower
                    data.remove(node.index + counter - 1);
                    counter--;
                    break;
                case SPLIT:
                    // add new contour
                    data.add(node.index, new ArrayList<Point>());
                    counter++;

                    // save to be merged
                    toMerge0.add(data.get(node.index - 1));
                    toMerge1.add(data.get(node.index));
                    break;
            }
        }
        operations.clear();
    }

    void finalCommit(List<List<Point>> result){
        for (int i = 0; i < toMerge0.size(); i++) {
            // find them in the result list
            int ind = result.indexOf(toMerge0.get(i));
            if(ind != -1)
                result.remove(ind);
            ind = result.indexOf(toMerge1.get(i));
            if(ind != -1)
                result.remove(ind);

            Collections.reverse(toMerge0.get(i));
            toMerge1.get(i).addAll(toMerge0.get(i));
            result.add(toMerge1.get(i));

            // remove from current to later not add again
            ind = data.indexOf(toMerge0.get(i));
            if(ind != -1)
                data.remove(ind);
            ind = data.indexOf(toMerge1.get(i));
            if(ind != -1)
                data.remove(ind);
        }

        //Log.d("SPLIT", " merge? " + toMerge0.size());
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < data.size(); i++) {
            sb.append("[");
            List<Point> list = data.get(i);
            for (int j = 0; j < list.size(); j++) {
                sb.append("(");
                sb.append(list.get(j).x);
                sb.append(",");
                sb.append(list.get(j).y);
                sb.append("), ");
            }
            sb.append("]\n");
        }
        //Log.d("SPLIT", " rest: " + sb.toString());

        // add the remaining open ends (without partner) to result
        result.addAll(data);
    }
}

class QueuedListNode {
    enum Operation{
        ADD,
        INSERT,
        REMOVE,
        MERGE,
        SPLIT
    }

    Operation operation;
    int index;
    List<Point> additionalData;

    QueuedListNode(Operation operation){
        this.operation = operation;
    }

    QueuedListNode(Operation operation, int index){
        this.operation = operation;
        this.index = index;
    }

    QueuedListNode(Operation operation, int index, List<Point> additionalData){
        this.operation = operation;
        this.index = index;
        this.additionalData = additionalData;
    }
}
