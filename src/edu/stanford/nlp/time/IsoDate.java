package edu.stanford.nlp.time;


import edu.stanford.nlp.time.distributed.CanExpressTimeAsFunction;
import edu.stanford.nlp.time.distributed.ITimeDensityFunction;
import org.joda.time.DateTime;
import org.joda.time.DateTimeFieldType;

/*
 * This is mostly a helper class but it is also the most standard type of date that people are
 * used to working with.
 */
public class IsoDate extends PartialTime implements CanExpressTimeAsFunction {
    // TODO: We are also using this class for partial dates
    //       with just decade or century, but it is difficult
    //       to get that information out without using the underlying joda classes
    /** Era: BC is era 0, AD is era 1, Unknown is -1  */
    public int era = SUTime.ERA_UNKNOWN;
    /** Year of Era */
    public int year = -1;
    /** Month of Year */
    public int month = -1;
    /** Day of Month */
    public int day = -1;
    private ITimeDensityFunction _gnuFunc;

    public IsoDate(int y, int m, int d) {
        this(null, y, m, d);
    }

    public IsoDate(SUTime.StandardTemporalType temporalType, int y, int m, int d) {
        this.year = y;
        this.month = m;
        this.day = d;
        initBase();
        this.standardTemporalType = temporalType;
    }

    // TODO: Added for grammar parsing
    // TODO: Added for grammar parsing
    public IsoDate(Number y, Number m, Number d) {
        this(y, m, d, null, null);
    }
    
    public IsoDate(Number y, Number m, Number d, ITimeDensityFunction func) {
        this(y, m, d, null, null);
        _gnuFunc = func;
    }

    public IsoDate(Number y, Number m, Number d, Number era, Boolean yearEraAdjustNeeded) {
        this.year = (y != null) ? y.intValue() : -1;
        
        // Two-Digit Years.
        if (this.year < 99 && this.year > 0) {
            this.year = 2000 + this.year;
        }
        
        this.month = (m != null) ? m.intValue() : -1;
        this.day = (d != null) ? d.intValue() : -1;
        this.era = (era != null) ? era.intValue() : SUTime.ERA_UNKNOWN;
        if (yearEraAdjustNeeded != null && yearEraAdjustNeeded && this.era == SUTime.ERA_BC) {
            if (this.year > 0) {
                this.year--;
            }
        }
        initBase();
    }

    // Assumes y, m, d are ISO formatted
    // Assumes y, m, d are ISO formatted
    public IsoDate(String y, String m, String d) {
        if (y != null && !SUTime.PAD_FIELD_UNKNOWN4.equals(y)) {
            if (!y.matches("[+-]?[0-9X]{4}")) {
                throw new IllegalArgumentException("Year not in ISO format " + y);
            }
            if (y.startsWith("-")) {
                y = y.substring(1);
                era = SUTime.ERA_BC; // BC
                // BC
            } else if (y.startsWith("+")) {
                y = y.substring(1);
                era = SUTime.ERA_AD; // AD
                // AD
            }
            if (y.contains(SUTime.PAD_FIELD_UNKNOWN)) {
                if (y.matches("XX[0-9][0-9]")) {
                    int yearX = Integer.parseInt(y.substring(2, 4));
                    year = (yearX > 50 ? 1900 : 2000) + yearX;
                } else {
                    System.err.println("Could not parse year: " + y.toString());
                }
            } else {
                year = Integer.parseInt(y);
            }
        } else {
            y = SUTime.PAD_FIELD_UNKNOWN4;
        }
        if (m != null && !SUTime.PAD_FIELD_UNKNOWN2.equals(m)) {
            month = Integer.parseInt(m);
        } else {
            m = SUTime.PAD_FIELD_UNKNOWN2;
        }
        if (d != null && !SUTime.PAD_FIELD_UNKNOWN2.equals(d)) {
            day = Integer.parseInt(d);
        } else {
            d = SUTime.PAD_FIELD_UNKNOWN2;
        }
        initBase();
        if (year < 0 && !SUTime.PAD_FIELD_UNKNOWN4.equals(y)) {
            if (Character.isDigit(y.charAt(0)) && Character.isDigit(y.charAt(1))) {
                int century = Integer.parseInt(y.substring(0, 2));
                base = JodaTimeUtils.setField(base, DateTimeFieldType.centuryOfEra(), century);
            }
            if (Character.isDigit(y.charAt(2)) && Character.isDigit(y.charAt(3))) {
                int cy = Integer.parseInt(y.substring(2, 4));
                base = JodaTimeUtils.setField(base, DateTimeFieldType.yearOfCentury(), cy);
            } else if (Character.isDigit(y.charAt(2))) {
                int decade = Integer.parseInt(y.substring(2, 3));
                base = JodaTimeUtils.setField(base, JodaTimeUtils.DecadeOfCentury, decade);
            }
        }
    }

    private void initBase() {
        if (era >= 0) {
            base = JodaTimeUtils.setField(base, DateTimeFieldType.era(), era);
        }
        if (year >= 0) {
            base = JodaTimeUtils.setField(base, DateTimeFieldType.year(), year);
        }
        if (month >= 0) {
            base = JodaTimeUtils.setField(base, DateTimeFieldType.monthOfYear(), month);
        }
        if (day >= 0) {
            base = JodaTimeUtils.setField(base, DateTimeFieldType.dayOfMonth(), day);
        }
    }

    public String toString() {
        // TODO: is the right way to print this object?
        StringBuilder os = new StringBuilder();
        if (era == SUTime.ERA_BC) {
            os.append("-");
        } else if (era == SUTime.ERA_AD) {
            os.append("+");
        }
        if (year >= 0) {
            os.append(year);
        } else {
            os.append("XXXX");
        }
        os.append("-");
        if (month >= 0) {
            os.append(month);
        } else {
            os.append("XX");
        }
        os.append("-");
        if (day >= 0) {
            os.append(day);
        } else {
            os.append("XX");
        }
        return os.toString();
    }

    public int getYear() {
        return year;
    }

    // TODO: Should we allow setters??? Most time classes are immutable
    public void setYear(int y) {
        this.year = y;
        initBase();
    }

    public int getMonth() {
        return month;
    }

    // TODO: Should we allow setters??? Most time classes are immutable
    public void setMonth(int m) {
        this.month = m;
        initBase();
    }

    public int getDay() {
        return day;
    }

    // TODO: Should we allow setters??? Most time classes are immutable
    public void setDay(int d) {
        this.day = d;
        initBase();
    }

    // TODO: Should we allow setters??? Most time classes are immutable
    public void setDate(int y, int m, int d) {
        this.year = y;
        this.month = m;
        this.day = d;
        initBase();
    }
    private static final long serialVersionUID = 1;



    public void SetFunction(ITimeDensityFunction func) {
        _gnuFunc = func;
    }

    public ITimeDensityFunction GettimeDensityFunction() {

        if (this._gnuFunc != null) {
            return _gnuFunc;
        }
        
        // Otherwise, fall back to indicator functions.
        ITimeDensityFunction iTimeDensityFunction = new ITimeDensityFunction() {

            public double GetDensity(DateTime time) {
                if ((day > 0) && (time.getDayOfMonth() != day)) {
                    return 0;
                }
                if ((month > 0) && (time.getMonthOfYear() != month)) {
                    return 0;
                }
                if ((year > 0) && (time.getYear() != year)) {
                    return 0;
                }
                return 1;
            }

            @Override
            public String GetGNUPlot(String millTimeSecondsExpr) {

                if (_gnuFunc != null) {
                    return _gnuFunc.GetGNUPlot(millTimeSecondsExpr);
                }

                String expr = "1";

                if (day > 0) {
                    expr += "*(tm_mday(" + millTimeSecondsExpr + ")==" + Integer.toString(day) + ")";
                }
                if (month > 0) {
                    expr += "*(tm_mon(" + millTimeSecondsExpr + ")==" + Integer.toString(month) + ")";
                }
                if (year > 0) {
                    expr += "*(tm_year(" + millTimeSecondsExpr + ")==" + Integer.toString(year) + ")";
                }
                return expr;
            }
        };
        
        return iTimeDensityFunction;
    }

}
