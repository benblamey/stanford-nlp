package edu.stanford.nlp.time;



/**
 * Simple time (vague time that we don't really know what to do with)
 **/
public class SimpleTime extends Time {
    String label;

    public SimpleTime(String label) {
        this.label = label;
    }

    public String toFormattedString(int flags) {
        if (getTimeLabel() != null) {
            return getTimeLabel();
        }
        if ((flags & SUTime.FORMAT_ISO) != 0) {
            return null;
        } // TODO: is there iso standard?
        // TODO: is there iso standard?
        return label;
    }

    public Time add(Duration offset) {
        Time t = new RelativeTime(this, SUTime.TemporalOp.OFFSET, offset);
        // t.approx = this.approx;
        // t.mod = this.mod;
        return t;
    }
    private static final long serialVersionUID = 1;

}
