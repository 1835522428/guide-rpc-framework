package github.javaguide.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * 读取.properties文件内容的工具类
 * @author shuang.kou
 * @createTime 2020年07月21日 14:25:00
 **/
@Slf4j
public final class PropertiesFileUtil {
    private PropertiesFileUtil() {
    }

    public static Properties readPropertiesFile(String fileName) {
        // 获取当前线程的类加载器ClassLoader，并加载资源，空字符串代表加载根路径下的资源
        URL url = Thread.currentThread().getContextClassLoader().getResource("");
        String rpcConfigPath = "";
        if (url != null) {
            // 真正的路径是 根路径+文件名
            rpcConfigPath = url.getPath() + fileName;
        }
        Properties properties = null;
        // 加载rpcConfigPath路径的.properties文件
        try (InputStreamReader inputStreamReader = new InputStreamReader(
                new FileInputStream(rpcConfigPath), StandardCharsets.UTF_8)) {
            properties = new Properties();
            properties.load(inputStreamReader);
        } catch (IOException e) {
            log.error("occur exception when read properties file [{}]", fileName);
        }
        return properties;
    }
}
