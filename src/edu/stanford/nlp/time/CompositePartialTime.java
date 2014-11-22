package edu.stanford.nlp.time;


import edu.stanford.nlp.time.distributed.CanExpressTimeAsFunction;
import edu.stanford.nlp.time.distributed.TimeDensityFunction;
import edu.stanford.nlp.time.distributed.IntersectTimeExpression;
import java.util.ArrayList;
import java.util.List;
import org.joda.time.DateTimeFieldType;
import org.joda.time.DurationFieldType;
import org.joda.time.Instant;
import org.joda.time.Partial;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;

// Composite time - like PartialTime but with more, approximate fields
public class CompositePartialTime extends PartialTime {
    // Summer weekend morning in June
    Time tod; // Time of day
    // Time of day
    Time dow; // Day of week
    // Day of week
    Time poy; // Part of year
    // Part of year

    // Duration duration; // Underspecified time (like day in June)
    // Duration duration; // Underspecified time (like day in June)
    public CompositePartialTime(PartialTime t, Time poy, Time dow, Time tod) {
        super(t);
        this.poy = poy;
        this.dow = dow;
        this.tod = tod;
        
        List<CanExpressTimeAsFunction> pdfs =new ArrayList<CanExpressTimeAsFunction>();
        
        if (t.getTimeDensityFunction() != null) {
            pdfs.add(t);
        }
        addToPdfs(poy, pdfs);
        addToPdfs(dow, pdfs);
        addToPdfs(tod, pdfs);
        
        this.setTimeDensityFunction(new IntersectTimeExpression(pdfs));
    }

    @Override
    public TimeDensityFunction getTimeDensityFunction() {
        return super.getTimeDensityFunction(); //To change body of generated methods, choose Tools | Templates.
    }

    
    
    
    private void addToPdfs(Time poy, List<CanExpressTimeAsFunction> pdfs) {
        if (poy != null && poy instanceof CanExpressTimeAsFunction) {
            TimeDensityFunction func = ((CanExpressTimeAsFunction)poy).getTimeDensityFunction();
            if (func != null) pdfs.add((CanExpressTimeAsFunction)poy);
        }
    }
    

    public CompositePartialTime(PartialTime t, Partial p, Time poy, Time dow, Time tod) {
        this(t, poy, dow, tod);
        this.base = p;
    }

    public Instant getJodaTimeInstant() {
        Partial p = base;
        if (tod != null) {
            Partial p2 = tod.getJodaTimePartial();
            if (p2 != null && JodaTimeUtils.isCompatible(p, p2)) {
                p = JodaTimeUtils.combine(p, p2);
            }
        }
        if (dow != null) {
            Partial p2 = dow.getJodaTimePartial();
            if (p2 != null && JodaTimeUtils.isCompatible(p, p2)) {
                p = JodaTimeUtils.combine(p, p2);
            }
        }
        if (poy != null) {
            Partial p2 = poy.getJodaTimePartial();
            if (p2 != null && JodaTimeUtils.isCompatible(p, p2)) {
                p = JodaTimeUtils.combine(p, p2);
            }
        }
        return JodaTimeUtils.getInstant(p);
    }

    public Duration getDuration() {
        /*      TimeLabel tl = getTimeLabel();
        if (tl != null) {
        return tl.getDuration();
        } */
        SUTime.StandardTemporalType tlt = getStandardTemporalType();
        if (tlt != null) {
            return tlt.getDuration();
        }
        Duration bd = (base != null) ? Duration.getDuration(JodaTimeUtils.getJodaTimePeriod(base)) : null;
        if (tod != null) {
            Duration d = tod.getDuration();
            return (bd.compareTo(d) < 0) ? bd : d;
        }
        if (dow != null) {
            Duration d = dow.getDuration();
            return (bd.compareTo(d) < 0) ? bd : d;
        }
        if (poy != null) {
            Duration d = poy.getDuration();
            return (bd.compareTo(d) < 0) ? bd : d;
        }
        return bd;
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
        Duration bd = null;
        if (base != null) {
            DateTimeFieldType mostGeneral = JodaTimeUtils.getMostGeneral(base);
            DurationFieldType df = mostGeneral.getRangeDurationType();
            if (df == null) {
                df = mostGeneral.getDurationType();
            }
            if (df != null) {
                bd = new DurationWithFields(new Period().withField(df, 1));
            }
        }
        if (poy != null) {
            Duration d = poy.getPeriod();
            return (bd.compareTo(d) > 0) ? bd : d;
        }
        if (dow != null) {
            Duration d = dow.getPeriod();
            return (bd.compareTo(d) > 0) ? bd : d;
        }
        if (tod != null) {
            Duration d = tod.getPeriod();
            return (bd.compareTo(d) > 0) ? bd : d;
        }
        return bd;
    }

    public Range getRange(int flags, Duration granularity) {
        Duration d = getDuration();
        if (tod != null) {
            Range r = tod.getRange(flags, granularity);
            if (r != null) {
                CompositePartialTime cpt = new CompositePartialTime(this, poy, dow, null);
                Time t1 = cpt.intersect(r.beginTime());
                Time t2 = cpt.intersect(r.endTime());
                return new Range(t1, t2, d);
            } else {
                return super.getRange(flags, granularity);
            }
        }
        if (dow != null) {
            Range r = dow.getRange(flags, granularity);
            if (r != null) {
                CompositePartialTime cpt = new CompositePartialTime(this, poy, dow, null);
                Time t1 = cpt.intersect(r.beginTime());
                if (t1 instanceof PartialTime) {
                    ((PartialTime) t1).withStandardFields();
                }
                Time t2 = cpt.intersect(r.endTime());
                if (t2 instanceof PartialTime) {
                    ((PartialTime) t2).withStandardFields();
                }
                return new Range(t1, t2, d);
            } else {
                return super.getRange(flags, granularity);
            }
        }
        if (poy != null) {
            Range r = poy.getRange(flags, granularity);
            if (r != null) {
                CompositePartialTime cpt = new CompositePartialTime(this, poy, null, null);
                Time t1 = cpt.intersect(r.beginTime());
                Time t2 = cpt.intersect(r.endTime());
                return new Range(t1, t2, d);
            } else {
                return super.getRange(flags, granularity);
            }
        }
        return super.getRange(flags, granularity);
    }

    public Time intersect(Time t) {
        if (t == null || t == SUTime.TIME_UNKNOWN) {
            return this;
        }
        if (base == null) {
            return t;
        }
        if (t instanceof PartialTime) {
            if (!isCompatible((PartialTime) t)) {
                return null;
            }
            Partial p = JodaTimeUtils.combine(this.base, ((PartialTime) t).base);
            if (t instanceof CompositePartialTime) {
                CompositePartialTime cpt = (CompositePartialTime) t;
                Time ntod = Time.intersect(tod, cpt.tod);
                Time ndow = Time.intersect(dow, cpt.dow);
                Time npoy = Time.intersect(poy, cpt.poy);
                if (ntod == null && (tod != null || cpt.tod != null)) {
                    return null;
                }
                if (ndow == null && (dow != null || cpt.dow != null)) {
                    return null;
                }
                if (npoy == null && (poy != null || cpt.poy != null)) {
                    return null;
                }
                return new CompositePartialTime(this, p, npoy, ndow, ntod);
            } else {
                return new CompositePartialTime(this, p, poy, dow, tod);
            }
        } else {
            return super.intersect(t);
        }
    }

    protected PartialTime addSupported(Period p, int scalar) {
        return new CompositePartialTime(this, base.withPeriodAdded(p, 1), poy, dow, tod);
    }

    protected PartialTime addUnsupported(Period p, int scalar) {
        return new CompositePartialTime(this, JodaTimeUtils.addForce(base, p, scalar), poy, dow, tod);
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
        DateTimeFieldType mgf = null;
        if (poy != null) {
            mgf = JodaTimeUtils.QuarterOfYear;
        } else if (dow != null) {
            mgf = DateTimeFieldType.dayOfWeek();
        } else if (tod != null) {
            mgf = DateTimeFieldType.halfdayOfDay();
        }
        Partial p = (base != null) ? JodaTimeUtils.combineMoreGeneralFields(base, partialRef, mgf) : partialRef;
        if (p.isSupported(DateTimeFieldType.dayOfWeek())) {
            p = JodaTimeUtils.resolveDowToDay(p, partialRef);
        } else if (dow != null) {
            p = JodaTimeUtils.resolveWeek(p, partialRef);
        }
        if (p == base) {
            return this;
        } else {
            return new CompositePartialTime(this, p, poy, dow, tod);
        }
    }

    public DateTimeFormatter getFormatter(int flags) {
        DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder();
        boolean hasDate = appendDateFormats(builder, flags);
        if (poy != null) {
            if (!JodaTimeUtils.hasField(base, DateTimeFieldType.monthOfYear())) {
                // Assume poy is compatible with whatever was built and
                // poy.toISOString() does the correct thing
                builder.appendLiteral("-");
                builder.appendLiteral(poy.toISOString());
                hasDate = true;
            }
        }
        if (dow != null) {
            if (!JodaTimeUtils.hasField(base, DateTimeFieldType.monthOfYear()) && !JodaTimeUtils.hasField(base, DateTimeFieldType.dayOfWeek())) {
                builder.appendLiteral("-");
                builder.appendLiteral(dow.toISOString());
                hasDate = true;
            }
        }
        if (hasTime()) {
            if (!hasDate) {
                builder.clear();
            }
            appendTimeFormats(builder, flags);
        } else if (tod != null) {
            if (!hasDate) {
                builder.clear();
            }
            // Assume tod is compatible with whatever was built and
            // tod.toISOString() does the correct thing
            builder.appendLiteral("T");
            builder.appendLiteral(tod.toISOString());
        }
        return builder.toFormatter();
    }

    public SUTime.TimexType getTimexType() {
        return (hasTime() || tod != null) ? SUTime.TimexType.TIME : SUTime.TimexType.DATE;
    }
    private static final long serialVersionUID = 1;



}
