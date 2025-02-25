package github.javaguide.registry.zk;

import github.javaguide.enums.LoadBalanceEnum;
import github.javaguide.enums.RpcErrorMessageEnum;
import github.javaguide.exception.RpcException;
import github.javaguide.extension.ExtensionLoader;
import github.javaguide.loadbalance.LoadBalance;
import github.javaguide.registry.ServiceDiscovery;
import github.javaguide.registry.zk.util.CuratorUtils;
import github.javaguide.remoting.dto.RpcRequest;
import github.javaguide.utils.CollectionUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * 基于ZooKeeper进行服务发现，在Consumer发送RPC请求时sendRpcRequest时，会先来找目的地址
 * service discovery based on zookeeper
 *
 * @author shuang.kou
 * @createTime 2020年06月01日 15:16:00
 */
@Slf4j
public class ZkServiceDiscoveryImpl implements ServiceDiscovery {

    // 随机负载均衡算法或者一致性哈希负载均衡算法
    private final LoadBalance loadBalance;

    public ZkServiceDiscoveryImpl() {
        this.loadBalance = ExtensionLoader.getExtensionLoader(LoadBalance.class).getExtension(LoadBalanceEnum.LOADBALANCE.getName());
    }

    /**
     * 从ZooKeeper中查找一个可用的服务并返回，接受的是RPC请求参数
     *
     * @param rpcRequest rpc service pojo
     * @return
     */
    @Override
    public InetSocketAddress lookupService(RpcRequest rpcRequest) {

        // rpcServiceName = interface name + version + group
        String rpcServiceName = rpcRequest.getRpcServiceName();

        /*
            获取ZooKeeper连接，在ZooKeeper中，服务存储的方式是节点，节点名称：
                /my-rpc/github.javaguide.HelloService/127.0.0.1:9999
            所以要想获得某个服务的全部服务器IP，其实就是找/my-rpc/github.javaguide.HelloService这个节点的全部子节点
            返回的List就是一堆URL，即serviceUrlList这个List<String>
         */
        CuratorFramework zkClient = CuratorUtils.getZkClient();
        List<String> serviceUrlList = CuratorUtils.getChildrenNodes(zkClient, rpcServiceName);
        if (CollectionUtil.isEmpty(serviceUrlList)) {
            // 没有该服务
            throw new RpcException(RpcErrorMessageEnum.SERVICE_CAN_NOT_BE_FOUND, rpcServiceName);
        }
        // load balancing 负载均衡从服务列表里找一个服务器出来
        String targetServiceUrl = loadBalance.selectServiceAddress(serviceUrlList, rpcRequest);
        log.info("Successfully found the service address:[{}]", targetServiceUrl);
        // 返回 127.0.0.1:9999，根据":"分隔开，前面是IP，后面是端口
        String[] socketAddressArray = targetServiceUrl.split(":");
        String host = socketAddressArray[0];
        int port = Integer.parseInt(socketAddressArray[1]);
        return new InetSocketAddress(host, port);
    }
}
