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
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class OtherMember extends Member {

    private static final Logger logger = LoggerFactory.getLogger(OtherMember.class);
    private static final Gson GSON = new Gson();
    private static final Random RANDOM = new Random();
    private static final AtomicInteger PROMISED_PROPOSAL_NUMBER = new AtomicInteger(-1);

    private static String ACCEPTED_VALUE = null;

    private final String nodeId;

    public OtherMember(String nodeId, int acceptorPort) {
        this.acceptorPort = acceptorPort;
        this.nodeId = nodeId;
    }

    // Acceptor角色：处理请求
    public void startAcceptor(int port) {
        logger.info("{}Member starting acceptor on port {}", nodeId, port);
        PaxosCoordinator.applyDelay();
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
                        switch (messageType) {
                            case "PREPARE":
                                logger.info("{}Member received proposal: {}", nodeId, proposalValue);
                                handlePrepare(proposalNumber, proposalValue, out);
                                break;
                            case "ACCEPT":
                                logger.info("{}Member received result: {}", nodeId, proposalValue);
                                handleAccept(proposalNumber, proposalValue, out);
                                break;
                            default:
                                logger.warn("Unknown message type: {}", messageType);
                        }
                    } catch (Exception e) {
                        logger.error("Error processing request in acceptor thread", e);
                    }
                }
            } catch (Exception e) {
                logger.error("Error starting acceptor", e);
            }
        }).start();
    }

    private void handlePrepare(int proposalNumber, String proposalValue, PrintWriter out) {
        MessageDTO response = new MessageDTO();
        response.setProposalId(proposalNumber);

        logger.info("{} PROMISED_PROPOSAL_NUMBER == {}, proposalNumber == {}, proposalNumber >= PROMISED_PROPOSAL_NUMBER.get() is {}",
                nodeId, PROMISED_PROPOSAL_NUMBER.get(), proposalNumber, proposalNumber >= PROMISED_PROPOSAL_NUMBER.get());

        if (proposalNumber >= PROMISED_PROPOSAL_NUMBER.get()) {
            response.setInfo(ACCEPTED_VALUE);
            PROMISED_PROPOSAL_NUMBER.set(proposalNumber);

            if (RANDOM.nextBoolean()) {
                response.setType("AGREE");
                logger.info("{} agreed to proposalNumber == {}, proposal: {}", nodeId, proposalNumber, proposalValue);
            } else {
                response.setType("REJECT");
                logger.info("{} rejected proposalNumber == {}, proposal: {}", nodeId, proposalNumber, proposalValue);
            }
        } else {
            response.setType("REJECT");
            logger.info("{} rejected proposalNumber == {}, proposal: {}, because version outdated!!",
                    nodeId, proposalNumber, proposalValue);
        }

        String jsonResponse = GSON.toJson(response);
        out.println(jsonResponse);
    }

    private void handleAccept(int proposalNumber, String proposalValue, PrintWriter out) {
        MessageDTO response = new MessageDTO();
        response.setProposalId(proposalNumber);

        if (proposalNumber >= PROMISED_PROPOSAL_NUMBER.get()) {
            PROMISED_PROPOSAL_NUMBER.set(proposalNumber);
            ACCEPTED_VALUE = proposalValue;
            response.setType("ACCEPTED");
            response.setInfo(proposalValue);
            logger.info("{} accepted proposal: {}", nodeId, proposalValue);
        } else {
            response.setType("REJECT");
            logger.info("{} rejected proposal: {}", nodeId, proposalValue);
        }

        String jsonResponse = GSON.toJson(response);
        out.println(jsonResponse);
    }
}
