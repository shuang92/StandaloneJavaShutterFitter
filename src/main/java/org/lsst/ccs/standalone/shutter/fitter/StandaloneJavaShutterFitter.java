package org.lsst.ccs.standalone.shutter.fitter;

import java.io.IOException;
import java.net.URL;
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

//        EncoderSample es = new EncoderSample(CCSTimeStamp.currentTime(), 50);
//        HallTransition ht = new HallTransition(CCSTimeStamp.currentTime(), 6, 60, true);
//        MotionDone md
//                = new MotionDone.Builder()
//                        .side(ShutterSide.MINUSX)
//                        .startPosition(0)
//                        .targetPosition(100)
//                        .startTime(CCSTimeStamp.currentTime())
//                        .targetDuration(Duration.ofMillis(900))
//                        .endPosition(100)
//                        .actualDuration(Duration.ofMillis(999))
//                        .encoderSamples(Collections.singletonList(es))
//                        .hallTransitions(Collections.singletonList(ht))
//                        .build();
//        objectMapper.writeValue(new File("target/car.json"), md);

        // TODO: Replace this with code which reads from .ser file
        URL json = StandaloneJavaShutterFitter.class.getResource("/shutterMotion_1.json");
        MotionDone md = readerWriter.read(json);
        System.out.println(md);
        
        for (EncoderSample es : md.encoderSamples()) {
            System.out.printf("EncoderSample: %d %g\n", es.getTime().getUTCInstant().toEpochMilli() , es.getPosition());
        }

        for (HallTransition ht : md.hallTransitions()) {
            System.out.printf("EncoderSample: %d %g %d %s\n", ht.getTime().getUTCInstant().toEpochMilli() , ht.getPosition(), ht.getSensorId(), ht.isOn());
        }
    }
}
