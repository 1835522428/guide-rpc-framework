package github.javaguide.loadbalance.loadbalancer;

import github.javaguide.loadbalance.AbstractLoadBalance;
import github.javaguide.remoting.dto.RpcRequest;

import java.util.List;
import java.util.Random;

/**
 * 随机负载均衡，直接从所有服务器地址里面随机拿出来一个
 * Implementation of random load balancing strategy
 *
 * @author shuang.kou
 * @createTime 2020年06月21日 07:47:00
 */
public class RandomLoadBalance extends AbstractLoadBalance {
    @Override
    protected String doSelect(List<String> serviceAddresses, RpcRequest rpcRequest) {
        Random random = new Random();
        return serviceAddresses.get(random.nextInt(serviceAddresses.size()));
    }
}
