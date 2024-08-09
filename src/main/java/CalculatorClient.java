package main.java;

import java.rmi.Naming;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static java.lang.Thread.sleep;

public class CalculatorClient {
    public static void main(String[] args) {
        //start three threads to check Stack independent
        MyThread thread1 = new MyThread("Thread 1");
        MyThread thread2 = new MyThread("Thread 2");
        MyThread thread3 = new MyThread("Thread 3");

        thread1.start();
        try {
            //avoid concurrent
            sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        thread2.start();
        try {
            sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        thread3.start();
    }

    /**
     * define thread
     */
    static class MyThread extends Thread {
        private static String threadName;

        MyThread(String name) {
            threadName = name;
        }

        @Override
        public void run() {
            System.out.println(threadName + " is running");
            try {
                // find remote object in rmi server
                Calculator calculator = (Calculator) Naming.lookup("//localhost/CalculatorService");
                System.out.println(calculator.toString());
                // call remote method
                try {
                    System.out.println(threadName + ", stackSize==" + calculator.stackSize());
                    System.out.println(calculator.pop());

                } catch (Exception e) {
                    System.out.println(threadName + " stack is empty");
                }
                List<Integer> inputNumbers = new ArrayList<>();
                Random random = new Random();
                for (int i = 0; i < 5; i ++){
                    int num = random.nextInt(100);
                    calculator.pushValue(num*3);
                    inputNumbers.add(num);
                }
                System.out.println("inputNumbers====" + inputNumbers);
                System.out.println("last =====" + calculator.pop());
                int min= calculator.pushOperation("abc");
                System.out.println("operation min ====" + min);
//                int max= calculator.pushOperation("max");
//                System.out.println("operation max ====" + max);
                calculator.pushValue(115);
                long start = System.currentTimeMillis();

                System.out.println("delayPop======="+calculator.delayPop(2000));

                long end = System.currentTimeMillis();
                System.out.println("time cost == " + (end- start));
//            System.out.println("Result of 5 + 3 = " + result);
                calculator.pushValue(3);
                System.out.println(threadName + " " + calculator.stackSize());

            } catch (Exception e) {
                throw new RuntimeException("something wrong with the server, please contact the administrator",
                        e);
            }
        }
    }

}
