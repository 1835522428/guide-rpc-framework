package github.javaguide.loadbalance;

import github.javaguide.extension.SPI;
import github.javaguide.remoting.dto.RpcRequest;

import java.util.List;

/**
 * 负载均衡接口，主要方法就是选一个服务器进行RPC请求
 * 这个方法传入两个参数：所有可选服务器列表和当前的RPC请求
 * Interface to the load balancing policy
 *
 * @author shuang.kou
 * @createTime 2020年06月21日 07:44:00
 */
@SPI
public interface LoadBalance {
    /**
     * Choose one from the list of existing service addresses list
     *
     * @param serviceUrlList Service address list
     * @param rpcRequest
     * @return target service address
     */
    String selectServiceAddress(List<String> serviceUrlList, RpcRequest rpcRequest);
}
