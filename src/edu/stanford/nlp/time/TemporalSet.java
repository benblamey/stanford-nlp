package edu.stanford.nlp.time;



/**
 * Exciting set of times
 */
public abstract class TemporalSet extends Temporal {

    public TemporalSet() {
    }

    public TemporalSet(TemporalSet t) {
        super(t);
    }

    // public boolean includeTimexAltValue() { return true; }
    public SUTime.TimexType getTimexType() {
        return SUTime.TimexType.SET;
    }
    private static final long serialVersionUID = 1;

}
