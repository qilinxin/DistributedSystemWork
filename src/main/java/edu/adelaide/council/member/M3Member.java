package edu.adelaide.council.member;

import edu.adelaide.council.dto.MessageDTO;
import edu.adelaide.council.paxos.PaxosCoordinator;

import java.io.PrintWriter;
import java.util.List;

/**
 * The M3Member class represents a specific member in the Paxos protocol
 * identified as "M3". This member has unique behavior simulating complete
 * disconnection, such as being "camping in the Coorong," to reflect real-world
 * network failures.
 */
public class M3Member extends Member {

    // A flag indicating whether M3 is currently disconnected
    private static boolean DISCONNECTED;

    /**
     * Constructor for M3Member.
     *
     * @param acceptorPort      The port number on which this member accepts connections.
     * @param acceptorAddresses The list of addresses for other members in the Paxos protocol.
     */
    public M3Member(int acceptorPort, List<String> acceptorAddresses) {
        this.nodeId = "M3"; // Unique identifier for this member
        this.acceptorPort = acceptorPort; // Port number for accepting connections
        this.acceptorAddresses = acceptorAddresses; // List of other members' addresses

        // Simulate random disconnection based on a probability calculation
        DISCONNECTED = PaxosCoordinator.getRandomWithProbability(
                0.33 * Integer.parseInt(PaxosCoordinator.DELAY_VALUE)
        );
    }

    /**
     * Returns the proposal value specific to M3.
     *
     * @return A string representing M3's proposal value.
     */
    @Override
    public String getProposedValue() {
        return "Suggest M3 to become chairman";
    }

    /**
     * Initiates a proposal process if M3 is not disconnected.
     * Overrides the default behavior of the propose method in Member class.
     */
    @Override
    public void propose() {
        // Check if M3 is disconnected before initiating the proposal
        if (DISCONNECTED) {
            logger.info("M3 is camping in the Coorong and is completely disconnected.");
            PaxosCoordinator.setStatusCache(99); // Mark the election process for retry
            return;
        }
        // Call the parent class's implementation if connectivity is available
        super.propose();
    }

    /**
     * Handles the "PREPARE" phase of the Paxos protocol for incoming requests.
     *
     * @param proposalNumber The proposal number of the incoming request.
     * @param proposalValue  The proposed value from the proposer.
     * @param out            The PrintWriter object to send the response back to the proposer.
     */
    @Override
    public void handlePrepare(int proposalNumber, String proposalValue, PrintWriter out) {
        // Check if M3 is disconnected during the PREPARE phase
        if (DISCONNECTED) {
            logger.info("M3 is camping in the Coorong and is completely disconnected.");
            PaxosCoordinator.setStatusCache(99); // Mark the election process for retry
            return;
        }

        // Create a new response message
        MessageDTO response = new MessageDTO();
        response.setProposalId(proposalNumber);

        // Log the current state for debugging
        logger.info("PROMISED_PROPOSAL_NUMBER == {}, proposalNumber == {}, proposalNumber > PROMISED_PROPOSAL_NUMBER.get(): {}",
                PROMISED_PROPOSAL_NUMBER.get(), proposalNumber, proposalNumber > PROMISED_PROPOSAL_NUMBER.get());

        // Check if the proposal number is valid and relevant to M3
        if (proposalNumber >= PROMISED_PROPOSAL_NUMBER.get()) {
            if (proposalValue.contains("M3")) {
                PROMISED_PROPOSAL_NUMBER.set(proposalNumber); // Update the promised proposal number
                response.setType("AGREE"); // Respond with agreement
                logger.info("M3 agreed to proposalNumber == {}, proposal: {}", proposalNumber, proposalValue);
            } else {
                // Reject the proposal if the value is not specific to M3
                logger.info("M3 rejected proposalNumber == {}, proposal: {}, because not M3!", proposalNumber, proposalValue);
            }
        } else {
            // Reject the proposal if the proposal number is outdated
            response.setType("REJECT");
            logger.info("M3 rejected proposalNumber == {}, proposal: {}, because version outdated!", proposalNumber, proposalValue);
        }

        // Serialize the response to JSON and send it back to the proposer
        String jsonResponse = GSON.toJson(response);
        out.println(jsonResponse);
    }

    /**
     * Handles the "ACCEPT" phase of the Paxos protocol for incoming requests.
     *
     * @param proposalNumber The proposal number of the incoming request.
     * @param proposalValue  The proposed value from the proposer.
     * @param out            The PrintWriter object to send the response back to the proposer.
     */
    @Override
    public void handleAccept(int proposalNumber, String proposalValue, PrintWriter out) {
        // Check if M3 is disconnected during the ACCEPT phase
        if (DISCONNECTED) {
            logger.info("M3 is camping in the Coorong and is completely disconnected.");
            PaxosCoordinator.setStatusCache(99); // Mark the election process for retry
            return;
        }

        // Create a new response message
        MessageDTO response = new MessageDTO();
        response.setProposalId(proposalNumber);

        // Check if the proposal number is valid for acceptance
        if (proposalNumber >= PROMISED_PROPOSAL_NUMBER.get()) {
            PROMISED_PROPOSAL_NUMBER.set(proposalNumber); // Update the promised proposal number
            response.setType("ACCEPTED"); // Respond with acceptance
            response.setInfo(proposalValue); // Include the proposal value in the response
            logger.info("M3 accepted proposal: {}", proposalValue);
        } else {
            // Reject the proposal if the proposal number is outdated
            response.setType("REJECT");
            logger.info("M3 rejected proposal: {}", proposalValue);
        }

        // Serialize the response to JSON and send it back to the proposer
        out.println(GSON.toJson(response));
    }

    /**
     * Returns the status code associated with M3 when it becomes the chairman.
     *
     * @return An integer representing the status code for M3.
     */
    @Override
    protected int getMemberStatusCode() {
        return 3; // Status code indicating M3's success
    }
}
