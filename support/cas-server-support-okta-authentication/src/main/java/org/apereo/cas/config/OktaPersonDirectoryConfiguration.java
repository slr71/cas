package org.apereo.cas.config;

import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.configuration.support.CasFeatureModule;
import org.apereo.cas.okta.OktaPersonAttributeDao;
import org.apereo.cas.persondir.PersonDirectoryAttributeRepositoryPlanConfigurer;
import org.apereo.cas.util.CollectionUtils;
import org.apereo.cas.util.function.FunctionUtils;
import org.apereo.cas.util.spring.beans.BeanCondition;
import org.apereo.cas.util.spring.beans.BeanContainer;
import org.apereo.cas.util.spring.beans.BeanSupplier;
import org.apereo.cas.util.spring.boot.ConditionalOnFeature;

import com.okta.sdk.client.Client;
import lombok.val;
import org.apereo.services.persondir.IPersonAttributeDao;
import org.apereo.services.persondir.support.SimpleUsernameAttributeProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ScopedProxyMode;

/**
 * This is {@link OktaPersonDirectoryConfiguration}.
 *
 * @author Misagh Moayyed
 * @since 6.4.0
 */
@Configuration(value = "OktaPersonDirectoryConfiguration", proxyBeanMethods = false)
@EnableConfigurationProperties(CasConfigurationProperties.class)
@ConditionalOnFeature(feature = CasFeatureModule.FeatureCatalog.PersonDirectory, module = "okta")
public class OktaPersonDirectoryConfiguration {
    private static final BeanCondition CONDITION = BeanCondition.on("cas.authn.attribute-repository.okta.organization-url");
    
    @ConditionalOnMissingBean(name = "oktaPersonDirectoryClient")
    @Bean
    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    public Client oktaPersonDirectoryClient(
        final ConfigurableApplicationContext applicationContext,
        final CasConfigurationProperties casProperties) {
        return BeanSupplier.of(Client.class)
            .when(CONDITION.given(applicationContext.getEnvironment()))
            .supply(() -> {
                val properties = casProperties.getAuthn().getAttributeRepository().getOkta();
                return OktaConfigurationFactory.buildClient(properties);
            })
            .otherwiseProxy()
            .get();
    }

    @ConditionalOnMissingBean(name = "oktaPersonAttributeDaos")
    @Bean
    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    public BeanContainer<IPersonAttributeDao> oktaPersonAttributeDaos(
        final ConfigurableApplicationContext applicationContext,
        @Qualifier("oktaPersonDirectoryClient")
        final Client oktaPersonDirectoryClient,
        final CasConfigurationProperties casProperties) {
        return BeanSupplier.of(BeanContainer.class)
            .when(CONDITION.given(applicationContext.getEnvironment()))
            .supply(() -> {
                val properties = casProperties.getAuthn().getAttributeRepository().getOkta();
                val dao = new OktaPersonAttributeDao(oktaPersonDirectoryClient);
                dao.setUsernameAttributeProvider(new SimpleUsernameAttributeProvider(properties.getUsernameAttribute()));
                dao.setOrder(properties.getOrder());
                FunctionUtils.doIfNotNull(properties.getId(), dao::setId);
                return BeanContainer.of(CollectionUtils.wrapList(dao));
            })
            .otherwise(BeanContainer::empty)
            .get();
    }

    @ConditionalOnMissingBean(name = "oktaAttributeRepositoryPlanConfigurer")
    @Bean
    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    public PersonDirectoryAttributeRepositoryPlanConfigurer oktaAttributeRepositoryPlanConfigurer(
        final ConfigurableApplicationContext applicationContext,
        @Qualifier("oktaPersonAttributeDaos")
        final BeanContainer<IPersonAttributeDao> oktaPersonAttributeDaos) {
        return BeanSupplier.of(PersonDirectoryAttributeRepositoryPlanConfigurer.class)
            .when(CONDITION.given(applicationContext.getEnvironment()))
            .supply(() -> plan -> oktaPersonAttributeDaos.toList().forEach(plan::registerAttributeRepository))
            .otherwiseProxy()
            .get();
    }

}
