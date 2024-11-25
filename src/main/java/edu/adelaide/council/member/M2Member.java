package edu.adelaide.council.member;

import com.google.gson.Gson;
import edu.adelaide.council.paxos.PaxosCoordinator;
import edu.adelaide.council.dto.MessageDTO;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class M2Member extends Member {

    private static final Gson GSON = new Gson();
    private static final AtomicInteger PROMISED_PROPOSAL_NUMBER = new AtomicInteger(-1);
    private static final String ACCEPTED_VALUE = null;

    private static boolean atCafe;

    public M2Member(int acceptorPort, List<String> acceptorAddresses) {
        this.acceptorPort = acceptorPort;
        this.acceptorAddresses = acceptorAddresses;
        atCafe = PaxosCoordinator.getRandomWithProbability(1 - (0.1 * Integer.parseInt(PaxosCoordinator.DELAY_VALUE)));
    }

    // Proposer角色：发起提案
    public void propose() {
        if (!atCafe) {
            System.out.println("M2 has a poor internet connection and may not respond promptly.");
            //需要重新处罚选举流程，设置缓存为指定值
            PaxosCoordinator.setStatusCache(99);
            return;
        }

        int proposalId = PaxosCoordinator.getNextProposalId();
        int agreeCount = 0;
        String proposed_value = "Suggest M2 to become chairman";
        // 发送PREPARE请求给所有的接受者，增加超时重试机制
        for (String address : acceptorAddresses) {
            boolean success = false;
            int retryCount = 0;
            int maxRetries = 3;

            while (!success && retryCount < maxRetries) {
                try {
                    String[] parts = address.split(":");
                    String host = parts[0];
                    int port = Integer.parseInt(parts[1]);

                    try (Socket socket = new Socket(host, port)) {
                        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                        // 构建MessageDTO对象并转换为JSON字符串
                        MessageDTO message = new MessageDTO();
                        message.setType("PREPARE");
                        message.setProposalId(proposalId);
                        message.setInfo(proposed_value);
                        String jsonMessage = GSON.toJson(message);
                        System.out.println("propose jsonMessage: " + jsonMessage);
                        out.println(jsonMessage);
                        String jsonResponse = in.readLine();
                        MessageDTO response = GSON.fromJson(jsonResponse, MessageDTO.class);

                        if ("AGREE".equals(response.getType())) {
                            agreeCount++;
                        }
                        success = true;
                    }
                } catch (SocketTimeoutException e) {
                    retryCount++;
                    System.err.println("Timeout while sending PREPARE to: " + address + ", retrying... (" + retryCount + "/" + maxRetries + ")");
                } catch (Exception e) {
                    System.err.println("Failed to send PREPARE to: " + address);
                    e.printStackTrace();
                    break; // 非超时的异常，跳出重试循环
                }
            }
        }

        // 如果收到多数承诺，发送ACCEPT请求
        if (agreeCount > acceptorAddresses.size() / 2) {
            System.out.println("M2-------Member proposal promise received: " + agreeCount);
            int acceptCount = 0;
            for (String address : acceptorAddresses) {
                boolean success = false;
                int retryCount = 0;
                int maxRetries = 3;

                while (!success && retryCount < maxRetries) {
                    try {
                        String[] parts = address.split(":");
                        String host = parts[0];
                        int port = Integer.parseInt(parts[1]);

                        try (Socket socket = new Socket(host, port)) {
                            socket.setSoTimeout(10000); // 设置超时时间为2000毫秒
                            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                            // 构建MessageDTO对象并转换为JSON字符串
                            MessageDTO message = new MessageDTO();
                            message.setType("ACCEPT");
                            message.setProposalId(proposalId);
                            message.setInfo(proposed_value);
                            String jsonMessage = GSON.toJson(message);
                            System.out.println("ACCEPT jsonMessage: " + jsonMessage);
                            out.println(jsonMessage);

                            String jsonResponse = in.readLine();
                            MessageDTO response = GSON.fromJson(jsonResponse, MessageDTO.class);

                            if ("ACCEPTED".equals(response.getType())) {
                                acceptCount++;
                            }
                            success = true;
                        }
                    } catch (SocketTimeoutException e) {
                        retryCount++;
                        System.err.println("Timeout while sending ACCEPT to: " + address + ", retrying... (" + retryCount + "/" + maxRetries + ")");
                    } catch (Exception e) {
                        System.err.println("Failed to send ACCEPT to: " + address);
                        e.printStackTrace();
                        break; // 非超时的异常，跳出重试循环
                    }
                }
            }

            // 如果超过半数节点接受了提案,更新缓存信息，结束选举
            if (acceptCount > acceptorAddresses.size() / 2) {
                System.out.println("Proposal accepted by majority, ending program...");
                PaxosCoordinator.setStatusCache(2);
            } else {
                //需要重新处罚选举流程，设置缓存为指定值
                PaxosCoordinator.setStatusCache(99);
            }
        } else {
            System.out.println("Proposal failed to gather majority promises.");
            //需要重新处罚选举流程，设置缓存为指定值
            PaxosCoordinator.setStatusCache(99);
        }

    }

    // Acceptor角色：处理请求
    public void startAcceptor(int port) {
        if (!atCafe) {
            System.out.println("M2 has a poor internet connection and may not respond promptly.");
            return;
        }
        System.out.println("M2Member starting acceptor on port " + port);
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
                                System.out.println("M2Member receive proposal:" + proposalValue);
                                handlePrepare(proposalNumber, proposalValue, out);
                                break;
                            case "ACCEPT":
                                System.out.println("M2Member receive result:" + proposalValue);
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

        if (proposalNumber > PROMISED_PROPOSAL_NUMBER.get()) {
            if (proposalValue.contains("M2")) {
                PROMISED_PROPOSAL_NUMBER.set(proposalNumber);
                response.setType("AGREE");
                response.setInfo(ACCEPTED_VALUE);
                System.out.println("M2 agree proposalNumber == " + proposalNumber + ", proposal: " + proposalValue);
            } else {
                System.out.println("M2 reject proposalNumber == " + proposalNumber + ", proposal: " + proposalValue + ", because not M2!!");
            }
        } else {
            response.setType("REJECT");
            System.out.println("M2 reject proposalNumber == " + proposalNumber + ", proposal: " + proposalValue + ", because version outdated!!");
        }

        String jsonResponse = GSON.toJson(response);
        out.println(jsonResponse);
    }

    private void handleAccept(int proposalNumber, String proposalValue, PrintWriter out) {
        MessageDTO response = new MessageDTO();
        response.setProposalId(proposalNumber);

        if (proposalNumber >= PROMISED_PROPOSAL_NUMBER.get()) {
            PROMISED_PROPOSAL_NUMBER.set(proposalNumber);
            response.setType("ACCEPTED");
            response.setInfo(proposalValue);
        } else {
            response.setType("REJECT");
        }

        String jsonResponse = GSON.toJson(response);
        out.println(jsonResponse);
    }
}
