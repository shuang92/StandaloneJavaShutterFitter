package org.lsst.ccs.standalone.shutter.fitter.json;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.lsst.ccs.subsystem.shutter.status.MotionDone;

/**
 *
 * @author tonyj
 */
@JsonAutoDetect(fieldVisibility = Visibility.ANY, getterVisibility = Visibility.NONE)
@JsonDeserialize(builder = MotionDone.Builder.class)
abstract class MotionDoneMixin {
////    @JsonProperty("_side")
////    public abstract MotionDone.Builder side(final ShutterSide side);
//    @JsonProperty("_startTime")
//    public abstract MotionDone.Builder startTime(final ShutterSide side);

}
