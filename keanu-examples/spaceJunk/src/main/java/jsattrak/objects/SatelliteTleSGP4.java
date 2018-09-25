package jsattrak.objects;

import name.gano.astro.AstroConst;
import name.gano.astro.GeoFunctions;
import name.gano.astro.coordinates.J2kCoordinateConversion;
import name.gano.astro.propogators.sgp4_cssi.SGP4SatData;
import name.gano.astro.propogators.sgp4_cssi.SGP4unit;

/**
 * @author ganos
 */
public class SatelliteTleSGP4 {

    public static PropagationResult propogate2JulDate(SGP4SatData sgp4SatData, double julDate) {

        double[] posTEME = new double[3];  // true-equator, mean equinox TEME of date position for LLA calcs, meters
        double[] velTEME = new double[3]; // meters/sec

        // using JulDate because function uses time diff between jultDate of ephemeris, SGP4 uses UTC
        // propogate satellite to given date - saves result in TEME to posTEME and velTEME in km, km/s
        boolean propSuccess = SGP4unit.sgp4Prop2JD(sgp4SatData, julDate, posTEME, velTEME);
        if (!propSuccess) {
            System.out.println("Error SGP4 Propagation failed for sat: " + sgp4SatData.name + ", JD: " + sgp4SatData.jdsatepoch + ", error code: " + sgp4SatData.error);
            System.out.println(sgp4SatData.name);
            System.out.println(sgp4SatData.line1);
            System.out.println(sgp4SatData.line2);
        }

//        // scale output to meters
//        for (int i = 0; i < 3; i++) {
//            // TEME
//            posTEME[i] = posTEME[i] * 1000.0;
//            velTEME[i] = velTEME[i] * 1000.0;
//        }

        // SEG - 11 June 2009 -- new information (to me) on SGP4 propogator coordinate system:
        // SGP4 output is in true equator and mean equinox (TEME) of Date *** note some think of epoch, but STK beleives it is of date from tests **
        // It depends also on the source for the TLs if from the Nasa MCC might be MEME but most US Gov - TEME
        // Also the Lat/Lon/Alt calculations are based on TEME (of Date) so that is correct as it was used before!
        // References:
        // http://www.stk.com/pdf/STKandSGP4/STKandSGP4.pdf  (STK's stance on SGP4)
        // http://www.agi.com/resources/faqSystem/files/2144.pdf  (newer version of above)
        // http://www.satobs.org/seesat/Aug-2004/0111.html
        // http://celestrak.com/columns/v02n01/ "Orbital Coordinate Systems, Part I" by Dr. T.S. Kelso
        // http://en.wikipedia.org/wiki/Earth_Centered_Inertial
        // http://ccar.colorado.edu/asen5050/projects/projects_2004/aphanuphong/p1.html  (bad coefficients? conversion between TEME and J2000 (though slightly off?))
        //  http://www.centerforspace.com/downloads/files/pubs/AIAA-2000-4025.pdf
        // http://celestrak.com/software/vallado-sw.asp  (good software)


        // get position information back out - convert to J2000 (does TT time need to be used? - no)
        //j2kPos = CoordinateConversion.EquatorialEquinoxToJ2K(mjd, sdp4Prop.itsR); //julDate-2400000.5
        //j2kVel = CoordinateConversion.EquatorialEquinoxToJ2K(mjd, sdp4Prop.itsV);
        // based on new info about coordinate system, to get the J2K other conversions are needed!
        // precession from rk5 -> mod
        double mjd = julDate - AstroConst.JDminusMJD;
        double ttt = (mjd - AstroConst.MJD_J2000) / 36525.0;
        double[][] A = J2kCoordinateConversion.teme_j2k(J2kCoordinateConversion.Direction.to, ttt, 24, 2, 'a');
        // rotate position and velocity
        double[] j2kPosMeters = J2kCoordinateConversion.matvecmult(A, posTEME);
        double[] j2kVelMetersPerSec = J2kCoordinateConversion.matvecmult(A, velTEME);

        // calculate Lat,Long,Alt - must use Mean of Date (MOD) Position
        // lat,long,alt  [radians, radians, m ]
        double[] latLonAltitudeInMeters = GeoFunctions.GeodeticLLA(posTEME, julDate - AstroConst.JDminusMJD); // j2kPos

        return new PropagationResult(posTEME, velTEME, latLonAltitudeInMeters);
    }


}
