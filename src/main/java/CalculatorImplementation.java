package main.java;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Thread.sleep;

public class CalculatorImplementation implements Calculator, Serializable {

    public static List<Integer> stack = new ArrayList<>();

    @Override
    public void pushValue(int val) {
        stack.add(val);
    }

    @Override
    public void pushOperation(String operator) {

    }

    @Override
    public int pop() {
        return stack.get(stack.size()-1);
    }

    @Override
    public boolean isEmpty() {
        return stack.isEmpty();
    }

    @Override
    public int delayPop(int millis) {
        try {
            sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return this.pop();
    }
}
