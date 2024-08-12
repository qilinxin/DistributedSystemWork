package main.java;

import java.math.BigInteger;
import java.io.Serializable;
import java.rmi.MarshalException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static java.lang.Thread.sleep;

public class CalculatorImplementation implements Calculator, Serializable {

    private static final List<Integer> STACK = new ArrayList<>();

    @Override
    public void pushValue(int val) {
        STACK.add(val);
    }

    @Override
    public int pushOperation(String operator) throws MarshalException {
        if (STACK.isEmpty() || operator == null || operator.isEmpty()) {
            throw new MarshalException(" cannot push operation");
        }
        int res = 0;
        //for min - push the min value of all the popped values;
        if ("min".equals(operator)) {
            // use Stream API to get the min element
            Optional<Integer> minElement = STACK.stream()
                    .min(Comparator.naturalOrder());
            res = minElement.get();
        }
        //for max - push the max value of all the popped values
        else if ("max".equals(operator)) {
            Optional<Integer> maxElement = STACK.stream()
                    .max(Comparator.naturalOrder());
            //clear the stack
            res = maxElement.get();
        }
        //for lcm - push the least common multiple of all the popped values;
        else if ("lcm".equals(operator)) {
            res = calculateLCM();
        }
        //for gcd - push the greatest common divisor of all the popped values.
        else if ("gcd".equals(operator)) {
            res = calculateGCD();
        } else {
            throw new MarshalException("unknown operator " + operator);
        }
        STACK.clear();
        return res;
    }

    @Override
    public int pop() {
        int res =  STACK.get(stackSize() - 1);
        STACK.remove(stackSize() - 1);
        return res;
    }

    @Override
    public boolean isEmpty() {
        return STACK.isEmpty();
    }

    @Override
    public int delayPop(int millis) {
        try {
            sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return this.pop();
    }

    public int stackSize() {
        return STACK.size();
    }

    @Override
    public void clear() throws RemoteException {
        STACK.clear();
    }

//=====================

    private static int calculateLCM() {
        // from first element
        int lcm = STACK.get(0);
        for (int i = 1; i < STACK.size(); i++) {
            // 依次计算 LCM
            lcm = lcm(lcm, STACK.get(i));
        }
        return lcm;
    }

    /**
     * private calculate lcm function
     * @param a first number
     * @param b second number
     * @return lcm
     */
    private static int lcm(int a, int b) {
        return a * (b / gcdFunction(a, b));
    }

    /**
     * private calculate gcd function
     * @param a first number
     * @param b second number
     * @return gcd
     */
    private static int gcdFunction(int a, int b) {
        return BigInteger.valueOf(a).gcd(BigInteger.valueOf(b)).intValue();
    }

    /**
     * calculate the greatest common divisor of a set of numbers
     * @return
     */
    private static int calculateGCD() {
        // 从第一个元素开始
        int gcd = STACK.get(0);
        for (int i = 1; i < STACK.size(); i++) {
            gcd = gcdFunction(gcd, STACK.get(i));
        }
        return gcd;
    }

}
