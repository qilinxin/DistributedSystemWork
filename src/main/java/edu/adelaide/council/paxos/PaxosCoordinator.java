package edu.adelaide.council.paxos;

import edu.adelaide.council.member.*;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

public class PaxosCoordinator {

    private static AtomicInteger proposalNumberGenerator = new AtomicInteger(0);
    private static final Random random = new Random();


    //-1 init; 0 election is over; 99 select again
    private static AtomicInteger STATUS_CODE = new AtomicInteger(-1);
    private final List<Member> members = new ArrayList<>();

    private List<Integer> endResult = Arrays.asList(1,2,3);

    public void runElection() {
        runElection(1);
    }

    // 运行选举过程
    public void runElection(int concurrentNumber) {

        System.out.println("runElection   start222222221");
        int numMembers = 1;

        // 配置每个节点的网络行为
        for (int i = 1; i <= numMembers; i ++) {
            String nodeId = "M" + i;
            int acceptorPort = 5000 + i;  // 分配端口用于Acceptor和Learner

            List<String> acceptorAddresses = new ArrayList<>();
            for (int j = 1; j <= numMembers; j ++) {
                acceptorAddresses.add("localhost:" + (5000 + j));
            }
            // 创建成员实例
            Member member;
            if (i == 1) {
                member = new M1Member(acceptorPort, acceptorAddresses);
            } else if (i == 2) {
                member = new M2Member(acceptorPort, acceptorAddresses);
            } else if (i == 3) {
                member = new M3Member(acceptorPort, acceptorAddresses);
            } else {
                member = new OtherMember(nodeId, acceptorPort, acceptorAddresses);
            }
            members.add(member);
        }

        ExecutorService executorService = Executors.newFixedThreadPool(9);

        for (Member member : members) {
            int acceptorPort = member.getAcceptorPort();
            executorService.execute(() -> {
                member.startAcceptor(acceptorPort);
            });
        }
        Random random = new Random();
        for (int z = 0; z < concurrentNumber; z ++) {
            //TODO change to 3 after test
            int randomInt = random.nextInt(1);
            executePropose(randomInt, executorService);
        }

        // 监控共识状态，如果达成共识，则关闭线程池并结束程序
        AtomicInteger retryTimes = new AtomicInteger(3);
        new Thread(() -> {
            while (!endResult.contains(STATUS_CODE.get())) {
                System.out.println("current status is " + STATUS_CODE.get());
                if (STATUS_CODE.get() == 99) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    if (retryTimes.get() == 0) {
                        // 未能达成共识，关闭所有服务
                        System.out.println("Failure to elect a chairperson within three elections ends the election");
                        executorService.shutdownNow();
                    } else {
                        int randomInt = random.nextInt(1);
                        executePropose(randomInt, executorService);
                        retryTimes.addAndGet(-1);
                    }
                } else {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            // 达成共识，关闭所有服务
            System.out.println("Consensus reached! M" + STATUS_CODE.get() + " become the chairman !!");
            executorService.shutdownNow();
            System.exit(0);
        }).start();
    }

    public void executePropose(int randomInt, ExecutorService executorService) {
        // 调用M1的propose方法
        executorService.execute(() -> {
            try {
                // 等待所有节点初始化完成
                Thread.sleep(2000);
                members.get(randomInt).propose();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }
        });
    }

    // 获取下一个提案编号
    public static int getNextProposalId() {
        return proposalNumberGenerator.incrementAndGet();
    }

    // 获取随机节点编号，避免重复
    private static int getRandomMember(int min, int max, Set<Integer> selectedMembers) {
        int member;
        do {
            member = random.nextInt((max - min) + 1) + min;
        } while (selectedMembers.contains(member));
        return member;
    }

    public static AtomicInteger getStatusCode() {
        return STATUS_CODE;
    }

    public static void setStatusCache(int statusCode) {
        STATUS_CODE.set(statusCode);
    }

}
