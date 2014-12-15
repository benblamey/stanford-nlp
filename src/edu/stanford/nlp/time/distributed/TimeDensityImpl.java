package edu.stanford.nlp.time.distributed;

import edu.stanford.nlp.time.distributed.TimeDensityFunction;
import org.joda.time.DateTime;
import org.joda.time.DateTimeFieldType;
import org.joda.time.Partial;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

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
        String expression = "";
        
        int[] values = _base.getValues();
        int i = 0;
        
        for (DateTimeFieldType fieldType : _base.getFieldTypes()) {
            int tempexFieldValue = values[i];
            
            if (fieldType == DateTimeFieldType.yearOfCentury()) {
                expression = "(tm_year("+millTimeSecondsExpr+") % 1000 == " + tempexFieldValue+ ")";
            } else if (fieldType == DateTimeFieldType.year()) {
                expression = "(tm_year("+millTimeSecondsExpr+") == " + tempexFieldValue+ ")";
            } else if (fieldType == DateTimeFieldType.dayOfMonth()) {   
                expression = "(tm_mday("+millTimeSecondsExpr+") == " + tempexFieldValue+ ")";
            } else if (fieldType == DateTimeFieldType.dayOfWeek()) {
                expression = "(tm_wday("+millTimeSecondsExpr+") == " + tempexFieldValue+ ")";
            } else if (fieldType == DateTimeFieldType.monthOfYear()) {
                expression = "(tm_mon("+millTimeSecondsExpr+") == " + tempexFieldValue+ ")";
            } else {
                throw new NotImplementedException();
            }
        }
        
        return expression;
    }

}
