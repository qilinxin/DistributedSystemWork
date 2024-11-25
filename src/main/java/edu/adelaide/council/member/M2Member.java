package edu.adelaide.council.member;

import com.google.gson.Gson;
import edu.adelaide.council.paxos.PaxosCoordinator;
import edu.adelaide.council.dto.MessageDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class M2Member extends Member {

    private static final Logger logger = LoggerFactory.getLogger(M2Member.class);
    private static final Gson GSON = new Gson();
    private static final AtomicInteger PROMISED_PROPOSAL_NUMBER = new AtomicInteger(-1);
    private static final String ACCEPTED_VALUE = null;

    private static boolean atCafe;

    public M2Member(int acceptorPort, List<String> acceptorAddresses) {
        this.acceptorPort = acceptorPort;
        this.acceptorAddresses = acceptorAddresses;
        atCafe = PaxosCoordinator.getRandomWithProbability(1 - (0.33 * Integer.parseInt(PaxosCoordinator.DELAY_VALUE)));
    }

    // Proposer角色：发起提案
    public void propose() {
        if (atCafe) {
            logger.info("M2 has a poor internet connection and may not respond promptly.");
            //需要重新触发选举流程，设置缓存为指定值
            PaxosCoordinator.setStatusCache(99);
            return;
        }

        int proposalId = PaxosCoordinator.getNextProposalId();
        int agreeCount = 0;
        String proposedValue = "Suggest M2 to become chairman";

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
                        message.setInfo(proposedValue);
                        String jsonMessage = GSON.toJson(message);
                        logger.info("Propose jsonMessage: {}", jsonMessage);
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
                    logger.warn("Timeout while sending PREPARE to: {}, retrying... ({}/{})", address, retryCount, maxRetries);
                } catch (Exception e) {
                    logger.error("Failed to send PREPARE to: {}", address, e);
                    break; // 非超时的异常，跳出重试循环
                }
            }
        }

        // 如果收到多数承诺，发送ACCEPT请求
        logger.info("M2-------Member proposal promise received: {}", agreeCount);

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
                            socket.setSoTimeout(10000);
                            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                            // 构建MessageDTO对象并转换为JSON字符串
                            MessageDTO message = new MessageDTO();
                            message.setType("ACCEPT");
                            message.setProposalId(proposalId);
                            message.setInfo(proposedValue);
                            String jsonMessage = GSON.toJson(message);
                            logger.info("ACCEPT jsonMessage: {}", jsonMessage);
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
                        logger.warn("Timeout while sending ACCEPT to: {}, retrying... ({}/{})", address, retryCount, maxRetries);
                    } catch (Exception e) {
                        logger.error("Failed to send ACCEPT to: {}", address, e);
                        break; // 非超时的异常，跳出重试循环
                    }
                }
            }

            // 如果超过半数节点接受了提案，更新缓存信息，结束选举
            if (acceptCount > acceptorAddresses.size() / 2) {
                logger.info("Proposal accepted by majority, ending program...");
                PaxosCoordinator.setStatusCache(2);
            } else {
                PaxosCoordinator.setStatusCache(99);
            }
        } else {
            logger.info("Proposal failed to gather majority promises.");
            PaxosCoordinator.setStatusCache(99);
        }
    }

    // Acceptor角色：处理请求
    public void startAcceptor(int port) {
        if (!atCafe) {
            logger.info("M2 has a poor internet connection and may not respond promptly.");
            return;
        }
        logger.info("M2Member starting acceptor on port {}", port);
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
                                logger.info("M2Member received proposal: {}", proposalValue);
                                handlePrepare(proposalNumber, proposalValue, out);
                                break;
                            case "ACCEPT":
                                logger.info("M2Member received result: {}", proposalValue);
                                handleAccept(proposalNumber, proposalValue, out);
                                break;
                            default:
                                logger.warn("Unknown message type: {}", messageType);
                        }
                    } catch (Exception e) {
                        logger.error("Error processing request", e);
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

        if (proposalNumber >= PROMISED_PROPOSAL_NUMBER.get()) {
            if (proposalValue.contains("M2")) {
                PROMISED_PROPOSAL_NUMBER.set(proposalNumber);
                response.setType("AGREE");
                response.setInfo(ACCEPTED_VALUE);
                logger.info("M2 agreed to proposalNumber == {}, proposal: {}", proposalNumber, proposalValue);
            } else {
                logger.info("M2 rejected proposalNumber == {}, proposal: {}, because not M2!", proposalNumber, proposalValue);
            }
        } else {
            response.setType("REJECT");
            logger.info("M2 rejected proposalNumber == {}, proposal: {}, because version outdated!", proposalNumber, proposalValue);
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
            logger.info("M2 accepted proposal: {}", proposalValue);
        } else {
            response.setType("REJECT");
            logger.info("M2 rejected proposal: {}", proposalValue);
        }

        String jsonResponse = GSON.toJson(response);
        out.println(jsonResponse);
    }
}
