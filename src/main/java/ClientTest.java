package main.java;

import org.junit.Before;
import org.junit.Test;

import java.rmi.Naming;
import java.rmi.RemoteException;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

public class ClientTest {

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
