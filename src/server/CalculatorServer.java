package server;

import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;

public class CalculatorServer {
    public static void main(String[] args) {
        try {
            // 启动 RMI 注册表（如果没有运行）
            LocateRegistry.createRegistry(1099);

            // 创建远程对象实例
            Calculator calculator = new CalculatorImplementation();

            // 将远程对象绑定到 RMI 注册表中
            Naming.rebind("rmi://localhost/CalculatorService", calculator);

            System.out.println("Calculator Service is running...");
        } catch (Exception e) {
            System.out.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }
}
