package edu.stanford.nlp.time.distributed;

import edu.stanford.nlp.time.Time;
import java.util.ArrayList;
import java.util.List;
import org.joda.time.DateTime;

public class IntersectTimeExpression extends TimeDensityFunction {

    ArrayList<TimeDensityFunction> _models = new ArrayList<TimeDensityFunction>();
    ArrayList<Time> _debuggingModels = new ArrayList<Time>();
    
    public IntersectTimeExpression() {
    }
    
    public IntersectTimeExpression(Time left, Time right) {
        _debuggingModels.add(left);
        _models.add(left.getTimeExpression());
        _debuggingModels.add(right);
        _models.add(right.getTimeExpression());
    }

    public IntersectTimeExpression(Time... models) {
        for (Object obj : models) {
            Time obj2 = (Time) obj;
            _debuggingModels.add(obj2);
            _models.add(obj2.getTimeExpression());
        }
    }
    
    public IntersectTimeExpression(TimeDensityFunction... models) {
        for (TimeDensityFunction obj : models) {
            _models.add(obj);
        }
    }
    
    public IntersectTimeExpression(List<TimeDensityFunction> models) {
        for (TimeDensityFunction obj : models) {
            _models.add(obj);
        }
    }


    
//    public IntersectTimeExpression(List<CanExpressTimeAsFunction> models) {
//        for (Object obj : models) {
//            CanExpressTimeAsFunction obj2 = (CanExpressTimeAsFunction) obj;
//            _debuggingModels.add(obj2);
//            _models.add(obj2.getTimeDensityFunction());
//        }
//    }

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
