package org.apereo.cas.config;

import org.apereo.cas.authentication.CasSSLContext;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.configuration.support.CasFeatureModule;
import org.apereo.cas.consent.ConsentRepository;
import org.apereo.cas.consent.RedisConsentRepository;
import org.apereo.cas.redis.core.CasRedisTemplate;
import org.apereo.cas.redis.core.RedisObjectFactory;
import org.apereo.cas.util.spring.beans.BeanCondition;
import org.apereo.cas.util.spring.beans.BeanSupplier;
import org.apereo.cas.util.spring.boot.ConditionalOnFeature;

import lombok.val;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * This is {@link CasConsentRedisConfiguration}.
 *
 * @author Misagh Moayyed
 * @since 5.2.0
 */
@EnableConfigurationProperties(CasConfigurationProperties.class)
@ConditionalOnFeature(feature = CasFeatureModule.FeatureCatalog.Consent, module = "redis")
@Configuration(value = "CasConsentRedisConfiguration", proxyBeanMethods = false)
public class CasConsentRedisConfiguration {
    private static final BeanCondition CONDITION = BeanCondition.on("cas.consent.redis.enabled").isTrue().evenIfMissing();

    @Bean
    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    public ConsentRepository consentRepository(
        final ConfigurableApplicationContext applicationContext,
        @Qualifier("consentRedisTemplate")
        final CasRedisTemplate consentRedisTemplate,
        final CasConfigurationProperties casProperties) {
        return BeanSupplier.of(ConsentRepository.class)
            .when(CONDITION.given(applicationContext.getEnvironment()))
            .supply(() -> new RedisConsentRepository(consentRedisTemplate, casProperties.getConsent().getRedis().getScanCount()))
            .otherwiseProxy()
            .get();
    }

    @Bean
    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    @ConditionalOnMissingBean(name = "redisConsentConnectionFactory")
    public RedisConnectionFactory redisConsentConnectionFactory(
        final ConfigurableApplicationContext applicationContext,
        @Qualifier(CasSSLContext.BEAN_NAME)
        final CasSSLContext casSslContext,
        final CasConfigurationProperties casProperties) {
        return BeanSupplier.of(RedisConnectionFactory.class)
            .when(CONDITION.given(applicationContext.getEnvironment()))
            .supply(() -> {
                val redis = casProperties.getConsent().getRedis();
                return RedisObjectFactory.newRedisConnectionFactory(redis, casSslContext);
            })
            .otherwiseProxy()
            .get();
    }

    @Bean
    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    @ConditionalOnMissingBean(name = "consentRedisTemplate")
    public CasRedisTemplate consentRedisTemplate(
        final ConfigurableApplicationContext applicationContext,
        @Qualifier("redisConsentConnectionFactory")
        final RedisConnectionFactory redisConsentConnectionFactory) {
        return BeanSupplier.of(CasRedisTemplate.class)
            .when(CONDITION.given(applicationContext.getEnvironment()))
            .supply(() -> RedisObjectFactory.newRedisTemplate(redisConsentConnectionFactory))
            .otherwiseProxy()
            .get();
    }
}
