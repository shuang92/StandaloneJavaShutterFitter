package org.lsst.ccs.standalone.shutter.fitter.json;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import org.lsst.ccs.utilities.taitime.CCSTimeStamp;

/**
 *
 * @author tonyj
 */
public class CCSTimeStampDeserializer extends StdDeserializer<CCSTimeStamp> {

    DateTimeFormatter sdf = DateTimeFormatter.ISO_INSTANT;

    public CCSTimeStampDeserializer() {
        super(CCSTimeStamp.class);
    }

    @Override
    public CCSTimeStamp deserialize(JsonParser jp, DeserializationContext dc) throws IOException, JacksonException {
        JsonNode node = jp.getCodec().readTree(jp);
        String utc = node.get("utc").asText();
        TemporalAccessor accessor = sdf.parse(utc);
        Instant instant = Instant.from(accessor);
        return CCSTimeStamp.currentTimeFromMillis(instant.toEpochMilli());
    }

}
