package edu.stanford.nlp.time;


import org.joda.time.DurationFieldType;
import org.joda.time.Instant;
import org.joda.time.MutablePeriod;
import org.joda.time.Period;
import org.joda.time.ReadablePeriod;

/**
 * Duration that is specified using fields such as milliseconds, days, etc.
 */
public class DurationWithFields extends Duration {
    // Use Inexact duration to be able to specify duration with uncertain number
    // Like a few years
    ReadablePeriod period;

    public DurationWithFields() {
        this.period = null;
    }

    public DurationWithFields(ReadablePeriod period) {
        this.period = period;
    }

    public DurationWithFields(Duration d, ReadablePeriod period) {
        super(d);
        this.period = period;
    }

    public Duration multiplyBy(int m) {
        if (m == 1 || period == null) {
            return this;
        } else {
            MutablePeriod p = period.toMutablePeriod();
            for (int i = 0; i < period.size(); i++) {
                p.setValue(i, period.getValue(i) * m);
            }
            return new DurationWithFields(p);
        }
    }

    public Duration divideBy(int m) {
        if (m == 1 || period == null) {
            return this;
        } else {
            MutablePeriod p = new MutablePeriod();
            for (int i = 0; i < period.size(); i++) {
                int oldVal = period.getValue(i);
                DurationFieldType field = period.getFieldType(i);
                int remainder = oldVal % m;
                p.add(field, oldVal - remainder);
                if (remainder != 0) {
                    DurationFieldType f;
                    int standardUnit = 1;
                    // TODO: This seems silly, how to do this with jodatime???
                    if (DurationFieldType.centuries().equals(field)) {
                        f = DurationFieldType.years();
                        standardUnit = 100;
                    } else if (DurationFieldType.years().equals(field)) {
                        f = DurationFieldType.months();
                        standardUnit = 12;
                    } else if (DurationFieldType.halfdays().equals(field)) {
                        f = DurationFieldType.hours();
                        standardUnit = 12;
                    } else if (DurationFieldType.days().equals(field)) {
                        f = DurationFieldType.hours();
                        standardUnit = 24;
                    } else if (DurationFieldType.hours().equals(field)) {
                        f = DurationFieldType.minutes();
                        standardUnit = 60;
                    } else if (DurationFieldType.minutes().equals(field)) {
                        f = DurationFieldType.seconds();
                        standardUnit = 60;
                    } else if (DurationFieldType.seconds().equals(field)) {
                        f = DurationFieldType.millis();
                        standardUnit = 1000;
                    } else if (DurationFieldType.months().equals(field)) {
                        f = DurationFieldType.days();
                        standardUnit = 30;
                    } else if (DurationFieldType.weeks().equals(field)) {
                        f = DurationFieldType.days();
                        standardUnit = 7;
                    } else if (DurationFieldType.millis().equals(field)) {
                        // No more granularity units....
                        f = DurationFieldType.millis();
                        standardUnit = 0;
                    } else {
                        throw new UnsupportedOperationException("Unsupported duration type: " + field + " when dividing");
                    }
                    p.add(f, standardUnit * remainder);
                }
            }
            for (int i = 0; i < p.size(); i++) {
                p.setValue(i, p.getValue(i) / m);
            }
            return new DurationWithFields(p);
        }
    }

    public Period getJodaTimePeriod() {
        return (period != null) ? period.toPeriod() : null;
    }

    public org.joda.time.Duration getJodaTimeDuration() {
        return (period != null) ? period.toPeriod().toDurationFrom(JodaTimeUtils.INSTANT_ZERO) : null;
    }

    public Duration resolve(Time refTime, int flags) {
        Instant instant = (refTime != null) ? refTime.getJodaTimeInstant() : null;
        if (instant != null) {
            if ((flags & SUTime.DUR_RESOLVE_FROM_AS_REF) != 0) {
                return new DurationWithMillis(this, period.toPeriod().toDurationFrom(instant));
            } else if ((flags & SUTime.DUR_RESOLVE_TO_AS_REF) != 0) {
                return new DurationWithMillis(this, period.toPeriod().toDurationTo(instant));
            }
        }
        return this;
    }

    public Duration add(Duration d) {
        Period p = period.toPeriod().plus(d.getJodaTimePeriod());
        if (this instanceof InexactDuration || d instanceof InexactDuration) {
            return new InexactDuration(this, p);
        } else {
            return new DurationWithFields(this, p);
        }
    }
    private static final long serialVersionUID = 1;

}
