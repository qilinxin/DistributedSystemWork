
import edu.adelaide.council.paxos.PaxosCoordinator;


public class TestCase1 {

    //10 points -  Paxos implementation works when two councillors send voting proposals at the same time
    public static void main(String[] args) {
        PaxosCoordinator pc = new PaxosCoordinator();
        pc.runElection(2);

    }
}
