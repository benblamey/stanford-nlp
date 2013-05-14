package edu.stanford.nlp.time;


import edu.stanford.nlp.util.HashIndex;
import edu.stanford.nlp.util.Index;

// Index of time id to temporal object
public class TimeIndex {
    Index<Temporal> temporalIndex = new HashIndex<Temporal>();
    Index<Temporal> temporalFuncIndex = new HashIndex<Temporal>();

    public TimeIndex() {
        addTemporal(SUTime.TIME_REF);
    }

    public void clear() {
        temporalIndex.clear();
        temporalFuncIndex.clear();
        addTemporal(SUTime.TIME_REF);
    }

    public Temporal getTemporal(int i) {
        return temporalIndex.get(i);
    }

    public Temporal getTemporalFunc(int i) {
        return temporalFuncIndex.get(i);
    }

    public boolean addTemporal(Temporal t) {
        return temporalIndex.add(t);
    }

    public boolean addTemporalFunc(Temporal t) {
        return temporalFuncIndex.add(t);
    }

    public int indexOfTemporal(Temporal t, boolean add) {
        return temporalIndex.indexOf(t, add);
    }

    public int indexOfTemporalFunc(Temporal t, boolean add) {
        return temporalFuncIndex.indexOf(t, add);
    }

}
