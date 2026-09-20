package com.example.qms.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "qms.redis")
public class QmsRedisProperties {

    private List<String> nodes;
    private Map<String, String> natMap;

    public List<String> getNodes() {
        return nodes;
    }

    public void setNodes(List<String> nodes) {
        this.nodes = nodes;
    }

    public Map<String, String> getNatMap() {
        return natMap;
    }

    public void setNatMap(Map<String, String> natMap) {
        this.natMap = natMap;
    }
}
