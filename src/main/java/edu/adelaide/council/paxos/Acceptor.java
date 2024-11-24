package edu.adelaide.council.paxos;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class Acceptor {
    // 保存当前承诺的最高提案编号
    private AtomicInteger promisedProposalNumber = new AtomicInteger(-1);
    // 保存已接受的提案编号及其值（可以根据需要存储其他信息）
    private int acceptedProposalNumber = -1;
    private String acceptedValue = null;
    private Random random = new Random();

    public void start(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Acceptor started on port: " + port);

            while (true) {
                Socket proposerSocket = serverSocket.accept();
                BufferedReader in = new BufferedReader(new InputStreamReader(proposerSocket.getInputStream()));
                PrintWriter out = new PrintWriter(proposerSocket.getOutputStream(), true);

                // 读取来自提议者的请求
                String[] message = in.readLine().split(",");
                String messageType = message[0];
                int proposalNumber = Integer.parseInt(message[1]);
                String proposalValue = message.length > 2 ? message[2] : null;

                switch (messageType) {
                    case "PREPARE":
                        handlePrepare(proposalNumber, proposalValue, out);
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
    }

    private void handlePrepare(int proposalNumber, String proposalValue, PrintWriter out) {
        // 如果提案是M1-M3自身的提案，直接承诺；否则模拟 50% 的概率承诺该提案编号
        if (proposalValue != null && (proposalValue.equals("M1") || proposalValue.equals("M2") || proposalValue.equals("M3"))) {
            promisedProposalNumber.set(proposalNumber);
            out.println("PROMISE," + proposalNumber + "," + acceptedProposalNumber + "," + acceptedValue);
        } else if (proposalNumber > promisedProposalNumber.get() && random.nextBoolean()) {
            promisedProposalNumber.set(proposalNumber);
            out.println("PROMISE," + proposalNumber + "," + acceptedProposalNumber + "," + acceptedValue);
        } else {
            // 拒绝承诺较低的提案编号或随机拒绝
            out.println("REJECT," + proposalNumber);
        }
    }

    private void handleAccept(int proposalNumber, String proposalValue, PrintWriter out) {
        // 如果提案是M1-M3自身的提案，直接接受；否则模拟 50% 的概率接受该提案
        if (proposalValue != null && (proposalValue.equals("M1") || proposalValue.equals("M2") || proposalValue.equals("M3"))) {
            promisedProposalNumber.set(proposalNumber);
            acceptedProposalNumber = proposalNumber;
            acceptedValue = proposalValue;
            out.println("ACCEPTED," + proposalNumber + "," + proposalValue);
        } else if (proposalNumber >= promisedProposalNumber.get() && random.nextBoolean()) {
            promisedProposalNumber.set(proposalNumber);
            acceptedProposalNumber = proposalNumber;
            acceptedValue = proposalValue;
            out.println("ACCEPTED," + proposalNumber + "," + proposalValue);
        } else {
            // 拒绝接受较低的提案编号或随机拒绝
            out.println("REJECT," + proposalNumber);
        }
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java Acceptor <port>");
            return;
        }
        int port = Integer.parseInt(args[0]);
        Acceptor acceptor = new Acceptor();
        acceptor.start(port);
    }
}
