package com.feign.local.core;

import com.feign.local.properties.LocalFeignProperties;
import com.feign.local.util.ComponentScanner;
import com.feign.local.util.FeignCommonUtil;
import feign.Client;
import feign.Contract;
import feign.Feign;
import feign.codec.Decoder;
import feign.codec.Encoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Set;

/**
 * @author : Hydra
 * @version: 1.0
 */
@Slf4j
public class LocalFeignClientRegistrar implements
        ImportBeanDefinitionRegistrar, ResourceLoaderAware,
        EnvironmentAware, BeanFactoryAware{

    private ResourceLoader resourceLoader;
    private BeanFactory beanFactory;
    private Environment environment;

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        ClassPathScanningCandidateComponentProvider scanner = ComponentScanner.getScanner(environment);
        scanner.setResourceLoader(resourceLoader);
        AnnotationTypeFilter annotationTypeFilter = new AnnotationTypeFilter(FeignClient.class);
        scanner.addIncludeFilter(annotationTypeFilter);

        String basePackage =environment.getProperty("feign.local.basePackage");
        log.info("begin to scan {}",basePackage);

        Set<BeanDefinition> candidateComponents = scanner.findCandidateComponents(basePackage);

        for (BeanDefinition candidateComponent : candidateComponents) {
            if (candidateComponent instanceof AnnotatedBeanDefinition) {
                log.info(candidateComponent.getBeanClassName());

                // verify annotated class is an interface
                AnnotatedBeanDefinition beanDefinition = (AnnotatedBeanDefinition) candidateComponent;
                AnnotationMetadata annotationMetadata = beanDefinition.getMetadata();
                Assert.isTrue(annotationMetadata.isInterface(),
                        "@FeignClient can only be specified on an interface");

                Map<String, Object> attributes = annotationMetadata
                        .getAnnotationAttributes(FeignClient.class.getCanonicalName());

                String name = FeignCommonUtil.getClientName(attributes);
                registerFeignClient(registry, annotationMetadata, attributes);
            }
        }
    }

    private void registerFeignClient(BeanDefinitionRegistry registry,
                                     AnnotationMetadata annotationMetadata, Map<String, Object> attributes) {
        String className = annotationMetadata.getClassName();
        Class clazz = ClassUtils.resolveClassName(className, null);
        ConfigurableBeanFactory beanFactory = registry instanceof ConfigurableBeanFactory
                ? (ConfigurableBeanFactory) registry : null;
        String contextId = FeignCommonUtil.getContextId(beanFactory, attributes,environment);
        String name = FeignCommonUtil.getName(attributes,environment);

        BeanDefinitionBuilder definition = BeanDefinitionBuilder
                .genericBeanDefinition(clazz, () -> {
                    Contract contract = beanFactory.getBean(Contract.class);
                    Client defaultClient = (Client) beanFactory.getBean("defaultClient");
                    Client ribbonClient = (Client) beanFactory.getBean("ribbonClient");
                    Encoder encoder = beanFactory.getBean(Encoder.class);
                    Decoder decoder = beanFactory.getBean(Decoder.class);

                    LocalFeignProperties properties = beanFactory.getBean(LocalFeignProperties.class);
                    Map<String, String> addressMapping = properties.getAddressMapping();

                    Feign.Builder builder = Feign.builder()
                            .encoder(encoder)
                            .decoder(decoder)
                            .contract(contract);

                    String serviceUrl = addressMapping.get(name);
                    String originUrl = FeignCommonUtil.getUrl(beanFactory, attributes, environment);

                    Object target;
                    if (StringUtils.hasText(serviceUrl)){
                        target = builder.client(defaultClient)
                                .target(clazz, serviceUrl);
                    }else if (StringUtils.hasText(originUrl)){
                        target = builder.client(defaultClient)
                                .target(clazz,originUrl);
                    }else {
                        target = builder.client(ribbonClient)
                                .target(clazz,"http://"+name);
                    }

                    return target;
                });

        definition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
        definition.setLazyInit(true);
        FeignCommonUtil.validate(attributes);

        AbstractBeanDefinition beanDefinition = definition.getBeanDefinition();
        beanDefinition.setAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE, className);

        // has a default, won't be null
        boolean primary = (Boolean) attributes.get("primary");
        beanDefinition.setPrimary(primary);

        String[] qualifiers = FeignCommonUtil.getQualifiers(attributes);
        if (ObjectUtils.isEmpty(qualifiers)) {
            qualifiers = new String[] { contextId + "FeignClient" };
        }

        BeanDefinitionHolder holder = new BeanDefinitionHolder(beanDefinition, className,
                qualifiers);
        BeanDefinitionReaderUtils.registerBeanDefinition(holder, registry);
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader=resourceLoader;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment=environment;
    }

}
