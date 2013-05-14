package edu.stanford.nlp.time;


import java.util.Map;

// Relative Time (something not quite resolved)
public class RelativeTime extends Time {
    Time base = SUTime.TIME_REF;
    SUTime.TemporalOp tempOp;
    Temporal tempArg;
    int opFlags;

    public RelativeTime(Time base, SUTime.TemporalOp tempOp, Temporal tempArg, int flags) {
        super(base);
        this.base = base;
        this.tempOp = tempOp;
        this.tempArg = tempArg;
        this.opFlags = flags;
    }

    public RelativeTime(Time base, SUTime.TemporalOp tempOp, Temporal tempArg) {
        super(base);
        this.base = base;
        this.tempOp = tempOp;
        this.tempArg = tempArg;
    }

    public RelativeTime(SUTime.TemporalOp tempOp, Temporal tempArg) {
        this.tempOp = tempOp;
        this.tempArg = tempArg;
    }

    public RelativeTime(SUTime.TemporalOp tempOp, Temporal tempArg, int flags) {
        this.tempOp = tempOp;
        this.tempArg = tempArg;
        this.opFlags = flags;
    }

    public RelativeTime(Duration offset) {
        this(SUTime.TIME_REF, SUTime.TemporalOp.OFFSET, offset);
    }

    public RelativeTime(Time base, Duration offset) {
        this(base, SUTime.TemporalOp.OFFSET, offset);
    }

    public RelativeTime(Time base) {
        this.base = base;
    }

    public RelativeTime() {
    }

    public boolean isGrounded() {
        return (base != null) && base.isGrounded();
    }

    // TODO: compute duration/range => uncertainty of this time
    public Duration getDuration() {
        return null;
    }

    public Range getRange(int flags, Duration granularity) {
        return new Range(this, this);
    }

    public Map<String, String> getTimexAttributes(TimeIndex timeIndex) {
        Map<String, String> map = super.getTimexAttributes(timeIndex);
        String tfid = getTfidString(timeIndex);
        map.put(SUTime.TimexAttr.temporalFunction.name(), "true");
        map.put(SUTime.TimexAttr.valueFromFunction.name(), tfid);
        if (base != null) {
            map.put(SUTime.TimexAttr.anchorTimeID.name(), base.getTidString(timeIndex));
        }
        return map;
    }

    // / NOTE: This is not ISO or timex standard
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
        StringBuilder sb = new StringBuilder();
        if (base != null && base != SUTime.TIME_REF) {
            sb.append(base.toFormattedString(flags));
        }
        if (tempOp != null) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(tempOp);
            if (tempArg != null) {
                sb.append(" ").append(tempArg.toFormattedString(flags));
            }
        }
        return sb.toString();
    }

    public Temporal resolve(Time refTime, int flags) {
        Temporal groundedBase = null;
        if (base == SUTime.TIME_REF) {
            groundedBase = refTime;
        } else if (base != null) {
            groundedBase = base.resolve(refTime, flags);
        }
        if (tempOp != null) {
            // NOTE: Should be always safe to resolve and then apply since
            // we will terminate here (no looping hopefully)
            Temporal t = tempOp.apply(groundedBase, tempArg, opFlags);
            if (t != null) {
                t = t.addModApprox(mod, approx);
                return t;
            } else {
                // NOTE: this can be difficult if applying op
                // gives back same stuff stuff as before
                // Try applying op and then resolving
                t = tempOp.apply(base, tempArg, opFlags);
                if (t != null) {
                    t = t.addModApprox(mod, approx);
                    if (!this.equals(t)) {
                        return t.resolve(refTime, flags);
                    } else {
                        // Applying op doesn't do much....
                        return this;
                    }
                } else {
                    return null;
                }
            }
        } else {
            return (groundedBase != null) ? groundedBase.addModApprox(mod, approx) : null;
        }
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RelativeTime that = (RelativeTime) o;
        if (opFlags != that.opFlags) {
            return false;
        }
        if (base != null ? !base.equals(that.base) : that.base != null) {
            return false;
        }
        if (tempArg != null ? !tempArg.equals(that.tempArg) : that.tempArg != null) {
            return false;
        }
        if (tempOp != that.tempOp) {
            return false;
        }
        return true;
    }

    public int hashCode() {
        int result = base != null ? base.hashCode() : 0;
        result = 31 * result + (tempOp != null ? tempOp.hashCode() : 0);
        result = 31 * result + (tempArg != null ? tempArg.hashCode() : 0);
        result = 31 * result + opFlags;
        return result;
    }

    public Time add(Duration offset) {
        Time t;
        Duration d = offset;
        if (this.tempOp == null) {
            t = new RelativeTime(base, d);
            t.approx = this.approx;
            t.mod = this.mod;
        } else if (this.tempOp == SUTime.TemporalOp.OFFSET) {
            d = ((Duration) this.tempArg).add(offset);
            t = new RelativeTime(base, d);
            t.approx = this.approx;
            t.mod = this.mod;
        } else {
            t = new RelativeTime(this, d);
        }
        return t;
    }

    public Temporal intersect(Temporal t) {
        return new RelativeTime(this, SUTime.TemporalOp.INTERSECT, t);
    }
    private static final long serialVersionUID = 1;

}
