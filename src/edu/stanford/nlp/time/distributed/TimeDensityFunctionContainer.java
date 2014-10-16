package edu.stanford.nlp.time.distributed;


class TimeDensityFunctionContainer implements CanExpressTimeAsFunction {
    
    private final TimeDensityFunction _func;

    public TimeDensityFunctionContainer(TimeDensityFunction func, String message) {
        if (func == null) {
            throw new RuntimeException("func cannot be null.");
        }
        _func = func;
    }
    
    public TimeDensityFunction GettimeDensityFunction() {
        return _func;
    }

    public void SetFunction(TimeDensityFunction func) {
        throw new UnsupportedOperationException("Readonly- Not supported.");
    }

}
