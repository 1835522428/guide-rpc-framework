package github.javaguide.remoting.transport.socket;

import github.javaguide.enums.ServiceDiscoveryEnum;
import github.javaguide.exception.RpcException;
import github.javaguide.extension.ExtensionLoader;
import github.javaguide.registry.ServiceDiscovery;
import github.javaguide.remoting.dto.RpcRequest;
import github.javaguide.remoting.transport.RpcRequestTransport;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * 基于 Socket 传输 RpcRequest
 *
 * @author shuang.kou
 * @createTime 2020年05月10日 18:40:00
 */
@AllArgsConstructor
@Slf4j
public class SocketRpcClient implements RpcRequestTransport {

    // 服务发现实例，用于连接ZooKeeper，通过负载均衡算法返回一个可用的连接
    private final ServiceDiscovery serviceDiscovery;

    public SocketRpcClient() {
        this.serviceDiscovery = ExtensionLoader.getExtensionLoader(ServiceDiscovery.class).getExtension(ServiceDiscoveryEnum.ZK.getName());
    }

    @Override
    public Object sendRpcRequest(RpcRequest rpcRequest) {
        // 从ZooKeeper（或缓存中）中拿到服务列表，经过负载均衡后返回一个可用的服务器URL
        InetSocketAddress inetSocketAddress = serviceDiscovery.lookupService(rpcRequest);
        // 新建一个socket连接
        try (Socket socket = new Socket()) {
            socket.connect(inetSocketAddress);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            // Send data to the server through the output stream，通过 socket.getOutputStream() 发送请求
            objectOutputStream.writeObject(rpcRequest);
            ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
            // Read RpcResponse from the input stream，通过 socket.getInputStream() 接收返回结果
            // 具体Provider做的是什么事情就要归Provider管了，Provider返回的是一个RpcResponse<Object>类型的结果
            return objectInputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RpcException("调用服务失败:", e);
        }
    }
}
