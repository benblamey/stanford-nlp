package edu.stanford.nlp.time;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import org.joda.time.DateTimeZone;

/**
 * Basic temporal object
 *
 * <p>
 * There are 4 main types of temporal objects
 * <ol>
 * <li>Time - Conceptually a point in time
 * <br>NOTE: Due to limitation in precision, it is
 * difficult to get an exact point in time
 * </li>
 * <li>Duration - Amount of time in a time interval
 *  <ul><li>DurationWithMillis - Duration specified in milliseconds
 *          (wrapper around JodaTime Duration)</li>
 *      <li>DurationWithFields - Duration specified with
 *         fields like day, year, etc (wrapper around JodaTime Period)</lI>
 *      <li>DurationRange - A duration that falls in a particular range (with min to max)</li>
 *  </ul>
 * </li>
 * <li>Range - Time Interval with a start time, end time, and duration</li>
 * <li>TemporalSet - A set of temporal objects
 *  <ul><li>ExplicitTemporalSet - Explicit set of temporals (not used)
 *         <br>Ex: Tuesday 1-2pm, Wednesday night</li>
 *      <li>PeriodicTemporalSet - Reoccuring times
 *         <br>Ex: Every Tuesday</li>
 *  </ul>
 * </li>
 * </ol>
 */
public abstract class Temporal implements Cloneable, Serializable {
    public String mod;
    public boolean approx;
    SUTime.StandardTemporalType standardTemporalType;
    public String timeLabel;

    public Temporal() {
    }

    public Temporal(Temporal t) {
        this.mod = t.mod;
        this.approx = t.approx;
        //      this.standardTimeType = t.standardTimeType;
        //      this.timeLabel = t.timeLabel;
    }

    public abstract boolean isGrounded();

    // Returns time representation for Temporal (if available)
    public abstract Time getTime();

    // Returns duration (estimate of how long the temporal expression is for)
    public abstract Duration getDuration();

    // Returns range (start/end points of temporal, automatic granularity)
    public Range getRange() {
        return getRange(SUTime.RANGE_FLAGS_PAD_AUTO);
    }

    // Returns range (start/end points of temporal)
    public Range getRange(int flags) {
        return getRange(flags, null);
    }

    // Returns range (start/end points of temporal), using specified flags
    public abstract Range getRange(int flags, Duration granularity);

    // Returns how often this time would repeat
    // Ex: friday repeat weekly, hour repeat hourly, hour in a day repeat daily
    public Duration getPeriod() {
        /*    TimeLabel tl = getTimeLabel();
        if (tl != null) {
        return tl.getPeriod();
        } */
        SUTime.StandardTemporalType tlt = getStandardTemporalType();
        if (tlt != null) {
            return tlt.getPeriod();
        }
        return null;
    }

    // Returns the granularity to which this time or duration is specified
    // Typically the most specific time unit
    public Duration getGranularity() {
        SUTime.StandardTemporalType tlt = getStandardTemporalType();
        if (tlt != null) {
            return tlt.getGranularity();
        }
        return null;
    }

    // Resolves this temporal expression with respect to the specified reference
    // time using flags
    public Temporal resolve(Time refTime) {
        return resolve(refTime, 0);
    }

    public abstract Temporal resolve(Time refTime, int flags);

    public SUTime.StandardTemporalType getStandardTemporalType() {
        return standardTemporalType;
    }

    // Returns if the current temporal expression is an reference
    public boolean isRef() {
        return false;
    }

    // Return sif the current temporal expression is approximate
    public boolean isApprox() {
        return approx;
    }

    // TIMEX related functions
    public int getTid(TimeIndex timeIndex) {
        return timeIndex.indexOfTemporal(this, true);
    }

    public String getTidString(TimeIndex timeIndex) {
        return "t" + getTid(timeIndex);
    }

    public int getTfid(TimeIndex timeIndex) {
        return timeIndex.indexOfTemporalFunc(this, true);
    }

    public String getTfidString(TimeIndex timeIndex) {
        return "tf" + getTfid(timeIndex);
    }

    // Returns attributes to convert this temporal expression into timex object
    public boolean includeTimexAltValue() {
        return false;
    }

    public Map<String, String> getTimexAttributes(TimeIndex timeIndex) {
        Map<String, String> map = new LinkedHashMap<String, String>();
        map.put(SUTime.TimexAttr.tid.name(), getTidString(timeIndex));
        // NOTE: GUTime used "VAL" instead of TIMEX3 standard "value"
        // NOTE: attributes are case sensitive, GUTIME used mostly upper case
        // attributes....
        String val = getTimexValue();
        if (val != null) {
            map.put(SUTime.TimexAttr.value.name(), val);
        }
        if (val == null || includeTimexAltValue()) {
            String str = toFormattedString(SUTime.FORMAT_FULL);
            if (str != null) {
                map.put("alt_value", str);
            }
        }
        /*     Range r = getRange();
        if (r != null) map.put("range", r.toString());    */
        /*     map.put("str", toString());        */
        map.put(SUTime.TimexAttr.type.name(), getTimexType().name());
        if (mod != null) {
            map.put(SUTime.TimexAttr.mod.name(), mod);
        }
        return map;
    }

    // Returns the timex type
    public SUTime.TimexType getTimexType() {
        if (getStandardTemporalType() != null) {
            return getStandardTemporalType().getTimexType();
        } else {
            return null;
        }
    }

    // Returns timex value (by default it is the ISO string representation of
    // this object)
    public String getTimexValue() {
        return toFormattedString(SUTime.FORMAT_TIMEX3_VALUE);
    }

    public String toISOString() {
        return toFormattedString(SUTime.FORMAT_ISO);
    }

    public String toString() {
        // TODO: Full string representation
        return toFormattedString(SUTime.FORMAT_FULL);
    }

    public String getTimeLabel() {
        return timeLabel;
    }

    public String toFormattedString(int flags) {
        return getTimeLabel();
    }

    // Temporal operations...
    public static Temporal setTimeZone(Temporal t, DateTimeZone tz) {
        if (t == null) {
            return null;
        }
        return t.setTimeZone(tz);
    }

    public Temporal setTimeZone(DateTimeZone tz) {
        return this;
    }

    // public abstract Temporal add(Duration offset);
    public Temporal next() {
        Duration per = getPeriod();
        if (per != null) {
            if (this instanceof Duration) {
                return new RelativeTime(new RelativeTime(SUTime.TemporalOp.THIS, this, SUTime.DUR_RESOLVE_TO_AS_REF), SUTime.TemporalOp.OFFSET, per);
            } else {
                // return new RelativeTime(new RelativeTime(TemporalOp.THIS, this),
                // TemporalOp.OFFSET, per);
                return SUTime.TemporalOp.OFFSET.apply(this, per);
            }
        }
        return null;
    }

    public Temporal prev() {
        Duration per = getPeriod();
        if (per != null) {
            if (this instanceof Duration) {
                return new RelativeTime(new RelativeTime(SUTime.TemporalOp.THIS, this, SUTime.DUR_RESOLVE_FROM_AS_REF), SUTime.TemporalOp.OFFSET, per.multiplyBy(-1));
            } else {
                // return new RelativeTime(new RelativeTime(TemporalOp.THIS, this),
                // TemporalOp.OFFSET, per.multiplyBy(-1));
                return SUTime.TemporalOp.OFFSET.apply(this, per.multiplyBy(-1));
            }
        }
        return null;
    }

    /* abstract*/
    public Temporal intersect(Temporal t) {
        return null;
    }

    public String getMod() {
        return mod;
    }

    /*   public void setMod(String mod) {
    this.mod = mod;
    } */
    public Temporal addMod(String mod) {
        try {
            Temporal t = (Temporal) this.clone();
            t.mod = mod;
            return t;
        } catch (CloneNotSupportedException ex) {
            throw new RuntimeException(ex);
        }
    }

    public Temporal addModApprox(String mod, boolean approx) {
        try {
            Temporal t = (Temporal) this.clone();
            t.mod = mod;
            t.approx = approx;
            return t;
        } catch (CloneNotSupportedException ex) {
            throw new RuntimeException(ex);
        }
    }
    private static final long serialVersionUID = 1;

}
