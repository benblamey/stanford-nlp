package edu.stanford.nlp.time.distributed;

import edu.stanford.nlp.time.Range;
import edu.stanford.nlp.time.Time;
import edu.stanford.nlp.time.distributed.TimeDensityFunction;
import org.joda.time.DateTime;
import org.joda.time.Instant;
import org.joda.time.Seconds;

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
        
        if (_range.begin.getJodaTimeInstant() == null || _range.end.getJodaTimeInstant() == null)
        {
            // Unkonwn -- return all the times.
            return "1";
        }
        
        return "(" 
                + "(" + millTimeSecondsExpr + " > " + toSecondsSinceY2K(_range.begin.getJodaTimeInstant())  + ")"
                + " && " 
                + "(" + millTimeSecondsExpr + " > " + toSecondsSinceY2K(_range.end.getJodaTimeInstant()) + ")"
                +")";
    }
    
        /**
     * Seconds since January 1st 2000 is used for gnuplot.
     * @return 
     */
    public static int toSecondsSinceY2K(Instant dateTime) {
        try {
            return Seconds.secondsBetween(s_startMillenium, dateTime).getSeconds();
        } catch (ArithmeticException e) {
            System.out.println("offending date: " + dateTime.toString());
            return 0;
        }
    }
    private static final DateTime s_startMillenium = new DateTime(2000, 1, 1, 0, 0);

}
