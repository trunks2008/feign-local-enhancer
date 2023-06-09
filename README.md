## feign-local-enhancer



简化本地环境feign调用，或指定特地url进行测试。

本地打包：

```shell
mvn clean install
```

引入依赖：

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

