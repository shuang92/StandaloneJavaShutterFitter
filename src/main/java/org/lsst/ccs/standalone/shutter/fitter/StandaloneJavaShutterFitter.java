package org.lsst.ccs.standalone.shutter.fitter;

import java.lang.reflect.*;
import java.lang.Math;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.math3.analysis.ParametricUnivariateFunction;
import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.SimpleCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoint;
import org.apache.commons.math3.fitting.WeightedObservedPoints;
import org.lsst.ccs.standalone.shutter.fitter.json.MotionDoneReaderWriter;
import org.lsst.ccs.subsystem.shutter.common.EncoderSample;
import org.lsst.ccs.subsystem.shutter.common.HallTransition;
import org.lsst.ccs.subsystem.shutter.status.MotionDone;

/**
 *
 * @author tonyj
 */
public class StandaloneJavaShutterFitter {

    public static void main(String[] args) throws IOException {

        MotionDoneReaderWriter readerWriter = new MotionDoneReaderWriter();

        // TODO: Replace this with code which reads from .ser file
        URL json = StandaloneJavaShutterFitter.class.getResource("/shutterMotion.json");
        MotionDone md = readerWriter.read(json);
        System.out.println(md);

	long startTime = md.startTime().getUTCInstant().toEpochMilli();
	double actualDuration = md.actualDuration().getSeconds() + md.actualDuration().getNano()*1e-9;

	double startPosition = md.startPosition();
	double endPosition = md.endPosition();

	String direction = "x";
	if (startPosition < endPosition) {
		direction = "+";
	} else if (startPosition > endPosition) {
		direction = "-";
	}
	System.out.println(direction);

        // Lets do a simple fit to the hall sensors
        // See: https://commons.apache.org/proper/commons-math/userguide/fitting.html
        // See: https://commons.apache.org/proper/commons-math/javadocs/api-3.6.1/overview-summary.html
        final WeightedObservedPoints obs = new WeightedObservedPoints();
        for (HallTransition ht : md.hallTransitions()) {
            obs.add(ht.getTime().getUTCInstant().toEpochMilli(), ht.getPosition());
        }

        final PolynomialCurveFitter fitter = PolynomialCurveFitter.create(3);
        final double[] coeff = fitter.fit(obs.toList());
        System.out.println(Arrays.toString(coeff));

        // Try a better example using SimpleCurveFitter and ParametricUnivariateFunction
        double t0p = 0.00;
        double t1p = 0.225;
        double t2p = 0.675;
        double j0p = 30000;
        double j1p = -30000;
        double j2p = 30000;
        double[] startPoint_physical = {t0p, t1p, t2p, j0p, j1p, j2p};

        double t0s = 0.00 / actualDuration;
        double t1s = 0.225 / actualDuration;
        double t2s = 0.675 / actualDuration;
        double j0s = 30000 * Math.pow(actualDuration, 3) / 750;
        double j1s = -30000 * Math.pow(actualDuration, 3) / 750;
        double j2s = 30000 * Math.pow(actualDuration, 3) / 750;
        double[] startPoint_scaled = {t0s, t1s, t2s, j0s, j1s, j2s};

        SixParameterModel model = new SixParameterModel();

        // Convert times to seconds since startTime
        double[] times_physical = md.hallTransitions().stream().mapToDouble(ht -> (ht.getTime().getUTCInstant().toEpochMilli() - startTime) / 1000.0).toArray();
	double[] positions_physical = md.hallTransitions().stream().mapToDouble(ht -> ht.getPosition()).toArray();

	// append the last data point from MotionDone meta data
	times_physical = Arrays.copyOf(times_physical, times_physical.length+1);
	positions_physical = Arrays.copyOf(positions_physical, positions_physical.length+1);

	times_physical[times_physical.length-1] = actualDuration;
	positions_physical[positions_physical.length-1] = endPosition;

	// normalize the data to [0,1]
	int dataNumber = times_physical.length;
	double[] times_scaled = new double[dataNumber];
	double[] positions_scaled = new double[dataNumber];

        for (int i = 0; i < dataNumber; i++) {
		times_scaled[i] = times_physical[i] / actualDuration;
		if (direction.equals("+")) {
			positions_scaled[i] = (positions_physical[i] - startPosition) / 750;
		} else if (direction.equals("-")) {
			positions_scaled[i] = (startPosition - positions_physical[i]) / 750;
		}
	}

        final List<WeightedObservedPoint> points = new ArrayList<>();

        System.out.println("====================================== Start point");
        System.out.printf("HallTransition: %s %s %s %s %s %s\n", "scaled time", "positions", "model;", "physical time", "positions", "model");
        for (int i = 0; i < dataNumber; i++) {
            double p_scaled = model.value(times_scaled[i], t0s, t1s, t2s, j0s, j1s, j2s);
            double p_physical = model.value(times_physical[i], t0p, t1p, t2p, j0p, j1p, j2p);
            System.out.printf("HallTransition: %g %g %g; %g %g %g\n", times_scaled[i], positions_scaled[i], p_scaled, 
									times_physical[i], positions_physical[i], p_physical);
            points.add(new WeightedObservedPoint(1.0, times_scaled[i], positions_scaled[i]));
        }
        System.out.printf("grad: %s\n", Arrays.toString(model.gradient(0.5, startPoint_scaled)));

        SimpleCurveFitter curveFitter = SimpleCurveFitter.create(model, startPoint_scaled);
        double[] fit_scaled = curveFitter.fit(points);
	double[] fit_physical = new double[fit_scaled.length];
	fit_physical[0] = fit_scaled[0] * actualDuration;
	fit_physical[1] = fit_scaled[1] * actualDuration;
	fit_physical[2] = fit_scaled[2] * actualDuration;
	fit_physical[3] = fit_scaled[3] * 750 / Math.pow(actualDuration, 3);
	fit_physical[4] = fit_scaled[4] * 750 / Math.pow(actualDuration, 3);
	fit_physical[5] = fit_scaled[5] * 750 / Math.pow(actualDuration, 3);

        System.out.println("Fit result: "+Arrays.toString(fit_physical));
        System.out.println("====================================== After fit");
        System.out.printf("HallTransition: %s %s %s %s %s %s\n", "scaled time", "positions", "model;", "physical time", "positions", "model");
        for (int i = 0; i < times_physical.length; i++) {
            double p_scaled = model.value(times_scaled[i], fit_scaled);
            double p_physical = model.value(times_physical[i], fit_physical);
            System.out.printf("HallTransition: %g %g %g; %g %g %g\n", times_scaled[i], positions_scaled[i], p_scaled, 
									times_physical[i], positions_physical[i], p_physical);
        }   
    }

    private static class SixParameterModel implements ParametricUnivariateFunction {

        @Override
        public double value(double t, double... parameters) {
            // Value of function from https://www.overleaf.com/project/637813358c20c4b04c7ff190
            // Probably wrong (seems like t0 is not really used??)
            double t0 = parameters[0];
            double t1 = parameters[1];
            double t2 = parameters[2];
            double j0 = parameters[3];
            double j1 = parameters[4];
            double j2 = parameters[5];

//            if (t < t0) {
//                return 0;
//           } else

	    t = t - t0;
	    if (t < t1) {
                return j0 * t * t * t / 6;
            } else {
                double A1 = (j0 - j1) * t1;
                double V1 = (j0 - j1) * t1 * t1 / 2 - A1 * t1;
                double S1 = (j0 - j1) * t1 * t1 * t1 / 6 - A1 * t1 * t1 / 2 - V1 * t1;
                if (t < t2) {
                    return j1 * t * t * t / 6 + A1 * t * t / 2 + V1 * t + S1;
                } else {
                    double A2 = A1 + (j1 - j2) * t2;
                    double V2 = (j1 - j2) * t2 * t2 / 2 + (A1 - A2) * t2 + V1;
                    double S2 = (j1 - j2) * t2 * t2 * t2 / 6 + (A1 - A2) * t2 * t2 / 2 + (V1 - V2) * t2 + S1;
                    return j2 * t * t * t / 6 + A2 * t * t / 2 + V2 * t + S2;
                }
            }
        }

        @Override
        public double[] gradient(double t, double... parameters) {
            // Extremely naive gradient computation. Would be better to have analytical gradients
            double delta = 0.001;
            double[] result = new double[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                double original = parameters[i];
                parameters[i] = original - delta;
                double v1 = value(t, parameters);
                parameters[i] = original + delta;
                double v2 = value(t, parameters);
                parameters[i] = original;
                result[i] = (v2 - v1) / (2 * delta);
            }
            return result;
        }
    }
}
