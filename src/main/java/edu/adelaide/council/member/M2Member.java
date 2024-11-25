package edu.adelaide.council.member;

import edu.adelaide.council.dto.MessageDTO;
import edu.adelaide.council.paxos.PaxosCoordinator;

import java.io.PrintWriter;
import java.util.List;

/**
 * The M2Member class represents a specific member in the Paxos protocol
 * identified as "M2". This member has unique behavior, including
 * random connectivity issues that simulate real-world network conditions.
 */
public class M2Member extends Member {

    // A flag indicating whether M2 is currently at a cafe with poor connectivity
    private static boolean atCafe;

    /**
     * Constructor for M2Member.
     *
     * @param acceptorPort      The port number on which this member accepts connections.
     * @param acceptorAddresses The list of addresses for other members in the Paxos protocol.
     */
    public M2Member(int acceptorPort, List<String> acceptorAddresses) {
        this.nodeId = "M2"; // Set unique identifier for this member
        this.acceptorPort = acceptorPort; // Set the port for accepting connections
        this.acceptorAddresses = acceptorAddresses; // Store the list of other members' addresses

        // Simulate random connectivity issues based on a probability calculation
        atCafe = PaxosCoordinator.getRandomWithProbability(
                1 - (0.33 * Integer.parseInt(PaxosCoordinator.DELAY_VALUE))
        );
    }

    /**
     * Returns the proposal value specific to M2.
     *
     * @return A string representing M2's proposal value.
     */
    @Override
    public String getProposedValue() {
        return "Suggest M2 to become chairman";
    }

    /**
     * Initiates a proposal process if M2 is not experiencing connectivity issues.
     * Overrides the default behavior of the propose method in Member class.
     */
    @Override
    public void propose() {
        // Check connectivity before initiating the proposal
        if (!atCafe) {
            logger.info("M2 has a poor internet connection and may not respond promptly.");
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
        // Simulate connectivity issues during the PREPARE phase
        if (!atCafe) {
            logger.info("M2 has a poor internet connection and may not respond promptly.");
            PaxosCoordinator.setStatusCache(99); // Mark the election process for retry
            return;
        }

        // Create a new response message
        MessageDTO response = new MessageDTO();
        response.setProposalId(proposalNumber);

        // Check if the proposal number is valid and relevant to M2
        if (proposalNumber >= PROMISED_PROPOSAL_NUMBER.get()) {
            if (proposalValue.contains("M2")) {
                PROMISED_PROPOSAL_NUMBER.set(proposalNumber); // Update the promised proposal number
                response.setType("AGREE"); // Respond with agreement
                logger.info("M2 agreed to proposalNumber == {}, proposal: {}", proposalNumber, proposalValue);
            } else {
                // Reject the proposal if the value is not specific to M2
                logger.info("M2 rejected proposalNumber == {}, proposal: {}, because not M2!", proposalNumber, proposalValue);
            }
        } else {
            // Reject the proposal if the proposal number is outdated
            response.setType("REJECT");
            logger.info("M2 rejected proposalNumber == {}, proposal: {}, because version outdated!", proposalNumber, proposalValue);
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
        // Simulate connectivity issues during the ACCEPT phase
        if (!atCafe) {
            logger.info("M2 has a poor internet connection and may not respond promptly.");
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
            logger.info("M2 accepted proposal: {}", proposalValue);
        } else {
            // Reject the proposal if the proposal number is outdated
            response.setType("REJECT");
            logger.info("M2 rejected proposal: {}", proposalValue);
        }

        // Serialize the response to JSON and send it back to the proposer
        out.println(GSON.toJson(response));
    }

    /**
     * Returns the status code associated with M2 when it becomes the chairman.
     *
     * @return An integer representing the status code for M2.
     */
    @Override
    protected int getMemberStatusCode() {
        return 2; // Status code indicating M2's success
    }
}
