package edu.adelaide.council.member;

import edu.adelaide.council.dto.MessageDTO;

import java.io.PrintWriter;
import java.util.List;

public class OtherMember extends Member {

    public OtherMember(String nodeId, int acceptorPort) {
        this.nodeId = nodeId;
        this.acceptorPort = acceptorPort;
    }


    @Override
    public String getProposedValue() {
        return "";
    }

    @Override
    public void handlePrepare(int proposalNumber, String proposalValue, PrintWriter out) {
        MessageDTO response = new MessageDTO();
        response.setProposalId(proposalNumber);

//        logger.info("{} PROMISED_PROPOSAL_NUMBER == {}, proposalNumber == {}, proposalNumber >= PROMISED_PROPOSAL_NUMBER.get() is {}",
//                nodeId, PROMISED_PROPOSAL_NUMBER.get(), proposalNumber, proposalNumber >= PROMISED_PROPOSAL_NUMBER.get());

        if (proposalNumber >= PROMISED_PROPOSAL_NUMBER.get()) {
            PROMISED_PROPOSAL_NUMBER.set(proposalNumber);

            if (RANDOM.nextBoolean()) {
                response.setType("AGREE");
                logger.info("{} agreed to proposalNumber == {}, proposal: {}", nodeId, proposalNumber, proposalValue);
            } else {
                response.setType("REJECT");
                logger.info("{} rejected proposalNumber == {}, proposal: {}", nodeId, proposalNumber, proposalValue);
            }
        } else {
            response.setType("REJECT");
            logger.info("{} rejected proposalNumber == {}, proposal: {}, because version outdated!!",
                    nodeId, proposalNumber, proposalValue);
        }

        String jsonResponse = GSON.toJson(response);
        out.println(jsonResponse);
    }

    @Override
    public void handleAccept(int proposalNumber, String proposalValue, PrintWriter out) {
        MessageDTO response = new MessageDTO();
        response.setProposalId(proposalNumber);

        if (proposalNumber >= PROMISED_PROPOSAL_NUMBER.get()) {
            PROMISED_PROPOSAL_NUMBER.set(proposalNumber);
            response.setType("ACCEPTED");
            response.setInfo(proposalValue);
            logger.info("{} accepted proposal: {}", nodeId, proposalValue);
        } else {
            response.setType("REJECT");
            logger.info("{} rejected proposal: {}", nodeId, proposalValue);
        }

        out.println(GSON.toJson(response));
    }

    @Override
    protected int getMemberStatusCode() {
        return 0; // Generic status code for Other members
    }
}
