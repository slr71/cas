package org.apereo.cas.pm.web.flow.actions;

import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.pm.PasswordManagementQuery;
import org.apereo.cas.pm.PasswordManagementService;
import org.apereo.cas.pm.web.flow.PasswordManagementWebflowUtils;
import org.apereo.cas.web.flow.actions.BaseCasWebflowAction;
import org.apereo.cas.web.support.WebUtils;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

/**
 * This is {@link AccountProfilePreparePasswordManagementAction}.
 *
 * @author Misagh Moayyed
 * @since 6.6.0
 */
@RequiredArgsConstructor
@Getter
public class AccountProfilePreparePasswordManagementAction extends BaseCasWebflowAction {
    private final PasswordManagementService passwordManagementService;

    private final CasConfigurationProperties casProperties;

    @Override
    protected Event doExecute(final RequestContext requestContext) throws Exception {
        WebUtils.putPasswordManagementEnabled(requestContext, casProperties.getAuthn().getPm().getCore().isEnabled());
        val secQuestionsEnabled = casProperties.getAuthn().getPm().getReset().isSecurityQuestionsEnabled()
                                  && casProperties.getAuthn().getPm().getCore().isEnabled();
        WebUtils.putSecurityQuestionsEnabled(requestContext, secQuestionsEnabled);
        val tgt = WebUtils.getTicketGrantingTicket(requestContext);
        if (secQuestionsEnabled && tgt != null) {
            val principal = tgt.getAuthentication().getPrincipal();
            val query = PasswordManagementQuery.builder().username(principal.getId()).build();
            val questions = passwordManagementService.getSecurityQuestions(query);
            PasswordManagementWebflowUtils.putPasswordResetSecurityQuestions(requestContext, questions);
        }
        return null;
    }
}
