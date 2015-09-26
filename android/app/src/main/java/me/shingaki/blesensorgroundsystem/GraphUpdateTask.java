package me.shingaki.blesensorgroundsystem;

import android.graphics.Color;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.TimerTask;

/**
 * Created by shiva on 15/09/17.
 */
public class GraphUpdateTask extends TimerTask {

    private final static String TAG = GraphUpdateTask.class.getSimpleName();


    private int lastX1 = 0;
    private int lastX2 = 0;

    private LineGraphSeries<DataPoint> mSeries1;
    private LineGraphSeries<DataPoint> mSeries2;


    private TextView mTextTemperature;
    private TextView mTextHumidity;

    private MainActivity activity;
    private SensorMonitorFragment sensorMonitorFragment;

    private Handler mHandler = new Handler();

    public GraphUpdateTask(SensorMonitorFragment sensorMonitorFragment, View view) {
        this.sensorMonitorFragment = sensorMonitorFragment;

        mTextTemperature = (TextView) view.findViewById(R.id.text_temperature);
        mTextHumidity = (TextView) view.findViewById(R.id.text_humidity);

        GraphView graph = (GraphView) view.findViewById(R.id.graph);
        mSeries1 = new LineGraphSeries<DataPoint>();
        mSeries2 = new LineGraphSeries<DataPoint>();
        graph.addSeries(mSeries1);
        graph.getSecondScale().addSeries(mSeries2);
        mSeries2.setColor(Color.BLUE);
        graph.getGridLabelRenderer().setVerticalLabelsSecondScaleColor(Color.BLUE);

        mSeries1.setTitle("Temperature");
        mSeries1.setColor(Color.RED);
        graph.getGridLabelRenderer().setVerticalLabelsColor(Color.RED);
        mSeries1.setDrawDataPoints(true);

        mSeries2.setTitle("Humidity");
        mSeries2.setDrawDataPoints(true);
        graph.getLegendRenderer().setVisible(true);
        graph.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);

        graph.getSecondScale().setMinY(0);
        graph.getSecondScale().setMaxY(1000);

//        graph.setScaleY(0);
//        graph.setScaleY(50);

        Viewport viewport = graph.getViewport();
        viewport.setXAxisBoundsManual(true);
        viewport.setYAxisBoundsManual(true);
        viewport.setScrollable(true);
        viewport.setMinX(0);
        viewport.setMaxX(20);
        viewport.setMinY(0);
        viewport.setMaxY(60);
    }

    @Override
    public void run() {
        //Log.d(TAG, "GraphUpdate");

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                addSeriesEntry(mSeries1, sensorMonitorFragment.mBleService.getTemperture(), lastX1, 0, 1);
                lastX1++;
                mTextTemperature.setText("" + sensorMonitorFragment.mBleService.getTemperture());

                addSeriesEntry(mSeries2, sensorMonitorFragment.mBleService.getHumidity(), lastX2, 0, 1);
                lastX2++;
                mTextHumidity.setText("" + sensorMonitorFragment.mBleService.getHumidity());
            }
        });


    }


    // add random data to graph
    private void addSeriesEntry(LineGraphSeries<DataPoint> series, Integer data, int num, float offset, float divBy) {
        // here, we choose to display max 10 points on the viewport and we scroll to end
        int plotData = (int) ((data + offset) / divBy);
        series.appendData(new DataPoint(num, plotData), true, 300);
    }

}