package edu.stanford.nlp.time.distributed;

import edu.stanford.nlp.util.DateUtil;
import edu.stanford.nlp.util.ModuloMathUtils;
import org.joda.time.DateTime;

public class AnnualNormalDistribution implements ITimeDensityFunction {

    Number _mixture;
    Number _meanSecondsMillTime;
    Number _standardDeviationSeconds;
    final double _k;

    public AnnualNormalDistribution(Number mixtureCoefficient, Number meanSeconds, Number standardDeviationSeconds) {
        _mixture = mixtureCoefficient;
        _meanSecondsMillTime = meanSeconds;
        _standardDeviationSeconds = standardDeviationSeconds;
        _k = 1.0 / Math.sqrt(2 * Math.PI
                * Math.pow(_standardDeviationSeconds.intValue(), 2));
    }

    @Override
    public double GetDensity(DateTime time) {

        DateTime mean = DateUtil.FromMilleniumTime(_meanSecondsMillTime.intValue());
        DateTime meanInSameYear = mean.year().setCopy(time.getYear());

        double distanceSeconds = ModuloMathUtils.distUndermod(
                meanInSameYear.getMillis() / 1000,
                time.getMillis() / 1000,
                60 * 60 * 24 * 365);

        return _k * _mixture.doubleValue() * Math.exp(
                -Math.pow(distanceSeconds, 2)
                / (2 * Math.pow(_standardDeviationSeconds.doubleValue(), 2)));
    }

    @Override
    public String GetGNUPlot(String millTimeSecondsExpr) {
        String equation = _mixture + "/("
                + "sqrt(" + _standardDeviationSeconds + "*2*pi))"
                + "*exp ( - dsyear( x , "
                + _meanSecondsMillTime + ")**2 / (2 * " + _standardDeviationSeconds + "**2) )";

        return equation;
    }
}
