package edu.adelaide.council.paxos;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;

public class Learner {
    // 保存最终被接受的提案编号及其值
    private AtomicInteger finalProposalNumber = new AtomicInteger(-1);
    private String finalProposalValue = null;

    public void start(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Learner started on port: " + port);

            while (true) {
                Socket acceptorSocket = serverSocket.accept();
                BufferedReader in = new BufferedReader(new InputStreamReader(acceptorSocket.getInputStream()));

                // 读取来自接受者的通知
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
    }

    private void handleAccepted(int proposalNumber, String proposalValue) {
        // 如果当前提案编号高于之前的编号，更新最终的提案值
        if (proposalNumber > finalProposalNumber.get()) {
            finalProposalNumber.set(proposalNumber);
            finalProposalValue = proposalValue;
            System.out.println("Learner learned new proposal: " + proposalNumber + " with value: " + proposalValue);
        }
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java Learner <port>");
            return;
        }
        int port = Integer.parseInt(args[0]);
        Learner learner = new Learner();
        learner.start(port);
    }
}

