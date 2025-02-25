package github.javaguide;

import github.javaguide.config.RpcServiceConfig;
import github.javaguide.proxy.RpcClientProxy;
import github.javaguide.remoting.transport.RpcRequestTransport;
import github.javaguide.remoting.transport.socket.SocketRpcClient;

/**
 * @author shuang.kou
 * @createTime 2020年05月10日 07:25:00
 */
public class SocketClientMain {
    public static void main(String[] args) {
        RpcRequestTransport rpcRequestTransport = new SocketRpcClient();                            // 新建rpc实例
        RpcServiceConfig rpcServiceConfig = new RpcServiceConfig();                                 // 一个rpc服务端所有的配置项都在这里了
        RpcClientProxy rpcClientProxy = new RpcClientProxy(rpcRequestTransport, rpcServiceConfig);  // 使用本项目的rpc框架，根据配置项生成代理对象
        HelloService helloService = rpcClientProxy.getProxy(HelloService.class);
        String hello = helloService.hello(new Hello("111", "222"));               // 调用代理对象方法，实际上h.invoke()会发送一个网络请求
        System.out.println(hello);
    }
}
