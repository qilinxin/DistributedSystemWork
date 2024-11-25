package edu.adelaide.council.member;

import com.google.gson.Gson;
import edu.adelaide.council.dto.MessageDTO;
import edu.adelaide.council.paxos.PaxosCoordinator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Abstract base class for all members in the Paxos protocol.
 * This class provides common functionality for Proposer, Acceptor, and Learner roles.
 * Subclasses should implement specific behaviors for each member type.
 */
public abstract class Member {

    // Logger for logging actions and errors
    protected static final Logger logger = LoggerFactory.getLogger(Member.class);

    // JSON serializer/deserializer for messages
    protected static final Gson GSON = new Gson();

    // Atomic variable to track the highest promised proposal number
    protected static final AtomicInteger PROMISED_PROPOSAL_NUMBER = new AtomicInteger(-1);

    // Random generator for simulating network behavior
    protected static final Random RANDOM = new Random();

    // Unique identifier for the member
    protected String nodeId;

    // Port number on which the member accepts connections
    protected int acceptorPort;

    // List of other members' addresses in the network
    protected List<String> acceptorAddresses;

    /**
     * Returns the proposal value specific to the member.
     * Subclasses must implement this method to provide their own proposal value.
     *
     * @return A string representing the proposal value.
     */
    public abstract String getProposedValue();

    /**
     * Handles the "PREPARE" phase of the Paxos protocol for incoming requests.
     * Subclasses must implement this method to define their specific behavior.
     *
     * @param proposalNumber The proposal number of the incoming request.
     * @param proposalValue  The proposed value from the proposer.
     * @param out            The PrintWriter object to send the response back to the proposer.
     */
    public abstract void handlePrepare(int proposalNumber, String proposalValue, PrintWriter out);

    /**
     * Handles the "ACCEPT" phase of the Paxos protocol for incoming requests.
     * Subclasses must implement this method to define their specific behavior.
     *
     * @param proposalNumber The proposal number of the incoming request.
     * @param proposalValue  The proposed value from the proposer.
     * @param out            The PrintWriter object to send the response back to the proposer.
     */
    public abstract void handleAccept(int proposalNumber, String proposalValue, PrintWriter out);

    /**
     * Initiates a proposal as the Proposer in the Paxos protocol.
     * Sends "PREPARE" messages to all acceptors and collects their responses.
     */
    public void propose() {
        int proposalId = PaxosCoordinator.getNextProposalId(); // Generate a unique proposal ID
        int agreeCount = 0; // Count of acceptors that agree to the proposal

        for (String address : acceptorAddresses) {
            boolean success = false;
            int retryCount = 0;
            int maxRetries = 3;

            // Retry mechanism for handling potential network failures
            while (!success && retryCount < maxRetries) {
                try {
                    String[] parts = address.split(":");
                    String host = parts[0];
                    int port = Integer.parseInt(parts[1]);

                    // Send a "PREPARE" message to the acceptor
                    try (Socket socket = new Socket(host, port)) {
                        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                        MessageDTO message = new MessageDTO();
                        message.setType("PREPARE");
                        message.setProposalId(proposalId);
                        message.setInfo(getProposedValue());
                        out.println(GSON.toJson(message));

                        String jsonResponse = in.readLine();
                        MessageDTO response = GSON.fromJson(jsonResponse, MessageDTO.class);

                        if (response != null && "AGREE".equals(response.getType())) {
                            agreeCount++;
                        }
                        success = true; // Exit retry loop on success
                    }
                } catch (SocketTimeoutException e) {
                    retryCount++;
                    logger.warn("Timeout while sending PREPARE to: {}, retrying... ({}/{})", address, retryCount, maxRetries);
                } catch (Exception e) {
                    logger.error("Failed to send PREPARE to: {}", address, e);
                    break; // Exit retry loop on non-recoverable exception
                }
            }
        }

        logger.info("The proposal {} sent by {} received {} agreements", proposalId, nodeId, agreeCount);

        // If majority agreement is reached, proceed to "ACCEPT" phase
        if (agreeCount > acceptorAddresses.size() / 2) {
            sendAccept(proposalId);
        } else {
            PaxosCoordinator.setStatusCache(99); // Mark the election process for retry
        }
    }

    /**
     * Sends "ACCEPT" messages to all acceptors after majority agreement is reached.
     *
     * @param proposalId The ID of the proposal being accepted.
     */
    private void sendAccept(int proposalId) {
        int acceptCount = 0; // Count of acceptors that accept the proposal

        for (String address : acceptorAddresses) {
            boolean success = false;
            int retryCount = 0;
            int maxRetries = 3;

            // Retry mechanism for handling potential network failures
            while (!success && retryCount < maxRetries) {
                try {
                    String[] parts = address.split(":");
                    String host = parts[0];
                    int port = Integer.parseInt(parts[1]);

                    // Send an "ACCEPT" message to the acceptor
                    try (Socket socket = new Socket(host, port)) {
                        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                        MessageDTO message = new MessageDTO();
                        message.setType("ACCEPT");
                        message.setProposalId(proposalId);
                        message.setInfo(getProposedValue());
                        out.println(GSON.toJson(message));

                        String jsonResponse = in.readLine();
                        MessageDTO response = GSON.fromJson(jsonResponse, MessageDTO.class);

                        if (response != null && "ACCEPTED".equals(response.getType())) {
                            acceptCount++;
                        }
                        success = true; // Exit retry loop on success
                    }
                } catch (SocketTimeoutException e) {
                    retryCount++;
                    logger.warn("Timeout while sending ACCEPT to: {}, retrying... ({}/{})", address, retryCount, maxRetries);
                } catch (Exception e) {
                    logger.error("Failed to send ACCEPT to: {}", address, e);
                    break; // Exit retry loop on non-recoverable exception
                }
            }
        }

        logger.info("The proposal {} was accepted by {} members", proposalId, acceptCount);

        // If majority acceptance is reached, update the status code
        if (acceptCount > acceptorAddresses.size() / 2) {
            PaxosCoordinator.setStatusCache(getMemberStatusCode());
        } else {
            PaxosCoordinator.setStatusCache(99); // Mark the election process for retry
        }
    }

    /**
     * Starts the Acceptor role, listening for incoming connections and handling requests.
     *
     * @param port The port number on which the acceptor listens.
     */
    public void startAcceptor(int port) {
        logger.info("{} starting acceptor on port {}", this.getClass().getSimpleName(), port);

        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                while (true) {
                    Socket proposerSocket = serverSocket.accept();
                    try (BufferedReader in = new BufferedReader(new InputStreamReader(proposerSocket.getInputStream()));
                         PrintWriter out = new PrintWriter(proposerSocket.getOutputStream(), true)) {

                        String jsonMessage = in.readLine();
                        MessageDTO message = GSON.fromJson(jsonMessage, MessageDTO.class);

                        String messageType = message.getType();
                        int proposalNumber = message.getProposalId();
                        String proposalValue = message.getInfo();

                        if ("PREPARE".equals(messageType)) {
                            handlePrepare(proposalNumber, proposalValue, out);
                        } else if ("ACCEPT".equals(messageType)) {
                            handleAccept(proposalNumber, proposalValue, out);
                        } else {
                            logger.warn("Unknown message type: {}", messageType);
                        }
                    } catch (Exception e) {
                        logger.error("Error processing request", e);
                    }
                }
            } catch (Exception e) {
                logger.error("Error starting acceptor", e);
            }
        }).start();
    }

    /**
     * Returns the status code associated with the member when it becomes the chairman.
     * Subclasses must implement this method to provide their specific status code.
     *
     * @return An integer representing the status code.
     */
    protected abstract int getMemberStatusCode();

    // Getters and setters for member attributes

    public int getAcceptorPort() {
        return acceptorPort;
    }

    public void setAcceptorPort(int acceptorPort) {
        this.acceptorPort = acceptorPort;
    }

    public List<String> getAcceptorAddresses() {
        return acceptorAddresses;
    }

    public void setAcceptorAddresses(List<String> acceptorAddresses) {
        this.acceptorAddresses = acceptorAddresses;
    }
}
