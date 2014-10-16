package edu.stanford.nlp.time.distributed;

import org.joda.time.DateTime;

public abstract class TimeDensityFunction {

    public abstract double getDensity(DateTime time);
    
    public abstract String getGNUPlot(String millTimeSecondsExpr);
    
}
