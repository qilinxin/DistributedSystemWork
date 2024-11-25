
import edu.adelaide.council.paxos.PaxosCoordinator;


public class TestCase3 {

    //30 points – Paxos implementation works when M1 – M9 have responses to voting queries suggested by several profiles (immediate response, small delay, large delay and no response), including when M2 or M3 propose and then go offline
    public static void main(String[] args) {
        PaxosCoordinator pc = new PaxosCoordinator();
//        pc.buildElectionData();
        pc.runElection("3");

    }
}
