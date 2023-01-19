package org.lsst.ccs.standalone.shutter.fitter;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import org.apache.commons.math3.fitting.PolynomialCurveFitter;
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
            System.out.printf("EncoderSample: %d %g\n", es.getTime().getUTCInstant().toEpochMilli() , es.getPosition());
        }

        for (HallTransition ht : md.hallTransitions()) {
            System.out.printf("EncoderSample: %d %g %d %s\n", ht.getTime().getUTCInstant().toEpochMilli() , ht.getPosition(), ht.getSensorId(), ht.isOn());
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
        
        // TODO: provide a better example using SimpleCurveFitter and ParametricUnivariateFunction
    }
}
