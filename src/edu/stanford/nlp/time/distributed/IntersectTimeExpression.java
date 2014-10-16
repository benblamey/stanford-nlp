package edu.stanford.nlp.time.distributed;

import java.util.ArrayList;
import java.util.List;
import org.joda.time.DateTime;

public class IntersectTimeExpression extends TimeDensityFunction {

    ArrayList<TimeDensityFunction> _models = new ArrayList<TimeDensityFunction>();
    ArrayList<CanExpressTimeAsFunction> _debuggingModels = new ArrayList<CanExpressTimeAsFunction>();
    
    public IntersectTimeExpression(CanExpressTimeAsFunction left, CanExpressTimeAsFunction right) {
        _debuggingModels.add(left);
        _models.add(left.GettimeDensityFunction());
        _debuggingModels.add(right);
        _models.add(right.GettimeDensityFunction());
    }

    public IntersectTimeExpression(CanExpressTimeAsFunction... models) {
        for (Object obj : models) {
            CanExpressTimeAsFunction obj2 = (CanExpressTimeAsFunction) obj;
            _debuggingModels.add(obj2);
            _models.add(obj2.GettimeDensityFunction());
        }
    }
    
    public IntersectTimeExpression(List<CanExpressTimeAsFunction> models) {
        for (Object obj : models) {
            CanExpressTimeAsFunction obj2 = (CanExpressTimeAsFunction) obj;
            _debuggingModels.add(obj2);
            _models.add(obj2.GettimeDensityFunction());
        }
    }

    public double getDensity(DateTime time) {
        double density = 1;
        for (TimeDensityFunction f : _models) {
            density *= f.getDensity(time);
            if (density == 0) {
                break;
            }
        }
        return density;
    }

    public String getGNUPlot(String millTimeSecondsExpr) {

        String expr = "";

        for (int i = 0; i < _models.size(); i++) {
            String expr_i = "(" + _models.get(i).getGNUPlot(millTimeSecondsExpr) + ")";

            if (i < _models.size() - 1) {
                expr_i += "  *  ";
            }

            expr += expr_i;
        }

        return expr;
    }

}
