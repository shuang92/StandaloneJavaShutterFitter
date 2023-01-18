package org.lsst.ccs.standalone.shutter.fitter.json;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import org.lsst.ccs.subsystem.shutter.common.EncoderSample;
import org.lsst.ccs.subsystem.shutter.common.HallTransition;
import org.lsst.ccs.subsystem.shutter.status.MotionDone;
import org.lsst.ccs.utilities.taitime.CCSTimeStamp;

/**
 *
 * @author tonyj
 */
public class MotionDoneReaderWriter {

    private final ObjectMapper objectMapper;

    public MotionDoneReaderWriter() {
        objectMapper = new ObjectMapper();

        SimpleModule module
                = new SimpleModule("CustomMotionDoneSerializer", new Version(1, 0, 0, null, null, null));
        //module.addSerializer(MotionDone.class, new CustomMotionDoneSerializer());
        module.addSerializer(CCSTimeStamp.class, new CCSTimeStampSerializer());
        module.addDeserializer(CCSTimeStamp.class, new CCSTimeStampDeserializer());
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        objectMapper.registerModule(module);
        objectMapper.addMixIn(MotionDone.class, MotionDoneMixin.class);
        objectMapper.addMixIn(EncoderSample.class, EncoderSampleMixin.class);
        objectMapper.addMixIn(HallTransition.class, HallTransitionMixin.class);
    }

    public MotionDone read(URL url) throws IOException {
        return objectMapper.readValue(url, MotionDone.class);
    }

    public void write(File file, MotionDone md) throws IOException {
        objectMapper.writeValue(file, md);
    }
}
