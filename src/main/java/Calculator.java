package main.java;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Calculator extends Remote {
    void pushValue(int val) throws RemoteException;

    int pushOperation(String operator) throws RemoteException;

    int pop();

    boolean isEmpty() throws RemoteException;

    int delayPop(int millis) throws RemoteException;

    int stackSize();
}
