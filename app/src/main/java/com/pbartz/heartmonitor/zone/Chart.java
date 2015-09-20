package com.pbartz.heartmonitor.zone;

import android.util.Log;

import java.util.ArrayList;

/**
 * Created by yura.ilyaev on 9/19/2015.
 */
public class Chart {


    public class HrPoint {

        public int hrValue;
        public int zone;
        public long tStamp;

        public HrPoint(int hrValue, int zone) {
            this.hrValue = hrValue;
            this.zone = zone;
            tStamp = System.currentTimeMillis();
        }

    }

    public class HrDataSet {

        public ArrayList<HrPoint> data;

        public int[] zoneCount = {0, 0, 0, 0, 0};

        public int thresholdTop = 300;
        public int listSize = 200;

        public HrDataSet() {
            data = new ArrayList<HrPoint>();
        }

        public void pushPoint(int hrValue) {
            HrPoint point = new HrPoint(hrValue, Config.getZoneByHr(hrValue));
            data.add(point);
            zoneCount[point.zone] += 1;

            if (data.size() > thresholdTop) {
                ArrayList<HrPoint> subList = new ArrayList<HrPoint>();

                int lSize = data.size();

                for(int i = lSize - listSize ; i < lSize ; i++) {
                    subList.add(data.get(i));
                }

                data = subList;
            }

        }

        public int[] getZoneCounts() {

            return zoneCount;

        }
    }

    public HrDataSet dataSet;

    public Chart() {

        dataSet = new HrDataSet();

    }

    public void push(int hrValue) {
        dataSet.pushPoint(hrValue);
    }

}
