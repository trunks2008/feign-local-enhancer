package com.feign.local.config;

import com.feign.local.core.LocalFeignClientRegistrar;
import com.feign.local.properties.LocalFeignProperties;
import feign.Client;
import feign.Contract;
import feign.codec.Decoder;
import feign.codec.Encoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.ribbon.CachingSpringLoadBalancerFactory;
import org.springframework.cloud.openfeign.ribbon.LoadBalancerFeignClient;
import org.springframework.cloud.openfeign.support.ResponseEntityDecoder;
import org.springframework.cloud.openfeign.support.SpringDecoder;
import org.springframework.cloud.openfeign.support.SpringEncoder;
import org.springframework.cloud.openfeign.support.SpringMvcContract;
import org.springframework.context.annotation.*;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.GsonHttpMessageConverter;

/**
 * @author : Hydra
 * @version: 1.0
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({LocalFeignProperties.class})
public class FeignAutoConfiguration {

    @Import({LocalFeignClientRegistrar.class})
    @ConditionalOnProperty(value = "feign.local.enable", havingValue = "true")
    public static class LocalFeignConfiguration{
        static{
            log.info("feign local route started");
        }

        @Bean
        @Primary
        public Contract contract(){
            return new SpringMvcContract();
        }

        @Bean(name = "defaultClient")
        public Client defaultClient(){
            return new Client.Default(null,null);
        }

        @Bean(name = "ribbonClient")
        public Client ribbonClient(CachingSpringLoadBalancerFactory cachingFactory,
                                   SpringClientFactory clientFactory){
            return new LoadBalancerFeignClient(defaultClient(), cachingFactory,
                    clientFactory);
        }

        @Bean
        public Decoder decoder(){
            HttpMessageConverter httpMessageConverter=new GsonHttpMessageConverter();
            ObjectFactory<HttpMessageConverters> messageConverters= () -> new HttpMessageConverters(httpMessageConverter);
            SpringDecoder springDecoder = new SpringDecoder(messageConverters);
            return new ResponseEntityDecoder(springDecoder);
        }

        @Bean
        public Encoder encoder(){
            HttpMessageConverter httpMessageConverter=new GsonHttpMessageConverter();
            ObjectFactory<HttpMessageConverters> messageConverters= () -> new HttpMessageConverters(httpMessageConverter);
            return new SpringEncoder(messageConverters);
        }
    }

    @Configuration
    @ConditionalOnProperty(value = "feign.local.enable", havingValue = "false")
    @EnableFeignClients(basePackages = "${feign.local.basePackage}")
    public static class OriginFeiConfigration{
        static{
            log.info("origin feign");
        }
    }

}
