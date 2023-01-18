package org.lsst.ccs.standalone.shutter.fitter.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import org.lsst.ccs.utilities.taitime.CCSTimeStamp;

/**
 *
 * @author tonyj
 */
public class CCSTimeStampSerializer extends StdSerializer<CCSTimeStamp> {
    
    DateTimeFormatter sdf = DateTimeFormatter.ISO_INSTANT;

    public CCSTimeStampSerializer() {
        super(CCSTimeStamp.class);
    }

    @Override
    public void serialize(CCSTimeStamp ts, JsonGenerator jg, SerializerProvider sp) throws IOException {
        jg.writeStartObject();
        jg.writeStringField("tai", sdf.format(ts.getTAIInstant()));
        jg.writeStringField("utc", sdf.format(ts.getUTCInstant()));
        jg.writeEndObject();
    }
    
}
