package edu.adelaide.council.member;

import java.util.Arrays;
import java.util.List;

public class Member {
    // Fields
    private String memberId; // Unique identifier for each member
    private List<String> acceptorAddresses;
    private List<String> roles = Arrays.asList("Proposer","Acceptor");

    // Constructor
    public Member(String memberId, List<String> acceptorAddresses) {
        this.memberId = memberId;
        this.acceptorAddresses = acceptorAddresses;
    }

    public Member() {
    }

}
