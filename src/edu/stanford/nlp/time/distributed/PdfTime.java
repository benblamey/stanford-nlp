package edu.stanford.nlp.time.distributed;

import edu.stanford.nlp.time.Duration;
import edu.stanford.nlp.time.Temporal;
import edu.stanford.nlp.time.Time;
import edu.stanford.nlp.time.TimeIndex;
import java.util.Map;

/**
 * A temporal representation who's representation is purely a probability density function.
 * @author Ben Blamey (blamey.ben@gmail.com)
 */
public class PdfTime extends Time {

    private String label = "PdfTime";
    
    public PdfTime(TimeDensityFunction plot) {
        setTimeExpression(plot);
    }
    
    @Override
    public Time add(Duration offset) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getTimexValue() {
        return super.getTimexValue();
    }


    @Override
    public String getTimeLabel() {
        return label; // super.getTimeLabel();
    }

    @Override
    public Map<String, String> getTimexAttributes(TimeIndex timeIndex) {
        Map<String, String> timexAttributes = super.getTimexAttributes(timeIndex);
        return timexAttributes;
    }


}
