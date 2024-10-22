package com.specknet.pdiotapp.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;


public class RespeckAnalysis {

    // get the data from the training file
    RespeckData respeckData;

    // do something with the data
    public RespeckAnalysis(RespeckData respeckData) {
        this.respeckData = respeckData;
    }

    public List<List<String>> readCSV(String filePath) {
        List<List<String>> data = new ArrayList<>();
        try (Scanner sc = new Scanner(new File(filePath))) {
            sc.useDelimiter("\n");
            while (sc.hasNext())
                data.add(Arrays.asList(sc.next().split(",")));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return data;
    }

    public void analyseRespeckData(Arrays respeckData) {

    }


}
