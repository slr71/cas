package org.apereo.cas.pm.web.flow;

import org.apereo.cas.pm.config.PasswordManagementConfiguration;
import org.apereo.cas.pm.config.PasswordManagementWebflowConfiguration;
import org.apereo.cas.web.flow.BaseWebflowConfigurerTests;
import org.apereo.cas.web.flow.CasWebflowConfigurer;
import org.apereo.cas.web.flow.CasWebflowConstants;
import org.apereo.cas.web.flow.config.CasWebflowAccountProfileConfiguration;

import lombok.val;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.webflow.definition.registry.FlowDefinitionRegistry;
import org.springframework.webflow.engine.Flow;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This is {@link PasswordManagementAccountProfileWebflowConfigurerTests}.
 *
 * @author Misagh Moayyed
 * @since 6.6.0
 */
@Import({
    PasswordManagementConfiguration.class,
    PasswordManagementWebflowConfiguration.class,
    CasWebflowAccountProfileConfiguration.class
})
@Tag("WebflowConfig")
@TestPropertySource(properties = {
    "cas.authn.pm.core.enabled=true",
    "CasFeatureModule.AccountManagement.enabled=true"
})
public class PasswordManagementAccountProfileWebflowConfigurerTests extends BaseWebflowConfigurerTests {
    @Autowired
    @Qualifier("accountProfileFlowRegistry")
    private FlowDefinitionRegistry accountProfileFlowRegistry;

    @Test
    public void verifyOperation() {
        val flow = (Flow) accountProfileFlowRegistry.getFlowDefinition(CasWebflowConfigurer.FLOW_ID_ACCOUNT);
        assertNotNull(flow);
        assertTrue(Arrays.stream(flow.getStartActionList().toArray())
            .anyMatch(ac -> ac.toString().contains(CasWebflowConstants.ACTION_ID_PREPARE_ACCOUNT_PASSWORD_MANAGEMENT)));
    }
}
