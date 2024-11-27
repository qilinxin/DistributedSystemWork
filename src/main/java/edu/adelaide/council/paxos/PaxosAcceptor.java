package edu.adelaide.council.paxos;

import com.google.gson.Gson;
import edu.adelaide.council.dto.MessageDTO;
import edu.adelaide.council.member.Member;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.util.Random;

public class PaxosAcceptor {

    private static final Logger logger = LoggerFactory.getLogger(PaxosAcceptor.class);

    private static final Random RANDOM = new Random();

    // JSON serializer/deserializer for messages
    protected static final Gson GSON = new Gson();
    private final Member member;

    public PaxosAcceptor(Member member) {
        this.member = member;
    }

    public void handlePrepare(int proposalNumber, String proposalValue, PrintWriter out) {
        MessageDTO response = new MessageDTO();
        response.setProposalId(proposalNumber);

        logger.info("{} PROMISED_PROPOSAL_NUMBER == {}, proposalNumber == {}, proposalNumber >= PROMISED_PROPOSAL_NUMBER.get() is {}",
                member.getNodeId(), Member.PROMISED_PROPOSAL_NUMBER.get(), proposalNumber, proposalNumber >= Member.PROMISED_PROPOSAL_NUMBER.get());

        if (proposalNumber >= Member.PROMISED_PROPOSAL_NUMBER.get()) {
            Member.PROMISED_PROPOSAL_NUMBER.set(proposalNumber);

            if (RANDOM.nextBoolean()) {
                response.setType("AGREE");
                logger.info("{} agreed to proposalNumber == {}, proposal: {}", member.getNodeId(), proposalNumber, proposalValue);
            } else {
                response.setType("REJECT");
                logger.info("{} rejected proposalNumber == {}, proposal: {}", member.getNodeId(), proposalNumber, proposalValue);
            }
        } else {
            response.setType("REJECT");
            logger.info("{} rejected proposalNumber == {}, proposal: {}, because version outdated!!",
                    member.getNodeId(), proposalNumber, proposalValue);
        }

        String jsonResponse = GSON.toJson(response);
        out.println(jsonResponse);
    }

    public void handleAccept(int proposalNumber, String proposalValue, PrintWriter out) {
        MessageDTO response = new MessageDTO();
        response.setProposalId(proposalNumber);

        if (proposalNumber >= Member.PROMISED_PROPOSAL_NUMBER.get()) {
            Member.PROMISED_PROPOSAL_NUMBER.set(proposalNumber);
            response.setType("ACCEPTED");
            response.setInfo(proposalValue);
            logger.info("{} accepted proposal: {}", member.getNodeId(), proposalValue);
        } else {
            response.setType("REJECT");
            logger.info("{} rejected proposal: {}", member.getNodeId(), proposalValue);
        }

        String jsonResponse = GSON.toJson(response);
        out.println(jsonResponse);
    }
}
