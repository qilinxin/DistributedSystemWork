package main.java;

import java.rmi.Naming;

public class CalculatorClient {
    public static void main(String[] args) {
        try {
            // 查找远程对象
            Calculator calculator = (Calculator) Naming.lookup("//localhost/CalculatorService");

            // 调用远程方法
            System.out.println(calculator.isEmpty());
            calculator.pushValue(5);
            calculator.pushValue(5);
            System.out.println(calculator.pop());

            calculator.pushValue(5);
            System.out.println(calculator.isEmpty());

//            System.out.println("Result of 5 + 3 = " + result);
        } catch (Exception e) {
            System.out.println("RMIClient exception: " + e.toString());
            e.printStackTrace();
        }
    }
}
