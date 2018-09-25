package com.example.coal;

public class Model {

    public static void main(String[] args) {

        System.out.println("Loading data from a csv file");
        Data data = Data.load("data.csv");

        System.out.println("Creating model using loaded data");
        Model coalMiningDisastersModel = new Model(data);

        System.out.println("Running model...");
        coalMiningDisastersModel.run();
        System.out.println("Run complete");

    }

    public Model(Data data) {

    }

    /**
     * Runs the MetropolisHastings algorithm and saves the resulting samples to results
     */
    public void run() {

    }


}
