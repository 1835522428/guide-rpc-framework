package github.javaguide.registry.zk;

import github.javaguide.registry.ServiceRegistry;
import github.javaguide.registry.zk.util.CuratorUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;

import java.net.InetSocketAddress;

/**
 * 向ZooKeeper注册服务
 * ZooKeeper本身是一个树状的目录服务
 * service registration  based on zookeeper
 *
 * @author shuang.kou
 * @createTime 2020年05月31日 10:56:00
 */
@Slf4j
public class ZkServiceRegistryImpl implements ServiceRegistry {

    @Override
    public void registerService(String rpcServiceName, InetSocketAddress inetSocketAddress) {
        /*
            注册节点：/my-rpc/rpcServiceName/SocketAddress，可以看到这个注册只有路径，没有值
                eg: /my-rpc/github.javaguide.HelloService/127.0.0.1:9999
            那么要获取某个服务的所有服务器IP列表怎么办呢？就是获得 /my-rpc/rpcServiceName 这个路径的所有子节点
            因为子节点的名称就是SocketAddress，直接获得名称即可

            通过这种方式就把某个服务器IP提供的某个服务注册到ZooKeeper里面了
         */
        String servicePath = CuratorUtils.ZK_REGISTER_ROOT_PATH + "/" + rpcServiceName + inetSocketAddress.toString();
        // 获取ZooKeeper客户端连接
        CuratorFramework zkClient = CuratorUtils.getZkClient();
        CuratorUtils.createPersistentNode(zkClient, servicePath);
    }
}
