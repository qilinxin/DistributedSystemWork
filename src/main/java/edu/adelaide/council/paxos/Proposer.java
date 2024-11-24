package edu.adelaide.council.paxos;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Proposer {
    // 提案编号生成器
    private AtomicInteger proposalNumberGenerator = new AtomicInteger(0);
    private String proposerId;
    private List<String> acceptorAddresses;

    public Proposer(String proposerId, List<String> acceptorAddresses) {
        this.proposerId = proposerId;
        this.acceptorAddresses = acceptorAddresses;
    }

    public void propose(String proposalValue) {
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

    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: java Proposer <proposerId> <proposalValue> <acceptor1> <acceptor2> ...");
            return;
        }

        String proposerId = args[0];
        String proposalValue = args[1];
        List<String> acceptorAddresses = new ArrayList<>();
        for (int i = 2; i < args.length; i++) {
            acceptorAddresses.add(args[i]);
        }

        Proposer proposer = new Proposer(proposerId, acceptorAddresses);
        proposer.propose(proposalValue);
    }
}
