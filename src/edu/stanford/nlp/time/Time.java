package edu.stanford.nlp.time;


import edu.stanford.nlp.util.FuzzyInterval;
import edu.stanford.nlp.util.HasInterval;
import edu.stanford.nlp.util.Interval;
import org.joda.time.Instant;
import org.joda.time.Partial;
import org.joda.time.Period;

/**
 * Time represents a time point on some time scale.
 * It is the base class for representing various types of time points.
 * Typically, since most time scales have marks with certain granularity
 *   each time point can be represented as an interval.
 */
public abstract class Time extends Temporal implements FuzzyInterval.FuzzyComparable<Time>, HasInterval<Time> {

    public Time() {
    }

    public Time(Time t) {
        super(t); /*this.hasTime = t.hasTime; */ /*this.hasTime = t.hasTime; */
    }

    // Represents a point in time - there is typically some
    // uncertainty/imprecision in the exact time
    public boolean isGrounded() {
        return false;
    }

    // A time is defined by a begin and end point, and a duration
    public Time getTime() {
        return this;
    }

    // Default is a instant in time with same begin and end point
    // Every time should return a non-null range
    public Range getRange(int flags, Duration granularity) {
        return new Range(this, this);
    }

    // Default duration is zero
    public Duration getDuration() {
        return SUTime.DURATION_NONE;
    }

    public Duration getGranularity() {
        SUTime.StandardTemporalType tlt = getStandardTemporalType();
        if (tlt != null) {
            return tlt.getGranularity();
        }
        Partial p = this.getJodaTimePartial();
        return Duration.getDuration(JodaTimeUtils.getJodaTimePeriod(p));
    }

    public Interval<Time> getInterval() {
        Range r = getRange();
        if (r != null) {
            return r.getInterval();
        } else {
            return null;
        }
    }

    public boolean isComparable(Time t) {
        Instant i = this.getJodaTimeInstant();
        Instant i2 = t.getJodaTimeInstant();
        return i != null && i2 != null;
    }

    public int compareTo(Time t) {
        Instant i = this.getJodaTimeInstant();
        Instant i2 = t.getJodaTimeInstant();
        return i.compareTo(i2);
    }

    public boolean hasTime() {
        return false;
    }

    public SUTime.TimexType getTimexType() {
        if (getStandardTemporalType() != null) {
            return getStandardTemporalType().getTimexType();
        }
        return (hasTime()) ? SUTime.TimexType.TIME : SUTime.TimexType.DATE;
    }

    // Time operations
    public boolean contains(Time t) {
        // Check if this time contains other time
        return getRange().contains(t.getRange());
    }

    // public boolean isBefore(Time t);
    // public boolean isAfter(Time t);
    // public boolean overlaps(Time t);
    // Add duration to time
    public abstract Time add(Duration offset);

    public Time offset(Duration offset) {
        return add(offset);
    }

    public Time subtract(Duration offset) {
        return add(offset.multiplyBy(-1));
    }

    // Return closest time
    public static Time closest(Time ref, Time... times) {
        Time res = null;
        long refMillis = ref.getJodaTimeInstant().getMillis();
        long min = 0;
        for (Time t : times) {
            long d = Math.abs(refMillis - t.getJodaTimeInstant().getMillis());
            if (res == null || d < min) {
                res = t;
                min = d;
            }
        }
        return res;
    }

    // Get absolute difference between times
    public static Duration distance(Time t1, Time t2) {
        if (t1.compareTo(t2) < 0) {
            return difference(t1, t2);
        } else {
            return difference(t2, t1);
        }
    }

    // Get difference between times
    public static Duration difference(Time t1, Time t2) {
        // TODO: Difference does not work between days of the week
        // Get duration from this t1 to t2
        if (t1 == null || t2 == null) {
            return null;
        }
        Instant i1 = t1.getJodaTimeInstant();
        Instant i2 = t2.getJodaTimeInstant();
        if (i1 == null || i2 == null) {
            return null;
        }
        Duration d = new DurationWithMillis(i2.getMillis() - i1.getMillis());
        Duration g1 = t1.getGranularity();
        Duration g2 = t2.getGranularity();
        Duration g = Duration.max(g1, g2);
        if (g != null) {
            Period p = g.getJodaTimePeriod();
            p = p.normalizedStandard();
            Period p2 = JodaTimeUtils.discardMoreSpecificFields(d.getJodaTimePeriod(), p.getFieldType(p.size() - 1), i1.getChronology());
            return new DurationWithFields(p2);
        } else {
            return d;
        }
    }

    public static CompositePartialTime makeComposite(PartialTime pt, Time t) {
        CompositePartialTime cp = null;
        SUTime.StandardTemporalType tlt = t.getStandardTemporalType();
        if (tlt != null) {
            switch (tlt) {
                case TIME_OF_DAY:
                    cp = new CompositePartialTime(pt, null, null, t);
                    break;
                case PART_OF_YEAR:
                case QUARTER_OF_YEAR:
                case SEASON_OF_YEAR:
                    cp = new CompositePartialTime(pt, t, null, null);
                    break;
                case DAYS_OF_WEEK:
                    cp = new CompositePartialTime(pt, null, t, null);
                    break;
            }
        }
        return cp;
    }

    public Temporal resolve(Time t, int flags) {
        return this;
    }

    public Temporal intersect(Temporal t) {
        if (t == null) {
            return this;
        }
        if (t == SUTime.TIME_UNKNOWN || t == SUTime.DURATION_UNKNOWN) {
            return this;
        }
        if (t instanceof Time) {
            return intersect((Time) t);
        } else if (t instanceof Range) {
            return t.intersect(this);
        } else if (t instanceof Duration) {
            return new RelativeTime(this, SUTime.TemporalOp.INTERSECT, t);
        }
        return null;
    }

    protected Time intersect(Time t) {
        return null; //new RelativeTime(this, TemporalOp.INTERSECT, t);
        //new RelativeTime(this, TemporalOp.INTERSECT, t);
    }

    protected static Time intersect(Time t1, Time t2) {
        if (t1 == null) {
            return t2;
        }
        if (t2 == null) {
            return t1;
        }
        return t1.intersect(t2);
    }

    public static Time min(Time t1, Time t2) {
        if (t2 == null) {
            return t1;
        }
        if (t1 == null) {
            return t2;
        }
        if (t1.isComparable(t2)) {
            int c = t1.compareTo(t2);
            return (c < 0) ? t1 : t2;
        }
        return t1;
    }

    public static Time max(Time t1, Time t2) {
        if (t1 == null) {
            return t2;
        }
        if (t2 == null) {
            return t1;
        }
        if (t1.isComparable(t2)) {
            int c = t1.compareTo(t2);
            return (c >= 0) ? t1 : t2;
        }
        return t2;
    }

    // Conversions to joda time
    public Instant getJodaTimeInstant() {
        return null;
    }

    public Partial getJodaTimePartial() {
        return null;
    }
    private static final long serialVersionUID = 1;

}
