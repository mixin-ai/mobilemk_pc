package com.mobilemouse.pcjava;

import java.net.Inet4Address;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.InetAddress;
import java.util.Collections;

public final class Main {
    public static void main(String[] args) throws Exception {
        int port = 8988;
        if (args.length > 0) {
            try { port = Integer.parseInt(args[0]); } catch (Exception ignored) {}
        }

        System.setProperty("java.awt.headless", "false");
        System.out.println("MobileMouse PC (Java 8) 启动中...");
        System.out.println("监听 ws://0.0.0.0:" + port + "/ws");
        printIPv4();

        MobileMouseServer server = new MobileMouseServer(port);
        server.start();
    }

    private static void printIPv4() {
        try {
            for (NetworkInterface nif : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!nif.isUp() || nif.isLoopback()) continue;
                for (InetAddress addr : Collections.list(nif.getInetAddresses())) {
                    if (addr instanceof Inet4Address) {
                        System.out.println("IPv4: " + addr.getHostAddress());
                    }
                }
            }
        } catch (SocketException e) {
            System.out.println("无法枚举网络接口: " + e.getMessage());
        }
    }
}