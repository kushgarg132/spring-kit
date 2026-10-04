package io.github.kushgarg132.kit;

import io.github.kushgarg132.kit.error.GlobalExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

/** Everything else in the kit is a plain class the app constructs from its own properties. */
@AutoConfiguration
public class KitAutoConfiguration {

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnClass(name = {
        "org.springframework.security.access.AccessDeniedException",
        "org.springframework.dao.DataIntegrityViolationException"
    })
    @ConditionalOnMissingBean(GlobalExceptionHandler.class)
    @ConditionalOnProperty(name = "kit.error-handler.enabled", matchIfMissing = true)
    GlobalExceptionHandler kitGlobalExceptionHandler() {
        return new GlobalExceptionHandler();
    }
}
