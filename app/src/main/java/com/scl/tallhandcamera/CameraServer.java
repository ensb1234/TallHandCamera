package com.scl.tallhandcamera;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;

import org.java_websocket.WebSocket;
import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

public class CameraServer extends WebSocketServer {

    public WebSocket user;
    //无参构造函数默认使用80端口，据说小于1024需要root权限，故应使用如下几个重载，以指定端口。
    public CameraServer(int port) throws UnknownHostException {
        //使用通配符地址（0.0.0.0）和指定端口号创建服务端。
        super(new InetSocketAddress(port));
    }

    public CameraServer(InetSocketAddress address) {
        //比起上面，InetSocketAddress在new时可以指定ip地址。
        super(address);
    }

    public CameraServer(int port, Draft_6455 draft) {
        //除确定ip和端口外，还确定必须遵守的协议。
        super(new InetSocketAddress(port), Collections.<Draft>singletonList(draft));
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        //新的客户端加入成功时发生。
        conn.send("Welcome to the server!"); //发给新加入的客户端
        broadcast("new connection: " + handshake.getResourceDescriptor()); //发给所有已经连接的客户端。
        System.out.println(conn.getRemoteSocketAddress().getAddress().getHostAddress() + " entered the room!");
        if(null == user) {
            user = conn;
        }
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        //关闭连接之后调用。
        broadcast(conn + " has left the room!");//通知其他客户端，已经有客户端离开。
        System.out.println(conn + " has left the room!");
        if(conn == user){
            user = null;
        }
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        //群发回调，字符串
        broadcast(message);
        System.out.println(conn + ": " + message);
    }

    @Override
    public void onMessage(WebSocket conn, ByteBuffer message) {
        //群发回调，字节流
        broadcast(message.array());
        System.out.println(conn + ": " + message);
    }

    //原例子中的实现，可参考
    public static void main(String[] args) throws InterruptedException, IOException {
        int port = 8887; // 843 flash policy port
        try {
            port = Integer.parseInt(args[0]);
        } catch (Exception ex) {
        }
        CameraServer s = new CameraServer(port);
        s.start();
        System.out.println("ChatServer started on port: " + s.getPort());

        BufferedReader sysin = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            String in = sysin.readLine();
            s.broadcast(in);
            if (in.equals("exit")) {
                s.stop(1000);
                break;
            }
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();//打印错误流
        if (conn != null) {
            // some errors like port binding failed may not be assignable to a specific websocket
        }
    }

    @Override
    public void onStart() {
        //在服务器成功启动时调用。如果发生任何错误，则调用onError。
        System.out.println("Server started!");
        setConnectionLostTimeout(0);//不是很懂为什么要先停用
        setConnectionLostTimeout(100);//每100毫秒检查丢失连接
    }

    /**
     * 获取设备的局域网 IP 地址。
     * @return 设备的局域网 IP 地址，如果无法获取则返回 null。
     */
    public static String getLocalIpAddress() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        // 这里过滤了回环地址，通常我们需要的是 IPv4 地址
                        String sAddr = addr.getHostAddress().toUpperCase();
                        boolean isIPv4 = addr instanceof java.net.Inet4Address;
                        if (isIPv4)
                            return sAddr; // 返回第一个非回环的 IPv4 地址
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }
}
