package edu.adelaide.council.member;

import java.util.Arrays;
import java.util.List;

public class Member {
    // Fields
    private int memberId; // Unique identifier for each member
    private List<String> roles = Arrays.asList("Proposer","Acceptor");
    protected int acceptorPort;

    public List<String> getAcceptorAddresses() {
        return acceptorAddresses;
    }

    public void setAcceptorAddresses(List<String> acceptorAddresses) {
        this.acceptorAddresses = acceptorAddresses;
    }

    public int getAcceptorPort() {
        return acceptorPort;
    }

    public void setAcceptorPort(int acceptorPort) {
        this.acceptorPort = acceptorPort;
    }

    public int getMemberId() {
        return memberId;
    }

    public void setMemberId(int memberId) {
        this.memberId = memberId;
    }

    protected List<String> acceptorAddresses;

    // Constructor
    public Member(String memberId, List<String> acceptorAddresses) {

    }

    public Member() {

    }

    public void startAcceptor(int acceptorPort) {
    }

    public void startLearner(int i) {
    }

    public void propose() {
    }
}
