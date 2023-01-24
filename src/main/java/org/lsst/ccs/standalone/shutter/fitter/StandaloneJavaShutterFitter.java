package org.lsst.ccs.standalone.shutter.fitter;

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

        for (EncoderSample es : md.encoderSamples()) {
            System.out.printf("EncoderSample: %d %g\n", es.getTime().getUTCInstant().toEpochMilli(), es.getPosition());
        }

        for (HallTransition ht : md.hallTransitions()) {
            System.out.printf("HallTransition: %d %g %d %s\n", ht.getTime().getUTCInstant().toEpochMilli(), ht.getPosition(), ht.getSensorId(), ht.isOn());
        }

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
        double t0 = 0.0;
        double t1 = 0.3;
        double t2 = 0.6;
        double j0 = 30000;
        double j1 = -30000;
        double j2 = 30000;
        double[] startPoint = {t0, t1, t2, j0, j1, j2};

        SixParameterModel model = new SixParameterModel();
        long timeOffset = 1657846414500L;

        // Convert times to seconds since timeOffset
        double[] times = md.hallTransitions().stream().mapToDouble(ht -> (ht.getTime().getUTCInstant().toEpochMilli() - timeOffset) / 1000.0).toArray();
        double[] positions = md.hallTransitions().stream().mapToDouble(ht -> ht.getPosition()).toArray();
        final List<WeightedObservedPoint> points = new ArrayList<>();

        System.out.println("====================================== Start point");
        for (int i = 0; i < times.length; i++) {
            double p = model.value(times[i], t0, t1, t2, j0, j1, j2);
            System.out.printf("HallTransition: %g %g %g\n", times[i], positions[i], p);
            points.add(new WeightedObservedPoint(1.0, times[i], positions[i]));
        }
        System.out.printf("grad: %s\n", Arrays.toString(model.gradient(0.5, startPoint)));

        SimpleCurveFitter curveFitter = SimpleCurveFitter.create(model, startPoint);
        double[] fit = curveFitter.fit(points);
        System.out.println("Fit result: "+Arrays.toString(fit));
        System.out.println("====================================== After fit");
        for (int i = 0; i < times.length; i++) {
            double p = model.value(times[i], fit);
            System.out.printf("HallTransition: %g %g %g\n", times[i], positions[i], p);
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

            if (t < t0) {
                return 0;
            } else if (t < t1) {
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
