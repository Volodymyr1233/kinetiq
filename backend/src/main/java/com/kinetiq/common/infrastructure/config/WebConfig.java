package com.kinetiq.common.infrastructure.config;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import org.jspecify.annotations.Nullable;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;
import org.springframework.web.servlet.resource.PathResourceResolver;

@Configuration(proxyBeanMethods = false)
public class WebConfig implements WebMvcConfigurer {

    // the SPA sends its UI language as Accept-Language, so emails follow what the person reads
    @Bean
    LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setSupportedLocales(List.of(Locale.ENGLISH, Locale.forLanguageTag("pl")));
        resolver.setDefaultLocale(Locale.ENGLISH);
        return resolver;
    }

    // client-side routes have no file, so they get index.html; /api and names with an extension stay real 404s
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Resource index = new ClassPathResource("static/index.html");
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected @Nullable Resource getResource(String resourcePath, Resource location)
                            throws IOException {
                        Resource file = location.createRelative(resourcePath);
                        if (file.isReadable()) {
                            return file;
                        }
                        boolean route = !resourcePath.startsWith("api/") && !resourcePath.contains(".");
                        return route && index.isReadable() ? index : null;
                    }
                });
    }
}
