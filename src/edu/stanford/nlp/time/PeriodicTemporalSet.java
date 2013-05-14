package edu.stanford.nlp.time;


import java.util.Map;
import org.joda.time.DateTimeZone;

/**
 * PeriodicTemporalSet represent a set of times that occurs with some frequency.
 * Example: At 2-3pm every friday from September 1, 2011 to December 30, 2011.
 */
public class PeriodicTemporalSet extends TemporalSet {
    /** Start and end times for when this set of times is suppose to be happening
     *  (e.g. 2011-09-01 to 2011-12-30) */
    Range occursIn;
    /** Temporal that re-occurs (e.g. Friday 2-3pm) */
    Temporal base;
    /** The periodicity of re-occurrence (e.g. week) */
    Duration periodicity;
    // How often (once, twice)
    // int count;
    /** Quantifier - every, every other */
    String quant;
    /** String representation of frequency (3 days = P3D, 3 times = P3X) */
    String freq;

    // public ExplicitTemporalSet toExplicitTemporalSet();
    // public ExplicitTemporalSet toExplicitTemporalSet();
    public PeriodicTemporalSet(Temporal base, Duration periodicity, String quant, String freq) {
        this.base = base;
        this.periodicity = periodicity;
        this.quant = quant;
        this.freq = freq;
    }

    public PeriodicTemporalSet(PeriodicTemporalSet p, Temporal base, Duration periodicity, Range range, String quant, String freq) {
        super(p);
        this.occursIn = range;
        this.base = base;
        this.periodicity = periodicity;
        this.quant = quant;
        this.freq = freq;
    }

    public PeriodicTemporalSet setTimeZone(DateTimeZone tz) {
        return new PeriodicTemporalSet(this, (Time) Temporal.setTimeZone(base, tz), periodicity, (Range) Temporal.setTimeZone(occursIn, tz), quant, freq);
    }

    public PeriodicTemporalSet multiplyDurationBy(int scale) {
        return new PeriodicTemporalSet(this, this.base, periodicity.multiplyBy(scale), this.occursIn, this.quant, this.freq);
    }

    public PeriodicTemporalSet divideDurationBy(int scale) {
        return new PeriodicTemporalSet(this, this.base, periodicity.divideBy(scale), this.occursIn, this.quant, this.freq);
    }

    public boolean isGrounded() {
        return occursIn != null && occursIn.isGrounded();
    }

    public Duration getPeriod() {
        return periodicity;
    }

    public Time getTime() {
        return null;
    }

    public Duration getDuration() {
        return null;
    }

    public Range getRange(int flags, Duration granularity) {
        return occursIn;
    }

    public Map<String, String> getTimexAttributes(TimeIndex timeIndex) {
        Map<String, String> map = super.getTimexAttributes(timeIndex);
        if (quant != null) {
            map.put(SUTime.TimexAttr.quant.name(), quant);
        }
        if (freq != null) {
            map.put(SUTime.TimexAttr.freq.name(), freq);
        }
        if (periodicity != null) {
            map.put("periodicity", periodicity.getTimexValue());
        }
        return map;
    }

    public Temporal resolve(Time refTime, int flags) {
        Range resolvedOccursIn = (occursIn != null) ? occursIn.resolve(refTime, flags) : null;
        Temporal resolvedBase = (base != null) ? base.resolve(null, 0) : null;
        return new PeriodicTemporalSet(this, resolvedBase, this.periodicity, resolvedOccursIn, this.quant, this.freq);
    }

    public String toFormattedString(int flags) {
        if (getTimeLabel() != null) {
            return getTimeLabel();
        }
        if ((flags & SUTime.FORMAT_ISO) != 0) {
            // TODO: is there iso standard?
            return null;
        }
        if (base != null) {
            return base.toFormattedString(flags);
        } else {
            if (periodicity != null) {
                return periodicity.toFormattedString(flags);
            }
        }
        return null;
    }

    public Temporal intersect(Temporal t) {
        if (t instanceof Range) {
            if (occursIn == null) {
                return new PeriodicTemporalSet(this, base, periodicity, (Range) t, quant, freq);
            }
        } else if (base != null) {
            Temporal merged = base.intersect(t);
            return new PeriodicTemporalSet(this, merged, periodicity, occursIn, quant, freq);
        } else {
            return new PeriodicTemporalSet(this, t, periodicity, occursIn, quant, freq);
        }
        return null;
    }
    private static final long serialVersionUID = 1;

}
