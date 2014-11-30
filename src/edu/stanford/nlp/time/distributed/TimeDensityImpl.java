package edu.stanford.nlp.time.distributed;

import edu.stanford.nlp.time.distributed.TimeDensityFunction;
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
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
