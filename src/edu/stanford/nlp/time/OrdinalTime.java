package edu.stanford.nlp.time;


import edu.stanford.nlp.time.distributed.TimeDensityFunction;
import java.util.List;

// The nth temporal
public class OrdinalTime extends Time {
    Temporal base;
    int n;

    public OrdinalTime(Temporal base, int n) {
        this.base = base;
        this.n = n;
    }
    
                    /**
     * Derived classes should override this method to provide a 
     * TimeDensityFunction representing the temporal information they contain.
     * @return 
     */
    public TimeDensityFunction createDefaultTimeExpression() {
        
     //   _t.
        
        System.err.println("todo: implement OrdinalTime");
        return null;
    }

    public Time add(Duration offset) {
        return new RelativeTime(this, SUTime.TemporalOp.OFFSET, offset);
    }

    public String toFormattedString(int flags) {
        if (getTimeLabel() != null) {
            return getTimeLabel();
        }
        if ((flags & SUTime.FORMAT_ISO) != 0) {
            return null;
        } // TODO: is there iso standard?
        // TODO: is there iso standard?
        if ((flags & SUTime.FORMAT_TIMEX3_VALUE) != 0) {
            return null;
        } // TODO: is there timex3 standard?
        // TODO: is there timex3 standard?
        if (base != null) {
            String str = base.toFormattedString(flags);
            if (str != null) {
                return str + "-#" + n;
            }
        }
        return null;
    }

    /*    public Temporal intersect(Temporal t) {
    if (base instanceof PartialTime && t instanceof PartialTime) {
    return new OrdinalTime(base.intersect(t), n);
    } else {
    return new RelativeTime(this, TemporalOp.INTERSECT, t);
    }
    }  */
    public Time intersect(Time t) {
        if (base instanceof PartialTime && t instanceof PartialTime) {
            return new OrdinalTime(base.intersect(t), n);
        } else {
            return new RelativeTime(this, SUTime.TemporalOp.INTERSECT, t);
        }
    }

    public Temporal resolve(Time t, int flags) {
        if (base instanceof PartialTime) {
            PartialTime pt = (PartialTime) base;
            List<Temporal> list = pt.toList();
            if (list != null && list.size() >= n) {
                return list.get(n - 1);
            }
        }
        return this;
    }
    private static final long serialVersionUID = 1;

}
