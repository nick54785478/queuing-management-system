package com.example.qms.config.config;

import com.example.qms.config.properties.QmsRedisProperties;
import io.lettuce.core.internal.HostAndPort;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import io.lettuce.core.resource.DnsResolvers;
import io.lettuce.core.resource.MappingSocketAddressResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LettuceConfig {

    /**
     * 設定 Lettuce 的 NAT 映射，解決 Redis Cluster 在 Docker 內部 IP
     * 與外部主機存取 IP 不一致的問題，避免連線 Timeout。
     */
    @Bean(destroyMethod = "shutdown")
    public ClientResources clientResources(QmsRedisProperties qmsRedisProperties) {
        MappingSocketAddressResolver resolver = MappingSocketAddressResolver.create(
                DnsResolvers.JVM_DEFAULT,
                hostAndPort -> {
                    if (qmsRedisProperties.getNatMap() == null) {
                        return hostAndPort;
                    }
                    String key = hostAndPort.getHostText() + ":" + hostAndPort.getPort();
                    String keyWithBrackets = "[" + key + "]";
                    String mapped = qmsRedisProperties.getNatMap().get(key);
                    if (mapped == null) {
                        mapped = qmsRedisProperties.getNatMap().get(keyWithBrackets);
                    }
                    if (mapped != null) {
                        String[] parts = mapped.split(":");
                        return HostAndPort.of(parts[0], Integer.parseInt(parts[1]));
                    }
                    return hostAndPort;
                }
        );

        return DefaultClientResources.builder()
                .socketAddressResolver(resolver)
                .build();
    }
}
