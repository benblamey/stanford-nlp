package edu.stanford.nlp.time;


import edu.stanford.nlp.util.FuzzyInterval;
import edu.stanford.nlp.util.HasInterval;
import edu.stanford.nlp.util.Interval;
import java.util.Map;
import org.joda.time.DateTimeZone;

/**
 * A time interval
 */
public class Range extends Temporal implements HasInterval<Time> {
    public Time begin = SUTime.TIME_UNKNOWN;
    public Time end = SUTime.TIME_UNKNOWN;
    Duration duration = SUTime.DURATION_UNKNOWN;

    public Range(Time begin, Time end) {
        this.begin = begin;
        this.end = end;
        this.duration = Time.difference(begin, end);
    }

    public Range(Time begin, Time end, Duration duration) {
        this.begin = begin;
        this.end = end;
        this.duration = duration;
    }

    public Range(Range r, Time begin, Time end, Duration duration) {
        super(r);
        this.begin = begin;
        this.end = end;
        this.duration = duration;
    }

    public Range setTimeZone(DateTimeZone tz) {
        return new Range(this, (Time) Temporal.setTimeZone(begin, tz), (Time) Temporal.setTimeZone(end, tz), duration);
    }

    public Interval<Time> getInterval() {
        return FuzzyInterval.toInterval(begin, end);
    }

    public org.joda.time.Interval getJodaTimeInterval() {
        return new org.joda.time.Interval(begin.getJodaTimeInstant(), end.getJodaTimeInstant());
    }

    public boolean isGrounded() {
        return begin.isGrounded() && end.isGrounded();
    }

    public Time getTime() {
        return begin;
    } // TODO: return something that makes sense for time...
    // TODO: return something that makes sense for time...

    public Duration getDuration() {
        return duration;
    }

    public Range getRange(int flags, Duration granularity) {
        return this;
    }

    public SUTime.TimexType getTimexType() {
        return SUTime.TimexType.DURATION;
    }

    public Map<String, String> getTimexAttributes(TimeIndex timeIndex) {
        String beginTidStr = (begin != null) ? begin.getTidString(timeIndex) : null;
        String endTidStr = (end != null) ? end.getTidString(timeIndex) : null;
        Map<String, String> map = super.getTimexAttributes(timeIndex);
        if (beginTidStr != null) {
            map.put(SUTime.TimexAttr.beginPoint.name(), beginTidStr);
        }
        if (endTidStr != null) {
            map.put(SUTime.TimexAttr.endPoint.name(), endTidStr);
        }
        return map;
    }

    // public boolean includeTimexAltValue() { return true; }
    public String toFormattedString(int flags) {
        if ((flags & (SUTime.FORMAT_ISO | SUTime.FORMAT_TIMEX3_VALUE)) != 0) {
            if (getTimeLabel() != null) {
                return getTimeLabel();
            }
            String beginStr = (begin != null) ? begin.toFormattedString(flags) : null;
            String endStr = (end != null) ? end.toFormattedString(flags) : null;
            String durationStr = (duration != null) ? duration.toFormattedString(flags) : null;
            if ((flags & SUTime.FORMAT_ISO) != 0) {
                if (beginStr != null && endStr != null) {
                    return beginStr + "/" + endStr;
                } else if (beginStr != null && durationStr != null) {
                    return beginStr + "/" + durationStr;
                } else if (durationStr != null && endStr != null) {
                    return durationStr + "/" + endStr;
                }
            }
            return durationStr;
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("(");
            if (begin != null) {
                sb.append(begin);
            }
            sb.append(",");
            if (end != null) {
                sb.append(end);
            }
            sb.append(",");
            if (duration != null) {
                sb.append(duration);
            }
            sb.append(")");
            return sb.toString();
        }
    }

    public Range resolve(Time refTime, int flags) {
        if (refTime == null) {
            return this;
        }
        if (isGrounded()) {
            return this;
        }
        if ((flags & SUTime.RANGE_RESOLVE_TIME_REF) != 0 && (begin == SUTime.TIME_REF || end == SUTime.TIME_REF)) {
            Time groundedBegin = begin;
            Duration groundedDuration = duration;
            if (begin == SUTime.TIME_REF) {
                groundedBegin = (Time) begin.resolve(refTime, flags);
                groundedDuration = (duration != null) ? duration.resolve(refTime, flags | SUTime.DUR_RESOLVE_FROM_AS_REF) : null;
            }
            Time groundedEnd = end;
            if (end == SUTime.TIME_REF) {
                groundedEnd = (Time) end.resolve(refTime, flags);
                groundedDuration = (duration != null) ? duration.resolve(refTime, flags | SUTime.DUR_RESOLVE_TO_AS_REF) : null;
            }
            return new Range(this, groundedBegin, groundedEnd, groundedDuration);
        } else {
            return this;
        }
    }

    // TODO: Implement some range operations....
    public Range offset(Duration d) {
        return offset(d, SUTime.RANGE_OFFSET_BEGIN | SUTime.RANGE_OFFSET_END);
    }

    public Range offset(Duration d, int flags) {
        Time b2 = begin;
        if ((flags & SUTime.RANGE_OFFSET_BEGIN) != 0) {
            b2 = (begin != null) ? begin.offset(d) : null;
        }
        Time e2 = end;
        if ((flags & SUTime.RANGE_OFFSET_END) != 0) {
            e2 = (end != null) ? end.offset(d) : null;
        }
        return new Range(this, b2, e2, duration);
    }

    public Range subtract(Duration d) {
        return subtract(d, SUTime.RANGE_EXPAND_FIX_BEGIN);
    }

    public Range subtract(Duration d, int flags) {
        return add(d.multiplyBy(-1), SUTime.RANGE_EXPAND_FIX_BEGIN);
    }

    public Range add(Duration d) {
        return add(d, SUTime.RANGE_EXPAND_FIX_BEGIN);
    }

    public Range add(Duration d, int flags) {
        Duration d2 = duration.add(d);
        Time b2 = begin;
        Time e2 = end;
        if ((flags & SUTime.RANGE_EXPAND_FIX_BEGIN) == 0) {
            b2 = (end != null) ? end.offset(d2.multiplyBy(-1)) : null;
        } else if ((flags & SUTime.RANGE_EXPAND_FIX_END) == 0) {
            e2 = (begin != null) ? begin.offset(d2) : null;
        }
        return new Range(this, b2, e2, d2);
    }

    public Time begin() {
        return begin;
    }

    public Time end() {
        return end;
    }

    public Time beginTime() {
        if (begin != null) {
            Range r = begin.getRange();
            if (r != null && !begin.equals(r.begin)) {
                // return r.beginTime();
                return r.begin;
            }
        }
        return begin;
    }

    public Time endTime() {
        /*    if (end != null) {
        Range r = end.getRange();
        if (r != null && !end.equals(r.end)) {
        //return r.endTime();
        return r.end;
        }
        }        */
        return end;
    }

    public Time mid() {
        if (duration != null && begin != null) {
            return begin.add(duration.divideBy(2));
        } else if (duration != null && end != null) {
            return end.subtract(duration.divideBy(2));
        } else if (begin != null && end != null) {
            // TODO: ....
            // TODO: ....
        } else if (begin != null) {
            return begin;
        } else if (end != null) {
            return end;
        }
        return null;
    }

    // TODO: correct implementation
    public Temporal intersect(Temporal t) {
        if (t instanceof Time) {
            return new RelativeTime((Time) t, SUTime.TemporalOp.INTERSECT, this);
        } else if (t instanceof Range) {
            Range rt = (Range) t;
            // Assume begin/end defined (TODO: handle if duration defined)
            Time b = Time.max(begin, rt.begin);
            Time e = Time.min(end, rt.end);
            return new Range(b, e);
        } else if (t instanceof Duration) {
            return new InexactTime(null, (Duration) t, this);
        }
        return null;
    }

    public boolean contains(Range r) {
        return false;
    }
    private static final long serialVersionUID = 1;

}
