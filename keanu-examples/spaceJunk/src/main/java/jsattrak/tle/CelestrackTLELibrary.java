package jsattrak.tle;


import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CelestrackTLELibrary {

    private static final String rootWeb = "http://celestrak.com/NORAD/elements/";

    // names of all TLE files to update
    private static final String[] fileNames = new String[]{
        "sts.txt",
        "stations.txt",
        "tle-new.txt",
        "weather.txt",
        "noaa.txt",
        "goes.txt",
        "resource.txt",
        "sarsat.txt",
        "dmc.txt",
        "tdrss.txt",
        "geo.txt",
        "intelsat.txt",
        "gorizont.txt",
        "raduga.txt",
        "molniya.txt",
        "iridium.txt",
        "iridium-next.txt",
        "orbcomm.txt",
        "globalstar.txt",
        "amateur.txt",
        "x-comm.txt",
        "other-comm.txt",
        "gps-ops.txt",
        "glo-ops.txt",
        "galileo.txt",
        "sbas.txt",
        "nnss.txt",
        "musson.txt",
        "science.txt",
        "geodetic.txt",
        "engineering.txt",
        "education.txt",
        "military.txt",
        "radar.txt",
        "cubesat.txt",
        "other.txt",
        "iridium-33-debris.txt",
        "cosmos-2251-debris.txt",
        "1999-025.txt" // FENGYUN Debris
    };

    //Concat filenames to url
    public static final List<String> fileUrls = Arrays.stream(fileNames)
        .map(fileName -> rootWeb + fileName)
        .collect(Collectors.toList());
}
