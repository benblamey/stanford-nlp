package edu.stanford.nlp.time.distributed;


class TimeDensityFunctionContainer implements CanExpressTimeAsFunction {
    
    private final ITimeDensityFunction _func;

    public TimeDensityFunctionContainer(ITimeDensityFunction func, String message) {
        if (func == null) {
            throw new RuntimeException("func cannot be null.");
        }
        _func = func;
    }
    
    public ITimeDensityFunction GettimeDensityFunction() {
        return _func;
    }

    public void SetFunction(ITimeDensityFunction func) {
        throw new UnsupportedOperationException("Readonly- Not supported.");
    }

}
