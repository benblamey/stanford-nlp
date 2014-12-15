package edu.stanford.nlp.time.distributed;

import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.time.Temporal;

public class TimePDF {

    public static class TimePDFAnnotation implements CoreAnnotation<TimeDensityFunction> {

        public Class<TimeDensityFunction> getType() {
            return TimeDensityFunction.class;
        }
    }
    
    public static class TemporalAnnotation implements CoreAnnotation<Temporal> {

        public Class<Temporal> getType() {
            return Temporal.class;
        }
    }

}
