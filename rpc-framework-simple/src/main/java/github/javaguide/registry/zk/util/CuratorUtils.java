package github.javaguide.registry.zk.util;

import github.javaguide.enums.RpcConfigEnum;
import github.javaguide.utils.PropertiesFileUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * ZooKeeper核心工具类，其中CuratorFramework是官方提供的ZooKeeper交互类
 * Curator(zookeeper client) utils
 *
 * @author shuang.kou
 * @createTime 2020年05月31日 11:38:00
 */
@Slf4j
public final class CuratorUtils {

    private static final int BASE_SLEEP_TIME = 1000;
    private static final int MAX_RETRIES = 3;
    public static final String ZK_REGISTER_ROOT_PATH = "/my-rpc";   // 注册中心节点，所有数据放在这个节点下："create /path data"、"get /path"、"set /path data"
    private static final Map<String, List<String>> SERVICE_ADDRESS_MAP = new ConcurrentHashMap<>(); // 缓存，第一次调用ZooKeeper查到某个服务的所有提供方之后就保存到这个Map中，后续直接从Map中拿，但是拿到了之后依然要根据负载均衡算法选一个
    private static final Set<String> REGISTERED_PATH_SET = ConcurrentHashMap.newKeySet();   // 存储所有的ZooKeeper注册节点，用ZooKeeper客户端直接去检测也行：zkClient.checkExists().forPath(path)
    private static CuratorFramework zkClient;   // CuratorFramework是Apache Curator库的核心组件，用于简化与Zookeeper的交互
    private static final String DEFAULT_ZOOKEEPER_ADDRESS = "127.0.0.1:2181";   // 默认ZooKeeper地址

    private CuratorUtils() {
    }

    /**
     * 增加一个永久性的节点
     * Create persistent nodes. Unlike temporary nodes, persistent nodes are not removed when the client disconnects
     *
     * @param path node path
     */
    public static void createPersistentNode(CuratorFramework zkClient, String path) {
        try {
            if (REGISTERED_PATH_SET.contains(path) || zkClient.checkExists().forPath(path) != null) {
                // 如果该节点已经存在
                log.info("The node already exists. The node is:[{}]", path);
            } else {
                // 不存在节点，新增一个
                //eg: /my-rpc/github.javaguide.HelloService/127.0.0.1:9999
                zkClient.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).forPath(path);
                log.info("The node was created successfully. The node is:[{}]", path);
            }
            REGISTERED_PATH_SET.add(path);
        } catch (Exception e) {
            log.error("create persistent node for path [{}] fail", path);
        }
    }

    /**
     * 在ZooKeeper中，获取一个节点的全部子节点，这个方法是用来找某个服务的全部提供者IP
     * 因为在存储所有服务提供方时，是通过节点存储的，就像一个树状结构：
     *      /my-rpc/github.javaguide.HelloServicetest2version/127.0.0.1:9999
     *      /my-rpc/github.javaguide.HelloServicetest2version/127.0.0.1:10000
     * 所以如果想要找到 github.javaguide.HelloServicetest2version 这个服务的所有提供方，就要找到
     * /my-rpc/github.javaguide.HelloServicetest2version 的所有子节点，返回的List里面就是一堆的URL
     * <p>
     * 参数：
     *      zkClient：一个ZooKeeper连接
     *      rpcServiceName = interface name + version + group，例如：github.javaguide.HelloServicetest2version
     * <p>
     * Gets the children under a node
     *
     * @param rpcServiceName rpc service name eg:github.javaguide.HelloServicetest2version1
     * @return All child nodes under the specified node
     */
    public static List<String> getChildrenNodes(CuratorFramework zkClient, String rpcServiceName) {
        /*
            这个SERVICE_ADDRESS_MAP是一个本地缓存，如果第一次调用ZooKeeper查到了这个服务的所有提供方IP
            那么就保存到缓存中，后续直接从缓存中拿结果
         */
        if (SERVICE_ADDRESS_MAP.containsKey(rpcServiceName)) {
            return SERVICE_ADDRESS_MAP.get(rpcServiceName);
        }

        /*
            如果缓存中没有，去ZooKeeper中查，就要构造一个“父路径”：/my-rpc/方法标识
         */
        List<String> result = null;
        String servicePath = ZK_REGISTER_ROOT_PATH + "/" + rpcServiceName;
        try {
            result = zkClient.getChildren().forPath(servicePath);
            SERVICE_ADDRESS_MAP.put(rpcServiceName, result);    // 放入缓存中，方便下次查询
            registerWatcher(rpcServiceName, zkClient);  // 注册一个监视器，监听信息变更，防止一直使用缓存数据，如果有服务方信息变更，要及时更新缓存
        } catch (Exception e) {
            log.error("get children nodes for path [{}] fail", servicePath);
        }
        return result;
    }

    /**
     * 挨个清除ZooKeeper中的注册节点
     * Empty the registry of data
     */
    public static void clearRegistry(CuratorFramework zkClient, InetSocketAddress inetSocketAddress) {
        REGISTERED_PATH_SET.stream().parallel().forEach(p -> {
            try {
                if (p.endsWith(inetSocketAddress.toString())) {
                    zkClient.delete().forPath(p);
                }
            } catch (Exception e) {
                log.error("clear registry for path [{}] fail", p);
            }
        });
        log.info("All registered services on the server are cleared:[{}]", REGISTERED_PATH_SET.toString());
    }

    /**
     * 获取一个ZooKeeper连接客户端
     * @return
     */
    public static CuratorFramework getZkClient() {
        // check if user has set zk address，这个其实就是去配置文件读rpc.properties的配置，返回一个Properties对象
        Properties properties = PropertiesFileUtil.readPropertiesFile(RpcConfigEnum.RPC_CONFIG_PATH.getPropertyValue());
        // 尝试从properties文件中获取rpc.zookeeper.address属性，如果没有设置，就返回一个默认的ZooKeeper地址，默认的是"127.0.0.1:2181"
        String zookeeperAddress = properties != null && properties.getProperty(RpcConfigEnum.ZK_ADDRESS.getPropertyValue()) != null ? properties.getProperty(RpcConfigEnum.ZK_ADDRESS.getPropertyValue()) : DEFAULT_ZOOKEEPER_ADDRESS;

        // if zkClient has been started, return directly
        // zkClient就是一个ZooKeeper客户端，如果客户端已经被启动了（zkClient != null），就直接返回客户端连接
        if (zkClient != null && zkClient.getState() == CuratorFrameworkState.STARTED) {
            return zkClient;
        }

        // 如果没有ZooKeeper客户端，就新建一个连接
        // Retry strategy. Retry 3 times, and will increase the sleep time between retries.
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(BASE_SLEEP_TIME, MAX_RETRIES);
        zkClient = CuratorFrameworkFactory.builder()
                // the server to connect to (can be a server list)
                .connectString(zookeeperAddress)
                .retryPolicy(retryPolicy)
                .build();
        zkClient.start();
        try {
            // wait 30s until connect to the zookeeper
            if (!zkClient.blockUntilConnected(30, TimeUnit.SECONDS)) {
                throw new RuntimeException("Time out waiting to connect to ZK!");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return zkClient;
    }

    /**
     * 注册一个ZooKeeper的监听器，监听某个服务的子节点是否会发生变更，变更时也就意味着服务提供方IP变了，要及时更新缓存SERVICE_ADDRESS_MAP
     * Registers to listen for changes to the specified node
     *
     * @param rpcServiceName rpc service name eg:github.javaguide.HelloServicetest2version
     */
    private static void registerWatcher(String rpcServiceName, CuratorFramework zkClient) throws Exception {
        // 监听 /my-rpc/github.javaguide.HelloServicetest2version 这个节点的子节点变更
        String servicePath = ZK_REGISTER_ROOT_PATH + "/" + rpcServiceName;
        PathChildrenCache pathChildrenCache = new PathChildrenCache(zkClient, servicePath, true);

        PathChildrenCacheListener pathChildrenCacheListener = (curatorFramework, pathChildrenCacheEvent) -> {
            // 监听到子节点变更之后，更新缓存 SERVICE_ADDRESS_MAP
            List<String> serviceAddresses = curatorFramework.getChildren().forPath(servicePath);
            SERVICE_ADDRESS_MAP.put(rpcServiceName, serviceAddresses);
        };

        pathChildrenCache.getListenable().addListener(pathChildrenCacheListener);
        pathChildrenCache.start();  // 开启监听线程
    }

}
