
package edu.stanford.nlp.time.distributed;

public interface CanExpressTimeAsFunction {
    

    public TimeDensityFunction getTimeDensityFunction();
    
    /**
     * Replace any existing probability density function with that specified.
     * @param func 
     */
    public void setTimeDensityFunction(TimeDensityFunction func);
}
