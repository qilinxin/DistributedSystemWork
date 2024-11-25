import edu.adelaide.council.paxos.PaxosCoordinator;


public class TestCase2 {

    //30 points – Paxos implementation works in the case where all M1-M9 have immediate responses to voting queries
    public static void main(String[] args) {
        PaxosCoordinator pc = new PaxosCoordinator();
//        pc.buildElectionData();
        pc.runElection("0");

    }
}
