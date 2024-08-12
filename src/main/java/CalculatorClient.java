package main.java;

import org.junit.Before;
import org.junit.Test;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static java.lang.Thread.sleep;
import static org.junit.Assert.*;

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


    private Calculator calculator;

    @Before
    public void setUp() throws Exception {
        // Assuming the CalculatorImpl class is the implementation of the Calculator interface
        // and the RMI registry is correctly set up
        calculator = (Calculator) Naming.lookup("rmi://localhost/CalculatorService");
    }

    @Test
    public void testPushValue() throws RemoteException {
        calculator.pushValue(10);
        assertEquals(1, calculator.stackSize());
    }

    @Test
    public void testPushOperationMin() throws RemoteException {
        calculator.clear();
        calculator.pushValue(10);
        calculator.pushValue(20);
        int result = calculator.pushOperation("min");
        assertEquals(10, result);
        assertEquals(0, calculator.stackSize());
    }

    @Test
    public void testPushOperationMax() throws RemoteException {
        calculator.clear();

        calculator.pushValue(10);
        calculator.pushValue(20);
        int result = calculator.pushOperation("max");
        assertEquals(20, result);
        assertEquals(0, calculator.stackSize());
    }

    @Test
    public void testPushOperationLcm() throws RemoteException {
        calculator.clear();

        calculator.pushValue(6);
        calculator.pushValue(8);
        int result = calculator.pushOperation("lcm");
        assertEquals(24, result);
        assertEquals(0, calculator.stackSize());
    }

    @Test
    public void testPushOperationGcd() throws RemoteException {
        calculator.clear();

        calculator.pushValue(8);
        calculator.pushValue(12);
        int result = calculator.pushOperation("gcd");
        assertEquals(4, result);
        assertEquals(0, calculator.stackSize());
    }

    @Test(expected = RemoteException.class)
    public void testPushOperationInvalid() throws RemoteException {
        calculator.pushOperation("invalid");
    }

    @Test
    public void testPop() throws RemoteException {
        calculator.clear();

        calculator.pushValue(10);
        int result = calculator.pop();
        System.out.println("=========="+calculator.stackSize());
        assertEquals(10, result);
        assertTrue(calculator.isEmpty());
    }

    @Test
    public void testIsEmpty() throws RemoteException {
        assertTrue(calculator.isEmpty());
        calculator.pushValue(10);
        assertFalse(calculator.isEmpty());
    }

    @Test
    public void testDelayPop() throws RemoteException {
        calculator.clear();

        calculator.pushValue(10);
        int result = calculator.delayPop(1000); // 1 second delay
        assertEquals(10, result);
        assertTrue(calculator.isEmpty());
    }

    @Test
    public void testStackSize() throws RemoteException {
        assertEquals(0, calculator.stackSize());
        calculator.pushValue(10);
        calculator.pushValue(20);
        assertEquals(2, calculator.stackSize());
    }

}
