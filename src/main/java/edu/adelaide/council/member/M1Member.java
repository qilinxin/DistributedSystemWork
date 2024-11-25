package edu.adelaide.council.member;

import edu.adelaide.council.dto.MessageDTO;

import java.io.PrintWriter;
import java.util.List;

/**
 * The M1Member class represents a specific member in the Paxos protocol
 * identified as "M1". This class extends the generic Member class
 * and provides specific implementations for handling prepare and accept requests.
 */
public class M1Member extends Member {

    /**
     * Constructor for M1Member.
     *
     * @param acceptorPort      The port number on which this member accepts connections.
     * @param acceptorAddresses The list of addresses for other members in the Paxos protocol.
     */
    public M1Member(int acceptorPort, List<String> acceptorAddresses) {
        this.nodeId = "M1"; // Unique identifier for this member
        this.acceptorPort = acceptorPort; // Port number for accepting connections
        this.acceptorAddresses = acceptorAddresses; // List of acceptor addresses
    }

    /**
     * Returns the proposal value specific to M1.
     *
     * @return A string representing M1's proposal value.
     */
    @Override
    public String getProposedValue() {
        return "Suggest M1 to become chairman";
    }

    /**
     * Handles the "PREPARE" phase of the Paxos protocol.
     *
     * @param proposalNumber The proposal number of the incoming request.
     * @param proposalValue  The proposed value from the proposer.
     * @param out            The PrintWriter object to send the response back to the proposer.
     */
    @Override
    public void handlePrepare(int proposalNumber, String proposalValue, PrintWriter out) {
        // Create a new response message
        MessageDTO response = new MessageDTO();
        response.setProposalId(proposalNumber);

        // Check if the proposal number is greater than or equal to the promised number
        if (proposalNumber >= PROMISED_PROPOSAL_NUMBER.get()) {
            // Check if the proposal value is specific to M1
            if (proposalValue.contains("M1")) {
                // Update the promised proposal number
                PROMISED_PROPOSAL_NUMBER.set(proposalNumber);
                // Set the response type to "AGREE"
                response.setType("AGREE");
                logger.info("M1 agree proposalNumber == " + proposalNumber + ", proposal: " + proposalValue);
            } else {
                // Log and handle rejection due to an invalid proposal value
                logger.info("M1 reject proposalNumber == " + proposalNumber + ", proposal: " + proposalValue + ", because not M1!!");
            }
        } else {
            // Log and handle rejection due to an outdated proposal number
            response.setType("REJECT");
            logger.info("M1 reject proposalNumber == " + proposalNumber + ", proposal: " + proposalValue + ", because version outdated!!");
        }

        // Serialize the response message to JSON and send it back
        String jsonResponse = GSON.toJson(response);
        out.println(jsonResponse);
    }

    /**
     * Handles the "ACCEPT" phase of the Paxos protocol.
     *
     * @param proposalNumber The proposal number of the incoming request.
     * @param proposalValue  The proposed value from the proposer.
     * @param out            The PrintWriter object to send the response back to the proposer.
     */
    @Override
    public void handleAccept(int proposalNumber, String proposalValue, PrintWriter out) {
        // Create a new response message
        MessageDTO response = new MessageDTO();
        response.setProposalId(proposalNumber);

        // Check if the proposal number is greater than or equal to the promised number
        if (proposalNumber >= PROMISED_PROPOSAL_NUMBER.get()) {
            // Update the promised proposal number
            PROMISED_PROPOSAL_NUMBER.set(proposalNumber);
            // Set the response type to "ACCEPTED"
            response.setType("ACCEPTED");
            response.setInfo(proposalValue);
            logger.info("M1 accepted proposal: {}", proposalValue);
        } else {
            // Log and handle rejection due to an outdated proposal number
            response.setType("REJECT");
            logger.info("M1 rejected proposal: {}", proposalValue);
        }

        // Serialize the response message to JSON and send it back
        out.println(GSON.toJson(response));
    }

    /**
     * Returns the status code associated with M1 when it becomes the chairman.
     *
     * @return An integer representing the status code for M1.
     */
    @Override
    protected int getMemberStatusCode() {
        return 1; // Status code indicating M1's success
    }
}
