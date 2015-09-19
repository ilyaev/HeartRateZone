package com.pbartz.heartmonitor.zone;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by yura.ilyaev on 9/16/2015.
 */
public class Config {

    public static int hrMax;

    public static ArrayList<Item> zoneMap;

    public static void init(int hrMax) {
        Config.hrMax = hrMax;

        if (zoneMap == null) {
            zoneMap = new ArrayList<>(5);
        } else {
            zoneMap.clear();
        }

        int[] perMap = {58,77,87,96,110};



        for(int i = 0 ; i < 5 ; i++) {

            int leftBorder = 0;
            int rightBorder = perMap[i];

            if (i == 0) {
                leftBorder = 0;
            } else {
                leftBorder = perMap[i - 1];
            }

            Item item = new Item(leftBorder, rightBorder, hrMax, "Z" + (i + 1));
            zoneMap.add(i, item);
        }

    }

    public static float hrValueToScreenHeight(int hrValue, float scrHeight) {
        float res = 0;

        for(int i = 0 ; i < 5 ; i++) {
            Item item = zoneMap.get(i);

            if (hrValue >= item.hrValueFrom && hrValue <= item.hrValueTo) {

                float zDiff = item.hrValueTo - item.hrValueFrom;
                float vDiff = hrValue - item.hrValueFrom;
                float perc = vDiff / (zDiff / 100);

                float zoneHeight = scrHeight / 5;

                res = i * zoneHeight + ((zoneHeight / 100) * perc);

                break;
            }

        }

        return res;
    }

    public static int getZoneByHr(int hr) {
        int res = 0;
        for(int i = 0 ; i < 5 ; i++) {

            Item item = zoneMap.get(i);
            Log.i("TEG", "" + item.hrValueFrom + " / " + item.hrValueTo);

            if (hr >= zoneMap.get(i).hrValueFrom && hr <= zoneMap.get(i).hrValueTo) {
                res = i;
            }
        }
        return res;

    }
}
