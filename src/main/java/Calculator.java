package main.java;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Calculator extends Remote {
    /**
     * add a integer into stack
     * @param val inputValue
     * @throws RemoteException exceptions
     */
    void pushValue(int val) throws RemoteException;

    /**
     * according to operator type to do different things
     * min - push the min value of all the popped values;
     * max - push the max value of all the popped values
     * lcm - push the least common multiple of all the popped values;
     * gcd - push the greatest common divisor of all the popped values.
     * if operator type is empty or undefined,stack is empty throw exception
     * @param operator operatorName
     * @return res
     * @throws RemoteException exceptions
     */
    int pushOperation(String operator) throws RemoteException;

    /**
     * pop the last push value
     * @return value
     */
    int pop();

    /**
     * check the stack is empty
     * @return true equals empty
     * @throws RemoteException e
     */
    boolean isEmpty() throws RemoteException;

    /**
     * pop the value with a specified delay
     * @param millis time
     * @return  value
     * @throws RemoteException e
     */
    int delayPop(int millis) throws RemoteException;

    /**
     * query the size of stack
     * @return size
     */
    int stackSize();
}
