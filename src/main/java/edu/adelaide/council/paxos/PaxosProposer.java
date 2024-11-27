package edu.adelaide.council.paxos;

import com.google.gson.Gson;
import edu.adelaide.council.dto.MessageDTO;
import edu.adelaide.council.member.Member;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.List;

public class PaxosProposer {

    private static final Logger logger = LoggerFactory.getLogger(PaxosProposer.class);
    // JSON serializer/deserializer for messages
    protected static final Gson GSON = new Gson();
    private final Member member;

    public PaxosProposer(Member member) {
        this.member = member;
    }

    public void propose() {
        int proposalId = PaxosCoordinator.getNextProposalId();
        int agreeCount = 0;

        for (String address : member.getAcceptorAddresses()) {
            boolean success = false;
            int retryCount = 0;
            int maxRetries = 3;

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
                        message.setInfo(member.getProposedValue());
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

        logger.info("The proposal {} sent by {} received {} agreements", proposalId, member.getNodeId(), agreeCount);

        // If majority agreement is reached, send "ACCEPT"
        if (agreeCount > member.getAcceptorAddresses().size() / 2) {
            sendAccept(proposalId);
        } else {
            PaxosCoordinator.setStatusCache(99);
        }
    }

    private void sendAccept(int proposalId) {
        int acceptCount = 0; // Count of acceptors that accept the proposal
        for (String address : member.getAcceptorAddresses()) {
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
                        message.setInfo(member.getProposedValue());
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
        if (acceptCount > member.getAcceptorAddresses().size() / 2) {
            PaxosCoordinator.setStatusCache(member.getMemberStatusCode());
        } else {
            PaxosCoordinator.setStatusCache(99); // Mark the election process for retry
        }
    }
}
