package edu.stanford.nlp.time;


import org.joda.time.Period;

/**
 * A range of durations.  For instance, 2 to 3 days.
 */
public class DurationRange extends Duration {
    Duration minDuration;
    Duration maxDuration;

    public DurationRange(DurationRange d, Duration min, Duration max) {
        super(d);
        this.minDuration = min;
        this.maxDuration = max;
    }

    public DurationRange(Duration min, Duration max) {
        this.minDuration = min;
        this.maxDuration = max;
    }

    public boolean includeTimexAltValue() {
        return true;
    }

    public String toFormattedString(int flags) {
        if ((flags & (SUTime.FORMAT_ISO | SUTime.FORMAT_TIMEX3_VALUE)) != 0) {
            // return super.toFormattedString(flags);
            return null;
        }
        StringBuilder sb = new StringBuilder();
        if (minDuration != null) {
            sb.append(minDuration.toFormattedString(flags));
        }
        sb.append("/");
        if (maxDuration != null) {
            sb.append(maxDuration.toFormattedString(flags));
        }
        return sb.toString();
    }

    public Period getJodaTimePeriod() {
        if (minDuration == null) {
            return maxDuration.getJodaTimePeriod();
        }
        if (maxDuration == null) {
            return minDuration.getJodaTimePeriod();
        }
        Duration mid = minDuration.add(maxDuration).divideBy(2);
        return mid.getJodaTimePeriod();
    }

    public org.joda.time.Duration getJodaTimeDuration() {
        if (minDuration == null) {
            return maxDuration.getJodaTimeDuration();
        }
        if (maxDuration == null) {
            return minDuration.getJodaTimeDuration();
        }
        Duration mid = minDuration.add(maxDuration).divideBy(2);
        return mid.getJodaTimeDuration();
    }

    public Duration add(Duration d) {
        Duration min2 = (minDuration != null) ? minDuration.add(d) : null;
        Duration max2 = (maxDuration != null) ? maxDuration.add(d) : null;
        return new DurationRange(this, min2, max2);
    }

    public Duration multiplyBy(int m) {
        Duration min2 = (minDuration != null) ? minDuration.multiplyBy(m) : null;
        Duration max2 = (maxDuration != null) ? maxDuration.multiplyBy(m) : null;
        return new DurationRange(this, min2, max2);
    }

    public Duration divideBy(int m) {
        Duration min2 = (minDuration != null) ? minDuration.divideBy(m) : null;
        Duration max2 = (maxDuration != null) ? maxDuration.divideBy(m) : null;
        return new DurationRange(this, min2, max2);
    }
    private static final long serialVersionUID = 1;

}
