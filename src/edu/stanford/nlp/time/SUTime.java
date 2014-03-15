package edu.stanford.nlp.time;

import edu.stanford.nlp.ling.tokensregex.types.Expressions;
import edu.stanford.nlp.time.distributed.AnnualNormalDistribution;
import edu.stanford.nlp.time.distributed.AnnualUniformDistribution;
import edu.stanford.nlp.time.distributed.CanExpressTimeAsFunction;
import edu.stanford.nlp.time.distributed.ITimeDensityFunction;
import edu.stanford.nlp.time.distributed.IntersectTimeExpression;
import edu.stanford.nlp.util.*;

import edu.stanford.nlp.util.Interval;
import org.joda.time.*;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;

import java.io.Serializable;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.naming.OperationNotSupportedException;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import edu.stanford.nlp.time.distributed.SumTimeExpression;

/**
 * SUTime is a collection of data structures to represent various temporal
 * concepts and operations between them.
 *
 * Different types of time expressions <ul> <li>Time - A time point on a time
 * scale In most cases, we only know partial information (with a certain
 * granularity) about a point in time (8:00pm)</li> <li>Duration - A length of
 * time (3 days) </li> <li>Interval - A range of time with start and end
 * points</li> <li>Set - A set of time: Can be periodic (Friday every week) or
 * union (Thursday or Friday)</li> </ul>
 *
 * <p> Use {@link TimeAnnotator} to annotate.
 *
 * @author Angel Chang
 */
public class SUTime {

    // TODO:
    // 1. Decrease dependency on JodaTime...
    // 2. Number parsing
    // - Improve Number detection/normalization
    // - Handle four-years, one thousand two hundred and sixty years
    // - Currently custom word to number combo - integrate with Number classifier,
    // QuantifiableEntityNormalizer
    // - Stop repeated conversions of word to numbers
    // 3. Durations
    // - Underspecified durations
    // 4. Date Time
    // - Patterns
    // -- 1st/last week(end) of blah blah
    // -- Don't treat all 3 to 5 as times
    // - Holidays
    // - Too many classes - reduce number of classes
    // 5. Nest time expressions
    // - Before annotating: Can remove nested time expressions
    // - After annotating: types to combine time expressions
    // 6. Set of times (Timex3 standard is weird, timex2 makes more sense?)
    // - freq, quant
    // 7. Ground with respect to reference time - figure out what is reference
    // time to use for what
    // - news... things happen in the past, so favor resolving to past?
    // - Use heuristics from GUTime to figure out direction to resolve to
    // - tids for anchortimes...., valueFromFunctions for resolved relative times
    // (option to keep some nested times)?
    // 8. Composite time patterns
    // - Composite time operators
    // 9. Ranges
    // - comparing times (before, after, ...
    // - intersect, mid, resolving
    // - specify clear start/end for range (sonal)
    // 10. Clean up formatting
    // ISO/Timex3/Custom
    // 12. Keep modifiers
    // 13. Handle mid- (token not separated)
    // 14. future, plurals
    // 15. Resolve to future.... with year specified....
    // 16. Check recursive calls
    // 17. Add TimeWithFields (that doesn't use jodatime and is only field based?
    private SUTime() {
    }

    public static enum TimexType {

        DATE, TIME, DURATION, SET
    }

    public static enum TimexMod {

        BEFORE("<"), AFTER(">"), ON_OR_BEFORE("<="), ON_OR_AFTER("<="), LESS_THAN("<"), MORE_THAN(">"),
        EQUAL_OR_LESS("<="), EQUAL_OR_MORE(">="), START, MID, END, APPROX("~"), EARLY /* GUTIME */, LATE; /* GUTIME */

        String symbol;

        TimexMod() {
        }

        TimexMod(String symbol) {
            this.symbol = symbol;
        }

        public String getSymbol() {
            return symbol;
        }
    }

    public static enum TimexDocFunc {

        CREATION_TIME, EXPIRATION_TIME, MODIFICATION_TIME, PUBLICATION_TIME, RELEASE_TIME, RECEPTION_TIME, NONE
    }

    public static enum TimexAttr {

        type, value, tid, beginPoint, endPoint, quant, freq, mod, anchorTimeID, comment, valueFromFunction, temporalFunction, functionInDocument
    }
    public static final String PAD_FIELD_UNKNOWN = "X";
    public static final String PAD_FIELD_UNKNOWN2 = "XX";
    public static final String PAD_FIELD_UNKNOWN4 = "XXXX";
    public static final int RESOLVE_NOW = 0x01;
    public static final int RESOLVE_TO_THIS = 0x20;
    public static final int RESOLVE_TO_PAST = 0x40; // Resolve to a past time
    public static final int RESOLVE_TO_FUTURE = 0x80; // Resolve to a future time
    public static final int RESOLVE_TO_CLOSEST = 0x200; // Resolve to closest time
    public static final int DUR_RESOLVE_TO_AS_REF = 0x1000;
    public static final int DUR_RESOLVE_FROM_AS_REF = 0x2000;
    public static final int RANGE_RESOLVE_TIME_REF = 0x100000;
    public static final int RANGE_OFFSET_BEGIN = 0x0001;
    public static final int RANGE_OFFSET_END = 0x0002;
    public static final int RANGE_EXPAND_FIX_BEGIN = 0x0010;
    public static final int RANGE_EXPAND_FIX_END = 0x0020;
    public static final int RANGE_FLAGS_PAD_MASK = 0x000f; // Pad type
    public static final int RANGE_FLAGS_PAD_NONE = 0x0001; // Simple range
    // (without padding)
    public static final int RANGE_FLAGS_PAD_AUTO = 0x0002; // Automatic range
    // (whatever padding we
    // think is most
    // appropriate,
    // default)
    public static final int RANGE_FLAGS_PAD_FINEST = 0x0003; // Pad to most
    // specific (whatever
    // that is)
    public static final int RANGE_FLAGS_PAD_SPECIFIED = 0x0004; // Specified
    // granularity
    public static final int FORMAT_ISO = 0x01;
    public static final int FORMAT_TIMEX3_VALUE = 0x02;
    public static final int FORMAT_FULL = 0x04;
    public static final int FORMAT_PAD_UNKNOWN = 0x1000;
    protected static final int timexVersion = 3;

    public static <T extends Temporal> T createTemporal(StandardTemporalType timeType, T temporal) {
        temporal.standardTemporalType = timeType;
        return temporal;
    }

    public static <T extends Temporal> T createTemporal(StandardTemporalType timeType, String label, T temporal) {
        temporal.standardTemporalType = timeType;
        temporal.timeLabel = label;
        return temporal;
    }

    public static <T extends Temporal> T createTemporal(StandardTemporalType timeType, String label, String mod, T temporal) {
        temporal.standardTemporalType = timeType;
        temporal.timeLabel = label;
        temporal.mod = mod;
        return temporal;
    }
    // Basic time units (durations)
    public static final Duration YEAR = new DurationWithFields(Period.years(1)) {
        public DateTimeFieldType[] getDateTimeFields() {
            return new DateTimeFieldType[]{DateTimeFieldType.year(), DateTimeFieldType.yearOfCentury(), DateTimeFieldType.yearOfEra()};
        }
        private static final long serialVersionUID = 1;
    };
    public static final Duration DAY = new DurationWithFields(Period.days(1)) {
        public DateTimeFieldType[] getDateTimeFields() {
            return new DateTimeFieldType[]{DateTimeFieldType.dayOfMonth(), DateTimeFieldType.dayOfWeek(), DateTimeFieldType.dayOfYear()};
        }
        private static final long serialVersionUID = 1;
    };
    public static final Duration WEEK = new DurationWithFields(Period.weeks(1)) {
        public DateTimeFieldType[] getDateTimeFields() {
            return new DateTimeFieldType[]{DateTimeFieldType.weekOfWeekyear()};
        }
        private static final long serialVersionUID = 1;
    };
    public static final Duration FORTNIGHT = new DurationWithFields(Period.weeks(2));
    public static final Duration MONTH = new DurationWithFields(Period.months(1)) {
        public DateTimeFieldType[] getDateTimeFields() {
            return new DateTimeFieldType[]{DateTimeFieldType.monthOfYear()};
        }
        private static final long serialVersionUID = 1;
    };
    // public static final Duration QUARTER = new DurationWithFields(new
    // Period(JodaTimeUtils.Quarters)) {
    public static final Duration QUARTER = new DurationWithFields(Period.months(3)) {
        public DateTimeFieldType[] getDateTimeFields() {
            return new DateTimeFieldType[]{JodaTimeUtils.QuarterOfYear};
        }
        private static final long serialVersionUID = 1;
    };
    // public static final Duration QUARTER = new
    // InexactDuration(Period.months(3));
    public static final Duration MILLIS = new DurationWithFields(Period.millis(1)) {
        public DateTimeFieldType[] getDateTimeFields() {
            return new DateTimeFieldType[]{DateTimeFieldType.millisOfSecond(), DateTimeFieldType.millisOfDay()};
        }
        private static final long serialVersionUID = 1;
    };
    public static final Duration SECOND = new DurationWithFields(Period.seconds(1)) {
        public DateTimeFieldType[] getDateTimeFields() {
            return new DateTimeFieldType[]{DateTimeFieldType.secondOfMinute(), DateTimeFieldType.secondOfDay()};
        }
        private static final long serialVersionUID = 1;
    };
    public static final Duration MINUTE = new DurationWithFields(Period.minutes(1)) {
        public DateTimeFieldType[] getDateTimeFields() {
            return new DateTimeFieldType[]{DateTimeFieldType.minuteOfHour(), DateTimeFieldType.minuteOfDay()};
        }
        private static final long serialVersionUID = 1;
    };
    public static final Duration HOUR = new DurationWithFields(Period.hours(1)) {
        public DateTimeFieldType[] getDateTimeFields() {
            return new DateTimeFieldType[]{DateTimeFieldType.hourOfDay(), DateTimeFieldType.hourOfHalfday()};
        }
        private static final long serialVersionUID = 1;
    };
    public static final Duration HALFHOUR = new DurationWithFields(Period.minutes(30));
    public static final Duration QUARTERHOUR = new DurationWithFields(Period.minutes(15));
    public static final Duration DECADE = new DurationWithFields(Period.years(10)) {
        public DateTimeFieldType[] getDateTimeFields() {
            return new DateTimeFieldType[]{JodaTimeUtils.DecadeOfCentury};
        }
        private static final long serialVersionUID = 1;
    };
    public static final Duration CENTURY = new DurationWithFields(Period.years(100)) {
        public DateTimeFieldType[] getDateTimeFields() {
            return new DateTimeFieldType[]{DateTimeFieldType.centuryOfEra()};
        }
        private static final long serialVersionUID = 1;
    };
    public static final Duration MILLENNIUM = new DurationWithFields(Period.years(1000));
    public static final Time TIME_REF = new RefTime("REF") {
        private static final long serialVersionUID = 1;
    };
    public static final Time TIME_REF_UNKNOWN = new RefTime("UNKNOWN");
    public static final Time TIME_UNKNOWN = new SimpleTime("UNKNOWN");
    public static final Time TIME_NONE = null; // No time
    public static final Time TIME_NONE_OK = new SimpleTime("NOTIME");
    // The special time of now
    public static final Time TIME_NOW = new RefTime(StandardTemporalType.REFTIME, "PRESENT_REF", "NOW");
    public static final Time TIME_PRESENT = createTemporal(StandardTemporalType.REFDATE, "PRESENT_REF", new InexactTime(new Range(TIME_NOW, TIME_NOW)));
    public static final Time TIME_PAST = createTemporal(StandardTemporalType.REFDATE, "PAST_REF", new InexactTime(new Range(TIME_UNKNOWN, TIME_NOW)));
    public static final Time TIME_FUTURE = createTemporal(StandardTemporalType.REFDATE, "FUTURE_REF", new InexactTime(new Range(TIME_NOW, TIME_UNKNOWN)));
    public static final Duration DURATION_UNKNOWN = new DurationWithFields();
    public static final Duration DURATION_NONE = new DurationWithFields(Period.ZERO);
    // Basic dates/times
    // Day of week
    // Use constructors rather than calls to
    // StandardTemporalType.createTemporal because sometimes the class
    // loader seems to load objects in an incorrect order, resulting in
    // an exception.  This is especially evident when deserializing
    public static final Time MONDAY = new PartialTime(StandardTemporalType.DAY_OF_WEEK, new Partial(DateTimeFieldType.dayOfWeek(), 1));
    public static final Time TUESDAY = new PartialTime(StandardTemporalType.DAY_OF_WEEK, new Partial(DateTimeFieldType.dayOfWeek(), 2));
    public static final Time WEDNESDAY = new PartialTime(StandardTemporalType.DAY_OF_WEEK, new Partial(DateTimeFieldType.dayOfWeek(), 3));
    public static final Time THURSDAY = new PartialTime(StandardTemporalType.DAY_OF_WEEK, new Partial(DateTimeFieldType.dayOfWeek(), 4));
    public static final Time FRIDAY = new PartialTime(StandardTemporalType.DAY_OF_WEEK, new Partial(DateTimeFieldType.dayOfWeek(), 5));
    public static final Time SATURDAY = new PartialTime(StandardTemporalType.DAY_OF_WEEK, new Partial(DateTimeFieldType.dayOfWeek(), 6));
    public static final Time SUNDAY = new PartialTime(StandardTemporalType.DAY_OF_WEEK, new Partial(DateTimeFieldType.dayOfWeek(), 7));
    public static final Time WEEKDAY = createTemporal(StandardTemporalType.DAYS_OF_WEEK, "WD",
            new InexactTime(null, SUTime.DAY, new Range(SUTime.MONDAY, SUTime.FRIDAY)) {
                public Duration getDuration() {
                    return SUTime.DAY;
                }
                private static final long serialVersionUID = 1;
            });
    public static final Time WEEKEND = createTemporal(StandardTemporalType.DAYS_OF_WEEK, "WE",
            new TimeWithRange(new Range(SUTime.SATURDAY, SUTime.SUNDAY, SUTime.DAY.multiplyBy(2))));
    // Months
    // Use constructors rather than calls to
    // StandardTemporalType.createTemporal because sometimes the class
    // loader seems to load objects in an incorrect order, resulting in
    // an exception.  This is especially evident when deserializing
    public static final Time JANUARY = new IsoDate(StandardTemporalType.MONTH_OF_YEAR, -1, 1, -1);
    public static final Time FEBRUARY = new IsoDate(StandardTemporalType.MONTH_OF_YEAR, -1, 2, -1);
    public static final Time MARCH = new IsoDate(StandardTemporalType.MONTH_OF_YEAR, -1, 3, -1);
    public static final Time APRIL = new IsoDate(StandardTemporalType.MONTH_OF_YEAR, -1, 4, -1);
    public static final Time MAY = new IsoDate(StandardTemporalType.MONTH_OF_YEAR, -1, 5, -1);
    public static final Time JUNE = new IsoDate(StandardTemporalType.MONTH_OF_YEAR, -1, 6, -1);
    public static final Time JULY = new IsoDate(StandardTemporalType.MONTH_OF_YEAR, -1, 7, -1);
    public static final Time AUGUST = new IsoDate(StandardTemporalType.MONTH_OF_YEAR, -1, 8, -1);
    public static final Time SEPTEMBER = new IsoDate(StandardTemporalType.MONTH_OF_YEAR, -1, 9, -1);
    public static final Time OCTOBER = new IsoDate(StandardTemporalType.MONTH_OF_YEAR, -1, 10, -1);
    public static final Time NOVEMBER = new IsoDate(StandardTemporalType.MONTH_OF_YEAR, -1, 11, -1);
    public static final Time DECEMBER = new IsoDate(StandardTemporalType.MONTH_OF_YEAR, -1, 12, -1);
    // Dates are rough with respect to northern hemisphere (actual
    // solstice/equinox days depend on the year)
    public static final Time SPRING_EQUINOX = createTemporal(StandardTemporalType.DAY_OF_YEAR, "SP", new InexactTime(new Range(new IsoDate(-1, 3, 20), new IsoDate(-1, 3, 21))));
    public static final Time SUMMER_SOLSTICE = createTemporal(StandardTemporalType.DAY_OF_YEAR, "SU", new InexactTime(new Range(new IsoDate(-1, 6, 20), new IsoDate(-1, 6, 21))));
    public static final Time WINTER_SOLSTICE = createTemporal(StandardTemporalType.DAY_OF_YEAR, "WI", new InexactTime(new Range(new IsoDate(-1, 12, 21), new IsoDate(-1, 12, 22))));
    public static final Time FALL_EQUINOX = createTemporal(StandardTemporalType.DAY_OF_YEAR, "FA", new InexactTime(new Range(new IsoDate(-1, 9, 22), new IsoDate(-1, 9, 23))));

  
    public static final ITimeDensityFunction WINTER_DIST = new SumTimeExpression(
	new AnnualNormalDistribution(0.332196332118026,380435856.707,1204042.11586607),
	new AnnualNormalDistribution(0.312814001825274,408869365.796,973033.867210455),
	new AnnualNormalDistribution(0.193038509037954,382603192.964,983967.159767546),
	new AnnualNormalDistribution(0.0641220995690921,406160504.975,1204457.26860019),
	new AnnualNormalDistribution(0.0301735966610396,385855838.534,1029512.19745316),
	new AnnualUniformDistribution(0.0227302824848729),
	new AnnualNormalDistribution(0.0123090667330343,395531634.934,916827.335427853),
	new AnnualNormalDistribution(0.0120672895612644,402849211.26,1274023.0400464),
	new AnnualNormalDistribution(0.00832576518606784,393458187.757,1234243.63056689),
	new AnnualNormalDistribution(0.00672095981173758,399439509.719,1116218.20978957),
	new AnnualNormalDistribution(0.0055020970116363,389454260.529,1333408.97911959)
    );

    public static final ITimeDensityFunction SPRING_DIST = new SumTimeExpression(
            new AnnualNormalDistribution(0.445594445141809,386379881.414,991185.498830499),
            new AnnualNormalDistribution(0.19025635242184,389245545.056,1054577.27732744),
            new AnnualNormalDistribution(0.175453906063836,384232020.181,1061101.81507672),
            new AnnualNormalDistribution(0.0428926940032708,392211311.582,1077406.60190393),
            new AnnualNormalDistribution(0.0360612062781602,380724265.461,1078338.81630031),
            new AnnualNormalDistribution(0.0247791089817192,399797557.76,1126012.75149293),
            new AnnualUniformDistribution(0.0237395344272946),
            new AnnualNormalDistribution(0.0228545391156814,401908218.951,1224359.07320738),
            new AnnualNormalDistribution(0.0156215705145877,405614193.965,1178811.14314599),
            new AnnualNormalDistribution(0.0127034975729927,396201016.146,1198461.43779122),
            new AnnualNormalDistribution(0.0100431454788087,408554018.951,1467723.98999854)
    );

    public static final ITimeDensityFunction SUMMER_DIST = new SumTimeExpression(
            new AnnualNormalDistribution(0.293868748378889,396083115.724,1116628.25886232),
            new AnnualNormalDistribution(0.222956545854207,393264030.565,1078080.63909611),
            new AnnualNormalDistribution(0.214902872150514,398865458.907,1003586.73967376),
            new AnnualNormalDistribution(0.0690232340001536,390438381.21,1116382.71592155),
            new AnnualNormalDistribution(0.0470826760884972,401758633.467,1176809.23694348),
            new AnnualNormalDistribution(0.0344610163753501,380411765.066,984242.014310694),
            new AnnualNormalDistribution(0.0329109477404667,386793588.584,1122219.18042397),
            new AnnualNormalDistribution(0.0266154531401581,383403169.815,1186407.73752488),
            new AnnualUniformDistribution(0.0242948040188496),
            new AnnualNormalDistribution(0.0188585288044682,408814847.28,1110599.4137298),
            new AnnualNormalDistribution(0.0150251734484461,405421429.033,1279296.83868521)
    );

    public static final ITimeDensityFunction AUTUMN_DIST = new SumTimeExpression(
            new AnnualNormalDistribution(0.438693232224144,405055864.72,1029853.22456299),
            new AnnualNormalDistribution(0.398134343882415,403049902.031,955829.313707557),
            new AnnualNormalDistribution(0.0499697872804527,407492850.286,1008379.93605087),
            new AnnualNormalDistribution(0.0403661010267748,400377442.65,902986.283917415),
            new AnnualUniformDistribution(0.0216324261508443),
            new AnnualNormalDistribution(0.0168121058948726,389670711.21,1061801.67890577),
            new AnnualNormalDistribution(0.0117498607445376,386840040.415,1083522.80107301),
            new AnnualNormalDistribution(0.008654176839992,392102398.075,831428.445296124),
            new AnnualNormalDistribution(0.00591311929296498,380385559.062,1239565.99499243),
            new AnnualNormalDistribution(0.00575079251490961,383508833.384,1306464.87001057),
            new AnnualNormalDistribution(0.00232405414809239,397072500.45,1189331.47192302)
    );
  
    // Dates for seasons are rough with respect to northern hemisphere
    public static final Time SPRING = createTemporal(StandardTemporalType.SEASON_OF_YEAR, "SP",
            new InexactTime(SPRING_EQUINOX, QUARTER, new Range(SUTime.MARCH, SUTime.JUNE, SUTime.QUARTER), SPRING_DIST));
    public static final Time SUMMER = createTemporal(StandardTemporalType.SEASON_OF_YEAR, "SU",
            new InexactTime(SUMMER_SOLSTICE, QUARTER, new Range(SUTime.JUNE, SUTime.SEPTEMBER, SUTime.QUARTER), SUMMER_DIST )); 
    public static final Time FALL = createTemporal(StandardTemporalType.SEASON_OF_YEAR, "FA",
            new InexactTime(FALL_EQUINOX, QUARTER, new Range(SUTime.SEPTEMBER, SUTime.DECEMBER, SUTime.QUARTER), AUTUMN_DIST));
    public static final Time WINTER = createTemporal(StandardTemporalType.SEASON_OF_YEAR, "WI",
            new InexactTime(WINTER_SOLSTICE, QUARTER, new Range(SUTime.DECEMBER, SUTime.MARCH, SUTime.QUARTER), WINTER_DIST));
    // Time of day
    public static final PartialTime NOON = createTemporal(StandardTemporalType.TIME_OF_DAY, "MI", new IsoTime(12, 0, -1));
    public static final PartialTime MIDNIGHT = createTemporal(StandardTemporalType.TIME_OF_DAY, new IsoTime(0, 0, -1));
    public static final Time MORNING = createTemporal(StandardTemporalType.TIME_OF_DAY, "MO", new InexactTime(new Range(new InexactTime(new Partial(DateTimeFieldType.hourOfDay(), 6)), NOON)));
    public static final Time AFTERNOON = createTemporal(StandardTemporalType.TIME_OF_DAY, "AF", new InexactTime(new Range(NOON, new InexactTime(new Partial(DateTimeFieldType.hourOfDay(), 18)))));
    public static final Time EVENING = createTemporal(StandardTemporalType.TIME_OF_DAY, "EV", new InexactTime(new Range(new InexactTime(new Partial(DateTimeFieldType.hourOfDay(), 18)), new InexactTime(new Partial(DateTimeFieldType
            .hourOfDay(), 20)))));
    public static final Time NIGHT = createTemporal(StandardTemporalType.TIME_OF_DAY, "NI", new InexactTime(new Range(new InexactTime(new Partial(DateTimeFieldType.hourOfDay(), 19)), new InexactTime(new Partial(DateTimeFieldType
            .hourOfDay(), 5)))));
    public static final Time SUNRISE = createTemporal(StandardTemporalType.TIME_OF_DAY, "MO", TimexMod.EARLY.name(), new PartialTime());
    public static final Time SUNSET = createTemporal(StandardTemporalType.TIME_OF_DAY, "EV", TimexMod.EARLY.name(), new PartialTime());
    public static final Time DAWN = createTemporal(StandardTemporalType.TIME_OF_DAY, "MO", TimexMod.EARLY.name(), new PartialTime());
    public static final Time DUSK = createTemporal(StandardTemporalType.TIME_OF_DAY, "EV", new PartialTime());
    public static final Time DAYTIME = createTemporal(StandardTemporalType.TIME_OF_DAY, "DT", new InexactTime(new Range(SUNRISE, SUNSET)));
    public static final Time LUNCHTIME = createTemporal(StandardTemporalType.TIME_OF_DAY, "MI", new InexactTime(new Range(new InexactTime(new Partial(DateTimeFieldType.hourOfDay(), 12)), new InexactTime(new Partial(DateTimeFieldType
            .hourOfDay(), 14)))));
    public static final Time TEATIME = createTemporal(StandardTemporalType.TIME_OF_DAY, "AF", new InexactTime(new Range(new InexactTime(new Partial(DateTimeFieldType.hourOfDay(), 15)), new InexactTime(new Partial(DateTimeFieldType
            .hourOfDay(), 17)))));
    public static final Time DINNERTIME = createTemporal(StandardTemporalType.TIME_OF_DAY, "EV", new InexactTime(new Range(new InexactTime(new Partial(DateTimeFieldType.hourOfDay(), 18)), new InexactTime(new Partial(DateTimeFieldType
            .hourOfDay(), 20)))));
    public static final Time MORNING_TWILIGHT = createTemporal(StandardTemporalType.TIME_OF_DAY, "MO", new InexactTime(new Range(DAWN, SUNRISE)));
    public static final Time EVENING_TWILIGHT = createTemporal(StandardTemporalType.TIME_OF_DAY, "EV", new InexactTime(new Range(SUNSET, DUSK)));
    public static final TemporalSet TWILIGHT = createTemporal(StandardTemporalType.TIME_OF_DAY, "NI", new ExplicitTemporalSet(EVENING_TWILIGHT, MORNING_TWILIGHT));
    // Relative days
    public static final RelativeTime YESTERDAY = new RelativeTime(DAY.multiplyBy(-1));
    public static final RelativeTime TOMORROW = new RelativeTime(DAY.multiplyBy(+1));
    public static final RelativeTime TODAY = new RelativeTime(TemporalOp.THIS, SUTime.DAY);
    public static final RelativeTime TONIGHT = new RelativeTime(TemporalOp.THIS, SUTime.NIGHT);

    public static enum TimeUnit {
        // Basic time units

        MILLIS(SUTime.MILLIS), SECOND(SUTime.SECOND), MINUTE(SUTime.MINUTE), HOUR(SUTime.HOUR), DAY(SUTime.DAY), WEEK(SUTime.WEEK), MONTH(SUTime.MONTH), QUARTER(SUTime.QUARTER), YEAR(
        SUTime.YEAR), DECADE(SUTime.DECADE), CENTURY(SUTime.CENTURY), MILLENNIUM(SUTime.MILLENNIUM), UNKNOWN(SUTime.DURATION_UNKNOWN);
        protected Duration duration;

        TimeUnit(Duration d) {
            this.duration = d;
        }

        public Duration getDuration() {
            return duration;
        } // How long does this time last?

        public Duration getPeriod() {
            return duration;
        } // How often does this type of time occur?

        public Duration getGranularity() {
            return duration;
        } // What is the granularity of this time?

        public Temporal createTemporal(int n) {
            return duration.multiplyBy(n);
        }
    }

    public static enum StandardTemporalType {

        REFDATE(TimexType.DATE),
        REFTIME(TimexType.TIME),
        /*   MILLIS(TimexType.TIME, TimeUnit.MILLIS),
         SECOND(TimexType.TIME, TimeUnit.SECOND),
         MINUTE(TimexType.TIME, TimeUnit.MINUTE),
         HOUR(TimexType.TIME, TimeUnit.HOUR),
         DAY(TimexType.TIME, TimeUnit.DAY),
         WEEK(TimexType.TIME, TimeUnit.WEEK),
         MONTH(TimexType.TIME, TimeUnit.MONTH),
         QUARTER(TimexType.TIME, TimeUnit.QUARTER),
         YEAR(TimexType.TIME, TimeUnit.YEAR),  */
        TIME_OF_DAY(TimexType.TIME, TimeUnit.HOUR, SUTime.DAY) {
            public Duration getDuration() {
                return SUTime.HOUR.makeInexact();
            }
        },
        DAY_OF_YEAR(TimexType.DATE, TimeUnit.DAY, SUTime.YEAR) {
            protected Time _createTemporal(int n) {
                return new PartialTime(new Partial(DateTimeFieldType.dayOfYear(), n));
            }
        },
        DAY_OF_WEEK(TimexType.DATE, TimeUnit.DAY, SUTime.WEEK) {
            protected Time _createTemporal(int n) {
                return new PartialTime(new Partial(DateTimeFieldType.dayOfWeek(), n));
            }
        },
        DAYS_OF_WEEK(TimexType.DATE, TimeUnit.DAY, SUTime.WEEK) {
            public Duration getDuration() {
                return SUTime.DAY.makeInexact();
            }
        },
        WEEK_OF_YEAR(TimexType.DATE, TimeUnit.WEEK, SUTime.YEAR) {
            protected Time _createTemporal(int n) {
                return new PartialTime(new Partial(DateTimeFieldType.weekOfWeekyear(), n));
            }
        },
        MONTH_OF_YEAR(TimexType.DATE, TimeUnit.MONTH, SUTime.YEAR) {
            protected Time _createTemporal(int n) {
                //return new PartialTime(new Partial(DateTimeFieldType.monthOfYear(), n));
                return new IsoDate(-1, n, -1);
            }
        },
        PART_OF_YEAR(TimexType.DATE, TimeUnit.DAY, SUTime.YEAR) {
            public Duration getDuration() {
                return SUTime.DAY.makeInexact();
            }
        },
        SEASON_OF_YEAR(TimexType.DATE, TimeUnit.QUARTER, SUTime.YEAR),
        QUARTER_OF_YEAR(TimexType.DATE, TimeUnit.QUARTER, SUTime.YEAR) {
            protected Time _createTemporal(int n) {
                return new PartialTime(new Partial(JodaTimeUtils.QuarterOfYear, n));
            }
        };
        TimexType timexType;
        TimeUnit unit = TimeUnit.UNKNOWN;
        Duration period = SUTime.DURATION_NONE;

        StandardTemporalType(TimexType timexType) {
            this.timexType = timexType;
        }

        StandardTemporalType(TimexType timexType, TimeUnit unit) {
            this.timexType = timexType;
            this.unit = unit;
            this.period = unit.getPeriod();
        }

        StandardTemporalType(TimexType timexType, TimeUnit unit, Duration period) {
            this.timexType = timexType;
            this.unit = unit;
            this.period = period;
        }

        public TimexType getTimexType() {
            return timexType;
        }

        public Duration getDuration() {
            return unit.getDuration();
        } // How long does this time last?

        public Duration getPeriod() {
            return period;
        } // How often does this type of time occur?

        public Duration getGranularity() {
            return unit.getGranularity();
        } // What is the granularity of this time?

        protected Temporal _createTemporal(int n) {
            return null;
        }

        public Temporal createTemporal(int n) {
            Temporal t = _createTemporal(n);
            if (t != null) {
                t.standardTemporalType = this;
            }
            return t;
        }

        public Temporal create(Expressions.CompositeValue compositeValue) {
            StandardTemporalType temporalType = compositeValue.get("type");
            String label = compositeValue.get("label");
            String modifier = compositeValue.get("modifier");
            Temporal temporal = compositeValue.get("value");
            if (temporal == null) {
                temporal = new PartialTime();
            }
            return SUTime.createTemporal(temporalType, label, modifier, temporal);
        }
    }

    // Temporal operators (currently operates on two temporals and returns another
    // temporal)
    // Can add operators for:
    // lookup of temporal from string
    // creating durations, dates
    // public interface TemporalOp extends Function<Temporal,Temporal>();
    public static enum TemporalOp {
        // For durations: possible interpretation of next/prev:
        // next month, next week
        // NEXT: on Thursday, next week = week starting on next monday
        // ??? on Thursday, next week = one week starting from now
        // prev month, prev week
        // PREV: on Thursday, last week = week starting on the monday one week
        // before this monday
        // ??? on Thursday, last week = one week going back starting from now
        // For partial dates: two kind of next
        // next tuesday, next winter, next january
        // NEXT (PARENT UNIT, FAVOR): Example: on monday, next tuesday = tuesday of
        // the week after this
        // NEXT IMMEDIATE (NOT FAVORED): Example: on monday, next saturday =
        // saturday of this week
        // last saturday, last winter, last january
        // PREV (PARENT UNIT, FAVOR): Example: on wednesday, last tuesday = tuesday
        // of the week before this
        // PREV IMMEDIATE (NOT FAVORED): Example: on saturday, last tuesday =
        // tuesday of this week

        // (successor) Next week/day/...
        NEXT {
            public Temporal apply(Temporal arg1, Temporal arg2, int flags) {
                
                //System.out.println("Op NEXT applied to: " + arg1 + " " + arg2);
                
                if (arg2 == null) {
                    return arg1;
                }
                Temporal arg2Next = arg2.next();
                if (arg1 == null || arg2Next == null) {
                    return arg2Next;
                }
                if (arg1 instanceof Time) {
                    // TODO: flags?
                    Temporal resolved = arg2Next.resolve((Time) arg1, 0 /* RESOLVE_TO_FUTURE */);
                    return resolved;
                } else {
                    throw new UnsupportedOperationException("NEXT not implemented for arg1=" + arg1.getClass() + ", arg2=" + arg2.getClass());
                }
            }
        },
        // This coming week/friday
        NEXT_IMMEDIATE {
            public Temporal apply(Temporal arg1, Temporal arg2, int flags) {
                
                //System.out.println("Op NEXT_IMMEDIATE applied to: " + arg1 + " " + arg2);
                
                if (arg1 == null) {
                    return new RelativeTime(NEXT_IMMEDIATE, arg2);
                }
                if (arg2 == null) {
                    return arg1;
                }
                // Temporal arg2Next = arg2.next();
                // if (arg1 == null || arg2Next == null) { return arg2Next; }
                if (arg1 instanceof Time) {
                    Time t = (Time) arg1;
                    if (arg2 instanceof Duration) {
                        return ((Duration) arg2).toTime(t, flags | RESOLVE_TO_FUTURE);
                    } else {
                        // TODO: flags?
                        Temporal resolvedThis = arg2.resolve(t, RESOLVE_TO_FUTURE);
                        if (resolvedThis != null) {
                            if (resolvedThis instanceof Time) {
                                if (((Time) resolvedThis).compareTo(t) <= 0) {
                                    return NEXT.apply(arg1, arg2);
                                }
                            }
                        }
                        return resolvedThis;
                    }
                } else {
                    throw new UnsupportedOperationException("NEXT_IMMEDIATE not implemented for arg1=" + arg1.getClass() + ", arg2=" + arg2.getClass());
                }
            }
        },
        // Use arg1 as reference to resolve arg2 (take more general fields from arg1
        // and apply to arg2)
        THIS {
            public Temporal apply(Temporal arg1, Temporal arg2, int flags) {
                
                //System.out.println("Op THIS applied to: " + arg1 + " " + arg2);
                
                if (arg1 == null) {
                    return new RelativeTime(THIS, arg2, flags);
                }
                if (arg1 instanceof Time) {
                    if (arg2 instanceof Duration) {
                        return ((Duration) arg2).toTime((Time) arg1, flags);
                    } else {
                        // TODO: flags?
                        return arg2.resolve((Time) arg1, flags | RESOLVE_TO_THIS);
                    }
                } else {
                    throw new UnsupportedOperationException("THIS not implemented for arg1=" + arg1.getClass() + ", arg2=" + arg2.getClass());
                }
            }
        },
        // (predecessor) Previous week/day/...
        PREV {
            public Temporal apply(Temporal arg1, Temporal arg2, int flags) {
                
                //System.out.println("Op PREV applied to: " + arg1 + " " + arg2);
                
                if (arg2 == null) {
                    return arg1;
                }
                Temporal arg2Prev = arg2.prev();
                if (arg1 == null || arg2Prev == null) {
                    return arg2Prev;
                }
                if (arg1 instanceof Time) {
                    // TODO: flags?
                    Temporal resolved = arg2Prev.resolve((Time) arg1, 0 /*RESOLVE_TO_PAST */);
                    return resolved;
                } else {
                    throw new UnsupportedOperationException("PREV not implemented for arg1=" + arg1.getClass() + ", arg2=" + arg2.getClass());
                }
            }
        },
        // This past week/friday
        PREV_IMMEDIATE {
            public Temporal apply(Temporal arg1, Temporal arg2, int flags) {
                
                //System.out.println("Op PREV_IMMEDIATE applied to: " + arg1 + " " + arg2);
                
                if (arg1 == null) {
                    return new RelativeTime(PREV_IMMEDIATE, arg2);
                }
                if (arg2 == null) {
                    return arg1;
                }
                // Temporal arg2Prev = arg2.prev();
                // if (arg1 == null || arg2Prev == null) { return arg2Prev; }
                if (arg1 instanceof Time) {
                    Time t = (Time) arg1;
                    if (arg2 instanceof Duration) {
                        return ((Duration) arg2).toTime(t, flags | RESOLVE_TO_PAST);
                    } else {
                        // TODO: flags?
                        Temporal resolvedThis = arg2.resolve(t, RESOLVE_TO_PAST);
                        if (resolvedThis != null) {
                            if (resolvedThis instanceof Time) {
                                if (((Time) resolvedThis).compareTo(t) >= 0) {
                                    return PREV.apply(arg1, arg2);
                                }
                            }
                        }
                        return resolvedThis;
                    }
                } else {
                    throw new UnsupportedOperationException("PREV_IMMEDIATE not implemented for arg1=" + arg1.getClass() + ", arg2=" + arg2.getClass());
                }
            }
        },
        UNION {
            public Temporal apply(Temporal arg1, Temporal arg2, int flags) {
                
                //System.out.println("Op UNION applied to: " + arg1 + " " + arg2);
                
                if (arg1 == null) {
                    return arg2;
                }
                if (arg2 == null) {
                    return arg1;
                }
                // return arg1.union(arg2);
                throw new UnsupportedOperationException("UNION not implemented for arg1=" + arg1.getClass() + ", arg2=" + arg2.getClass());
            }
        },
        INTERSECT {
            public Temporal apply(Temporal arg1, Temporal arg2, int flags) {
                
                //System.out.println("Op INTERSECT applied to: " + arg1 + " " + arg2);
                
                if (arg1 == null) {
                    return arg2;
                }
                if (arg2 == null) {
                    return arg1;
                }
                Temporal t = arg1.intersect(arg2);
                if (t == null) {
                    t = arg2.intersect(arg1);
                }
                
                // throw new
                // UnsupportedOperationException("INTERSECT not implemented for arg1=" +
                // arg1.getClass() + ", arg2="+arg2.getClass());
                
                ITimeDensityFunction timeFunction = null;
                if ((arg1 instanceof CanExpressTimeAsFunction) && (arg2 instanceof CanExpressTimeAsFunction)) {
                    timeFunction = new IntersectTimeExpression((CanExpressTimeAsFunction) arg1, (CanExpressTimeAsFunction) arg2);
                } else if ((arg1 instanceof CanExpressTimeAsFunction) || (arg2 instanceof CanExpressTimeAsFunction)) {
                    System.err.println("arg1 is "+arg1.getClass().toString());
                    System.err.println("arg2 is "+arg2.getClass().toString());
                    throw new UnsupportedOperationException();
                }
            
                if (timeFunction != null) {
                    if (!(t instanceof CanExpressTimeAsFunction) ) {
                        System.out.println("t: " +t);
                        throw new UnsupportedOperationException();
                    } else {
                        ((CanExpressTimeAsFunction)t).SetFunction(timeFunction);
                    }
                }
               
                //System.out.println("Returning object with hash:" + System.identityHashCode(t));
                return t;
            }

        },
        // arg2 is "in" arg1, composite datetime
        IN {
            public Temporal apply(Temporal arg1, Temporal arg2, int flags) {
                
                //System.out.println("Op IN applied to: " + arg1 + " " + arg2);
                
                if (arg1 == null) {
                    return arg2;
                }
                if (arg1 instanceof Time) {
                    // TODO: flags?
                    return arg2.intersect((Time) arg1);
                } else {
                    throw new UnsupportedOperationException("IN not implemented for arg1=" + arg1.getClass() + ", arg2=" + arg2.getClass());
                }
            }
        },
        OFFSET {
            public Temporal apply(Temporal arg1, Temporal arg2, int flags) {
                
                //System.out.println("Op OFFSET applied to: " + arg1 + " " + arg2);
                
                if (arg1 == null) {
                    return new RelativeTime(OFFSET, arg2);
                }
                if (arg1 instanceof Time && arg2 instanceof Duration) {
                    return ((Time) arg1).offset((Duration) arg2);
                } else if (arg1 instanceof Range && arg2 instanceof Duration) {
                    return ((Range) arg1).offset((Duration) arg2);
                } else {
                    throw new UnsupportedOperationException("OFFSET not implemented for arg1=" + arg1.getClass() + ", arg2=" + arg2.getClass());
                }
            }
        },
        MINUS {
            public Temporal apply(Temporal arg1, Temporal arg2, int flags) {
                //System.out.println("Op MINUS applied to: " + arg1 + " " + arg2);
                
                if (arg1 == null) {
                    return arg2;
                }
                if (arg2 == null) {
                    return arg1;
                }
                if (arg1 instanceof Duration && arg2 instanceof Duration) {
                    return ((Duration) arg1).subtract((Duration) arg2);
                } else if (arg1 instanceof Time && arg2 instanceof Duration) {
                    return ((Time) arg1).subtract((Duration) arg2);
                } else if (arg1 instanceof Range && arg2 instanceof Duration) {
                    return ((Range) arg1).subtract((Duration) arg2);
                } else {
                    throw new UnsupportedOperationException("MINUS not implemented for arg1=" + arg1.getClass() + ", arg2=" + arg2.getClass());
                }
            }
        },
        PLUS {
            public Temporal apply(Temporal arg1, Temporal arg2, int flags) {
                
                //System.out.println("Op PLUS applied to: " + arg1 + " " + arg2);
                if (arg1 == null) {
                    return arg2;
                }
                if (arg2 == null) {
                    return arg1;
                }
                if (arg1 instanceof Duration && arg2 instanceof Duration) {
                    return ((Duration) arg1).add((Duration) arg2);
                } else if (arg1 instanceof Time && arg2 instanceof Duration) {
                    return ((Time) arg1).add((Duration) arg2);
                } else if (arg1 instanceof Range && arg2 instanceof Duration) {
                    return ((Range) arg1).add((Duration) arg2);
                } else {
                    throw new UnsupportedOperationException("PLUS not implemented for arg1=" + arg1.getClass() + ", arg2=" + arg2.getClass());
                }
            }
        },
        MIN {
            public Temporal apply(Temporal arg1, Temporal arg2, int flags) {
                
                //System.out.println("Op MIN applied to: " + arg1 + " " + arg2);
                
                if (arg1 == null) {
                    return arg2;
                }
                if (arg2 == null) {
                    return arg1;
                }
                if (arg1 instanceof Time && arg2 instanceof Time) {
                    return Time.min((Time) arg1, (Time) arg2);
                } else if (arg1 instanceof Duration && arg2 instanceof Duration) {
                    return Duration.min((Duration) arg1, (Duration) arg2);
                } else {
                    throw new UnsupportedOperationException("MIN not implemented for arg1=" + arg1.getClass() + ", arg2=" + arg2.getClass());
                }
            }
        },
        MAX {
            public Temporal apply(Temporal arg1, Temporal arg2, int flags) {
                
                //System.out.println("Op MAX applied to: " + arg1 + " " + arg2);
                
                if (arg1 == null) {
                    return arg2;
                }
                if (arg2 == null) {
                    return arg1;
                }
                if (arg1 instanceof Time && arg2 instanceof Time) {
                    return Time.max((Time) arg1, (Time) arg2);
                } else if (arg1 instanceof Duration && arg2 instanceof Duration) {
                    return Duration.max((Duration) arg1, (Duration) arg2);
                } else {
                    throw new UnsupportedOperationException("MAX not implemented for arg1=" + arg1.getClass() + ", arg2=" + arg2.getClass());
                }
            }
        },
        MULTIPLY {
            
            public Temporal apply(Duration d, int scale) {
                //System.out.println("Op MULTIPLY invoked with: " + d + " " + scale);
                if (d == null) {
                    return null;
                }
                if (scale == 1) {
                    return d;
                }
                return d.multiplyBy(scale);
            }

            public Temporal apply(PeriodicTemporalSet d, int scale) {
                if (d == null) {
                    return null;
                }
                if (scale == 1) {
                    return d;
                }
                return d.multiplyDurationBy(scale);
            }

            public Temporal apply(Object... args) {
                if (args.length == 2) {
                    if (args[0] instanceof Duration && (args[1] instanceof Integer || args[1] instanceof Long)) {
                        return apply((Duration) args[0], ((Number) args[1]).intValue());
                    }
                    if (args[0] instanceof PeriodicTemporalSet && (args[1] instanceof Integer || args[1] instanceof Long)) {
                        return apply((PeriodicTemporalSet) args[0], ((Number) args[1]).intValue());
                    }
                }
                throw new UnsupportedOperationException("apply(Object...) not implemented for TemporalOp " + this);
            }
        },
        DIVIDE {
            public Temporal apply(Duration d, int scale) {
                if (d == null) {
                    return null;
                }
                if (scale == 1) {
                    return d;
                }
                return d.divideBy(scale);
            }

            public Temporal apply(PeriodicTemporalSet d, int scale) {
                if (d == null) {
                    return null;
                }
                if (scale == 1) {
                    return d;
                }
                return d.divideDurationBy(scale);
            }

            public Temporal apply(Object... args) {
                if (args.length == 2) {
                    if (args[0] instanceof Duration && (args[1] instanceof Integer || args[1] instanceof Long)) {
                        return apply((Duration) args[0], ((Number) args[1]).intValue());
                    }
                    if (args[0] instanceof PeriodicTemporalSet && (args[1] instanceof Integer || args[1] instanceof Long)) {
                        return apply((PeriodicTemporalSet) args[0], ((Number) args[1]).intValue());
                    }
                }
                throw new UnsupportedOperationException("apply(Object...) not implemented for TemporalOp " + this);
            }
        },
        CREATE {
            public Temporal apply(TimeUnit tu, int n) {
                return tu.createTemporal(n);
            }

            public Temporal apply(Object... args) {
                if (args.length == 2) {
                    if (args[0] instanceof TimeUnit && args[1] instanceof Number) {
                        return apply((TimeUnit) args[0], ((Number) args[1]).intValue());
                    } else if (args[0] instanceof StandardTemporalType && args[1] instanceof Number) {
                        return ((StandardTemporalType) args[0]).createTemporal(((Number) args[1]).intValue());
                    } else if (args[0] instanceof Temporal && args[1] instanceof Number) {
                        return new OrdinalTime((Temporal) args[0], ((Number) args[1]).intValue());
                    }
                }
                throw new UnsupportedOperationException("apply(Object...) not implemented for TemporalOp " + this);
            }
        },
        ADD_MODIFIER {
            public Temporal apply(Temporal t, String modifier) {
                
                //System.out.println("Op ADD_MODIFIER invoked with: " + t + " " + modifier);
                
                return t.addMod(modifier);
            }

            public Temporal apply(Object... args) {
                if (args.length == 2) {
                    if (args[0] instanceof Temporal && args[1] instanceof String) {
                        return apply((Temporal) args[0], (String) args[1]);
                    }
                }
                throw new UnsupportedOperationException("apply(Object...) not implemented for TemporalOp " + this);
            }
        };

        public Temporal apply(Temporal arg1, Temporal arg2, int flags) {
            throw new UnsupportedOperationException("apply(Temporal, Temporal, int) not implemented for TemporalOp " + this);
        }

        public Temporal apply(Temporal arg1, Temporal arg2) {
            return apply(arg1, arg2, 0);
        }

        public Temporal apply(Temporal... args) {
            if (args.length == 2) {
                return apply(args[0], args[1]);
            }
            throw new UnsupportedOperationException("apply(Temporal...) not implemented for TemporalOp " + this);
        }

        public Temporal apply(Object... args) {
            throw new UnsupportedOperationException("apply(Object...) not implemented for TemporalOp " + this);
        }
    }
    public static final int ERA_BC = 0;
    public static final int ERA_AD = 1;
    public static final int ERA_UNKNOWN = -1;
    public static final int HALFDAY_AM = 0;
    public static final int HALFDAY_PM = 1;
    public static final int HALFDAY_UNKNOWN = -1;
    // TODO: Timezone...
    private static final Pattern PATTERN_ISO = Pattern.compile("(\\d\\d\\d\\d)-?(\\d\\d?)-?(\\d\\d?)(-?(?:T(\\d\\d):?(\\d\\d)?:?(\\d\\d)?(?:[.,](\\d{1,3}))?([+-]\\d\\d:?\\d\\d)?))?");
    private static final Pattern PATTERN_ISO_DATETIME = Pattern.compile("(\\d\\d\\d\\d)(\\d\\d)(\\d\\d):(\\d\\d)(\\d\\d)");
    private static final Pattern PATTERN_ISO_TIME = Pattern.compile("T(\\d\\d):?(\\d\\d)?:?(\\d\\d)?(?:[.,](\\d{1,3}))?([+-]\\d\\d:?\\d\\d)?");
    private static final Pattern PATTERN_ISO_DATE_1 = Pattern.compile(".*(\\d\\d\\d\\d)\\/(\\d\\d?)\\/(\\d\\d?).*");
    private static final Pattern PATTERN_ISO_DATE_2 = Pattern.compile(".*(\\d\\d\\d\\d)\\-(\\d\\d?)\\-(\\d\\d?).*");
    // Ambiguous pattern - interpret as MM/DD/YY(YY)
    private static final Pattern PATTERN_ISO_AMBIGUOUS_1 = Pattern.compile(".*(\\d\\d?)\\/(\\d\\d?)\\/(\\d\\d(\\d\\d)?).*");
    // Ambiguous pattern - interpret as MM-DD-YY(YY)
    private static final Pattern PATTERN_ISO_AMBIGUOUS_2 = Pattern.compile(".*(\\d\\d?)\\-(\\d\\d?)\\-(\\d\\d(\\d\\d)?).*");
    // Euro date
    // Ambiguous pattern - interpret as DD.MM.YY(YY)
    private static final Pattern PATTERN_ISO_AMBIGUOUS_3 = Pattern.compile(".*(\\d\\d?)\\.(\\d\\d?)\\.(\\d\\d(\\d\\d)?).*");
    private static final Pattern PATTERN_ISO_TIME_OF_DAY = Pattern.compile(".*(\\d?\\d):(\\d\\d)(:(\\d\\d)(\\.\\d+)?)?(\\s*([AP])\\.?M\\.?)?(\\s+([+\\-]\\d+|[A-Z][SD]T|GMT([+\\-]\\d+)?))?.*");

    /**
     * Converts a string that represents some kind of date into ISO 8601 format
     * and returns it as a SUTime.Time YYYYMMDDThhmmss
     *
     * @param dateStr
     */
    public static Time parseDateTime(String dateStr) {
        if (dateStr == null) {
            return null;
        }

        Matcher m = PATTERN_ISO.matcher(dateStr);
        if (m.matches()) {
            String time = m.group(4);
            IsoDate isoDate = new IsoDate(m.group(1), m.group(2), m.group(3));
            if (time != null) {
                IsoTime isoTime = new IsoTime(m.group(5), m.group(6), m.group(7), m.group(8));
                return new IsoDateTime(isoDate, isoTime);
            } else {
                return isoDate;
            }
        }

        m = PATTERN_ISO_DATETIME.matcher(dateStr);
        if (m.matches()) {
            IsoDate date = new IsoDate(m.group(1), m.group(2), m.group(3));
            IsoTime time = new IsoTime(m.group(4), m.group(5), null);
            return new IsoDateTime(date, time);
        }

        m = PATTERN_ISO_TIME.matcher(dateStr);
        if (m.matches()) {
            return new IsoTime(m.group(1), m.group(2), m.group(3), m.group(4));
        }

        IsoDate isoDate = null;
        if (isoDate == null) {
            m = PATTERN_ISO_DATE_1.matcher(dateStr);

            if (m.matches()) {
                isoDate = new IsoDate(m.group(1), m.group(2), m.group(3));
            }
        }

        if (isoDate == null) {
            m = PATTERN_ISO_DATE_2.matcher(dateStr);
            if (m.matches()) {
                isoDate = new IsoDate(m.group(1), m.group(2), m.group(3));
            }
        }

        if (isoDate == null) {
            m = PATTERN_ISO_AMBIGUOUS_1.matcher(dateStr);

            if (m.matches()) {
                isoDate = new IsoDate(m.group(3), m.group(1), m.group(2));
            }
        }

        if (isoDate == null) {
            m = PATTERN_ISO_AMBIGUOUS_2.matcher(dateStr);
            if (m.matches()) {
                isoDate = new IsoDate(m.group(3), m.group(1), m.group(2));
            }
        }

        if (isoDate == null) {
            m = PATTERN_ISO_AMBIGUOUS_3.matcher(dateStr);
            if (m.matches()) {
                isoDate = new IsoDate(m.group(3), m.group(2), m.group(1));
            }
        }

        // Now add Time of Day
        IsoTime isoTime = null;
        if (isoTime == null) {
            m = PATTERN_ISO_TIME_OF_DAY.matcher(dateStr);
            if (m.matches()) {
                // TODO: Fix
                isoTime = new IsoTime(m.group(1), m.group(2), m.group(4));
            }
        }

        if (isoDate != null && isoTime != null) {
            return new IsoDateTime(isoDate, isoTime);
        } else if (isoDate != null) {
            return isoDate;
        } else {
            return isoTime;
        }
    }
    public static final PeriodicTemporalSet HOURLY = new PeriodicTemporalSet(null, HOUR, "EVERY", "P1X");
    public static final PeriodicTemporalSet NIGHTLY = new PeriodicTemporalSet(NIGHT, DAY, "EVERY", "P1X");
    public static final PeriodicTemporalSet DAILY = new PeriodicTemporalSet(null, DAY, "EVERY", "P1X");
    public static final PeriodicTemporalSet MONTHLY = new PeriodicTemporalSet(null, MONTH, "EVERY", "P1X");
    public static final PeriodicTemporalSet QUARTERLY = new PeriodicTemporalSet(null, QUARTER, "EVERY", "P1X");
    public static final PeriodicTemporalSet YEARLY = new PeriodicTemporalSet(null, YEAR, "EVERY", "P1X");
    public static final PeriodicTemporalSet WEEKLY = new PeriodicTemporalSet(null, WEEK, "EVERY", "P1X");
}
