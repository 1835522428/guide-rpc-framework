package github.javaguide.provider;

import github.javaguide.config.RpcServiceConfig;

/**
 * 一个Service的提供者需要调用的所有函数
 * store and provide service object.
 *
 * @author shuang.kou
 * @createTime 2020年05月31日 16:52:00
 */
public interface ServiceProvider {

    /**
     * @param rpcServiceConfig rpc service related attributes
     */
    void addService(RpcServiceConfig rpcServiceConfig);

    /**
     * @param rpcServiceName rpc service name
     * @return service object
     */
    Object getService(String rpcServiceName);

    /**
     * @param rpcServiceConfig rpc service related attributes
     */
    void publishService(RpcServiceConfig rpcServiceConfig);

}
