# RMI Calculator Project

This project is a simple RMI (Remote Method Invocation) based calculator service implemented in Java. The service provides several operations that can be performed on a stack of integers, including pushing values, popping values, and performing mathematical operations such as calculating the minimum, maximum, least common multiple (LCM), and greatest common divisor (GCD) of the stack.

## Project Structure

- **Calculator.java**: The remote interface that defines the methods available for the calculator service. These methods include pushing values onto the stack, performing operations, popping values, checking if the stack is empty, and more.
- **CalculatorImplementation.java**: The implementation of the `Calculator` interface. This class provides the logic for handling the stack operations and the calculation of LCM, GCD, etc.
- **CalculatorServer.java**: The server class that initializes the RMI registry, binds the calculator implementation to a name, and starts the service.
- **CalculatorClient.java**: The client class that interacts with the remote calculator service. This class creates multiple threads to test the stack operations concurrently.

## Installation

1. **Clone the repository:**

   ```bash
   git clone https://github.com/qilinxin/DistributedSystemWork.git
