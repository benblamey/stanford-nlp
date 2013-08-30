package edu.stanford.nlp.time.distributed;

import edu.stanford.nlp.ling.CoreAnnotation;

public class TimePDF {

    public static class TimePDFAnnotation implements CoreAnnotation<CanExpressTimeAsFunction> {

        public Class<CanExpressTimeAsFunction> getType() {
            return CanExpressTimeAsFunction.class;
        }
    }
}
