package edu.stanford.nlp.time;

import edu.stanford.nlp.time.distributed.TimeDensityFunction;
import java.util.ArrayList;
import java.util.List;
import org.joda.time.DateTime;
import org.joda.time.DateTimeField;
import org.joda.time.DateTimeFieldType;
import org.joda.time.DateTimeZone;
import org.joda.time.DurationFieldType;
import org.joda.time.Instant;
import org.joda.time.Partial;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;

// Partial time with Joda Time fields
public class PartialTime extends Time {
    // There is typically some uncertainty/imprecision in the time

    Partial base; // For representing partial absolute time
    // For representing partial absolute time
    DateTimeZone dateTimeZone; // Datetime zone associated with this time
    // Datetime zone associated with this time

    // private static DateTimeFormatter isoDateFormatter =
    // ISODateTimeFormat.basicDate();
    // private static DateTimeFormatter isoDateTimeFormatter =
    // ISODateTimeFormat.basicDateTimeNoMillis();
    // private static DateTimeFormatter isoTimeFormatter =
    // ISODateTimeFormat.basicTTimeNoMillis();
    // private static DateTimeFormatter isoDateFormatter =
    // ISODateTimeFormat.date();
    // private static DateTimeFormatter isoDateTimeFormatter =
    // ISODateTimeFormat.dateTimeNoMillis();
    // private static DateTimeFormatter isoTimeFormatter =
    // ISODateTimeFormat.tTimeNoMillis();
    // private static DateTimeFormatter isoDateFormatter =
    // ISODateTimeFormat.basicDate();
    // private static DateTimeFormatter isoDateTimeFormatter =
    // ISODateTimeFormat.basicDateTimeNoMillis();
    // private static DateTimeFormatter isoTimeFormatter =
    // ISODateTimeFormat.basicTTimeNoMillis();
    // private static DateTimeFormatter isoDateFormatter =
    // ISODateTimeFormat.date();
    // private static DateTimeFormatter isoDateTimeFormatter =
    // ISODateTimeFormat.dateTimeNoMillis();
    // private static DateTimeFormatter isoTimeFormatter =
    // ISODateTimeFormat.tTimeNoMillis();
    public PartialTime(Time t, Partial p) {
        super(t);
        this.base = p;
    }

    public PartialTime(PartialTime pt) {
        super(pt);
        this.base = pt.base;
    }

    // public PartialTime(Partial base, String mod) { this.base = base; this.mod
    // = mod; }
    // public PartialTime(Partial base, String mod) { this.base = base; this.mod
    // = mod; }
    public PartialTime(Partial base) {
        this.base = base;
    }

    public PartialTime(SUTime.StandardTemporalType temporalType, Partial base) {
        this.base = base;
        this.standardTemporalType = temporalType;
    }

    public PartialTime() {
    }

    public PartialTime setTimeZone(DateTimeZone tz) {
        PartialTime tzPt = new PartialTime(this, base);
        tzPt.dateTimeZone = tz;
        return tzPt;
    }

    public Instant getJodaTimeInstant() {
        return JodaTimeUtils.getInstant(base);
    }

    public Partial getJodaTimePartial() {
        return base;
    }

    public boolean hasTime() {
        if (base == null) {
            return false;
        }
        DateTimeFieldType sdft = JodaTimeUtils.getMostSpecific(base);
        if (sdft != null && JodaTimeUtils.isMoreGeneral(DateTimeFieldType.dayOfMonth(), sdft, base.getChronology())) {
            return true;
        } else {
            return false;
        }
    }

    protected boolean appendDateFormats(DateTimeFormatterBuilder builder, int flags) {
        boolean alwaysPad = (flags & SUTime.FORMAT_PAD_UNKNOWN) != 0;
        boolean hasDate = true;
        boolean isISO = (flags & SUTime.FORMAT_ISO) != 0;
        boolean isTimex3 = (flags & SUTime.FORMAT_TIMEX3_VALUE) != 0;
        // ERA
        if (JodaTimeUtils.hasField(base, DateTimeFieldType.era())) {
            int era = base.get(DateTimeFieldType.era());
            if (era == 0) {
                builder.appendLiteral('-');
            } else if (era == 1) {
                builder.appendLiteral('+');
            }
        }
        // YEAR
        if (JodaTimeUtils.hasField(base, DateTimeFieldType.centuryOfEra()) || JodaTimeUtils.hasField(base, JodaTimeUtils.DecadeOfCentury) || JodaTimeUtils.hasField(base, DateTimeFieldType.yearOfCentury())) {
            if (JodaTimeUtils.hasField(base, DateTimeFieldType.centuryOfEra())) {
                builder.appendCenturyOfEra(2, 2);
            } else {
                builder.appendLiteral(SUTime.PAD_FIELD_UNKNOWN2);
            }
            if (JodaTimeUtils.hasField(base, JodaTimeUtils.DecadeOfCentury)) {
                builder.appendDecimal(JodaTimeUtils.DecadeOfCentury, 1, 1);
                builder.appendLiteral(SUTime.PAD_FIELD_UNKNOWN);
            } else if (JodaTimeUtils.hasField(base, DateTimeFieldType.yearOfCentury())) {
                builder.appendYearOfCentury(2, 2);
            } else {
                builder.appendLiteral(SUTime.PAD_FIELD_UNKNOWN2);
            }
        } else if (JodaTimeUtils.hasField(base, DateTimeFieldType.year())) {
            builder.appendYear(4, 4);
        } else if (JodaTimeUtils.hasField(base, DateTimeFieldType.weekyear())) {
            builder.appendWeekyear(4, 4);
        } else {
            builder.appendLiteral(SUTime.PAD_FIELD_UNKNOWN4);
            hasDate = false;
        }
        // Decide whether to include QUARTER, MONTH/DAY, or WEEK/WEEKDAY
        boolean appendQuarter = false;
        boolean appendMonthDay = false;
        boolean appendWeekDay = false;
        if (isISO || isTimex3) {
            if (JodaTimeUtils.hasField(base, DateTimeFieldType.monthOfYear()) && JodaTimeUtils.hasField(base, DateTimeFieldType.dayOfMonth())) {
                appendMonthDay = true;
            } else if (JodaTimeUtils.hasField(base, DateTimeFieldType.weekOfWeekyear()) || JodaTimeUtils.hasField(base, DateTimeFieldType.dayOfWeek())) {
                appendWeekDay = true;
            } else if (JodaTimeUtils.hasField(base, DateTimeFieldType.monthOfYear()) || JodaTimeUtils.hasField(base, DateTimeFieldType.dayOfMonth())) {
                appendMonthDay = true;
            } else if (JodaTimeUtils.hasField(base, JodaTimeUtils.QuarterOfYear)) {
                appendQuarter = true;
            }
        } else {
            appendQuarter = true;
            appendMonthDay = true;
            appendWeekDay = true;
        }
        // Quarter
        if (appendQuarter && JodaTimeUtils.hasField(base, JodaTimeUtils.QuarterOfYear)) {
            builder.appendLiteral("-Q");
            builder.appendDecimal(JodaTimeUtils.QuarterOfYear, 1, 1);
        }
        // MONTH
        if (appendMonthDay && (JodaTimeUtils.hasField(base, DateTimeFieldType.monthOfYear()) || JodaTimeUtils.hasField(base, DateTimeFieldType.dayOfMonth()))) {
            hasDate = true;
            builder.appendLiteral('-');
            if (JodaTimeUtils.hasField(base, DateTimeFieldType.monthOfYear())) {
                builder.appendMonthOfYear(2);
            } else {
                builder.appendLiteral(SUTime.PAD_FIELD_UNKNOWN2);
            }
            // Don't indicate day of month if not specified
            if (JodaTimeUtils.hasField(base, DateTimeFieldType.dayOfMonth())) {
                builder.appendLiteral('-');
                builder.appendDayOfMonth(2);
            } else if (alwaysPad) {
                builder.appendLiteral(SUTime.PAD_FIELD_UNKNOWN2);
            }
        }
        if (appendWeekDay && (JodaTimeUtils.hasField(base, DateTimeFieldType.weekOfWeekyear()) || JodaTimeUtils.hasField(base, DateTimeFieldType.dayOfWeek()))) {
            hasDate = true;
            builder.appendLiteral("-W");
            if (JodaTimeUtils.hasField(base, DateTimeFieldType.weekOfWeekyear())) {
                builder.appendWeekOfWeekyear(2);
            } else {
                builder.appendLiteral(SUTime.PAD_FIELD_UNKNOWN2);
            }
            // Don't indicate the day of the week if not specified
            if (JodaTimeUtils.hasField(base, DateTimeFieldType.dayOfWeek())) {
                builder.appendLiteral("-");
                builder.appendDayOfWeek(1);
            }
        }
        return hasDate;
    }

    protected boolean appendTimeFormats(DateTimeFormatterBuilder builder, int flags) {
        boolean alwaysPad = (flags & SUTime.FORMAT_PAD_UNKNOWN) != 0;
        boolean hasTime = hasTime();
        DateTimeFieldType sdft = JodaTimeUtils.getMostSpecific(base);
        if (hasTime) {
            builder.appendLiteral("T");
            if (JodaTimeUtils.hasField(base, DateTimeFieldType.hourOfDay())) {
                builder.appendHourOfDay(2);
            } else if (JodaTimeUtils.hasField(base, DateTimeFieldType.clockhourOfDay())) {
                builder.appendClockhourOfDay(2);
            } else {
                builder.appendLiteral(SUTime.PAD_FIELD_UNKNOWN2);
            }
            if (JodaTimeUtils.hasField(base, DateTimeFieldType.minuteOfHour())) {
                builder.appendLiteral(":");
                builder.appendMinuteOfHour(2);
            } else if (alwaysPad || JodaTimeUtils.isMoreGeneral(DateTimeFieldType.minuteOfHour(), sdft, base.getChronology())) {
                builder.appendLiteral(":");
                builder.appendLiteral(SUTime.PAD_FIELD_UNKNOWN2);
            }
            if (JodaTimeUtils.hasField(base, DateTimeFieldType.secondOfMinute())) {
                builder.appendLiteral(":");
                builder.appendSecondOfMinute(2);
            } else if (alwaysPad || JodaTimeUtils.isMoreGeneral(DateTimeFieldType.secondOfMinute(), sdft, base.getChronology())) {
                builder.appendLiteral(":");
                builder.appendLiteral(SUTime.PAD_FIELD_UNKNOWN2);
            }
            if (JodaTimeUtils.hasField(base, DateTimeFieldType.millisOfSecond())) {
                builder.appendLiteral(".");
                builder.appendMillisOfSecond(3);
            }
            // builder.append(isoTimeFormatter);
        }
        return hasTime;
    }

    protected DateTimeFormatter getFormatter(int flags) {
        DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder();
        boolean hasDate = appendDateFormats(builder, flags);
        boolean hasTime = hasTime();
        if (hasTime) {
            if (!hasDate) {
                builder.clear();
            }
            appendTimeFormats(builder, flags);
        }
        return builder.toFormatter();
    }

    public boolean isGrounded() {
        return false;
    }

    // TODO: compute duration/range => uncertainty of this time
    public Duration getDuration() {
        /*      TimeLabel tl = getTimeLabel();
        if (tl != null) {
        return tl.getDuration();
        } */
        SUTime.StandardTemporalType tlt = getStandardTemporalType();
        if (tlt != null) {
            return tlt.getDuration();
        }
        return Duration.getDuration(JodaTimeUtils.getJodaTimePeriod(base));
    }

    public Range getRange(int flags, Duration inputGranularity) {
        Duration d = getDuration();
        if (d != null) {
            int padType = flags & SUTime.RANGE_FLAGS_PAD_MASK;
            Time start = this;
            Duration granularity = inputGranularity;
            switch (padType) {
                case SUTime.RANGE_FLAGS_PAD_NONE:
                    // The most basic range
                    start = this;
                    break;
                case SUTime.RANGE_FLAGS_PAD_AUTO:
                    // More complex range
                    if (hasTime()) {
                        granularity = SUTime.MILLIS;
                    } else {
                        granularity = SUTime.DAY;
                    }
                    start = padMoreSpecificFields(granularity);
                    break;
                case SUTime.RANGE_FLAGS_PAD_FINEST:
                    granularity = SUTime.MILLIS;
                    start = padMoreSpecificFields(granularity);
                    break;
                case SUTime.RANGE_FLAGS_PAD_SPECIFIED:
                    start = padMoreSpecificFields(granularity);
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported pad type for getRange: " + flags);
            }
            if (start instanceof PartialTime) {
                ((PartialTime) start).withStandardFields();
            }
            Time end = start.add(d);
            if (granularity != null) {
                end = end.subtract(granularity);
            }
            return new Range(start, end, d);
        } else {
            return new Range(this, this);
        }
    }

    protected void withStandardFields() {
        if (base.isSupported(DateTimeFieldType.dayOfWeek())) {
            base = JodaTimeUtils.resolveDowToDay(base);
        } else if (base.isSupported(DateTimeFieldType.monthOfYear()) && base.isSupported(DateTimeFieldType.dayOfMonth())) {
            if (base.isSupported(DateTimeFieldType.weekOfWeekyear())) {
                base = base.without(DateTimeFieldType.weekOfWeekyear());
            }
            if (base.isSupported(DateTimeFieldType.dayOfWeek())) {
                base = base.without(DateTimeFieldType.dayOfWeek());
            }
        }
    }

    public PartialTime padMoreSpecificFields(Duration granularity) {
        Period period = null;
        if (granularity != null) {
            period = granularity.getJodaTimePeriod();
        }
        Partial p = JodaTimeUtils.padMoreSpecificFields(base, period);
        return new PartialTime(p);
    }

    public String toFormattedString(int flags) {
        if (getTimeLabel() != null) {
            return getTimeLabel();
        }
        String s = null;
        if (base != null) {
            // String s = ISODateTimeFormat.basicDateTime().print(base);
            // return s.replace('\ufffd', 'X');
            DateTimeFormatter formatter = getFormatter(flags);
            s = formatter.print(base);
        } else {
            s = "XXXX-XX-XX";
        }
        if (dateTimeZone != null) {
            DateTimeFormatter formatter = DateTimeFormat.forPattern("Z");
            formatter = formatter.withZone(dateTimeZone);
            s = s + formatter.print(0);
        }
        return s;
    }

    public Time resolve(Time ref, int flags) {
        if (ref == null || ref == SUTime.TIME_UNKNOWN || ref == SUTime.TIME_REF) {
            return this;
        }
        if (this == SUTime.TIME_REF) {
            return ref;
        }
        if (this == SUTime.TIME_UNKNOWN) {
            return this;
        }
        Partial partialRef = ref.getJodaTimePartial();
        if (partialRef == null) {
            throw new UnsupportedOperationException("Cannot resolve if reftime is of class: " + ref.getClass());
        }
        Partial p = (base != null) ? JodaTimeUtils.combineMoreGeneralFields(base, partialRef) : partialRef;
        p = JodaTimeUtils.resolveDowToDay(p, partialRef);
        Time resolved;
        if (p == base) {
            resolved = this;
        } else {
            resolved = new PartialTime(this, p);
            // System.err.println("Resolved " + this + " to " + resolved + ", ref=" + ref);
        }
        Duration resolvedGranularity = resolved.getGranularity();
        Duration refGranularity = ref.getGranularity();
        // System.err.println("refGranularity is " + refGranularity);
        // System.err.println("resolvedGranularity is " + resolvedGranularity);
        if (resolvedGranularity != null && refGranularity != null && resolvedGranularity.compareTo(refGranularity) >= 0) {
            if ((flags & SUTime.RESOLVE_TO_PAST) != 0) {
                if (resolved.compareTo(ref) > 0) {
                    Time t = (Time) this.prev();
                    if (t != null) {
                        resolved = (Time) t.resolve(ref, 0);
                    }
                }
                // System.err.println("Resolved " + this + " to past " + resolved + ", ref=" + ref);
            } else if ((flags & SUTime.RESOLVE_TO_FUTURE) != 0) {
                if (resolved.compareTo(ref) < 0) {
                    Time t = (Time) this.next();
                    if (t != null) {
                        resolved = (Time) t.resolve(ref, 0);
                    }
                }
                // System.err.println("Resolved " + this + " to future " + resolved + ", ref=" + ref);
            } else if ((flags & SUTime.RESOLVE_TO_CLOSEST) != 0) {
                if (resolved.compareTo(ref) > 0) {
                    Time t = (Time) this.prev();
                    if (t != null) {
                        Time resolved2 = (Time) t.resolve(ref, 0);
                        resolved = Time.closest(ref, resolved, resolved2);
                    }
                }
                if (resolved.compareTo(ref) < 0) {
                    Time t = (Time) this.next();
                    if (t != null) {
                        Time resolved2 = (Time) t.resolve(ref, 0);
                        resolved = Time.closest(ref, resolved, resolved2);
                    }
                }
                // System.err.println("Resolved " + this + " to closest " + resolved + ", ref=" + ref);
            }
        }
        return resolved;
    }

    public boolean isCompatible(PartialTime time) {
        return JodaTimeUtils.isCompatible(base, time.base);
    }

    public Duration getPeriod() {
        /*    TimeLabel tl = getTimeLabel();
        if (tl != null) {
        return tl.getPeriod();
        } */
        SUTime.StandardTemporalType tlt = getStandardTemporalType();
        if (tlt != null) {
            return tlt.getPeriod();
        }
        if (base == null) {
            return null;
        }
        DateTimeFieldType mostGeneral = JodaTimeUtils.getMostGeneral(base);
        DurationFieldType df = mostGeneral.getRangeDurationType();
        // if (df == null) {
        // df = mostGeneral.getDurationType();
        // }
        if (df != null) {
            try {
                return new DurationWithFields(new Period().withField(df, 1));
            } catch (Exception ex) {
                // TODO: Do something intelligent here
                // TODO: Do something intelligent here
            }
        }
        return null;
    }

    public List<Temporal> toList() {
        if (JodaTimeUtils.hasField(base, DateTimeFieldType.year()) && JodaTimeUtils.hasField(base, DateTimeFieldType.monthOfYear()) && JodaTimeUtils.hasField(base, DateTimeFieldType.dayOfWeek())) {
            List<Temporal> list = new ArrayList<Temporal>();
            Partial pt = new Partial();
            pt = JodaTimeUtils.setField(pt, DateTimeFieldType.year(), base.get(DateTimeFieldType.year()));
            pt = JodaTimeUtils.setField(pt, DateTimeFieldType.monthOfYear(), base.get(DateTimeFieldType.monthOfYear()));
            pt = JodaTimeUtils.setField(pt, DateTimeFieldType.dayOfMonth(), 1);
            Partial candidate = JodaTimeUtils.resolveDowToDay(base, pt);
            if (candidate.get(DateTimeFieldType.monthOfYear()) != base.get(DateTimeFieldType.monthOfYear())) {
                pt = JodaTimeUtils.setField(pt, DateTimeFieldType.dayOfMonth(), 8);
                candidate = JodaTimeUtils.resolveDowToDay(base, pt);
                if (candidate.get(DateTimeFieldType.monthOfYear()) != base.get(DateTimeFieldType.monthOfYear())) {
                    // give up
                    return null;
                }
            }
            while (candidate.get(DateTimeFieldType.monthOfYear()) == base.get(DateTimeFieldType.monthOfYear())) {
                list.add(new PartialTime(this, candidate));
                pt = JodaTimeUtils.setField(pt, DateTimeFieldType.dayOfMonth(), pt.get(DateTimeFieldType.dayOfMonth()) + 7);
                candidate = JodaTimeUtils.resolveDowToDay(base, pt);
            }
            return list;
        } else {
            return null;
        }
    }

    public Time intersect(Time t) {
        if (t == null || t == SUTime.TIME_UNKNOWN) {
            return this;
        }
        if (base == null) {
            return t;
        }
        if (t instanceof CompositePartialTime) {
            return t.intersect(this);
        } else if (t instanceof PartialTime) {
            if (!isCompatible((PartialTime) t)) {
                return null;
            }
            Partial p = JodaTimeUtils.combine(base, ((PartialTime) t).base);
            return new PartialTime(p);
        } else if (t instanceof GroundedTime) {
            return t.intersect(this);
        } else if (t instanceof RelativeTime) {
            return t.intersect(this);
        } else {
            Time cpt = makeComposite(this, t);
            if (cpt != null) {
                return cpt;
            }
            if (t instanceof InexactTime) {
                return t.intersect(this);
            }
        }
        return null;
        // return new RelativeTime(this, TemporalOp.INTERSECT, t);
    }

    /*public Temporal intersect(Temporal t) {
    if (t == null)
    return this;
    if (t == TIME_UNKNOWN || t == DURATION_UNKNOWN)
    return this;
    if (base == null)
    return t;
    if (t instanceof Time) {
    return intersect((Time) t);
    } else if (t instanceof Range) {
    return t.intersect(this);
    } else if (t instanceof Duration) {
    return new RelativeTime(this, TemporalOp.INTERSECT, t);
    }
    return null;
    }        */
    protected PartialTime addSupported(Period p, int scalar) {
        return new PartialTime(base.withPeriodAdded(p, scalar));
    }

    protected PartialTime addUnsupported(Period p, int scalar) {
        return new PartialTime(this, JodaTimeUtils.addForce(base, p, scalar));
    }

    public Time add(Duration offset) {
        if (base == null) {
            return this;
        }
        Period per = offset.getJodaTimePeriod();
        PartialTime p = addSupported(per, 1);
        Period unsupported = JodaTimeUtils.getUnsupportedDurationPeriod(p.base, per);
        Time t = p;
        if (unsupported != null) {
            if (JodaTimeUtils.hasField(unsupported, DurationFieldType.weeks()) && JodaTimeUtils.hasField(p.base, DateTimeFieldType.year()) && JodaTimeUtils.hasField(p.base, DateTimeFieldType.monthOfYear()) && JodaTimeUtils.hasField(p.base, DateTimeFieldType.dayOfMonth())) {
                // What if there are other unsupported fields...
                t = p.addUnsupported(per, 1);
            } else {
                if (JodaTimeUtils.hasField(unsupported, DurationFieldType.months()) && unsupported.getMonths() % 3 == 0 && JodaTimeUtils.hasField(p.base, JodaTimeUtils.QuarterOfYear)) {
                    Partial p2 = p.base.withFieldAddWrapped(JodaTimeUtils.Quarters, unsupported.getMonths() / 3);
                    p = new PartialTime(p, p2);
                    unsupported = unsupported.withMonths(0);
                }
                if (JodaTimeUtils.hasField(unsupported, DurationFieldType.years()) && unsupported.getYears() % 10 == 0 && JodaTimeUtils.hasField(p.base, JodaTimeUtils.DecadeOfCentury)) {
                    Partial p2 = p.base.withFieldAddWrapped(JodaTimeUtils.Decades, unsupported.getYears() / 10);
                    p = new PartialTime(p, p2);
                    unsupported = unsupported.withYears(0);
                }
                if (JodaTimeUtils.hasField(unsupported, DurationFieldType.years()) && unsupported.getYears() % 100 == 0 && JodaTimeUtils.hasField(p.base, DateTimeFieldType.centuryOfEra())) {
                    Partial p2 = p.base.withField(DateTimeFieldType.centuryOfEra(), p.base.get(DateTimeFieldType.centuryOfEra()) + unsupported.getYears() / 100);
                    p = new PartialTime(p, p2);
                    unsupported = unsupported.withYears(0);
                }
                if (unsupported.getDays() > 0 && !JodaTimeUtils.hasField(p.base, DateTimeFieldType.dayOfYear()) && !JodaTimeUtils.hasField(p.base, DateTimeFieldType.dayOfMonth()) && !JodaTimeUtils.hasField(p.base, DateTimeFieldType.dayOfWeek()) && JodaTimeUtils.hasField(p.base, DateTimeFieldType.monthOfYear())) {
                    Partial p2 = p.base.with(DateTimeFieldType.dayOfMonth(), unsupported.getDays());
                    p = new PartialTime(p, p2);
                    unsupported = unsupported.withDays(0);
                }
                if (!unsupported.equals(Period.ZERO)) {
                    t = new RelativeTime(p, new DurationWithFields(unsupported));
                    t.approx = this.approx;
                    t.mod = this.mod;
                } else {
                    t = p;
                }
            }
        }
        return t;
    }
    private static final long serialVersionUID = 1;

    public TimeDensityFunction getTimeDensityFunction() {
        
        TimeDensityFunction iTimeDensityFunction = new TimeDensityFunction() {
            public double getDensity(DateTime time) {
                for (DateTimeField f : base.getFields()) {
                    int get = base.get(f.getType());
                    DateTimeFieldType type = f.getType();

                    if (type == DateTimeFieldType.year()) {
                        if (get != time.getYear()) return 0;
                    } else if (type == DateTimeFieldType.yearOfCentury()) {
                        get = get + 2000;
                        if (get != time.getYear()) return 0;
                    } else if (type == DateTimeFieldType.monthOfYear()) {
                        if (get != time.getMonthOfYear()) return 0;
                    } else if (type == DateTimeFieldType.dayOfMonth()) {
                        if (get != time.getDayOfMonth()) return 0;
                   } else {
                        throw new UnsupportedOperationException("field type not supported.");
                   }
                }
                return 1;
            }

            @Override
            public String getGNUPlot(String millTimeSecondsExpr) {

                String func = "1";

                for (DateTimeField f : base.getFields()) {
                    int get = base.get(f.getType());
                    DateTimeFieldType type = f.getType();

                    if (type == DateTimeFieldType.year()) {
                        func += "*(tm_year(" + millTimeSecondsExpr + ")==" + get + ")";
                    } else if (type == DateTimeFieldType.yearOfCentury()) {
                        // problems using gnuplot mod for tm_year(..).
                        // Assume >2000 instead.
                        get = get + 2000;
                        func += "*(tm_year(" + millTimeSecondsExpr + ")==" + get + ")";
                    } else if (type == DateTimeFieldType.monthOfYear()) {
                        func += "*(tm_mon(" + millTimeSecondsExpr + ")==" + get + ")";
                    } else if (type == DateTimeFieldType.dayOfMonth()) {
                        func += "*(tm_mday(" + millTimeSecondsExpr + ")==" + get + ")";
                    } else if (type == DateTimeFieldType.hourOfDay()) {
                        // ignore.
                    } else if (type == DateTimeFieldType.minuteOfHour()) {
                        // ignore.
                    } else {
                        throw new UnsupportedOperationException("field type not supported.");
                    }
                }

                return func;
            }
        };
        
        return iTimeDensityFunction;
    }
}
