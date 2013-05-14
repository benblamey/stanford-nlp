package edu.stanford.nlp.time;



// Reference time (some kind of reference time)
public class RefTime extends Time {
    String label;

    public RefTime(String label) {
        this.label = label;
    }

    public RefTime(SUTime.StandardTemporalType timeType, String timeLabel, String label) {
        this.standardTemporalType = timeType;
        this.timeLabel = timeLabel;
        this.label = label;
    }

    public boolean isRef() {
        return true;
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
        return new RelativeTime(this, SUTime.TemporalOp.OFFSET, offset);
    }

    public Time resolve(Time refTime, int flags) {
        if (this == SUTime.TIME_REF) {
            return refTime;
        } else if (this == SUTime.TIME_NOW && (flags & SUTime.RESOLVE_NOW) != 0) {
            return refTime;
        } else {
            return this;
        }
    }
    private static final long serialVersionUID = 1;

}
