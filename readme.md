
# Paxos Voting Protocol Implementation

## Overview
This project is a voting system that uses thread pool management and sockets to transmit information. The system will create corresponding threads according to the number of members at startup and start listening on the corresponding port.
1. Randomly select one between M1-M3
2. The selected member initiates the voting action
3. Obtain the opinions of all members
4. If more than half of them agree, start transmitting the election results
5. End the election after more than half of the members receive the data
6. If no consensus is reached, jump to the first step again

### Project Features
- **Concurrent Proposal Handling**: The system can handle simultaneous voting proposals from two council members (M1, M2, etc.), ensuring a correct outcome even when proposals conflict.
- **Full Member Participation**: When all nine members (M1 to M9) respond to voting queries immediately, the implementation efficiently reaches consensus.
- **Dynamic Response Profiles**: Members respond under diverse conditions, including immediate response, small delay, large delay, and complete disconnection. The implementation is resilient, handling scenarios where members like M2 or M3 go offline after initiating a proposal.

## Scoring Criteria
The implementation is evaluated based on the following criteria:

### 1. Concurrent Proposal Handling - **10 Points**
The case start entry is TestCase1. By passing in different concurrent numbers, multiple proposers are selected to vote at the same time. The system will obtain the correct result based on the proposal version number. For detailed result log, see TestCase1.log

### 2. Immediate Response from All Members - **30 Points**
The case start entry is TestCase2. In this case, M2 and M3 must participate in the voting process. For the result log, see TestCase2.log

### 3. Variable Response Delays and Member Disconnection - **30 Points**
The case start entry is TestCase3. In this case, the probability of M2 and M3 being offline is magnified, and they may not be able to participate in the voting. For the result log, see TestCase3.log

## Configuration
- **DELAY_VALUE**: You can configure network delay levels to simulate different response times from members. The levels can be set from "0" to "3", with "0" being no delay and "3" representing a large delay.
- **Concurrent Number of Proposals**: The system starts a proposer by default, and can start concurrent proposals through configuration

## Dependencies
- **Java 8 or higher**
- **Maven** (for building the project)
- **SLF4J** for logging
- **Gson** for serializing and deserializing JSON messages

