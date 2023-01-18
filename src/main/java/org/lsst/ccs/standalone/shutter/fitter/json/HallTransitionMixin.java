package org.lsst.ccs.standalone.shutter.fitter.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.lsst.ccs.utilities.taitime.CCSTimeStamp;

/**
 *
 * @author tonyj
 */
abstract class HallTransitionMixin {
    @JsonCreator
    public HallTransitionMixin(@JsonProperty("time") CCSTimeStamp time, @JsonProperty("sensorId") int sensor, @JsonProperty("position") double position, @JsonProperty("on") boolean on) {}
        
}
