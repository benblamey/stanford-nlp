
package edu.stanford.nlp.time.distributed;

public interface CanExpressTimeAsFunction {
    

    public ITimeDensityFunction GettimeDensityFunction();
    
    /**
     * Replace any existing probability density function with that specified.
     * @param func 
     */
    public void SetFunction(ITimeDensityFunction func);
}
