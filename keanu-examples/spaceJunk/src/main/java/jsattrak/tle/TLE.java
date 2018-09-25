/*
 * TLE.java
 *=====================================================================
 *   This file is part of JSatTrak.
 *
 *   Copyright 2007-2013 Shawn E. Gano
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * =====================================================================
 * Created on July 24, 2007, 3:02 PM
 *
 * Contains a Two Line Element (Norad) for a satellite
 */

package jsattrak.tle;

import name.gano.astro.propogators.sgp4_cssi.SGP4SatData;
import name.gano.astro.propogators.sgp4_cssi.SGP4unit;
import name.gano.astro.propogators.sgp4_cssi.SGP4utils;

/**
 * @author ganos
 */
public class TLE {
    String line0 = ""; // name line
    String line1 = ""; // first line
    String line2 = ""; // second line

    public TLE(String name, String l1, String l2) {
        line0 = name;
        line1 = l1;
        line2 = l2;
    }

    public String getSatName() {
        return line0;
    }

    public String getLine1() {
        return line1;
    }

    public String getLine2() {
        return line2;
    }

    public SGP4SatData toSGP4SatData() {
        SGP4SatData sgp4SatData = new SGP4SatData();
        char opsmode = SGP4utils.OPSMODE_IMPROVED; // OPSMODE_IMPROVED
        SGP4unit.Gravconsttype gravconsttype = SGP4unit.Gravconsttype.wgs72;

        // load TLE data as strings and INI all SGP4 data
        SGP4utils.readTLEandIniSGP4(getSatName(), line1, line2, opsmode, gravconsttype, sgp4SatData);

        return sgp4SatData;
    }
}
