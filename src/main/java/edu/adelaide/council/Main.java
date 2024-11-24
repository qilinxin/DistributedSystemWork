package edu.adelaide.council;

import edu.adelaide.council.member.Member;
import edu.adelaide.council.paxos.PaxosCoordinator;

public class Main {


    public static void main(String[] args) {
        PaxosCoordinator pc = new PaxosCoordinator();
//        pc.buildElectionData();
        pc.runElection(2);
    }
}
