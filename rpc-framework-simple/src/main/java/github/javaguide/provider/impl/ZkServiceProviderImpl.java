package github.javaguide.provider.impl;

import github.javaguide.config.RpcServiceConfig;
import github.javaguide.enums.RpcErrorMessageEnum;
import github.javaguide.enums.ServiceRegistryEnum;
import github.javaguide.exception.RpcException;
import github.javaguide.extension.ExtensionLoader;
import github.javaguide.provider.ServiceProvider;
import github.javaguide.registry.ServiceRegistry;
import github.javaguide.remoting.transport.netty.server.NettyRpcServer;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author shuang.kou
 * @createTime 2020年05月13日 11:23:00
 */
@Slf4j
public class ZkServiceProviderImpl implements ServiceProvider {

    /**
     * key: rpc service name(interface name + version + group)
     * value: service object
     */
    private final Map<String, Object> serviceMap;
    private final Set<String> registeredService;
    private final ServiceRegistry serviceRegistry;

    public ZkServiceProviderImpl() {
        serviceMap = new ConcurrentHashMap<>();
        registeredService = ConcurrentHashMap.newKeySet();
        serviceRegistry = ExtensionLoader.getExtensionLoader(ServiceRegistry.class).getExtension(ServiceRegistryEnum.ZK.getName());
    }

    /**
     * 新增一个服务，这里只是把服务放到Map里面，还不向ZooKeeper注册
     * 这个函数由publishService负责调用，在注册到ZooKeeper之前，先放到本地的Map里面
     * <p>
     * 但是要注意，这个Map是放在本地线程的，而Consumer和Provider是两个线程，所以Consumer是看不到这个Map的
     * 这个Map只供Provider自己查看自己注册过哪些服务，在Consumer端要找到一个服务IP还是需要ZooKeeper注册中心
     * （其实这么一想，Redis也可以做这个注册，分布式场景下的数据一致性问题，Consumer和Provider能够互通数据就行）
     *
     * @param rpcServiceConfig rpc service related attributes
     */
    @Override
    public void addService(RpcServiceConfig rpcServiceConfig) {
        String rpcServiceName = rpcServiceConfig.getRpcServiceName();
        // 如果服务已经被注册过了，就返回
        if (registeredService.contains(rpcServiceName)) {
            return;
        }
        registeredService.add(rpcServiceName);
        serviceMap.put(rpcServiceName, rpcServiceConfig.getService());
        log.info("Add service: {} and interfaces:{}", rpcServiceName, rpcServiceConfig.getService().getClass().getInterfaces());
    }

    /**
     * Provider端从Map中直接获取自己注册的服务
     * @param rpcServiceName rpc service name
     * @return
     */
    @Override
    public Object getService(String rpcServiceName) {
        Object service = serviceMap.get(rpcServiceName);
        if (null == service) {
            throw new RpcException(RpcErrorMessageEnum.SERVICE_CAN_NOT_BE_FOUND);
        }
        return service;
    }

    /**
     * 将服务注册到ZooKeeper
     * @param rpcServiceConfig rpc service related attributes
     */
    @Override
    public void publishService(RpcServiceConfig rpcServiceConfig) {
        try {
            String host = InetAddress.getLocalHost().getHostAddress();
            this.addService(rpcServiceConfig);
            serviceRegistry.registerService(rpcServiceConfig.getRpcServiceName(), new InetSocketAddress(host, NettyRpcServer.PORT));
        } catch (UnknownHostException e) {
            log.error("occur exception when getHostAddress", e);
        }
    }

}
