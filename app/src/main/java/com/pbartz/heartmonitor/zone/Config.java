package com.pbartz.heartmonitor.zone;

import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by yura.ilyaev on 9/16/2015.
 */
public class Config {

    public static int hrMax;
    public static int hrResting;
    public static String schema;

    public static ArrayList<Item> zoneMap;

    public static Paint[] zoneColor = {

            new Paint(),
            new Paint(),
            new Paint(),
            new Paint(),
            new Paint()
    };

    public static final String[] zoneLevel = {
            "RECOVERY",
            "ENDURANCE",
            "STAMINA",
            "ECONOMY",
            "SPEED"
    };

    public static void init(int hrMax, int hrResting, String schema) {
        Config.hrMax = hrMax;
        Config.hrResting = hrResting;
        Config.schema = schema;

        Log.i("TAG", "Settings: " + hrMax + "/" + hrResting + "/" + schema);

        if (zoneMap == null) {
            zoneMap = new ArrayList<>(5);
        } else {
            zoneMap.clear();
        }

        zoneColor[0].setColor(Color.argb(255, 231, 217, 218));
        zoneColor[1].setColor(Color.argb(255, 229, 193, 193));
        zoneColor[2].setColor(Color.argb(255, 217, 167, 168));
        zoneColor[3].setColor(Color.argb(255, 251, 0, 23));
        zoneColor[4].setColor(Color.argb(255, 183, 3, 18));




        if(schema.toString().equalsIgnoreCase("By % of Max HR")) {

            int[] perMap = {60, 70, 80, 90, 110};
            for (int i = 0; i < 5; i++) {

                int leftBorder = 0;
                int rightBorder = perMap[i];

                if (i == 0) {
                    leftBorder = 0;
                } else {
                    leftBorder = perMap[i - 1];
                }

                Item item = new Item(leftBorder, rightBorder, hrMax, "z" + (i + 1));
                zoneMap.add(i, item);
            }

        } else if (schema.toString().equalsIgnoreCase("Karvonen modified")) {

            int hrr = hrMax - hrResting;

            float[] perMap = {0.60f, 0.70f, 0.80f, 0.90f, 1.10f};
            for (int i = 0; i < 5; i++) {

                float leftBorder = 0;
                float rightBorder = perMap[i];

                if (i == 0) {
                    leftBorder = 0;
                } else {
                    leftBorder = perMap[i - 1];
                }

                Item item = new Item(leftBorder, rightBorder, hrr, "z" + (i + 1));

                item.hrValueFrom = leftBorder * hrr + hrResting;
                item.hrValueTo = rightBorder * hrr + hrResting;

                zoneMap.add(i, item);
            }

        } else if (schema.toString().equalsIgnoreCase("Heart Rate Training")) {

            int[] perMap = {60, 75, 85, 95, 110};
            for (int i = 0; i < 5; i++) {

                int leftBorder = 0;
                int rightBorder = perMap[i];

                if (i == 0) {
                    leftBorder = 0;
                } else {
                    leftBorder = perMap[i - 1];
                }

                Item item = new Item(leftBorder, rightBorder, hrMax, "z" + (i + 1));
                zoneMap.add(i, item);
            }

        } else if (schema.toString().equalsIgnoreCase("Joe Friel Running")) {

            float[] perMap = {0.85f, 0.89f, 0.94f, 0.99f, 1.10f};

            float lthr = (float)(hrMax * 0.85f);

            for (int i = 0; i < 5; i++) {

                float leftBorder = 0;
                float rightBorder = perMap[i];

                if (i == 0) {
                    leftBorder = 0;
                } else {
                    leftBorder = perMap[i - 1];
                }

                Item item = new Item(leftBorder, rightBorder, lthr, "z" + (i + 1));

                item.hrValueFrom = leftBorder * lthr;
                item.hrValueTo = rightBorder * lthr;

                zoneMap.add(i, item);
            }

        } else if (schema.toString().equalsIgnoreCase("ABCC/BCF guidelines")) {

            int[] perMap = {65, 75, 82, 89, 110};

            for (int i = 0; i < 5; i++) {

                int leftBorder = 0;
                int rightBorder = perMap[i];

                if (i == 0) {
                    leftBorder = 0;
                } else {
                    leftBorder = perMap[i - 1];
                }

                Item item = new Item(leftBorder, rightBorder, hrMax, "z" + (i + 1));
                zoneMap.add(i, item);
            }

        }  else if (schema.toString().equalsIgnoreCase("Joe Friel Biking")) {

            float[] perMap = {0.81f, 0.89f, 0.93f, 0.99f, 1.10f};

            float lthr = (float)(hrMax * 0.85f);

            for (int i = 0; i < 5; i++) {

                float leftBorder = 0;
                float rightBorder = perMap[i];

                if (i == 0) {
                    leftBorder = 0;
                } else {
                    leftBorder = perMap[i - 1];
                }

                Item item = new Item(leftBorder, rightBorder, lthr, "z" + (i + 1));

                item.hrValueFrom = leftBorder * lthr;
                item.hrValueTo = rightBorder * lthr;

                zoneMap.add(i, item);
            }

        } else {
            int[] perMap = {58, 77, 87, 96, 110};
            for (int i = 0; i < 5; i++) {

                int leftBorder = 0;
                int rightBorder = perMap[i];

                if (i == 0) {
                    leftBorder = 0;
                } else {
                    leftBorder = perMap[i - 1];
                }

                Item item = new Item(leftBorder, rightBorder, hrMax, "z" + (i + 1));
                zoneMap.add(i, item);
            }
        }

    }

    public static int[] getZoneCounts() {
        int[] res = {0,0,0,0,0};

        return res;
    }

    public static Paint getPaintByZone(int zone) {
        return zoneColor[zone];
    }

    public static float hrValueToScreenHeight(int hrValue, float scrHeight) {
        float res = 0;

        for(int i = 0 ; i < 5 ; i++) {
            Item item = zoneMap.get(i);

            if ((hrValue >= item.hrValueFrom && hrValue <= item.hrValueTo) || i == 4) {

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
        int res = 4;
        for(int i = 0 ; i < 5 ; i++) {

            Item item = zoneMap.get(i);

            if (hr >= zoneMap.get(i).hrValueFrom && hr <= zoneMap.get(i).hrValueTo) {
                res = i;
            }
        }
        return res;

    }
}
