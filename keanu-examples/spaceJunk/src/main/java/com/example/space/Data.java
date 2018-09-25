package com.example.coal;

import java.util.Map;

import io.improbable.keanu.util.csv.CsvReader;
import io.improbable.keanu.util.csv.ReadCsv;

public class Data {

    public Data(Map<Integer, Integer> yearToDisasterData) {

    }

    public static Data load(String fileName) {
        //Load a csv file from src/main/resources
        CsvReader csvReader = ReadCsv.fromResources(fileName).expectHeader(false);

        return null;
    }

}
