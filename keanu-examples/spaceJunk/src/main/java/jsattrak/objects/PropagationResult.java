package jsattrak.objects;

import lombok.Value;

@Value
public class PropagationResult {

    double[] positionTEMEInKm;
    double[] velocityTEMEInKm;
    double[] latLonAltitudeInMeters;

}
