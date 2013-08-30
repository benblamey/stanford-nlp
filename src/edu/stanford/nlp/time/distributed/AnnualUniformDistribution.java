package edu.stanford.nlp.time.distributed;

import org.joda.time.DateTime;

public class AnnualUniformDistribution implements ITimeDensityFunction {

    private Number _mixtureCoefficient;

    public AnnualUniformDistribution(Number mixtureCoefficient) {
        _mixtureCoefficient = mixtureCoefficient;
    }

    public double GetDensity(DateTime time) {
        return _mixtureCoefficient.doubleValue() / (60 * 60 * 24 * 365);
    }

    public String GetGNUPlot(String millTimeSecondsExpr) {
        return _mixtureCoefficient.toString() + "/(60*60*24*366)";
    }
}
