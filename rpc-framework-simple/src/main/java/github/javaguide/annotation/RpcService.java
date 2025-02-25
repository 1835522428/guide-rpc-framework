package github.javaguide.annotation;


import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 这个接口跟Dubbo里面的定义一样，用于在服务端声明一个提供给外界的方法
 * 参考example-server里面的HelloServiceImpl实现类，只要是提供给外界的实现类，都要加这个方法供RPC框架扫描
 * 这个接口有两个属性：version防止有多个相同接口的实现类；group标明提供服务的集群
 * <p>
 * RPC service annotation, marked on the service implementation class
 *
 * @author shuang.kou
 * @createTime 2020年07月21日 13:11:00
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Inherited
public @interface RpcService {

    /**
     * Service version, default value is empty string
     */
    String version() default "";

    /**
     * Service group, default value is empty string
     */
    String group() default "";

}
