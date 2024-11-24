package edu.adelaide.council.paxos;

import edu.adelaide.council.member.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class PaxosCoordinator {

    private static AtomicInteger proposalNumberGenerator = new AtomicInteger(0);
    private static final Random random = new Random();

    public void runElection () {
        int numMembers = 9;


        ExecutorService executorService = Executors.newFixedThreadPool(numMembers);
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(2);
        List<Member> members = new ArrayList<>();


        // 配置每个节点的网络行为
        for (int i = 0; i < numMembers; i ++) {
            String nodeId = "M" + i;
            int port = 5000 + i;  // 分配端口用于Acceptor和Learner

            double disconnectProbability = 0.0;
            if (nodeId.equals("M2")) {
                disconnectProbability = 0.7; // M2网络差，有较高的断开可能
            } else if (nodeId.equals("M3")) {
                disconnectProbability = 0.5; // M3有中等概率断开
            }

            List<String> acceptorAddresses = new ArrayList<>();
            for (int j = 0; j < numMembers; j ++) {
                if (i != j) {
                    acceptorAddresses.add("localhost:" + (5000 + j));
                }
            }

            // 创建成员实例
            Member member;
            if (i == 1) {
                member = new M1Member(acceptorAddresses);
            } else if (i == 2) {
                member = new M2Member(acceptorAddresses);
            } else if (i == 3) {
                member = new M3Member(acceptorAddresses);
            } else {
                member = new OtherMember(nodeId, acceptorAddresses);
            }
            members.add(member);

        }

        // 关闭线程池
        executorService.shutdown();
        scheduledExecutorService.shutdown();
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
}
