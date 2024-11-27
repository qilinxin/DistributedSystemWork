package edu.adelaide.council.member;

import edu.adelaide.council.dto.MessageDTO;
import edu.adelaide.council.paxos.PaxosAcceptor;

import java.io.PrintWriter;
import java.util.List;

/**
 * The OtherMember class represents a generic member in the Paxos protocol.
 * This member has standard behavior, including random acceptance or rejection of proposals.
 */
public class OtherMember extends Member {

    private final PaxosAcceptor acceptor;

    /**
     * Constructor for OtherMember.
     *
     * @param nodeId       The unique identifier for this member.
     * @param acceptorPort The port number on which this member accepts connections.
     */
    public OtherMember(String nodeId, int acceptorPort) {
        this.nodeId = nodeId; // Set unique identifier for this member
        this.acceptorPort = acceptorPort; // Set the port for accepting connections

        // Initialize Paxos functionalities using composition
        this.acceptor = new PaxosAcceptor(this);
    }

    /**
     * Returns the proposal value specific to this member.
     *
     * @return An empty string, as generic members do not propose values.
     */
    @Override
    public String getProposedValue() {
        return ""; // Generic members do not propose values
    }

    /**
     * Initiates a proposal process using PaxosProposer.
     * In this case, OtherMember does not initiate proposals, so this method is not overridden.
     */
    // No need to override the propose method since OtherMember does not initiate proposals

    /**
     * Handles the "PREPARE" phase of the Paxos protocol for incoming requests.
     *
     * @param proposalNumber The proposal number of the incoming request.
     * @param proposalValue  The proposed value from the proposer.
     * @param out            The PrintWriter object to send the response back to the proposer.
     */
    @Override
    public void handlePrepare(int proposalNumber, String proposalValue, PrintWriter out) {
        // Delegate to PaxosAcceptor for handling the prepare request
        acceptor.handlePrepare(proposalNumber, proposalValue, out);
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
        // Delegate to PaxosAcceptor for handling the accept request
        acceptor.handleAccept(proposalNumber, proposalValue, out);
    }

    /**
     * Returns the status code associated with OtherMember.
     *
     * @return A generic status code for Other members.
     */
    @Override
    public int getMemberStatusCode() {
        return 0; // Generic status code for Other members
    }
}
