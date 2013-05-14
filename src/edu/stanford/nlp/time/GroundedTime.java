package edu.stanford.nlp.time;


import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.joda.time.MutableDateTime;
import org.joda.time.Partial;
import org.joda.time.Period;
import org.joda.time.ReadableInstant;

public class GroundedTime extends Time {
    // Represents an absolute time
    ReadableInstant base;

    public GroundedTime(Time p, ReadableInstant base) {
        super(p);
        this.base = base;
    }

    public GroundedTime(ReadableInstant base) {
        this.base = base;
    }

    public GroundedTime setTimeZone(DateTimeZone tz) {
        MutableDateTime tzBase = base.toInstant().toMutableDateTime();
        tzBase.setZone(tz); // TODO: setZoneRetainFields?
        // TODO: setZoneRetainFields?
        return new GroundedTime(this, tzBase);
    }

    public boolean hasTime() {
        return true;
    }

    public boolean isGrounded() {
        return true;
    }

    public Duration getDuration() {
        return SUTime.DURATION_NONE;
    }

    public Range getRange(int flags, Duration granularity) {
        return new Range(this, this);
    }

    public String toFormattedString(int flags) {
        return base.toString();
    }

    public Time resolve(Time refTime, int flags) {
        return this;
    }

    public Time add(Duration offset) {
        Period p = offset.getJodaTimePeriod();
        GroundedTime g = new GroundedTime(base.toInstant().withDurationAdded(p.toDurationFrom(base), 1));
        g.approx = this.approx;
        g.mod = this.mod;
        return g;
    }

    public Time intersect(Time t) {
        if (t.getRange().contains(this.getRange())) {
            return this;
        } else {
            return null;
        }
    }

    public Temporal intersect(Temporal other) {
        if (other == null) {
            return this;
        }
        if (other == SUTime.TIME_UNKNOWN) {
            return this;
        }
        if (other.getRange().contains(this.getRange())) {
            return this;
        } else {
            return null;
        }
    }

    public Instant getJodaTimeInstant() {
        return base.toInstant();
    }

    public Partial getJodaTimePartial() {
        return JodaTimeUtils.getPartial(base.toInstant(), JodaTimeUtils.EMPTY_ISO_PARTIAL);
    }
    private static final long serialVersionUID = 1;

}
