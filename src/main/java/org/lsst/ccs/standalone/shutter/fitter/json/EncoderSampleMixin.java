package org.lsst.ccs.standalone.shutter.fitter.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.lsst.ccs.utilities.taitime.CCSTimeStamp;

/**
 *
 * @author tonyj
 */
abstract class EncoderSampleMixin {
    @JsonCreator
    public EncoderSampleMixin(@JsonProperty("time") CCSTimeStamp time, @JsonProperty("position") double position) {}
        
}
