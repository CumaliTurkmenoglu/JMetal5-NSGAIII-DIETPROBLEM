package org.uma.jmetal.problem.multiobjective;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.uma.jmetal.problem.ConstrainedProblem;
import org.uma.jmetal.problem.impl.AbstractDoubleProblem;
import org.uma.jmetal.solution.DoubleSolution;
import org.uma.jmetal.util.solutionattribute.impl.NumberOfViolatedConstraints;
import org.uma.jmetal.util.solutionattribute.impl.OverallConstraintViolation;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.JMException;
import org.uma.jmetal.problem.impl.AbstractIntegerPermutationProblem;
import org.uma.jmetal.solution.PermutationSolution;
import org.uma.jmetal.util.solutionattribute.impl.BinaryRepresentation;
import org.uma.jmetal.util.solutionattribute.impl.Constraints;
import org.uma.jmetal.util.solutionattribute.impl.MostViolatedIndex;
import org.uma.jmetal.util.solutionattribute.impl.NumberOfItemsSet;

/**
 * Class representing problem Binh2
 */
@SuppressWarnings("serial")
public class MO_MKP2 extends AbstractIntegerPermutationProblem implements ConstrainedProblem<PermutationSolution<Integer>> {

    public OverallConstraintViolation<PermutationSolution<Integer>> overallConstraintViolationDegree;
    public NumberOfViolatedConstraints<PermutationSolution<Integer>> numberOfViolatedConstraints;
    public Constraints<PermutationSolution<Integer>> constraints;
    public MostViolatedIndex<PermutationSolution<Integer>> mostViolatedIndex;
    public NumberOfItemsSet<PermutationSolution<Integer>> numberofItemsSet;
    public BinaryRepresentation<PermutationSolution<Integer>> binaryRepresentation;
    public int age = 25;
    public String gender = "male";
    public int nConst, nItems = 0;// number of knapsacks,number of items,num. of Objectives
    public String[] nutrients;// used when getting values from input text
    public int[] nutrientId;// used when getting values from input text
    public int[] foodId;// used when getting values from input text
    public String[] foods;// 
    public double[][] values;//// m*n matrix values store in this integer list
    public double[] upLimits;//upper limits of constraints/knapsacks
    public double[] lowLimits;//upper limits of constraints/knapsacks
    public double[] preference;//Preferences
    public double[] cost;//cost
    public int nVar = 1;
    public int nObj = 2;

    /**
     * Constructor Creates a default instance of the Binh2 problem
     */
    public MO_MKP2() throws SQLException {
        load();
        setNumberOfVariables(nItems);//?
        setNumberOfObjectives(2);
        setNumberOfConstraints(nConst);
        setName("MultiObjectiveKP_DietP");

        List<Double> lowerLimit = Arrays.asList(0.0, 0.0);
        List<Double> upperLimit = Arrays.asList(5.0, 3.0);

        setLowerLimit(lowerLimit);
        setUpperLimit(upperLimit);
        numberofItemsSet = new NumberOfItemsSet<PermutationSolution<Integer>>();
        mostViolatedIndex = new MostViolatedIndex<PermutationSolution<Integer>>();
        constraints = new Constraints<PermutationSolution<Integer>>();
        overallConstraintViolationDegree = new OverallConstraintViolation<PermutationSolution<Integer>>();
        numberOfViolatedConstraints = new NumberOfViolatedConstraints<PermutationSolution<Integer>>();
        binaryRepresentation = new BinaryRepresentation<PermutationSolution<Integer>>();
    }

    /**
     * Evaluate() method
     */
    public void evaluate(PermutationSolution<Integer> solution) {
        double[] fitness = {0, 0};
        int[] binaryRep_ = new int[nItems];//binaryRepresentation.getAttribute(solution);
        int index = 0, nItemsSet = 0, nVar = solution.getNumberOfVariables();
        char ch;
        boolean viol = false;
        double[] constraints_ = new double[this.getNumberOfConstraints()];
        for (int i = 0; i < (nItems - 1); i++) {
            try {
                index = solution.getVariableValue(i);
                viol = checkConstraints(constraints_, index);
                if (viol == true) {
                    fitness[0] -= preference[index];
                    fitness[1] += cost[index];
                    binaryRep_[index] = 1;
                    nItemsSet++;
                    for (int n = 0; n < this.getNumberOfConstraints(); n++) {
                        constraints_[n] += values[n][index];
                    }
                }
            } // for
            catch (JMException ex) {
                Logger.getLogger(MO_MKP2.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        numberofItemsSet.setAttribute(solution, nItemsSet);
        //fitness[2] = solution.getOverallConstraintViolation();
        solution.setObjective(0, fitness[0]);// + (-10000 * solution.getOverallConstraintViolation()));
        solution.setObjective(1, fitness[1]);// +(-10000 * solution.getOverallConstraintViolation()));
        constraints.setAttribute(solution, constraints_);
        binaryRepresentation.setAttribute(solution, binaryRep_);
//solution.setConstraints(constraints);
    }

    /**
     * EvaluateConstraints() method
     */
    @Override
    public void evaluateConstraints(PermutationSolution<Integer> solution) {
        double[] violations = new double[nConst];
        double[] diffrates = new double[nConst];
        double[] constraints_ = constraints.getAttribute(solution);

        double total = 0.0, maxDiff = -999999, diffRate = 0, totViol = 0;
        int number = 0, index = -1;
        for (int i = 0; i < nConst; i++) {
            if (constraints_[i] < lowLimits[i]) {
                number++;
                diffRate = Math.abs(constraints_[i] - lowLimits[i]) / (upLimits[i] - lowLimits[i]);
                violations[i] = lowLimits[i] - constraints_[i];
                diffrates[i] = diffRate * 10000;
                if (diffRate > maxDiff) {
                    maxDiff = diffRate;
                    index = i;
                }
                totViol += Math.abs(violations[i]);
                total += diffRate;
            } else if (constraints_[i] > upLimits[i]) {
                number++;
                //Daha sonra up limiti önemsiz olan (suda çözünen vit. gibi) constraintler için up limiti kaldırır ya da cezasını azaltabiliriz
                diffRate = Math.abs(constraints_[i] - upLimits[i]) / (upLimits[i] - lowLimits[i]);
                violations[i] = constraints_[i] - upLimits[i];
                diffrates[i] = diffRate * 10000;
                if (diffRate > maxDiff) {
                    maxDiff = diffRate;
                    index = i;
                }
                totViol += Math.abs(violations[i]);
                total += diffRate;
            } else {
                violations[i] = 0;
            }
        }

//        solution.setConstraints(violations);
        //solution.setViolations(violations);
        //solution.setOverallConstraintViolation(-1 * Math.floor(total * 100) / 100);
        mostViolatedIndex.setAttribute(solution, index);
        overallConstraintViolationDegree.setAttribute(solution, total);
        numberOfViolatedConstraints.setAttribute(solution, number);
    }// evaluateConstraints

    @Override
    public int getPermutationLength() {
        return nItems;
    }

    public boolean checkConstraints(double[] constraints, int index) throws JMException {
        boolean viol = true;
        for (int j = 0; j < this.getNumberOfConstraints(); j++) {
            if (constraints[j] + values[j][index] > upLimits[j]) {
                viol = false;
            }
        }
        return viol;
    } // evaluateConstraints

    public void load() throws SQLException {
        Connection conn = DBConnection.establishConnection();
        PreparedStatement st = null;
        String selectQuery;
        ResultSet resultSet = null;
        selectQuery = "SELECT id as id, name as name, cost as cost, preference as preference FROM foods";
        st = conn.prepareStatement(selectQuery);
        resultSet = st.executeQuery();

        if (resultSet.last()) {
            nItems = resultSet.getRow();
            resultSet.beforeFirst();
        }
        foods = new String[nItems];
        foodId = new int[nItems];
        cost = new double[nItems];
        preference = new double[nItems];
        int n = 0;
        while (resultSet.next()) {
            foods[n] = resultSet.getString("name");
            foodId[n] = resultSet.getInt("id");
            cost[n] = resultSet.getFloat("cost");
            preference[n] = resultSet.getFloat("preference");
            n++;
        }

        selectQuery = "SELECT nutrients.name as name, dri.nutrient_id as nutrientId, dri.RLL as rll, dri.RUL as rul FROM dri INNER JOIN nutrients ON dri.nutrient_id=nutrients.id WHERE dri.low_age <" + age + " AND dri.up_age>" + age + " AND dri.gender='" + gender + "' ORDER BY dri.nutrient_id";
        st = conn.prepareStatement(selectQuery);
        resultSet = st.executeQuery();
        if (resultSet.last()) {
            nConst = resultSet.getRow();
            resultSet.beforeFirst();
        }
        values = new double[nConst][nItems];
        nutrients = new String[nConst];
        nutrientId = new int[nConst];
        lowLimits = new double[nConst];
        upLimits = new double[nConst];
        n = 0;
        while (resultSet.next()) {
            nutrients[n] = resultSet.getString("name");
            nutrientId[n] = resultSet.getInt("nutrientId");
            lowLimits[n] = resultSet.getFloat("rll");
            upLimits[n] = resultSet.getFloat("rul");
            n++;
        }
        int k = 0;
        for (int i = 0; i < nConst; i++) {
//            for (int j = 0; j < itemsnumber; j++) {
            selectQuery = "SELECT distinct(foodId), quantity FROM food_nutrients inner join foods on foods.id=food_nutrients.foodId where nutrientId= " + nutrientId[i] + " ORDER BY  foodId ASC ";
            st = conn.prepareStatement(selectQuery);
            resultSet = st.executeQuery();
            while (resultSet.next()) {
                values[i][k] = resultSet.getFloat("quantity");
                k++;
            }
            k = 0;
            //          }
        }
    }

    @Override
    public String getName() {
        return "DietProblem";
    }

    @Override
    public int getNumberOfConstraints() {
        return nConst;
    }

    @Override
    public int getNumberOfObjectives() {
        return nObj;
    }

    public int getFoodId(int k) {
        return foodId[k];
    }

    public String getFood(int k) {
        return foods[k];
    }

    public double[][] getValues() {
        return values;
    }

    public int getNutrientId(int k) {
        return nutrientId[k];
    }

    public String getNutrient(int k) {
        return nutrients[k];
    }

    public double getPreference(int k) {
        return preference[k];
    }

    public double getCost(int k) {
        return cost[k];
    }

    public double getUplimit(int k) {
        return upLimits[k];
    }

    public double getLowlimit(int k) {
        return lowLimits[k];
    }
}
