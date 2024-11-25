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
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class M1Member extends Member {

    private static final Logger log = LoggerFactory.getLogger(M1Member.class);
    private final Gson gson = new Gson();

    private final AtomicInteger promisedProposalNumber = new AtomicInteger(-1);
    private String acceptedValue = null;

    public M1Member(int acceptorPort, List<String> acceptorAddresses) {
        this.acceptorPort = acceptorPort;
        this.acceptorAddresses = acceptorAddresses;
    }

    // Proposer角色：发起提案
    public void propose() {
        int proposalId = PaxosCoordinator.getNextProposalId();
        int agreeCount = 0;
        String proposed_value = "Suggest M1 to become chairman";
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
                        String jsonMessage = gson.toJson(message);
                        log.info("propose jsonMessage: " + jsonMessage);
                        out.println(jsonMessage);
                        String jsonResponse = in.readLine();
                        MessageDTO response = gson.fromJson(jsonResponse, MessageDTO.class);

                        if ("AGREE".equals(response.getType())) {
                            agreeCount++;
                        }
                        success = true;
                    }
                } catch (SocketTimeoutException e) {
                    retryCount++;
                    log.error("Timeout while sending PREPARE to: " + address + ", retrying... (" + retryCount + "/" + maxRetries + ")");
                } catch (Exception e) {
                    log.error("Failed to send REQUEST to: " + address);
                    break; // 非超时的异常，跳出重试循环
                }
            }
        }

        // 如果收到多数承诺，发送ACCEPT请求
        log.info("M1-------Member proposal promise received: " + agreeCount);
        if (agreeCount > acceptorAddresses.size() / 2) {
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
                            socket.setSoTimeout(5000); // 设置超时时间为5000毫秒
                            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                            // 构建MessageDTO对象并转换为JSON字符串
                            MessageDTO message = new MessageDTO();
                            message.setType("ACCEPT");
                            message.setProposalId(proposalId);
                            message.setInfo(proposed_value);
                            String jsonMessage = gson.toJson(message);
                            log.info("ACCEPT jsonMessage: " + jsonMessage);
                            out.println(jsonMessage);

                            String jsonResponse = in.readLine();
                            MessageDTO response = gson.fromJson(jsonResponse, MessageDTO.class);

                            if ("ACCEPTED".equals(response.getType())) {
                                acceptCount++;
                            }
                            success = true;
                        }
                    } catch (SocketTimeoutException e) {
                        retryCount++;
                        log.error("Timeout while sending ACCEPT to: " + address + ", retrying... (" + retryCount + "/" + maxRetries + ")");
                    } catch (Exception e) {
                        log.error("Failed to send ACCEPT to: " + address, e);
                        break; // 非超时的异常，跳出重试循环
                    }
                }
            }

            // 如果超过半数节点接受了提案,更新缓存信息，结束选举
            if (acceptCount > acceptorAddresses.size() / 2) {
                log.info("Proposal accepted by majority, ending program...");
                PaxosCoordinator.setStatusCache(1);
            } else {
                //需要重新触发选举流程，设置缓存为指定值
                PaxosCoordinator.setStatusCache(99);
            }
        } else {
            log.info("Proposal failed to gather majority promises.");
            //需要重新触发选举流程，设置缓存为指定值
            PaxosCoordinator.setStatusCache(99);
        }

    }


    // Acceptor角色：处理请求
    public void startAcceptor(int port) {
        log.info("M1Member starting acceptor on port " + port);
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
                                log.info("M1Member receive proposal:" + proposalValue);
                                handlePrepare(proposalNumber, proposalValue, out);
                                break;
                            case "ACCEPT":
                                log.info("M1Member receive result:" + proposalValue);
                                handleAccept(proposalNumber, proposalValue, out);
                                break;
                            default:
                                log.error("Unknown message type: " + messageType);
                        }
                    } catch (Exception e) {
                        log.error("Error handling proposer socket", e);
                    }
                }
            } catch (Exception e) {
                log.error("Error starting acceptor", e);
            }
        }).start();
    }

    private void handlePrepare(int proposalNumber, String proposalValue, PrintWriter out) {
        MessageDTO response = new MessageDTO();
        response.setProposalId(proposalNumber);

        if (proposalNumber >= promisedProposalNumber.get()) {
            if (proposalValue.contains("M1")) {
                promisedProposalNumber.set(proposalNumber);
                response.setType("AGREE");
                response.setInfo(acceptedValue);
                log.info("M1 agree proposalNumber == " + proposalNumber + ", proposal: " + proposalValue);
            } else {
                log.info("M1 reject proposalNumber == " + proposalNumber + ", proposal: " + proposalValue + ", because not M1!!");
            }
        } else {
            response.setType("REJECT");
            log.info("M1 reject proposalNumber == " + proposalNumber + ", proposal: " + proposalValue + ", because version outdated!!");
        }

        String jsonResponse = gson.toJson(response);
        out.println(jsonResponse);
    }

    private void handleAccept(int proposalNumber, String proposalValue, PrintWriter out) {
        MessageDTO response = new MessageDTO();
        response.setProposalId(proposalNumber);

        if (proposalNumber >= promisedProposalNumber.get()) {
            promisedProposalNumber.set(proposalNumber);
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
