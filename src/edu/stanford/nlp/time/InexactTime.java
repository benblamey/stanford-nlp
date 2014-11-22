package edu.stanford.nlp.time;


import edu.stanford.nlp.time.distributed.CanExpressTimeAsFunction;
import edu.stanford.nlp.time.distributed.TimeDensityFunction;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.joda.time.Partial;

/**
 * Inexact time, not sure when this is, but have some guesses
 */
public class InexactTime extends Time implements CanExpressTimeAsFunction {
    Time base; // best guess
    // best guess
    Duration duration; // how long the time lasts
    // how long the time lasts
    Range range; // guess at range in which the time occurs
    // guess at range in which the time occurs

    int meanDay = -1;
    int sdDays = -1;
    private TimeDensityFunction _func;
    
    public InexactTime(Partial partial) {
        this.base = new PartialTime(partial);
        this.range = base.getRange();
        this.approx = true;
    }

    public InexactTime(Time base, Duration duration, Range range) {
        this.base = base;
        this.duration = duration;
        this.range = range;
        this.approx = true;
    }
    
    public InexactTime(Time base, Duration duration, Range range, TimeDensityFunction func) {
        this.base = base;
        this.duration = duration;
        this.range = range;
        this.approx = true;
        this._func = func;
        
    }
    
    public InexactTime(Time base, Duration duration, Range range, Integer meanDay, Integer sdDays) {
        this.base = base;
        this.duration = duration;
        this.range = range;
        this.approx = true;
        
        this.meanDay = meanDay.intValue();
        this.sdDays = sdDays.intValue();
    }


    public InexactTime(InexactTime t, Time base, Duration duration, Range range, TimeDensityFunction func) {
        super(t);
        this.base = base;
        this.duration = duration;
        this.range = range;
        this.approx = true;
        this._func = func;
    }

    
    public InexactTime(InexactTime t, Time base, Duration duration, Range range) {
        super(t);
        this.base = base;
        this.duration = duration;
        this.range = range;
        this.approx = true;
    }

    public InexactTime(Range range) {
        this.base = range.mid();
        this.range = range;
        this.approx = true;
    }

    public InexactTime setTimeZone(DateTimeZone tz) {
        return new InexactTime(this, (Time) Temporal.setTimeZone(base, tz), duration, (Range) Temporal.setTimeZone(range, tz));
    }

    public Time getTime() {
        return this;
    }

    public Duration getDuration() {
        if (duration != null) {
            return duration;
        }
        if (range != null) {
            return range.getDuration();
        } else if (base != null) {
            return base.getDuration();
        } else {
            return null;
        }
    }

    public Range getRange(int flags, Duration granularity) {
        if (range != null) {
            return range.getRange(flags, granularity);
        } else if (base != null) {
            return base.getRange(flags, granularity);
        } else {
            return null;
        }
    }

    public Time add(Duration offset) {
        //if (getTimeLabel() != null) {
        if (getStandardTemporalType() != null) {
            // Time has some meaning, keep as is
            return new RelativeTime(this, SUTime.TemporalOp.OFFSET, offset);
        } else {
            // Some other time, who know what it means
            // Try to do offset
            return new InexactTime(this, (Time) SUTime.TemporalOp.OFFSET.apply(base, offset), duration, (Range) SUTime.TemporalOp.OFFSET.apply(range, offset));
        }
    }

    public Time resolve(Time refTime, int flags) {
        CompositePartialTime cpt = makeComposite(new PartialTime(this, new Partial()), this);
        if (cpt != null) {
            return cpt.resolve(refTime, flags);
        }
        Time groundedBase = null;
        if (base == SUTime.TIME_REF) {
            groundedBase = refTime;
        } else if (base != null) {
            groundedBase = base.resolve(refTime, flags).getTime();
        }
        Range groundedRange = null;
        if (range != null) {
            groundedRange = range.resolve(refTime, flags).getRange();
        }
        /*    if (groundedRange == range && groundedBase == base) {
        return this;
        } */
        return SUTime.createTemporal(standardTemporalType, timeLabel, mod, new InexactTime(groundedBase, duration, groundedRange));
        //return new InexactTime(groundedBase, duration, groundedRange);
    }

    public Instant getJodaTimeInstant() {
        Instant p = null;
        if (base != null) {
            p = base.getJodaTimeInstant();
        }
        if (p == null && range != null) {
            p = range.mid().getJodaTimeInstant();
        }
        return p;
    }

    public Partial getJodaTimePartial() {
        Partial p = null;
        if (base != null) {
            p = base.getJodaTimePartial();
        }
        if (p == null && range != null) {
            p = range.mid().getJodaTimePartial();
        }
        return p;
    }

    public String toFormattedString(int flags) {
        if (getTimeLabel() != null) {
            return getTimeLabel();
        }
        if ((flags & SUTime.FORMAT_ISO) != 0) {
            return null;
        } // TODO: is there iso standard?
        // TODO: is there iso standard?
        if ((flags & SUTime.FORMAT_TIMEX3_VALUE) != 0) {
            return null;
        } // TODO: is there timex3 standard?
        // TODO: is there timex3 standard?
        StringBuilder sb = new StringBuilder();
        sb.append("~(");
        if (base != null) {
            sb.append(base.toFormattedString(flags));
        }
        if (duration != null) {
            sb.append(":");
            sb.append(duration.toFormattedString(flags));
        }
        if (range != null) {
            sb.append(" IN ");
            sb.append(range.toFormattedString(flags));
        }
        sb.append(")");
        return sb.toString();
    }
    private static final long serialVersionUID = 1;

    public void setTimeDensityFunction(TimeDensityFunction func) {
        _func = func;
    }

    public TimeDensityFunction getTimeDensityFunction() {
        return _func;
    }

}
