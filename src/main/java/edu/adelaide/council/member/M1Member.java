package edu.adelaide.council.member;

import edu.adelaide.council.paxos.PaxosAcceptor;
import edu.adelaide.council.paxos.PaxosProposer;
import edu.adelaide.council.dto.MessageDTO;

import java.io.PrintWriter;
import java.util.List;

/**
 * The M1Member class represents a specific member in the Paxos protocol
 * identified as "M1". This class uses composition to integrate Paxos functionality
 * such as proposing and accepting proposals.
 */
public class M1Member extends Member {

    private final PaxosProposer proposer;
    private final PaxosAcceptor acceptor;

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

        // Initialize Paxos functionalities using composition
        this.proposer = new PaxosProposer(this);
        this.acceptor = new PaxosAcceptor(this);
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
     * Initiates a proposal using the PaxosProposer.
     */
    @Override
    public void propose() {
        proposer.propose();
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
        logger.info("{} PROMISED_PROPOSAL_NUMBER == {}, proposalNumber == {}, proposalNumber >= PROMISED_PROPOSAL_NUMBER.get() is {}",
                nodeId, Member.PROMISED_PROPOSAL_NUMBER.get(), proposalNumber, proposalNumber >= Member.PROMISED_PROPOSAL_NUMBER.get());

        // Check if the proposal number is greater than or equal to the promised number
        if (proposalNumber >= PROMISED_PROPOSAL_NUMBER.get()) {
            // Check if the proposal value is specific to M1
            if (proposalValue.contains("M1")) {
                // Update the promised proposal number
                PROMISED_PROPOSAL_NUMBER.set(proposalNumber);
                // Set the response type to "AGREE"
                response.setType("AGREE");
                logger.info("M1 agree proposalNumber == {}, proposal: {}", proposalNumber, proposalValue);
            } else {
                // Log and handle rejection due to an invalid proposal value
                logger.info("M1 reject proposalNumber == {}, proposal: {}, because not M1!!", proposalNumber, proposalValue);
            }
        } else {
            // Log and handle rejection due to an outdated proposal number
            response.setType("REJECT");
            logger.info("M1 reject proposalNumber == {}, proposal: {}, because version outdated!!", proposalNumber, proposalValue);
        }

        // Serialize the response message to JSON and send it back
        String jsonResponse = GSON.toJson(response);
        out.println(jsonResponse);
    }

    /**
     * Handles the "ACCEPT" phase of the Paxos protocol using PaxosAcceptor.
     *
     * @param proposalNumber The proposal number of the incoming request.
     * @param proposalValue  The proposed value from the proposer.
     * @param out            The PrintWriter object to send the response back to the proposer.
     */
    @Override
    public void handleAccept(int proposalNumber, String proposalValue, PrintWriter out) {
        acceptor.handleAccept(proposalNumber, proposalValue, out);
    }

    /**
     * Returns the status code associated with M1 when it becomes the chairman.
     *
     * @return An integer representing the status code for M1.
     */
    @Override
    public int getMemberStatusCode() {
        return 1; // Status code indicating M1's success
    }
}
