package com.example.yannick.camera2test.AGP;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.example.yannick.camera2test.GraphicsProcessor;
import com.example.yannick.camera2test.Sqlite.DatabaseManager;

import java.util.ArrayList;
import java.util.PropertyResourceBundle;

public class AsyncGraphicsProcessor extends AsyncTask<Integer, Integer, Integer>
{
    ArrayList<GraphicsProcessor> task;
    ProgressBar progressBar;
    ImageView imageView;
    DatabaseManager dbm;
    Activity activity;

    // For a single task
    public AsyncGraphicsProcessor(GraphicsProcessor task, ProgressBar progressBar, ImageView imageView) {
        this.task = new ArrayList<GraphicsProcessor>();
        this.task.add(task);

        this.progressBar = progressBar;
        this.imageView = imageView;
    }

    public AsyncGraphicsProcessor(GraphicsProcessor task, ProgressBar progressBar, ImageView imageView, DatabaseManager dbm) {
        this.task = new ArrayList<GraphicsProcessor>();
        this.task.add(task);

        this.progressBar = progressBar;
        this.imageView = imageView;
        this.dbm = dbm;
    }

    public AsyncGraphicsProcessor(GraphicsProcessor task, ProgressBar progressBar, ImageView imageView, Activity activity) {
        this.task = new ArrayList<GraphicsProcessor>();
        this.task.add(task);

        this.progressBar = progressBar;
        this.imageView = imageView;
        this.activity = activity;
    }

    @Override
    protected Integer doInBackground(Integer... ints)
    {
        // run the computational tasks
        for(int i = 0; i < task.size(); i++) {
            task.get(i).passDBM(dbm);
            task.get(i).passActivity(activity);
            GraphicsProcessor.Status status = task.get(i).execute();

            // update progress
            publishProgress((int)((i / (float)task.size()) * 100f));

            // task passed, give data to next processor
            if (status == GraphicsProcessor.Status.PASSED) {
                if (i + 1 < task.size()) {
                    Log.d("TENSOR", "passing: bmp == 0: " + (task.get(i).getData().getBitmap() == null));
                    task.get(i + 1).passData(task.get(i).getData());
                    task.get(i + 1).passAdditionalData(task.get(i).getAdditionalData());
                }

                // successfully completed all tasks
                else
                    return 1;
            }
            else if (status == GraphicsProcessor.Status.FAILED) {
                Log.e("AsycGP", "Process Nr. " + i + " (" + task.get(i).task + ") failed to execute");
                return -i;
            }
        }
        return 0;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        // debug
        Log.d("GraphicsProcessor","(" + this.getClass().getName() + ") Progress: " + (Math.round((values[0] / 100f) * task.size() + 1)) + "/" + task.size());
    }

    @Override
    protected void onPostExecute(Integer integer) {

        // stop spinner
        progressBar.setVisibility(progressBar.GONE);

        // debug
        imageView.setImageBitmap(task.get(task.size() - 1).getBitmap());
    }
}
