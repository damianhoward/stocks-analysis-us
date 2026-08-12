package com.damianhoward.stocks.util;

import java.text.DecimalFormat;

public class Formatter {

    private static DecimalFormat df = new DecimalFormat("0.00");

    public static double format(double input) {
        return Double.valueOf(df.format(input));
    }

}
