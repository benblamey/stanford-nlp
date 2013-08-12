package edu.stanford.nlp.util;

import java.util.List;

/**
 * Various helper methods for doing modulo arithmetic.
 * 
 * @author Ben Blamey (blamey.ben@gmail.com)
 */
public class ModuloMathUtils {
    
    public static double distUndermod(double a, double b, double mod) {
        
        if (mod < 0) {
            throw new IllegalArgumentException("mod must be positive");
        }
        
        // compute difference in each direction.
        double diff_x = a-b;
        double diff_y = b-a;
        
        // take modulo.
        diff_x = diff_x % mod;
        diff_y = diff_y % mod;
        
        // ensure +ve.
        if (diff_x < 0) {
            diff_x += mod;
        }
        if (diff_y < 0) {
            diff_y += mod;
        }
        
        return Math.min(diff_x, diff_y);
    }
    
    public static double standardDeviationUnderModulo(List<Double> values, double mean, double mod) {
        
        double total = 0;
        int n = 0;
        
        for (Double x : values) {
            double distSeconds = ModuloMathUtils.distUndermod(x, mean, mod);
            total += distSeconds * distSeconds;
            n++;
        }     
        
        return Math.sqrt(total/n);
    }

}
