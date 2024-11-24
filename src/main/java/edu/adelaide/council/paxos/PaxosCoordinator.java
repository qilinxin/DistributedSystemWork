package edu.adelaide.council.paxos;

import edu.adelaide.council.member.*;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

public class PaxosCoordinator {

    private static final Random random = new Random();

    //-1 init; 1,2,3 election is over; 99 select again
    private static final AtomicInteger STATUS_CODE = new AtomicInteger(-1);
    private static final List<Member> MEMBERS = new ArrayList<>();
    private static final AtomicInteger PROPOSAL_NUMBER_GENERATOR = new AtomicInteger(0);

    private static final List<Integer> END_RESULT = Arrays.asList(1,2,3);

    private static String DELAY_VALUE= "0";
    public void runElection() {
        runElection(1, "0");
    }

    public void runElection(int concurrentNumber) {
        runElection(concurrentNumber, "0");
    }

    public void runElection(String delayLevel) {
        runElection(1, delayLevel);
    }

    // 运行选举过程
    public void runElection(int concurrentNumber, String delayLevel) {
        DELAY_VALUE = delayLevel;
        System.out.println("runElection   start222222221");
        int numMembers = 9;

        // 配置每个节点的网络行为
        for (int i = 1; i <= numMembers; i ++) {
            Member member = getMember(i, numMembers);
            MEMBERS.add(member);
        }

        ExecutorService executorService = Executors.newFixedThreadPool(9);

        for (Member member : MEMBERS) {
            int acceptorPort = member.getAcceptorPort();
            executorService.execute(() -> {
                member.startAcceptor(acceptorPort);
            });
        }
        Random random = new Random();
        for (int z = 0; z < concurrentNumber; z ++) {
            int randomInt = random.nextInt(3);
            executePropose(randomInt, executorService);
        }

        // 监控共识状态，如果达成共识，则关闭线程池并结束程序
        AtomicInteger retryTimes = new AtomicInteger(3);
        new Thread(() -> {
            while (!END_RESULT.contains(STATUS_CODE.get())) {
                System.out.println("current status is =====" + STATUS_CODE.get());
                if (STATUS_CODE.get() == 99) {
                    if (retryTimes.get() == 0) {
                        // 未能达成共识，关闭所有服务
                        System.out.println("Failure to elect a chairperson within three elections ends the election");
                        executorService.shutdownNow();
                        System.exit(0);
                    } else {
                        retryTimes.decrementAndGet();
                        int randomInt = random.nextInt(3);
                        executePropose(randomInt, executorService);
                    }
                } else {
                    try {
                        Thread.sleep(2000);
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

    private Member getMember(int i, int numMembers) {
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
            member = new OtherMember(nodeId, acceptorPort);
        }
        return member;
    }

    public void executePropose(int randomInt, ExecutorService executorService) {
        System.out.println("executeProposal start--------------------" + randomInt);
        setStatusCache(-1);
        // 调用M1的propose方法
        executorService.execute(() -> {
            try {
                System.out.println("STATUS_CODE ===== " + STATUS_CODE.get());
                // 等待所有节点初始化完成
                Thread.sleep(5000);
                MEMBERS.get(randomInt).propose();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }
        });
    }

    // 获取下一个提案编号
    public static int getNextProposalId() {
        return PROPOSAL_NUMBER_GENERATOR.incrementAndGet();
    }

    public static void setStatusCache(int statusCode) {
        STATUS_CODE.set(statusCode);
    }

    public static void applyDelay() {
        int delayValue = Integer.parseInt(DELAY_VALUE);
        // 定义最大延迟时间的映射（单位：毫秒）
        int[] maxDelays = {500, 1000, 1500, 2000};

        // 验证 delayValue 是否在有效范围内
        if (delayValue < 0 || delayValue >= maxDelays.length) {
            System.out.println("Invalid delayValue. Must be 0, 1, 2, or 3.");
            return;
        }

        // 获取对应的最大延迟时间
        int maxDelay = maxDelays[delayValue];

        // 生成 0 到 maxDelay 之间的随机延迟时间
        Random random = new Random();
        int delay = random.nextInt(maxDelay + 1); // 范围是 [0, maxDelay]

        System.out.println("Applying delay: " + delay + " ms");

        // 应用延迟
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // 恢复中断状态
            System.out.println("Thread was interrupted!");
        }

        System.out.println("Delay completed.");
    }
}
