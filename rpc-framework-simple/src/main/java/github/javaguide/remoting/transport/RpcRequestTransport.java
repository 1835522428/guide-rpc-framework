package github.javaguide.remoting.transport;

import github.javaguide.extension.SPI;
import github.javaguide.remoting.dto.RpcRequest;

/**
 * 用来发送一个RPC的Http请求的接口，有两种实现：Socket和Netty
 * send RpcRequest。
 *
 * @author shuang.kou
 * @createTime 2020年05月29日 13:26:00
 */
@SPI
public interface RpcRequestTransport {
    /**
     * send rpc request to server and get result
     *
     * @param rpcRequest message body
     * @return data from server
     */
    Object sendRpcRequest(RpcRequest rpcRequest);
}
