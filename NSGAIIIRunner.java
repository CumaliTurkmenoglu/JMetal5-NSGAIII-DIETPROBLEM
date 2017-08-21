package org.uma.jmetal.runner.multiobjective;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.SQLException;
import java.text.DecimalFormat;
import org.uma.jmetal.algorithm.Algorithm;
import org.uma.jmetal.algorithm.multiobjective.nsgaiii.NSGAIIIBuilder;
import org.uma.jmetal.operator.CrossoverOperator;
import org.uma.jmetal.operator.MutationOperator;
import org.uma.jmetal.operator.SelectionOperator;
import org.uma.jmetal.operator.impl.selection.BinaryTournamentSelection;
import org.uma.jmetal.runner.AbstractAlgorithmRunner;
import org.uma.jmetal.util.AlgorithmRunner;
import org.uma.jmetal.util.JMetalException;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;

import java.util.List;
import org.uma.jmetal.operator.impl.crossover.PMXCrossover;
import org.uma.jmetal.operator.impl.mutation.PermutationSwapMutation;
import org.uma.jmetal.problem.multiobjective.MO_MKP2;
import org.uma.jmetal.solution.PermutationSolution;
import org.uma.jmetal.util.pseudorandom.RandomGenerator;

/**
 * Class to configure and run the NSGA-III algorithm
 */
public class NSGAIIIRunner extends AbstractAlgorithmRunner {

    /**
     * @param args Command line arguments.
     * @throws java.io.IOException
     * @throws SecurityException
     * @throws ClassNotFoundException Usage: three options -
     * org.uma.jmetal.runner.multiobjective.NSGAIIIRunner -
     * org.uma.jmetal.runner.multiobjective.NSGAIIIRunner problemName -
     * org.uma.jmetal.runner.multiobjective.NSGAIIIRunner problemName
     * paretoFrontFile
     */
    public static void main(String[] args) throws JMetalException, SQLException {
        MO_MKP2 problem = new MO_MKP2();
        Algorithm<List<PermutationSolution<Integer>>> algorithm;
        CrossoverOperator<PermutationSolution<Integer>> crossover;
        MutationOperator<PermutationSolution<Integer>> mutation;
        SelectionOperator<List<PermutationSolution<Integer>>, PermutationSolution<Integer>> selection;

        //String problemName = "org.uma.jmetal.problem.multiobjective.MO_MKP2";
        //      problem = ProblemUtils.loadProblem(problemName);
        //int id = problem.getFoodId(5);
        double crossoverProbability = 0.9;
        RandomGenerator random = new RandomGenerator() {

            @Override
            public Object getRandomValue() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        };
        double mutationProbability = 1.0 /100 ;//problem.getNumberOfVariables();
        crossover = new PMXCrossover(crossoverProbability, random);
        mutation = new PermutationSwapMutation(mutationProbability);

        selection = new BinaryTournamentSelection<PermutationSolution<Integer>>();

        algorithm = new NSGAIIIBuilder<>(problem)
                .setCrossoverOperator(crossover)
                .setMutationOperator(mutation)
                .setPopulationSize(100)
                .setSelectionOperator(selection)
                .setMaxIterations(500000)
                .build();

        AlgorithmRunner algorithmRunner = new AlgorithmRunner.Executor(algorithm)
                .execute();

        List<PermutationSolution<Integer>> population = algorithm.getResult();
        long computingTime = algorithmRunner.getComputingTime();

        new SolutionListOutput(population)
                .setSeparator("\t")
                .setVarFileOutputContext(new DefaultFileOutputContext("VAR.tsv"))
                .setFunFileOutputContext(new DefaultFileOutputContext("FUN.tsv"))
                .print();
        PermutationSolution<Integer> solution;
        double[] objectives = new double[2];
            JMetalLogger.logger.info("Total execution time: " + computingTime + "ms");
            JMetalLogger.logger.info("Objectives values have been written to file FUN.tsv");
            JMetalLogger.logger.info("Variables values have been written to file VAR.tsv");

        for (int i = 0; i < population.size(); i++) {
            solution = population.get(i);
            objectives[0] = solution.getObjective(0);
            objectives[1] = solution.getObjective(1);
            System.out.println("Solution " + (i + 1) + ":");
            System.out.println("    Sack 1 Preference: " + objectives[0] * -1);
            System.out.println("    Sack 2 cost: " + objectives[1]);
        }
        printFoodsToFile(computingTime, "result.txt", population, problem);

    }

    public static void printFoodsToFile(Long estimatedTime, String path, List<PermutationSolution<Integer>> result, MO_MKP2 problem) {
        double[] nutrientValues = new double[problem.getNumberOfConstraints()];
        double fitness = 0.0;
        int[] b, d, binaryRep;
        DecimalFormat df = new DecimalFormat("0.00");
        try {
            FileOutputStream fos = new FileOutputStream(path);
            OutputStreamWriter osw = new OutputStreamWriter(fos);
            BufferedWriter bw = new BufferedWriter(osw);
            bw.write("Total execution time: " + estimatedTime + "ms ");
            bw.newLine();
            if (result.size() > 0) {
                int numberOfVariables = 1;//result.get(0).getNumberOfVariables();
                for (int i = 0; i < numberOfVariables; i++) {
                    for (int j = 0; j < result.size(); j++) {
                        fitness = (-1 * result.get(j).getObjective(0)) - (result.get(j).getObjective(1));
                        bw.write("United Obj: " + fitness + " Total Preference : " + -1 * result.get(j).getObjective(0) + " Total Cost : " + result.get(j).getObjective(1) + " #violated const : " + problem.numberOfViolatedConstraints.getAttribute(result.get(j)) + " Tot. ViolationRate : " + problem.overallConstraintViolationDegree.getAttribute(result.get(j)) + " # of foods : " + problem.numberofItemsSet.getAttribute(result.get(j)) + " ");
                        bw.newLine();
                        b = problem.binaryRepresentation.getAttribute(result.get(j));
                        for (int k = 0; k < b.length; k++) {
                            if (b[k] == 1) {
                                bw.write("Food Id : " + problem.getFoodId(k) + "  Food Name : " + problem.getFood(k) + " ");
                                bw.newLine();

                                for (int l = 0; l < problem.getNumberOfConstraints(); l++) {//get number of backpacknumber
                                    nutrientValues[l] += problem.getValues()[l][k];
                                }
                            }
                            // nutrientValues=result.get(j).getConstraints();
                        }
                        bw.write("Nutient");
                        bw.newLine();
                        for (int l = 0; l < problem.getNumberOfConstraints(); l++) {
                            bw.write("Id : " + problem.getNutrientId(l) + "        Name : " + problem.getNutrient(l) + "        Total Value : " + df.format(nutrientValues[l]) + "        lower-Upper Bounds:" + df.format(problem.getLowlimit(l)) + "-" + df.format(problem.getUplimit(l)));
                            bw.newLine();
                            nutrientValues[l] = 0;
                        }
                        bw.newLine();
                    }
                    bw.newLine();
                    bw.write("Details ................................................./n");
                    bw.newLine();
                    for (int j = 0; j < result.size(); j++) {
                        bw.write("Total Preference : " + -1 * result.get(j).getObjective(0) + " ");
                        bw.newLine();
                        bw.write("Total Cost : " + result.get(j).getObjective(1) + " ");
                        bw.newLine();
                        //boolean[] d = EncodingUtils.getBinary(result.get(j).getVariable(0));

                        b = problem.binaryRepresentation.getAttribute(result.get(j));
                        for (int k = 0; k < problem.nItems; k++) {
                            if (b[k] == 1) {
                                bw.write("Food Id : " + problem.getFoodId(result.get(j).getVariableValue(k)) + "  Food Name : " + problem.getFood(result.get(j).getVariableValue(k)) + " ");
                                bw.newLine();
                                bw.write("Preference : " + problem.getPreference(result.get(j).getVariableValue(k)) + " ");
                                bw.newLine();
                                bw.write("Cost : " + problem.getCost(result.get(j).getVariableValue(k)) + " ");
                                bw.newLine();
                                for (int l = 0; l < problem.getNumberOfConstraints(); l++) {
                                    bw.write("Nutient Id : " + problem.getNutrientId(l) + "        Nutrient Name : " + problem.getNutrient(l) + "        Quantity : " + df.format(problem.getValues()[l][result.get(j).getVariableValue(k)]));
                                    bw.newLine();
                                }
                                bw.write("NEWFOOD----------------------------------------------------------------------------------------");
                                bw.newLine();
                                bw.newLine();
                                bw.newLine();

                            }
                        }
                        bw.write("NEW SOLUTION############################################################################################");
                        bw.newLine();
                        bw.newLine();
                        bw.newLine();
                    }
                }
            }
            bw.close();
        } catch (IOException e) {
            JMetalLogger.logger.info("Error acceding to the file");
            e.printStackTrace();
        }
    } // printFoodsToFile

}
