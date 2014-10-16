package edu.stanford.nlp.time.distributed;

import org.joda.time.DateTime;

public class AnnualUniformDistribution extends TimeDensityFunction {

    private Number _mixtureCoefficient;

    public AnnualUniformDistribution(Number mixtureCoefficient) {
        _mixtureCoefficient = mixtureCoefficient;
    }

    @Override
    public double getDensity(DateTime time) {
        return _mixtureCoefficient.doubleValue() / (60 * 60 * 24 * 365);
    }

    @Override
    public String getGNUPlot(String millTimeSecondsExpr) {
        return _mixtureCoefficient.toString() + "/(60*60*24*366)";
    }
}
