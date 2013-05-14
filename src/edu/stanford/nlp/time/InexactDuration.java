package edu.stanford.nlp.time;


import org.joda.time.ReadablePeriod;

/**
 * Duration that is inexact.  Use for durations such as "several days"
 * in which case, we know the field is DAY, but we don't know the exact
 * number of days
 */
public class InexactDuration extends DurationWithFields {

    // Original duration is estimate of how long this duration is
    // but since some aspects of it is unknown....
    // for now all fields are inexact
    // TODO: Have inexact duration in which some fields are exact
    // add/toISOString
    // boolean[] exactFields;
    // Original duration is estimate of how long this duration is
    // but since some aspects of it is unknown....
    // for now all fields are inexact
    // TODO: Have inexact duration in which some fields are exact
    // add/toISOString
    // boolean[] exactFields;
    public InexactDuration(ReadablePeriod period) {
        this.period = period;
        // exactFields = new boolean[period.size()];
        this.approx = true;
    }

    public InexactDuration(Duration d) {
        super(d, d.getJodaTimePeriod());
        this.approx = true;
    }

    public InexactDuration(Duration d, ReadablePeriod period) {
        super(d, period);
        this.approx = true;
    }

    public String toFormattedString(int flags) {
        String s = super.toFormattedString(flags);
        return s.replaceAll("\\d+", SUTime.PAD_FIELD_UNKNOWN);
    }
    private static final long serialVersionUID = 1;

}
