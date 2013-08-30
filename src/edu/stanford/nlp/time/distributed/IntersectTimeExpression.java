package edu.stanford.nlp.time.distributed;

import java.util.ArrayList;
import org.joda.time.DateTime;

public class IntersectTimeExpression implements ITimeDensityFunction {

    public IntersectTimeExpression(CanExpressTimeAsFunction left, CanExpressTimeAsFunction right) {
        _models.add(left);
        _models.add(right);
    }
    ArrayList<CanExpressTimeAsFunction> _models = new ArrayList<CanExpressTimeAsFunction>();

    public IntersectTimeExpression(Object... models) {
        for (Object obj : models) {
            CanExpressTimeAsFunction obj2 = (CanExpressTimeAsFunction) obj;
            _models.add(obj2);
        }
    }

    public double GetDensity(DateTime time) {
        double density = 1;
        for (CanExpressTimeAsFunction f : _models) {
            density *= f.GettimeDensityFunction().GetDensity(time);
            if (density == 0) {
                break;
            }
        }
        return density;
    }

    public String GetGNUPlot(String millTimeSecondsExpr) {

        String expr = "";

        for (int i = 0; i < _models.size(); i++) {
            String expr_i = "(" + _models.get(i).GettimeDensityFunction().GetGNUPlot(millTimeSecondsExpr) + ")";

            if (i < _models.size() - 1) {
                expr_i += "  *  ";
            }

            expr += expr_i;
        }

        return expr;
    }

}
