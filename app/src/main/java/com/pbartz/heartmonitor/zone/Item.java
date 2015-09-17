package com.pbartz.heartmonitor.zone;

/**
 * Created by yura.ilyaev on 9/16/2015.
 */
public class Item extends Object{

    public int hrFrom;
    public int hrTo;

    public String label;

    public Item(int hrFrom, int hrTo, String label) {
        this.hrFrom = hrFrom;
        this.hrTo = hrTo;
        this.label = label;
    }
}
