package com.pbartz.heartmonitor.zone;

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

        zoneMap = new ArrayList<>(5);
        for(int i = 0 ; i < 5 ; i++) {
            Item item = new Item(10, 100, "Z" + (i + 1));
            zoneMap.add(i, item);
        }

    }

}
