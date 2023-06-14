## feign-local-enhancer

### 目的

简化本地环境Feign调用，或指定特地url进行测试。

### 原理

主要是通过`Feign.Builder`来生成FeignClient的代理对象，来替代OpenFeign源码中生成代理对象的过程。

具体介绍可以看我写的这篇：
> https://mp.weixin.qq.com/s/BDdG9oGh8Wew3WGSEN-dwg

### 使用

本地打包：

```shell
mvn clean install
```

引入依赖，这个包中已经导入了`OpenFeign`，所以不需要再额外引入`OpenFeign`了。

```xml
<dependency>
    <groupId>com.cn.hydra</groupId>
    <artifactId>feign-local-enhancer</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

配置文件：

```yml
feign:
  local:
    enable: true
    basePackage: com.service
    addressMapping:
      hydra-service: http://127.0.0.1:8088
      trunks-service: http://127.0.0.1:8099
```

FeignClient：

```java
@FeignClient(value = "hydra-service",
  url = "http://127.0.0.1:8099/")
public interface ClientA {
    @GetMapping("/test/get")
    String get();
}
```
项目启动时无需添加`@EnableFeignClient`注解。


我们在配置文件中配置了`hydra-service`这个微服务的地址为`http://127.0.0.1:8088`，所以这个微服务的请求调用会被打到这个特定地址。

即使我们在`FeignClient`注解上已经配置了这个url，也会被配置文件中的覆盖。

### 使用原生OpenFeign

如果想要使用原生OpenFeign的功能，那么把`enable`改为`false`，basePackage还是扫描FeignClient的路径：

```yml
feign:
  local:
    enable: false
    basePackage: com.service
```
使用这个配置的话，项目启动时仍然无需添加`@EnableFeignClient`注解。