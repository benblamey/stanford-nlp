package edu.stanford.nlp.time.distributed;

import edu.stanford.nlp.ling.CoreAnnotation;

public class TimePDF {

    public static class TimePDFAnnotation implements CoreAnnotation<TimeDensityFunction> {

        public Class<TimeDensityFunction> getType() {
            return TimeDensityFunction.class;
        }
    }
}
