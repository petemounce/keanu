package jsattrak.tle;


import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import name.gano.astro.propogators.sgp4_cssi.SGP4SatData;

public class TLELibrary {

    @Getter
    private Map<Integer, SGP4SatData> library = new HashMap<>();

    public void update() {
        Map<Integer, SGP4SatData> libraryUpdate = TLEDownloader.loadAllTLEs(CelestrackTLELibrary.fileUrls);
        mergeUpdate(libraryUpdate);
    }

    public void loadFile(String fileName) throws IOException {
        Map<Integer, SGP4SatData> libraryUpdate = TLEDownloader.loadAllTLEs(fileName);
        mergeUpdate(libraryUpdate);
    }

    private void mergeUpdate(Map<Integer, SGP4SatData> libraryUpdate) {
        for (Map.Entry<Integer, SGP4SatData> updatedData : libraryUpdate.entrySet()) {

            library.merge(updatedData.getKey(), updatedData.getValue(),
                (valA, valB) -> valA.elnum > valB.elnum ? valA : valB
            );
        }
    }


}
