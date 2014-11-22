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
public class PdfTime extends Time implements CanExpressTimeAsFunction {

    private TimeDensityFunction _plot;
    private String label = "PdfTime";
    
    public PdfTime(TimeDensityFunction plot) {
        _plot = plot;
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
    public Temporal intersect(Temporal t) {
        
        if (t instanceof CanExpressTimeAsFunction) {
            CanExpressTimeAsFunction tFunc = (CanExpressTimeAsFunction)t;
            this._plot = new IntersectTimeExpression(
                    new TimeDensityFunctionContainer(_plot, this.toString()),
                    tFunc
                    );
        } else {
            throw new UnsupportedOperationException("Other temporal doesn't support CanExpressTimeAsFunction.");
        }
        
        this.label += " INTERSECT " + t.toString();
        return this;
    }

    @Override
    public Map<String, String> getTimexAttributes(TimeIndex timeIndex) {
        Map<String, String> timexAttributes = super.getTimexAttributes(timeIndex);
        return timexAttributes;
    }

    public void setTimeDensityFunction(TimeDensityFunction func) {
        throw new UnsupportedOperationException("Not supported for this function.");
    }

    public TimeDensityFunction getTimeDensityFunction() {
        return _plot;
    }
}
