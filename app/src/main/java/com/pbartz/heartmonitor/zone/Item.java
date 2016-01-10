package com.pbartz.heartmonitor.zone;

/**
 * Created by yura.ilyaev on 9/16/2015.
 */
public class Item extends Object{

    public float hrFrom;
    public float hrTo;
    public float hrMax;

    public float hrValueFrom = 0;
    public float hrValueTo = 0;

    public String label;

    public Item(float hrFrom, float hrTo, String label) {
        this.hrValueTo = hrTo;
        this.hrValueFrom = hrFrom;
        this.label = label;
    }

    public Item(float hrFrom, float hrTo, float hrMax, String label) {
        this.hrFrom = hrFrom;
        this.hrTo = hrTo;
        this.hrMax = hrMax;
        this.label = label;

        if (hrFrom > 0) {
            this.hrValueFrom = ((float)hrMax / 100f) * hrFrom;
        }

        if (hrTo > 0) {
            this.hrValueTo = ((float)hrMax / 100f) * hrTo;
        }

    }
}
