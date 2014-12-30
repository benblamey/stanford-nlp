package edu.stanford.nlp.time.distributed;

import benblamey.gnuplot.GnuPlot;
import edu.stanford.nlp.time.Range;
import edu.stanford.nlp.time.Time;
import edu.stanford.nlp.time.distributed.TimeDensityFunction;
import org.joda.time.DateTime;

public class TimeRangeDensity extends TimeDensityFunction {
    private final Range _range;

    public TimeRangeDensity(Range range) {
        _range = range;
        Time begin = range.begin;
        Time end = range.end;

        if (begin == null || end == null) {
            throw new RuntimeException("start or end of range is null");
        }
    }

    @Override
    public double getDensity(DateTime time) {
        if (time.isAfter(_range.begin.getJodaTimeInstant()) && (time.isBefore(_range.begin.getJodaTimeInstant()))) {
            return 1;
        } else {
            return 0;
        }
    }

    @Override
    public String getGNUPlot(String millTimeSecondsExpr) {
        
        
        
        return "(" 
                + "(" + millTimeSecondsExpr + " > " + GnuPlot.toMillTime(_range.begin.getJodaTimeInstant())  + ")"
                + " && " 
                + "(" + millTimeSecondsExpr + " > " + GnuPlot.toMillTime(_range.end.getJodaTimeInstant()) + ")"
                +")";
    }

}
