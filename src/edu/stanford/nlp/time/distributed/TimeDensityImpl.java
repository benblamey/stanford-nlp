package edu.stanford.nlp.time.distributed;

import org.joda.time.DateTime;
import org.joda.time.DateTimeFieldType;
import org.joda.time.Partial;

public class TimeDensityImpl extends TimeDensityFunction {
    private final Partial _base;

    public TimeDensityImpl(Partial base) {
        _base = base;
        
    }

    @Override
    public double getDensity(DateTime time) {
        int[] values = _base.getValues();
        int i = 0;
        
        double density = 1;
        
        for (DateTimeFieldType fieldType : _base.getFieldTypes()) {
            int timeFieldValue = time.get(fieldType);
            int tempexFieldValue = values[i];
            if (timeFieldValue != tempexFieldValue) {
                return 0;
            }
            i++;
        }
        
        return density;
    }

    @Override
    public String getGNUPlot(String millTimeSecondsExpr) {
        
        final Partial _base1 = _base;
        
        String expression = PartialToTimeExpression(_base1, millTimeSecondsExpr);
        
        return expression;
    }

    public static String PartialToTimeExpression(final Partial partial, String millTimeSecondsExpr) {
        
        String expression = "1";
        int[] values = partial.getValues();
        for (DateTimeFieldType fieldType : partial.getFieldTypes()) {
            int tempexFieldValue = partial.get(fieldType);
            
            // For conversion see:
            // http://joda-time.sourceforge.net/key_instant.html
            // http://www.cplusplus.com/reference/ctime/tm/
            
            if (fieldType == DateTimeFieldType.yearOfCentury()) {
                expression += "*(tm_year("+millTimeSecondsExpr+") % 1000 == " + tempexFieldValue+ ")";
                
            } else if (fieldType == DateTimeFieldType.year()) {
                expression += "*(tm_year("+millTimeSecondsExpr+") == " + tempexFieldValue+ ")";
                
            } else if (fieldType == DateTimeFieldType.dayOfMonth()) {   
                expression += "*(tm_mday("+millTimeSecondsExpr+") == " + (tempexFieldValue)+ ")";
                
            } else if (fieldType == DateTimeFieldType.dayOfWeek()) {
                expression += "*(tm_wday("+millTimeSecondsExpr+") == " + (tempexFieldValue%7)+ ")";
                
            } else if (fieldType == DateTimeFieldType.monthOfYear()) {
                expression += "*(tm_mon("+millTimeSecondsExpr+") == " + (tempexFieldValue-1)+ ")"; // Jan is month 0 in gnuplot.
                
            } else if (fieldType == DateTimeFieldType.hourOfDay()) {
            } else if (fieldType == DateTimeFieldType.clockhourOfDay()) {
            } else if (fieldType == DateTimeFieldType.minuteOfHour()) {
            } else if (fieldType == DateTimeFieldType.secondOfMinute()) {
            } else {
                throw new UnsupportedOperationException();
            }
        }
        
        
        return expression;
    }

}
