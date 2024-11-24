package edu.adelaide.council.member;

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

public class M3Member extends Member{
    // 提案编号生成器
    private AtomicInteger proposalNumberGenerator = new AtomicInteger(0);
    private String proposerId;
    private List<String> acceptorAddresses;
    // 保存当前承诺的最高提案编号
    private AtomicInteger promisedProposalNumber = new AtomicInteger(-1);
    // 保存已接受的提案编号及其值
    private int acceptedProposalNumber = -1;
    private String acceptedValue = null;
    // 保存最终被接受的提案编号及其值
    private AtomicInteger finalProposalNumber = new AtomicInteger(-1);
    private String finalProposalValue = null;
    // 模拟M3的网络连接状态
    private boolean isDisconnected;

    public M3Member(List<String> acceptorAddresses) {
        this.acceptorAddresses = acceptorAddresses;
    }

    // Proposer角色：发起提案
    public void propose(String proposalValue) {
        if (isDisconnected) {
            System.out.println("M3 is disconnected (camping in the Coorong), skipping proposal.");
            return;
        }

        int proposalNumber = proposalNumberGenerator.incrementAndGet();
        int promisesReceived = 0;

        // 发送PREPARE请求给所有的接受者
        for (String address : acceptorAddresses) {
            // 模拟有时邮件完全无法到达
            if (ThreadLocalRandom.current().nextInt(10) < 3) {
                System.out.println("M3 failed to send PREPARE to: " + address);
                continue;
            }

            try {
                String[] parts = address.split(":");
                String host = parts[0];
                int port = Integer.parseInt(parts[1]);

                Socket socket = new Socket(host, port);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                out.println("PREPARE," + proposalNumber + "," + proposerId);
                String response = in.readLine();
                String[] responseParts = response.split(",");
                String responseType = responseParts[0];

                if ("PROMISE".equals(responseType)) {
                    promisesReceived++;
                }

                socket.close();
            } catch (Exception e) {
                System.err.println("Failed to send PREPARE to: " + address);
            }
        }

        // 如果收到多数承诺，发送ACCEPT请求
        if (promisesReceived > acceptorAddresses.size() / 2) {
            for (String address : acceptorAddresses) {
                // 模拟有时邮件完全无法到达
                if (ThreadLocalRandom.current().nextInt(10) < 3) {
                    System.out.println("M3 failed to send ACCEPT to: " + address);
                    continue;
                }

                try {
                    String[] parts = address.split(":");
                    String host = parts[0];
                    int port = Integer.parseInt(parts[1]);

                    Socket socket = new Socket(host, port);
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

                    out.println("ACCEPT," + proposalNumber + "," + proposalValue);

                    socket.close();
                } catch (Exception e) {
                    System.err.println("Failed to send ACCEPT to: " + address);
                }
            }
        } else {
            System.out.println("Proposal failed to gather majority promises.");
        }
    }

    // Acceptor角色：处理请求
    public void startAcceptor(int port) {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                System.out.println("M3 Acceptor started on port: " + port);

                while (true) {
                    if (isDisconnected && ThreadLocalRandom.current().nextInt(10) < 5) {
                        System.out.println("M3 is currently disconnected, ignoring incoming connection.");
                        continue;
                    }

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
        // 如果提案编号大于当前承诺的提案编号，则承诺该提案编号
        if (proposalNumber > promisedProposalNumber.get()) {
            promisedProposalNumber.set(proposalNumber);
            out.println("PROMISE," + proposalNumber + "," + acceptedProposalNumber + "," + acceptedValue);
        } else {
            // 拒绝承诺较低的提案编号
            out.println("REJECT," + proposalNumber);
        }
    }

    private void handleAccept(int proposalNumber, String proposalValue, PrintWriter out) {
        // 如果提案编号大于或等于当前承诺的提案编号，则接受该提案
        if (proposalNumber >= promisedProposalNumber.get()) {
            promisedProposalNumber.set(proposalNumber);
            acceptedProposalNumber = proposalNumber;
            acceptedValue = proposalValue;
            out.println("ACCEPTED," + proposalNumber + "," + proposalValue);
        } else {
            // 拒绝接受较低的提案编号
            out.println("REJECT," + proposalNumber);
        }
    }

    // Learner角色：学习最终的提案
    public void startLearner(int port) {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                System.out.println("M3 Learner started on port: " + port);

                while (true) {
                    if (isDisconnected && ThreadLocalRandom.current().nextInt(10) < 5) {
                        System.out.println("M3 is currently disconnected, ignoring incoming connection.");
                        continue;
                    }

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
        }).start();
    }

    private void handleAccepted(int proposalNumber, String proposalValue) {
        // 如果当前提案编号高于之前的编号，更新最终的提案值
        if (proposalNumber > finalProposalNumber.get()) {
            finalProposalNumber.set(proposalNumber);
            finalProposalValue = proposalValue;
            System.out.println("Learner learned new proposal: " + proposalNumber + " with value: " + proposalValue);
        }
    }
}
