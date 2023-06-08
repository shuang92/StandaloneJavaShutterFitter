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

        // Convert times to seconds since startTime
        double[] times_physical_hall = md.hallTransitions().stream().mapToDouble(ht -> (ht.getTime().getUTCInstant().toEpochMilli() - startTime) / 1000.0).toArray();
	double[] times_physical_encd = md.encoderSamples().stream().mapToDouble(es -> (es.getTime().getUTCInstant().toEpochMilli() - startTime) / 1000.0).toArray();

	double[] positions_physical_hall = md.hallTransitions().stream().mapToDouble(ht -> ht.getPosition()).toArray();
	double[] positions_physical_encd = md.encoderSamples().stream().mapToDouble(es -> es.getPosition()).toArray();


	// append the last data point from MotionDone meta data
	times_physical_hall = Arrays.copyOf(times_physical_hall, times_physical_hall.length+1);
	times_physical_encd = Arrays.copyOf(times_physical_encd, times_physical_encd.length+1);

	positions_physical_hall = Arrays.copyOf(positions_physical_hall, positions_physical_hall.length+1);
	positions_physical_encd = Arrays.copyOf(positions_physical_encd, positions_physical_encd.length+1);

	times_physical_hall[times_physical_hall.length-1] = actualDuration;
	times_physical_encd[times_physical_encd.length-1] = actualDuration;

	positions_physical_hall[positions_physical_hall.length-1] = endPosition;
	positions_physical_encd[positions_physical_encd.length-1] = endPosition;

	// normalize the data to [0,1]
	int dataNumberHall = times_physical_hall.length;
	int dataNumberEncoder = times_physical_encd.length;

	double[] times_scaled_hall = new double[dataNumberHall];
	double[] times_scaled_encd = new double[dataNumberEncoder];

	double[] positions_scaled_hall = new double[dataNumberHall];
	double[] positions_scaled_encd = new double[dataNumberEncoder];

	final List<WeightedObservedPoint> points_hall = new ArrayList<>();
	final List<WeightedObservedPoint> points_encd = new ArrayList<>();

        for (int i = 0; i < dataNumberHall; i++) {
		times_scaled_hall[i] = times_physical_hall[i] / actualDuration;
		if (direction.equals("+")) {
			positions_scaled_hall[i] = (positions_physical_hall[i] - startPosition) / 750;
		} else if (direction.equals("-")) {
			positions_scaled_hall[i] = (startPosition - positions_physical_hall[i]) / 750;
		}

		points_hall.add(new WeightedObservedPoint(1.0, times_scaled_hall[i], positions_scaled_hall[i]));
	}

        for (int i = 0; i < dataNumberEncoder; i++) {
		times_scaled_encd[i] = times_physical_encd[i] / actualDuration;
		if (direction.equals("+")) {
			positions_scaled_encd[i] = (positions_physical_encd[i] - startPosition) / 750;
		} else if (direction.equals("-")) {
			positions_scaled_encd[i] = (startPosition - positions_physical_encd[i]) / 750;
		}

		points_encd.add(new WeightedObservedPoint(1.0, times_scaled_encd[i], positions_scaled_encd[i]));
	}

        // Use SimpleCurveFitter and ParametricUnivariateFunction to fit shutter motion
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
        SimpleCurveFitter curveFitter = SimpleCurveFitter.create(model, startPoint_scaled);

        double[] fit_scaled_hall = curveFitter.fit(points_hall);
        double[] fit_scaled_encd = curveFitter.fit(points_encd);

	double[] fit_physical_hall = new double[fit_scaled_hall.length];
	double[] fit_physical_encd = new double[fit_scaled_encd.length];

	fit_physical_hall[0] = fit_scaled_hall[0] * actualDuration;
	fit_physical_hall[1] = fit_scaled_hall[1] * actualDuration;
	fit_physical_hall[2] = fit_scaled_hall[2] * actualDuration;
	fit_physical_hall[3] = fit_scaled_hall[3] * 750 / Math.pow(actualDuration, 3);
	fit_physical_hall[4] = fit_scaled_hall[4] * 750 / Math.pow(actualDuration, 3);
	fit_physical_hall[5] = fit_scaled_hall[5] * 750 / Math.pow(actualDuration, 3);

	fit_physical_encd[0] = fit_scaled_encd[0] * actualDuration;
	fit_physical_encd[1] = fit_scaled_encd[1] * actualDuration;
	fit_physical_encd[2] = fit_scaled_encd[2] * actualDuration;
	fit_physical_encd[3] = fit_scaled_encd[3] * 750 / Math.pow(actualDuration, 3);
	fit_physical_encd[4] = fit_scaled_encd[4] * 750 / Math.pow(actualDuration, 3);
	fit_physical_encd[5] = fit_scaled_encd[5] * 750 / Math.pow(actualDuration, 3);

        System.out.println("Fit result Hall Sensor: "+Arrays.toString(fit_physical_hall));
        System.out.println("Fit result Motor Encoder: "+Arrays.toString(fit_physical_encd));

        System.out.println("====================================== Hall Sensor Data");
        System.out.printf("HallTransition: %s %s %s %s %s %s\n", "[scaled] time", "positions", "model;", "[physical] time", "positions", "model");
        for (int i = 0; i < dataNumberHall; i++) {
            double p_scaled_hall = model.value(times_scaled_hall[i], fit_scaled_hall);
            double p_physical_hall = model.value(times_physical_hall[i], fit_physical_hall);
            System.out.printf("HallTransition: %g %g %g; %g %g %g\n", times_scaled_hall[i], positions_scaled_hall[i], p_scaled_hall, 
									times_physical_hall[i], positions_physical_hall[i], p_physical_hall);
        }   

        System.out.println("====================================== Motor Encoder Data");
        System.out.printf("EncoderSample: %s %s %s %s %s %s\n", "[scaled] time", "positions", "model;", "[physical] time", "positions", "model");
        for (int i = 0; i < dataNumberEncoder; i++) {
            double p_scaled_encd = model.value(times_scaled_encd[i], fit_scaled_encd);
            double p_physical_encd = model.value(times_physical_encd[i], fit_physical_encd);
            System.out.printf("EncoderSample: %g %g %g; %g %g %g\n", times_scaled_encd[i], positions_scaled_encd[i], p_scaled_encd, 
									times_physical_encd[i], positions_physical_encd[i], p_physical_encd);
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
