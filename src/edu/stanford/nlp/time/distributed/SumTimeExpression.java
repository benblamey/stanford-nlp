package edu.stanford.nlp.time.distributed;

import java.util.ArrayList;
import org.joda.time.DateTime;

public class SumTimeExpression implements ITimeDensityFunction {

    public SumTimeExpression(ITimeDensityFunction left, ITimeDensityFunction right) {
        _models.add(left);
        _models.add(right);
    }
    ArrayList<ITimeDensityFunction> _models = new ArrayList<ITimeDensityFunction>();

    public SumTimeExpression(Object... models) {
        for (Object obj : models) {
            ITimeDensityFunction obj2 = (ITimeDensityFunction) obj;
            _models.add(obj2);
        }
    }

    public double GetDensity(DateTime time) {
        double density = 1;
        for (ITimeDensityFunction f : _models) {
            density += f.GetDensity(time);
        }
        return density;
    }

    public String GetGNUPlot(String millTimeSecondsExpr) {

        String expr = "(";

        for (int i = 0; i < _models.size(); i++) {
            String expr_i = "(" + _models.get(i).GetGNUPlot(millTimeSecondsExpr) + ")";

            if (i < _models.size() - 1) {
                expr_i += "  +  ";
            }

            expr += expr_i;
        }

        expr += ")";

        return expr;
    }
    
}
