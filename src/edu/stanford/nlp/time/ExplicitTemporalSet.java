package edu.stanford.nlp.time;


import edu.stanford.nlp.util.CollectionUtils;
import edu.stanford.nlp.util.Generics;
import edu.stanford.nlp.util.StringUtils;
import java.util.Set;
import org.joda.time.DateTimeZone;

/**
 * Explicit set of times: like tomorrow and next week, not really used
 */
public class ExplicitTemporalSet extends TemporalSet {
    Set<Temporal> temporals;

    public ExplicitTemporalSet(Temporal... temporals) {
        this.temporals = CollectionUtils.asSet(temporals);
    }

    public ExplicitTemporalSet(Set<Temporal> temporals) {
        this.temporals = temporals;
    }

    public ExplicitTemporalSet(ExplicitTemporalSet p, Set<Temporal> temporals) {
        super(p);
        this.temporals = temporals;
    }

    public ExplicitTemporalSet setTimeZone(DateTimeZone tz) {
        Set<Temporal> tzTemporals = Generics.newHashSet(temporals.size());
        for (Temporal t : temporals) {
            tzTemporals.add(Temporal.setTimeZone(t, tz));
        }
        return new ExplicitTemporalSet(this, tzTemporals);
    }

    public boolean isGrounded() {
        return false;
    }

    public Time getTime() {
        return null;
    }

    public Duration getDuration() {
        // TODO: Return difference between min/max of set
        return null;
    }

    public Range getRange(int flags, Duration granularity) {
        // TODO: Return min/max of set
        return null;
    }

    public Temporal resolve(Time refTime, int flags) {
        Temporal[] newTemporals = new Temporal[temporals.size()];
        int i = 0;
        for (Temporal t : temporals) {
            newTemporals[i] = t.resolve(refTime, flags);
            i++;
        }
        return new ExplicitTemporalSet(newTemporals);
    }

    public String toFormattedString(int flags) {
        if (getTimeLabel() != null) {
            return getTimeLabel();
        }
        if ((flags & SUTime.FORMAT_ISO) != 0) {
            // TODO: is there iso standard?
            return null;
        }
        if ((flags & SUTime.FORMAT_TIMEX3_VALUE) != 0) {
            // TODO: is there timex3 standard?
            return null;
        }
        return "{" + StringUtils.join(temporals, ", ") + "}";
    }

    public Temporal intersect(Temporal other) {
        if (other == null) {
            return this;
        }
        if (other == SUTime.TIME_UNKNOWN || other == SUTime.DURATION_UNKNOWN) {
            return this;
        }
        Set<Temporal> newTemporals = Generics.newHashSet();
        for (Temporal t : temporals) {
            Temporal t2 = t.intersect(other);
            if (t2 != null) {
                newTemporals.add(t2);
            }
        }
        return new ExplicitTemporalSet(newTemporals);
    }
    private static final long serialVersionUID = 1;

}
