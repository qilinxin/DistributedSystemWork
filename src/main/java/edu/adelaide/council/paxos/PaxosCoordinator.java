package edu.adelaide.council.paxos;

import edu.adelaide.council.member.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The PaxosCoordinator is responsible for orchestrating the election process using the Paxos protocol.
 * It manages members, generates proposals, and ensures a consensus is reached among the nodes.
 */
public class PaxosCoordinator {

    // Logger for logging system actions and errors
    private static final Logger logger = LoggerFactory.getLogger(PaxosCoordinator.class);

    // Random instance for generating random values
    private static final Random random = new Random();

    /**
     * -1: Initial state
     * 1, 2, 3: Election successfully concluded with one of these members as the chairman
     * 99: Election failed, retrying
     */
    private static final AtomicInteger STATUS_CODE = new AtomicInteger(-1);

    // List of all participating members in the Paxos protocol
    private static final List<Member> MEMBERS = new ArrayList<>();

    // Generator for unique proposal numbers
    private static final AtomicInteger PROPOSAL_NUMBER_GENERATOR = new AtomicInteger(0);

    // Set of valid end statuses indicating consensus is reached
    private static final List<Integer> END_RESULT = Arrays.asList(1, 2, 3);

    // Configurable delay level for simulating network delays
    public static String DELAY_VALUE = "0";

    /**
     * Runs the election process with default settings.
     */
    public void runElection() {
        runElection(1, "0");
    }

    /**
     * Runs the election process with a specified level of concurrency.
     *
     * @param concurrentNumber Number of concurrent proposals to be executed.
     */
    public void runElection(int concurrentNumber) {
        runElection(concurrentNumber, "0");
    }

    /**
     * Runs the election process with a specified delay level.
     *
     * @param delayLevel Configurable delay level for simulating network delays.
     */
    public void runElection(String delayLevel) {
        runElection(1, delayLevel);
    }

    /**
     * Orchestrates the entire election process.
     * Initializes members, starts their acceptors, and begins proposals.
     *
     * @param concurrentNumber Number of concurrent proposals to be executed.
     * @param delayLevel       Configurable delay level for simulating network delays.
     */
    public void runElection(int concurrentNumber, String delayLevel) {
        DELAY_VALUE = delayLevel;
        logger.info("------------------runElection start------------------");
        int numMembers = 9;

        // Initialize all members in the system
        for (int i = 1; i <= numMembers; i++) {
            Member member = getMember(i, numMembers);
            MEMBERS.add(member);
        }

        // Create a thread pool for running acceptors and proposals concurrently
        ExecutorService executorService = Executors.newFixedThreadPool(9);

        // Start acceptor threads for each member
        for (Member member : MEMBERS) {
            int acceptorPort = member.getAcceptorPort();
            executorService.execute(() -> {
                member.startAcceptor(acceptorPort);
            });
        }

        // Execute proposals concurrently
        for (int z = 0; z < concurrentNumber; z++) {
            // Randomly select a proposer (M1, M2, or M3)
            int randomInt = random.nextInt(3);
            executePropose(randomInt, executorService);
        }

        // Monitor the consensus status and retry if necessary
        AtomicInteger retryTimes = new AtomicInteger(10);
        new Thread(() -> {
            while (!END_RESULT.contains(STATUS_CODE.get())) {
                logger.info("Current status is ===== {}", STATUS_CODE.get());
                if (STATUS_CODE.get() == 99) {
                    // Retry logic for failed elections
                    if (retryTimes.get() == 0) {
                        logger.error("Failure to elect a chairperson within three elections ends the election");
                        executorService.shutdownNow();
                        System.exit(0);
                    } else {
                        retryTimes.decrementAndGet();
                        for (int z = 0; z < concurrentNumber; z++) {
                            int randomInt = random.nextInt(3);
                            executePropose(randomInt, executorService);
                        }
                    }
                } else {
                    // Wait before checking the status again
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        logger.error("Thread interrupted", e);
                    }
                }
            }

            // Consensus is reached; shut down services
            logger.info("Consensus reached! M{} become the chairman !!", STATUS_CODE.get());
            executorService.shutdownNow();
            System.exit(0);
        }).start();
    }

    /**
     * Initializes a specific member based on its ID.
     *
     * @param i          The ID of the member (1 to numMembers).
     * @param numMembers Total number of members in the system.
     * @return The initialized Member instance.
     */
    private Member getMember(int i, int numMembers) {
        String nodeId = "M" + i;
        int acceptorPort = 5000 + i; // Assign a unique port for each member

        // Create a list of all member addresses
        List<String> acceptorAddresses = new ArrayList<>();
        for (int j = 1; j <= numMembers; j++) {
            acceptorAddresses.add("localhost:" + (5000 + j));
        }

        // Return the appropriate member type based on the ID
        if (i == 1) {
            return new M1Member(acceptorPort, acceptorAddresses);
        } else if (i == 2) {
            return new M2Member(acceptorPort, acceptorAddresses);
        } else if (i == 3) {
            return new M3Member(acceptorPort, acceptorAddresses);
        } else {
            return new OtherMember(nodeId, acceptorPort);
        }
    }

    /**
     * Executes a proposal from a randomly selected proposer.
     *
     * @param randomInt       Index of the proposer in the member list.
     * @param executorService The ExecutorService managing the proposal threads.
     */
    public void executePropose(int randomInt, ExecutorService executorService) {
        logger.info("-------- M{} prepare to send the proposal-------------------- ", randomInt + 1);
        setStatusCache(-1);

        // Submit the proposal task to the executor
        executorService.execute(() -> {
            try {
                // Wait for all nodes to initialize
                Thread.sleep(5000);
                MEMBERS.get(randomInt).propose();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Thread interrupted during proposal execution", e);
            }
        });
    }

    /**
     * Generates the next unique proposal ID.
     *
     * @return The next proposal ID.
     */
    public static int getNextProposalId() {
        return PROPOSAL_NUMBER_GENERATOR.incrementAndGet();
    }

    /**
     * Updates the global status cache with the provided status code.
     *
     * @param statusCode The new status code to set.
     */
    public static void setStatusCache(int statusCode) {
        STATUS_CODE.set(statusCode);
    }

    /**
     * Applies a delay to simulate network latency based on the current delay level.
     */
    public static void applyDelay() {
        int delayValue = Integer.parseInt(DELAY_VALUE);

        // Define maximum delays for each delay level
        int[] maxDelays = {500, 1000, 1500, 2000};

        // Validate delay level and fallback to default if invalid
        if (delayValue < 0 || delayValue >= maxDelays.length) {
            delayValue = 0;
        }

        int maxDelay = maxDelays[delayValue];
        int delay = random.nextInt(maxDelay + 1); // Generate a random delay within the range

        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Thread interrupted during delay", e);
        }
    }

    /**
     * Determines whether a random event occurs based on a given probability.
     *
     * @param probability The probability of the event occurring (0 to 1).
     * @return True if the event occurs, false otherwise.
     */
    public static boolean getRandomWithProbability(double probability) {
        if (probability < 0 || probability > 1) {
            throw new IllegalArgumentException("Probability must be between 0 and 1");
        }
        return random.nextDouble() < probability;
    }
}
