package github.javaguide.remoting.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;

/**
 * 一个RPC请求的所有参数
 * 不难想到，RPC消费者需要调用远端的服务，需要指定：
 *  接口名、方法名、参数列表、各个参数的类型；此外由于一个接口可能有多个实现，可能需要传入版本和集群
 *
 * @author shuang.kou
 * @createTime 2020年05月10日 08:24:00
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
@ToString
public class RpcRequest implements Serializable {
    private static final long serialVersionUID = 1905122041950251207L;
    private String requestId;
    private String interfaceName;
    private String methodName;
    private Object[] parameters;
    private Class<?>[] paramTypes;
    private String version;
    private String group;

    public String getRpcServiceName() {
        return this.getInterfaceName() + this.getGroup() + this.getVersion();
    }
}
