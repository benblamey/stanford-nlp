package edu.stanford.nlp.time.distributed;

import java.util.ArrayList;
import java.util.Collection;
import org.joda.time.DateTime;

public class SumTimeExpression extends TimeDensityFunction {

    public SumTimeExpression(TimeDensityFunction left, TimeDensityFunction right) {
        if (left == null) {
                throw new RuntimeException("left cannot be null");
        }
        if (right == null) {
                throw new RuntimeException("right cannot be null");
        }
        _models.add(left);
        _models.add(right);
    }
    
    public SumTimeExpression(Collection<TimeDensityFunction> densities) {
        if (densities.contains(null)) {
                throw new RuntimeException("density cannot be null");
        }
        _models.addAll(densities);
    }
    
    ArrayList<TimeDensityFunction> _models = new ArrayList<TimeDensityFunction>();

    public SumTimeExpression(Object... models) {
        for (Object obj : models) {
            if (obj == null) {
                throw new RuntimeException("obj cannot be null");
            }
            TimeDensityFunction obj2 = (TimeDensityFunction) obj;
            _models.add(obj2);
        }
    }

    public double getDensity(DateTime time) {
        double density = 0.0;
        for (TimeDensityFunction f : _models) {
            density += f.getDensity(time) / f.getTotalMass();
        }
        return density;
    }

    public String getGNUPlot(String millTimeSecondsExpr) {

        String expr = "(";

        for (int i = 0; i < _models.size(); i++) {
            String expr_i = "(" + _models.get(i).getGNUPlot(millTimeSecondsExpr) + ")";

            if (i < _models.size() - 1) {
                expr_i += "  +  ";
            }

            expr += expr_i;
        }

        expr += ")";

        return expr;
    }
    
}
