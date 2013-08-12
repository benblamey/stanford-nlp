package edu.stanford.nlp.util;

import org.joda.time.DateTime;
import org.joda.time.Seconds;

/**
 * Utility functions for JodaTime to allow conversion to and from 'Millenium' time used by Gnuplot.
 * @author Ben Blamey (blamey.ben@gmail.com)
 */
public class DateUtil {

    public static int ToMilleniumTime(DateTime dt) {
        return Seconds.secondsBetween(Y2K, dt).getSeconds();
    }
    
    public static DateTime FromMilleniumTime(int milleniumTime) {
        return MILLENIUM_EPOCH.plusSeconds(milleniumTime);
    }
    
    private static final DateTime Y2K = new DateTime(2000,1,1,0,0);
    private final static DateTime UNIX_EPOCH = new DateTime(1970,1,1,0,0);
    private final static DateTime MILLENIUM_EPOCH = new DateTime(2000,1,1,0,0);

}