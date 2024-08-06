package server;

import java.rmi.Remote;

public interface Calculator extends Remote {
    void pushValue(int val);

    void pushOperation(String operator);

    int pop();

    boolean isEmpty();

    int delayPop(int millis);

}
