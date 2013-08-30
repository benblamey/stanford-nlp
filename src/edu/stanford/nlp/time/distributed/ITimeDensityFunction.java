package edu.stanford.nlp.time.distributed;

import org.joda.time.DateTime;

public interface ITimeDensityFunction {

    public double GetDensity(DateTime time);
    
    public String GetGNUPlot(String millTimeSecondsExpr);
    
}
