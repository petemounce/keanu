/**
 * =====================================================================
 * This file is part of JSatTrak.
 * <p>
 * Copyright 2007-2013 Shawn E. Gano
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =====================================================================
 */


package jsattrak.tle;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;

import name.gano.astro.propogators.sgp4_cssi.SGP4SatData;

public class TLEDownloader {

    public static Map<Integer, SGP4SatData> loadAllTLEs(String fileUrl) {
        return loadAllTLEs(ImmutableList.of(fileUrl));
    }

    public static Map<Integer, SGP4SatData> loadAllTLEs(List<String> fileUrls) {

        Map<Integer, SGP4SatData> tles = fileUrls.stream()
            .flatMap(fileUrl -> loadTLEsFromUrl(fileUrl).stream())
            .map(TLE::toSGP4SatData)
            .collect(Collectors.toMap(
                sgp4SatData -> sgp4SatData.satnum,
                sgp4SatData -> sgp4SatData,
                (dataA, dataB) -> dataA.elnum > dataB.elnum ? dataA : dataB
            ));

        System.out.println("Loading complete with " + tles.size() + " loaded.");

        return tles;
    }

    /**
     * downloads all the TLEs without stopping inbetween each file
     *
     * @return if all files were downloaded successfullyxz
     */
    public static List<TLE> loadTLEsFromUrl(final String fileUrl) {

        List<TLE> tlesFromFileUrl = new ArrayList<>();

        System.out.println("Loading " + fileUrl);
        try {
            // open file on the web
            URL url = new URL(fileUrl);
            URLConnection c = url.openConnection();
            InputStreamReader isr = new InputStreamReader(c.getInputStream());

            try (BufferedReader br = new BufferedReader(isr)) {
                String nameLine;
                while ((nameLine = br.readLine()) != null) {
                    String line1 = br.readLine();
                    String line2 = br.readLine();

                    tlesFromFileUrl.add(new TLE(nameLine, line1, line2));
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Loaded " + tlesFromFileUrl.size() + " TLEs");

        return tlesFromFileUrl;
    }
}
