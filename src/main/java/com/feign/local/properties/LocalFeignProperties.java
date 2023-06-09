package com.feign.local.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author : Hydra
 * @version: 1.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "feign.local")
public class LocalFeignProperties {

    // 是否开启本地路由
    private String enable;

    //扫描FeignClient的包名
    private String basePackage;

    //路由地址映射
    private Map<String,String> addressMapping;

}
