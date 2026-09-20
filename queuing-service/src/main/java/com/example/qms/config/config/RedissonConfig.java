package com.example.qms.config.config;

import com.example.qms.config.properties.QmsRedisProperties;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

import org.redisson.misc.RedisURI;
import org.redisson.api.NatMapper;

/**
 * Redisson 客戶端配置
 * 用於連接 Redis Cluster 並提供分散式鎖、分散式資料結構等進階功能。
 */
@Configuration
public class RedissonConfig {

    @Value("${spring.data.redis.password}")
    private String redisPassword;



    /**
     * 註冊 RedissonClient Bean
     * 根據上述設定初始化 Redisson 客戶端，並啟用 Cluster 模式與 NAT 映射功能。
     * 
     * @return 初始化的 RedissonClient
     */
    @Bean
    public RedissonClient redissonClient(QmsRedisProperties qmsRedisProperties) {
        Config config = new Config();
        
        config.useClusterServers()
              .addNodeAddress(qmsRedisProperties.getNodes().toArray(new String[0]))
              .setPassword(redisPassword)
              .setNatMapper(new NatMapper() {
                  @Override
                  public RedisURI map(RedisURI uri) {
                      if (qmsRedisProperties.getNatMap() == null) {
                          return uri;
                      }
                      String key = uri.getHost() + ":" + uri.getPort();
                      String keyWithBrackets = "[" + key + "]";
                      String mapped = qmsRedisProperties.getNatMap().get(key);
                      if (mapped == null) {
                          mapped = qmsRedisProperties.getNatMap().get(keyWithBrackets);
                      }
                      if (mapped != null) {
                          String[] parts = mapped.split(":");
                          return new RedisURI(uri.getScheme() + "://" + parts[0] + ":" + parts[1]);
                      }
                      return uri;
                  }
              });

        return Redisson.create(config);
    }
}
