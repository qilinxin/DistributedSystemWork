package edu.adelaide.council.member;


import com.google.gson.Gson;
import edu.adelaide.council.dto.MessageDTO;
import edu.adelaide.council.paxos.PaxosCoordinator;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class OtherMember extends Member{

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
        System.out.println(nodeId + "Member starting acceptor on port " + port);
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
                                System.out.println(nodeId + "Member receive proposal:" + proposalValue);
                                handlePrepare(proposalNumber, proposalValue, out);
                                break;
                            case "ACCEPT":
                                System.out.println(nodeId + "Member receive result:" + proposalValue);
                                handleAccept(proposalNumber, proposalValue, out);
                                break;
                            default:
                                System.err.println("Unknown message type: " + messageType);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void handlePrepare(int proposalNumber, String proposalValue, PrintWriter out) {
        MessageDTO response = new MessageDTO();
        response.setProposalId(proposalNumber);
        System.out.println(nodeId +  " PROMISED_PROPOSAL_NUMBER == " +  PROMISED_PROPOSAL_NUMBER.get() + ", proposalNumber == " + proposalNumber + ",proposalNumber > PROMISED_PROPOSAL_NUMBER.get()" + (proposalNumber > PROMISED_PROPOSAL_NUMBER.get()));

        if (proposalNumber > PROMISED_PROPOSAL_NUMBER.get()) {
            response.setInfo(ACCEPTED_VALUE);
            PROMISED_PROPOSAL_NUMBER.set(proposalNumber);

            if (RANDOM.nextBoolean()) {
                response.setType("AGREE");
                System.out.println(nodeId + " agree proposalNumber == " + proposalNumber + ", proposal: " + proposalValue);
            } else {
                response.setType("REJECT");
                System.out.println(nodeId + " reject proposalNumber == " + proposalNumber + ", proposal: " + proposalValue);
            }
        } else {
            response.setType("REJECT");
            System.out.println(nodeId + " reject proposalNumber == " + proposalNumber + ", proposal: " + proposalValue + ", because version outdated!!");
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
        } else {
            response.setType("REJECT");
        }

        String jsonResponse = GSON.toJson(response);
        out.println(jsonResponse);
    }

}
