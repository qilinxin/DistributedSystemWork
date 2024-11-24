package edu.adelaide.council.member;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class M2Member extends Member{
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
    // 模拟M2网络连接的状态
    private boolean atCafe;

    public M2Member(int acceptorPort, List<String> acceptorAddresses) {
        this.acceptorPort = acceptorPort;
        this.acceptorAddresses = acceptorAddresses;
    }

    // Proposer角色：发起提案
    public void propose(String proposalValue) {
        if (!atCafe) {
            System.out.println("M2 is not at the cafe, skipping proposal due to poor internet.");
            return;
        }

        int proposalNumber = proposalNumberGenerator.incrementAndGet();
        int promisesReceived = 0;

        // 发送PREPARE请求给所有的接受者
        for (String address : acceptorAddresses) {
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
                System.out.println("M2 Acceptor started on port: " + port);

                while (true) {
                    Socket proposerSocket = serverSocket.accept();
                    BufferedReader in = new BufferedReader(new InputStreamReader(proposerSocket.getInputStream()));
                    PrintWriter out = new PrintWriter(proposerSocket.getOutputStream(), true);

                    // 模拟延迟回复
                    if (!atCafe && ThreadLocalRandom.current().nextInt(10) < 7) {
                        System.out.println("M2 is experiencing poor connectivity, skipping this message.");
                        proposerSocket.close();
                        continue;
                    }

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

}
