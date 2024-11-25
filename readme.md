
# Paxos Voting Protocol Implementation

## Overview
This project implements a Paxos voting protocol for electing a council president among members. The implementation is built to handle various scenarios of distributed consensus, simulating network delays, disconnections, and concurrent proposals.

### Project Features
- **Concurrent Proposal Handling**: The system can handle simultaneous voting proposals from two council members (M1, M2, etc.), ensuring a correct outcome even when proposals conflict.
- **Full Member Participation**: When all nine members (M1 to M9) respond to voting queries immediately, the implementation efficiently reaches consensus.
- **Dynamic Response Profiles**: Members respond under diverse conditions, including immediate response, small delay, large delay, and complete disconnection. The implementation is resilient, handling scenarios where members like M2 or M3 go offline after initiating a proposal.

## Scoring Criteria
The implementation is evaluated based on the following criteria:

### 1. Concurrent Proposal Handling - **10 Points**
The system is tested to ensure that Paxos works correctly when two council members send proposals at the same time. This involves managing conflicting proposals and ensuring that consensus is reached without deadlock or error.

### 2. Immediate Response from All Members - **30 Points**
In scenarios where all members (M1 to M9) respond immediately to voting proposals, the implementation should reach consensus efficiently. The test verifies the robustness of the Paxos algorithm in a highly responsive environment.

### 3. Variable Response Delays and Member Disconnection - **30 Points**
This feature tests the implementation in a dynamic network environment where members have different response times (immediate, small delay, large delay, no response). Additionally, it verifies that the system continues to function correctly when critical members (e.g., M2 or M3) go offline after sending a proposal. The system's resilience is key to meeting this criterion.


## Configuration
- **DELAY_VALUE**: You can configure network delay levels to simulate different response times from members. The levels can be set from "0" to "3", with "0" being no delay and "3" representing a large delay.
- **Concurrent Number of Proposals**: The system starts a proposer by default, and can start concurrent proposals through configuration

## Dependencies
- **Java 8 or higher**
- **Maven** (for building the project)
- **SLF4J** for logging
- **Gson** for serializing and deserializing JSON messages

## Logging
Logs are handled using SLF4J, and can be configured in the `logback.xml` file. Logs provide detailed information about each step of the proposal and acceptance phases, as well as any retries due to timeouts or disconnections.

