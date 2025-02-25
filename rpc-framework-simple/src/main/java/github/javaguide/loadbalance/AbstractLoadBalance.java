package github.javaguide.loadbalance;

import github.javaguide.remoting.dto.RpcRequest;
import github.javaguide.utils.CollectionUtil;

import java.util.List;

/**
 * 负载均衡的抽象实现类，这个里面的doSelect方法是抽象方法，所以这个类本身不能直接用
 * 只有RandomLoadBalance和ConsistentHashLoadBalance是可以直接调用的
 * Abstract class for a load balancing policy
 *
 * @author shuang.kou
 * @createTime 2020年06月21日 07:44:00
 */
public abstract class AbstractLoadBalance implements LoadBalance {
    @Override
    public String selectServiceAddress(List<String> serviceAddresses, RpcRequest rpcRequest) {
        // 如果可选服务列表为空，那就直接返回null
        if (CollectionUtil.isEmpty(serviceAddresses)) {
            return null;
        }
        // 只有一个RPC服务可选，直接返回
        if (serviceAddresses.size() == 1) {
            return serviceAddresses.get(0);
        }
        // 执行负载均衡算法，RandomLoadBalance是随机负载均衡，从列表中随机选出来一个
        // ConsistentHashLoadBalance是一致性哈希负载均衡
        return doSelect(serviceAddresses, rpcRequest);
    }

    protected abstract String doSelect(List<String> serviceAddresses, RpcRequest rpcRequest);

}
