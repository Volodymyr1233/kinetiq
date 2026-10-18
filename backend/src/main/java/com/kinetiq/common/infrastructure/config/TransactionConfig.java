package com.kinetiq.common.infrastructure.config;

import org.springframework.aop.Advisor;
import org.springframework.aop.ClassFilter;
import org.springframework.aop.support.ComposablePointcut;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.interceptor.MatchAlwaysTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.util.ClassUtils;

/** Runs every bean implementing a {@code domain.input.*UseCase} interface in a transaction, keeping Spring out of the domain. */
@Configuration(proxyBeanMethods = false)
@EnableTransactionManagement
public class TransactionConfig {

    // every use case method gets a read-write transaction; add a read-only split if queries need it
    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE) // required: the transaction auto-proxy only applies infrastructure advisors
    static Advisor useCaseTransactionAdvisor(BeanFactory beanFactory) {
        ClassFilter implementsUseCase = type -> ClassUtils.getAllInterfacesForClassAsSet(type).stream()
                .anyMatch(TransactionConfig::isUseCase);
        // transaction manager is looked up on first call, so this early advisor does not pull persistence into startup
        TransactionInterceptor interceptor = new TransactionInterceptor();
        interceptor.setTransactionAttributeSource(new MatchAlwaysTransactionAttributeSource());
        interceptor.setBeanFactory(beanFactory);
        return new DefaultPointcutAdvisor(new ComposablePointcut(implementsUseCase), interceptor);
    }

    private static boolean isUseCase(Class<?> type) {
        return type.getSimpleName().endsWith("UseCase")
                && type.getPackageName().startsWith("com.kinetiq.")
                && type.getPackageName().contains(".domain.input");
    }
}
