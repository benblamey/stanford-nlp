package edu.stanford.nlp.time.distributed;

import org.joda.time.DateTime;
import org.joda.time.Days;

public abstract class TimeDensityFunction {

    /**
     * For the time being, the maximum range is hard-coded.
     */
    private static final DateTime s_startDay = new DateTime(2005,1,1,0,0);
    private static final DateTime s_endDay = new DateTime(2014,12,31,0,0);
    private static final int s_numberOfDays = Days.daysBetween(s_startDay, s_endDay).getDays() + 1;
    private double _originalTotalMass;
    
    public abstract double getDensity(DateTime time);
    
    public abstract String getGNUPlot(String millTimeSecondsExpr);
    
    public double getTotalMass() {
        getDensities();
        return _originalTotalMass;
    }
    
    private double[] _densities = null;
    
    /**
     * Derived classes need to call this method before any modifications are allowed.
     * @return if true, no modifications allowed, throw a ReadOnlyException
     */
    protected boolean getIsfinalized() {
        return _densities != null;
    }
    
    private synchronized double[] getDensities() {
        
        if (_densities == null) {
            _densities = new double[s_numberOfDays];
            
            double totalDensity = 0;
            for (int i = 0; i < s_numberOfDays; i++) {
                DateTime sampleDay  = s_startDay.plusDays(i);
                double density = getDensity(sampleDay);
                totalDensity += density;
                _densities[i] = density;
            }
            
            for (int i = 0; i < s_numberOfDays; i++) {
                _densities[i] = _densities[i]/totalDensity;
            }
            _originalTotalMass = totalDensity;
        }
        
        return _densities;
    }
    
    
    public static double getSimilarity(TimeDensityFunction x, TimeDensityFunction y) {
        // Similarity is simply the scalar product of the density vectors associated with the two time densities.
        // Effectively just multiplying the densities without re-normalizing.
        
        if (x == null || y == null) {
            throw new IllegalArgumentException();
        }
        
        // Getting the densities performs normalization if it has not already been done so.
        double[] densitiesX = x.getDensities();
        double[] densitiesY = y.getDensities();
        
        if (densitiesX.length != densitiesY.length) {
                throw new RuntimeException("densities are not the same length.");
        }
        
        double similarity = 0;
        for (int i = 0; i < s_numberOfDays; i++) {
            if (densitiesX[i] < 0 || densitiesY[i] < 0) {
                throw new RuntimeException("negative probability density detected!");
            }
            similarity += densitiesX[i] * densitiesY[i];
        }
        
        return similarity;
    }

    @Override
    public String toString() {
        return this.getGNUPlot("x");
    }
    
    
    
}
