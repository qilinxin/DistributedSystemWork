package edu.adelaide.council.member;


import com.google.gson.Gson;
import edu.adelaide.council.dto.MessageDTO;
import edu.adelaide.council.paxos.PaxosCoordinator;

import java.io.BufferedReader;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.ArrayList;
import java.util.Properties;
import java.io.FileInputStream;

public class OtherMember extends Member{

    private static final Gson gson = new Gson();
    private static final Random random = new Random();

    private String nodeId;
    private AtomicInteger promisedProposalNumber = new AtomicInteger(-1);
    private int acceptedProposalNumber = -1;
    private String acceptedValue = null;
    private AtomicInteger finalProposalNumber = new AtomicInteger(-1);
    private String finalProposalValue = null;

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
                        MessageDTO message = gson.fromJson(jsonMessage, MessageDTO.class);
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

        if (proposalNumber > promisedProposalNumber.get() && random.nextBoolean()) {
            promisedProposalNumber.set(proposalNumber);
            response.setType("AGREE");
            response.setInfo(acceptedValue);
            System.out.println(nodeId + " agree proposal: " + proposalValue);
        } else {
            response.setType("REJECT");
            System.out.println(nodeId + " reject proposal: " + proposalValue);
        }

        String jsonResponse = gson.toJson(response);
        out.println(jsonResponse);
    }

    private void handleAccept(int proposalNumber, String proposalValue, PrintWriter out) {
        MessageDTO response = new MessageDTO();
        response.setProposalId(proposalNumber);

        if (proposalNumber >= promisedProposalNumber.get()) {
            promisedProposalNumber.set(proposalNumber);
            acceptedProposalNumber = proposalNumber;
            acceptedValue = proposalValue;
            response.setType("ACCEPTED");
            response.setInfo(proposalValue);
        } else {
            response.setType("REJECT");
        }

        String jsonResponse = gson.toJson(response);
        out.println(jsonResponse);
    }

}
