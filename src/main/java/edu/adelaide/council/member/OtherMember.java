package edu.adelaide.council.member;


import java.io.BufferedReader;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.ArrayList;
import java.util.Properties;
import java.io.FileInputStream;

public class OtherMember extends Member{
    private String nodeId;
    private AtomicInteger promisedProposalNumber = new AtomicInteger(-1);
    private int acceptedProposalNumber = -1;
    private String acceptedValue = null;
    private AtomicInteger finalProposalNumber = new AtomicInteger(-1);
    private String finalProposalValue = null;

    public OtherMember(String nodeId, List<String> acceptorAddresses) {
        this.nodeId = nodeId;
    }

    public void startAcceptor(int port) {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                System.out.println(nodeId + " Acceptor started on port: " + port);

                while (true) {
                    Socket proposerSocket = serverSocket.accept();
                    BufferedReader in = new BufferedReader(new InputStreamReader(proposerSocket.getInputStream()));
                    PrintWriter out = new PrintWriter(proposerSocket.getOutputStream(), true);

                    String[] message = in.readLine().split(",");
                    String messageType = message[0];
                    int proposalNumber = Integer.parseInt(message[1]);
                    String proposalValue = message.length > 2 ? message[2] : null;

                    switch (messageType) {
                        case "PREPARE":
                            handlePrepare(proposalNumber, out);
                            break;
                        case "ACCEPT":
                            handleAccept(proposalNumber, proposalValue, out);
                            break;
                        default:
                            System.err.println("Unknown message type: " + messageType);
                    }

                    proposerSocket.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void handlePrepare(int proposalNumber, PrintWriter out) {
        if (proposalNumber > promisedProposalNumber.get()) {
            promisedProposalNumber.set(proposalNumber);
            out.println("PROMISE," + proposalNumber + "," + acceptedProposalNumber + "," + acceptedValue);
        } else {
            out.println("REJECT," + proposalNumber);
        }
    }

    private void handleAccept(int proposalNumber, String proposalValue, PrintWriter out) {
        if (proposalNumber >= promisedProposalNumber.get()) {
            promisedProposalNumber.set(proposalNumber);
            acceptedProposalNumber = proposalNumber;
            acceptedValue = proposalValue;
            out.println("ACCEPTED," + proposalNumber + "," + proposalValue);
        } else {
            out.println("REJECT," + proposalNumber);
        }
    }

    public void startLearner(int port) {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                System.out.println(nodeId + " Learner started on port: " + port);

                while (true) {
                    Socket acceptorSocket = serverSocket.accept();
                    BufferedReader in = new BufferedReader(new InputStreamReader(acceptorSocket.getInputStream()));

                    String[] message = in.readLine().split(",");
                    String messageType = message[0];
                    int proposalNumber = Integer.parseInt(message[1]);
                    String proposalValue = message.length > 2 ? message[2] : null;

                    if ("ACCEPTED".equals(messageType)) {
                        handleAccepted(proposalNumber, proposalValue);
                    } else {
                        System.err.println("Unknown message type: " + messageType);
                    }

                    acceptorSocket.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void handleAccepted(int proposalNumber, String proposalValue) {
        if (proposalNumber > finalProposalNumber.get()) {
            finalProposalNumber.set(proposalNumber);
            finalProposalValue = proposalValue;
            System.out.println(nodeId + " learned new proposal: " + proposalNumber + " with value: " + proposalValue);
        }
    }

}
