package edu.stanford.nlp.time;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.tokensregex.*;
import edu.stanford.nlp.ling.tokensregex.types.*;
import edu.stanford.nlp.pipeline.ChunkAnnotationUtils;
import edu.stanford.nlp.pipeline.CoreMapAttributeAggregator;
import edu.stanford.nlp.time.Duration;
import edu.stanford.nlp.time.DurationRange;
import edu.stanford.nlp.time.InexactDuration;
import edu.stanford.nlp.time.Options;
import edu.stanford.nlp.time.PartialTime;
import edu.stanford.nlp.time.PeriodicTemporalSet;
import edu.stanford.nlp.time.Range;
import edu.stanford.nlp.time.RefTime;
import edu.stanford.nlp.time.SUTime;
import edu.stanford.nlp.time.SimpleTime;
import edu.stanford.nlp.time.Temporal;
import edu.stanford.nlp.time.Time;
import edu.stanford.nlp.time.TimeExpression;
import edu.stanford.nlp.time.TimeExpressionPatterns;
import edu.stanford.nlp.time.TimeFormatter;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.StringUtils;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Provides generic mechanism to convert natural language into temporal representation
 * by reading patterns/rules specifying mapping between text and temporal objects
 * from file.
 *
 * @author Angel Chang
 */
public class GenericTimeExpressionPatterns implements TimeExpressionPatterns {
  Env env;
  Options options;

  public GenericTimeExpressionPatterns(Options options) {
    this.options = options;
    initEnv();
    if (options.binders != null) {
      for (Env.Binder binder:options.binders) {
        binder.bind(env);
      }
    }
  }

  public CoreMapExpressionExtractor createExtractor() {
    List<String> filenames = StringUtils.split(options.grammarFilename, "\\s*[,;]\\s*");
    return CoreMapExpressionExtractor.createExtractorFromFiles(env, filenames);
  }

  private static class TimexTypeMatchNodePattern extends NodePattern<TimeExpression> {
    SUTime.TimexType type;
    public TimexTypeMatchNodePattern(SUTime.TimexType type) { this.type = type; }
    public boolean match(TimeExpression te) {
      if (te != null) {
                Temporal t = te.getTemporal();
        if (t != null) {
          return type.equals(t.getTimexType());
        }
      }
      return false;
    }
  }

  private static class MatchedExpressionValueTypeMatchNodePattern extends NodePattern<MatchedExpression> {
    String valueType;
    public MatchedExpressionValueTypeMatchNodePattern(String valueType) { this.valueType = valueType; }
    public boolean match(MatchedExpression me) {
      Value v = (me != null)? me.getValue():null;
      if (v != null) {
        return (valueType.equals(v.getType()));
      }
      return false;
    }
  }

  private void initEnv()
  {
    env = TokenSequencePattern.getNewEnv();
    env.setDefaultResultsAnnotationExtractor(TimeExpression.TimeExpressionConverter);
    env.setDefaultTokensAnnotationKey(CoreAnnotations.NumerizedTokensAnnotation.class);
    env.setDefaultResultAnnotationKey(TimeExpression.Annotation.class);
    env.setDefaultNestedResultsAnnotationKey(TimeExpression.ChildrenAnnotation.class);
    env.setDefaultTokensAggregators(CoreMapAttributeAggregator.DEFAULT_NUMERIC_TOKENS_AGGREGATORS);

    env.bind("nested", TimeExpression.ChildrenAnnotation.class);
    env.bind("time", new TimeFormatter.TimePatternExtractRuleCreator());
    // Do case insensitive matching
    env.setDefaultStringPatternFlags(Pattern.CASE_INSENSITIVE);

    env.bind("options", options);
    env.bind("TIME_REF", SUTime.TIME_REF);
    env.bind("TIME_REF_UNKNOWN", SUTime.TIME_REF_UNKNOWN);
    env.bind("TIME_UNKNOWN", SUTime.TIME_UNKNOWN);
    env.bind("TIME_NONE", SUTime.TIME_NONE);
    env.bind("ERA_AD", SUTime.ERA_AD);
    env.bind("ERA_BC", SUTime.ERA_BC);
    env.bind("ERA_UNKNOWN", SUTime.ERA_UNKNOWN);
    env.bind("HALFDAY_AM", SUTime.HALFDAY_AM);
    env.bind("HALFDAY_PM", SUTime.HALFDAY_PM);
    env.bind("HALFDAY_UNKNOWN", SUTime.HALFDAY_UNKNOWN);
    env.bind("RESOLVE_TO_THIS", SUTime.RESOLVE_TO_THIS);
    env.bind("RESOLVE_TO_PAST", SUTime.RESOLVE_TO_PAST);
    env.bind("RESOLVE_TO_FUTURE", SUTime.RESOLVE_TO_FUTURE);
    env.bind("RESOLVE_TO_CLOSEST", SUTime.RESOLVE_TO_CLOSEST);

    env.bind("numcomptype", CoreAnnotations.NumericCompositeTypeAnnotation.class);
    env.bind("numcompvalue", CoreAnnotations.NumericCompositeValueAnnotation.class);

    env.bind("temporal", TimeExpression.Annotation.class);
//    env.bind("tags", SequenceMatchRules.Tags.TagsAnnotation.class);
    env.bind("::IS_TIMEX_DATE", new TimexTypeMatchNodePattern(SUTime.TimexType.DATE));
    env.bind("::IS_TIMEX_DURATION", new TimexTypeMatchNodePattern(SUTime.TimexType.DURATION));
    env.bind("::IS_TIMEX_TIME", new TimexTypeMatchNodePattern(SUTime.TimexType.TIME));
    env.bind("::IS_TIMEX_SET", new TimexTypeMatchNodePattern(SUTime.TimexType.SET));
    env.bind("::IS_TIME_UNIT", new MatchedExpressionValueTypeMatchNodePattern("TIMEUNIT"));
    env.bind("::MONTH", new MatchedExpressionValueTypeMatchNodePattern("MONTH_OF_YEAR"));
    env.bind("::DAYOFWEEK", new MatchedExpressionValueTypeMatchNodePattern("DAY_OF_WEEK"));

    // BINDINGS for parsing from file!!!!!!!
    for (SUTime.TemporalOp t:SUTime.TemporalOp.values()) {
      env.bind(t.name(), new Expressions.PrimitiveValue<SUTime.TemporalOp>("TemporalOp", t));
    }
    for (SUTime.TimeUnit t: SUTime.TimeUnit.values()) {
      if (!t.equals(SUTime.TimeUnit.UNKNOWN)) {
        //env.bind(t.name(), new SequenceMatchRules.PrimitiveValue<SUTime.Temporal>("DURATION", t.getDuration(), "TIMEUNIT"));
        env.bind(t.name(), new Expressions.PrimitiveValue<Temporal>("TIMEUNIT", t.getDuration()));
      }
    }
    for (SUTime.StandardTemporalType t: SUTime.StandardTemporalType.values()) {
      env.bind(t.name(), new Expressions.PrimitiveValue<SUTime.StandardTemporalType>("TemporalType", t));
    }
    env.bind("Duration", new Expressions.PrimitiveValue<ValueFunction>(
            Expressions.TYPE_FUNCTION,
            new ValueFunctions.NamedValueFunction("Duration") {
              private Temporal addEndPoints(Duration d, Time beginTime, Time endTime)
              {
                Temporal t = d;
                if (d != null && (beginTime != null || endTime != null)) {
                    Time b = beginTime;
                    Time e = endTime;
                  // New so we get different time ids
                  if (b == SUTime.TIME_REF_UNKNOWN) {
                    b = new RefTime("UNKNOWN");
                  } else if (b == SUTime.TIME_UNKNOWN) {
                    b = new SimpleTime("UNKNOWN");
                  }
                  if (e == SUTime.TIME_REF_UNKNOWN) {
                    e = new RefTime("UNKNOWN");
                  } else if (e == SUTime.TIME_UNKNOWN) {
                    e = new SimpleTime("UNKNOWN");
                  }
                  t = new Range(b,e,d);
                }
                return t;
              }

              public boolean checkArgs(List<Value> in) {
                // TODO: Check args
                return true;
              }
              public Value apply(Env env, List<Value> in) {
                if (in.size() == 2) {
                    Duration d = (Duration) in.get(0).get();
                  if (in.get(1).get() instanceof Number) {
                    int m = ((Number) in.get(1).get()).intValue();
                    return new Expressions.PrimitiveValue("DURATION", d.multiplyBy(m));
                  } else if (in.get(1).get() instanceof String){
                    Number n = Integer.parseInt((String) in.get(1).get());
                    if (n != null) {
                      return new Expressions.PrimitiveValue("DURATION", d.multiplyBy(n.intValue()));
                    } else {
                      return null;
                    }
                  } else {
                    throw new IllegalArgumentException("Invalid arguments to " + name);
                  }
                } else if (in.size() == 5 || in.size() == 3) {
                  // TODO: Handle Strings...
                  List<? extends CoreMap> durationStartTokens = (List<? extends CoreMap>) in.get(0).get();
                  Number durationStartVal = (durationStartTokens != null)? durationStartTokens.get(0).get(CoreAnnotations.NumericCompositeValueAnnotation.class):null;
                  List<? extends CoreMap> durationEndTokens = (List<? extends CoreMap>) in.get(1).get();
                  Number durationEndVal = (durationEndTokens != null)? durationEndTokens.get(0).get(CoreAnnotations.NumericCompositeValueAnnotation.class):null;
                  // TODO: This should already be in durations....
                  List<? extends CoreMap> durationUnitTokens = (List<? extends CoreMap>) in.get(2).get();
                  //String durationUnitString = (durationUnitTokens != null)? durationUnitTokens.get(0).get(CoreAnnotations.TextAnnotation.class):null;
                  //SUTime.Duration durationUnit = getDuration(durationUnitString);
                  TimeExpression te = (durationUnitTokens != null)? durationUnitTokens.get(0).get(TimeExpression.Annotation.class):null;
                    Duration durationUnit = (Duration) te.getTemporal();

                  // TODO: Handle inexactness
                  // Create duration range...
                    Duration durationStart = (durationStartVal != null)? durationUnit.multiplyBy(durationStartVal.intValue()):null;
                    Duration durationEnd = (durationEndVal != null)? durationUnit.multiplyBy(durationEndVal.intValue()):null;
                    Duration duration = durationStart;
                  if (duration == null) {
                    if (durationEnd != null) {
                      duration = durationEnd;
                    } else {
                      duration = new InexactDuration(durationUnit);
                    }
                  }
                  else if (durationEnd != null) { duration = new DurationRange(durationStart, durationEnd); }

                  // Add begin and end times
                    Time beginTime = (in.size() > 3)? (Time) in.get(3).get():null;
                    Time endTime = (in.size() > 4)? (Time) in.get(4).get():null;
                    Temporal temporal = addEndPoints(duration, beginTime, endTime);
                  if (temporal instanceof Range) {
                    return new Expressions.PrimitiveValue("RANGE", temporal);
                  } else {
                    return new Expressions.PrimitiveValue("DURATION", temporal);
                  }
                } else {
                  throw new IllegalArgumentException("Invalid number of arguments to " + name);
                }
              }
            }
    ));
    env.bind("DayOfWeek", new Expressions.PrimitiveValue<ValueFunction>(
            Expressions.TYPE_FUNCTION,
            new ValueFunctions.NamedValueFunction("DayOfWeek") {
              public boolean checkArgs(List<Value> in) {
                if (in.size() != 1) {
                  return false;
                }
                if (in.get(0) == null || !(in.get(0).get() instanceof Number)) {
                  return false;
                }
                return true;
              }
              public Value apply(Env env, List<Value> in) {
                if (in.size() == 1) {
                  return new Expressions.PrimitiveValue(SUTime.StandardTemporalType.DAY_OF_WEEK.name(),
                          SUTime.StandardTemporalType.DAY_OF_WEEK.createTemporal(((Number) in.get(0).get()).intValue()));
                } else {
                  throw new IllegalArgumentException("Invalid number of arguments to " + name);
                }
              }
            }
    ));
    env.bind("MonthOfYear", new Expressions.PrimitiveValue<ValueFunction>(
            Expressions.TYPE_FUNCTION,
            new ValueFunctions.NamedValueFunction("MonthOfYear") {
              public boolean checkArgs(List<Value> in) {
                if (in.size() != 1) {
                  return false;
                }
                if (in.get(0) == null || !(in.get(0).get() instanceof Number)) {
                  return false;
                }
                return true;
              }
              public Value apply(Env env, List<Value> in) {
                if (in.size() == 1) {
                  return new Expressions.PrimitiveValue(SUTime.StandardTemporalType.MONTH_OF_YEAR.name(),
                          SUTime.StandardTemporalType.MONTH_OF_YEAR.createTemporal(((Number) in.get(0).get()).intValue()));
                } else {
                  throw new IllegalArgumentException("Invalid number of arguments to " + name);
                }
              }
            }
    ));
    env.bind("MakePeriodicTemporalSet", new Expressions.PrimitiveValue<ValueFunction>(
            Expressions.TYPE_FUNCTION,
            new ValueFunctions.NamedValueFunction("MakePeriodicTemporalSet") {
              // First argument is the temporal acting as the base of the periodic set
              // Second argument is the quantifier (string)
              // Third argument is the multiple (how much to scale the natural period)
              public boolean checkArgs(List<Value> in) {
                if (in.size() < 3) {
                  return false;
                }
                if (in.get(0) == null ||
                        (!(in.get(0).get() instanceof Temporal) && !(in.get(0).get() instanceof TimeExpression))) {
                  return false;
                }
                if (in.get(1) == null ||
                        (!(in.get(1).get() instanceof String) && !(in.get(1).get() instanceof List))) {
                  return false;
                }
                if (in.get(2) == null || !(in.get(2).get() instanceof Number)) {
                  return false;
                }
                return true;
              }
              public Value apply(Env env, List<Value> in) {
                if (in.size() >= 1) {
                    Temporal temporal = null;
                  Object t = in.get(0).get();
                  if (t instanceof Temporal) {
                    temporal = (Temporal) in.get(0).get();
                  } else if (t instanceof TimeExpression) {
                    temporal = ((TimeExpression) t).getTemporal();
                  } else {
                    throw new IllegalArgumentException("Type mismatch on arg0: Cannot apply " + this + " to " + in);
                  }
                  String quant = null;
                  int scale = 1;
                  if (in.size() >= 2 && in.get(1) != null) {
                    Object arg1 = in.get(1).get();
                    if (arg1 instanceof String) {
                      quant = (String) arg1;
                    } else if (arg1 instanceof List) {
                      List<CoreMap> cms = (List<CoreMap>) arg1;
                      quant = ChunkAnnotationUtils.getTokenText(cms, CoreAnnotations.TextAnnotation.class);
                      if (quant != null) {
                        quant = quant.toLowerCase();
                      }
                    } else {
                      throw new IllegalArgumentException("Type mismatch on arg1: Cannot apply " + this + " to " + in);
                    }
                  }
                  if (in.size() >= 3 && in.get(2) != null) {
                    Number arg2 = (Number) in.get(2).get();
                    if (arg2 != null) {
                      scale = arg2.intValue();
                    }
                  }
                    Duration period = temporal.getPeriod();
                  if (period != null & scale != 1) {
                    period = period.multiplyBy(scale);
                  }
                  return new Expressions.PrimitiveValue("PeriodicTemporalSet",
                          new PeriodicTemporalSet(temporal,period,quant,null/*"P1X"*/));
                } else {
                  throw new IllegalArgumentException("Invalid number of arguments to " + name);
                }
              }
            }
    ));

    env.bind("TemporalCompose", new Expressions.PrimitiveValue<ValueFunction>(
            
            // Arg 0 is temporal Op.
            // All other args are supposed to be temporals / or a matched expression
            Expressions.TYPE_FUNCTION,
            new ValueFunctions.NamedValueFunction("TemporalCompose") {
              public boolean checkArgs(List<Value> in) {
                if (in.size() < 1) {
                  return false;
                }
                if (in.get(0) == null || !(in.get(0).get() instanceof SUTime.TemporalOp)) {
                  return false;
                }
                return true;
              }
              public Value apply(Env env, List<Value> in) {
                if (in.size() > 1) {
                  SUTime.TemporalOp op = (SUTime.TemporalOp) in.get(0).get();
                  boolean allTemporalArgs = true;
                  Object[] args = new Object[in.size()-1];
                  for (int i = 0; i < args.length; i++) {
                    Value v = in.get(i+1);
                    if (v != null) {
                      args[i] = v.get();
                      if (args[i] instanceof MatchedExpression) {
                        Value v2 = ((MatchedExpression) args[i]).getValue();
                        args[i] = (v2 != null)? v2.get():null;
                      }
                      if (args[i] != null && !(args[i] instanceof Temporal)) {
                        allTemporalArgs = false;
                      }
                    }
                  }
                  if (allTemporalArgs) {
                        Temporal[] temporalArgs = new Temporal[args.length];
                    for (int i = 0; i < args.length; i++) {
                      temporalArgs[i] = (Temporal) args[i];
                    }
                    return new Expressions.PrimitiveValue(null, op.apply(temporalArgs));
                  } else {
                    return new Expressions.PrimitiveValue(null, op.apply(args));
                  }
                } else {
                  throw new IllegalArgumentException("Invalid number of arguments to " + name);
                }
              }
            }
    ));
  }

  public int determineRelFlags(CoreMap annotation, TimeExpression te)
  {
    int flags = 0;
    boolean flagsSet = false;
    if (te.value.getTags() != null) {
      Value v = te.value.getTags().getTag("resolveTo");
      if (v != null && v.get() instanceof Number) {
        flags = ((Number) v.get()).intValue();
        flagsSet = true;
      }
    }
    if (!flagsSet) {
      if (te.getTemporal() instanceof PartialTime) {
        flags = SUTime.RESOLVE_TO_CLOSEST;
      }
    }
    return flags;
  }
}
