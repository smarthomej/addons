/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
 * Copyright (c) 2021-2023 Contributors to the SmartHome/J project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.smarthomej.binding.mail.internal.action;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.mail.internet.AddressException;

import org.apache.commons.mail.EmailException;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.annotation.ActionInput;
import org.openhab.core.automation.annotation.ActionOutput;
import org.openhab.core.automation.annotation.RuleAction;
import org.openhab.core.thing.binding.ThingActions;
import org.openhab.core.thing.binding.ThingActionsScope;
import org.openhab.core.thing.binding.ThingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.binding.mail.internal.MailBuilder;
import org.smarthomej.binding.mail.internal.SMTPHandler;

/**
 * The {@link SendMailActions} class defines rule actions for sending mail
 *
 * @author Jan N. Klug - Initial contribution
 */
@ThingActionsScope(name = "mail")
@NonNullByDefault
@SuppressWarnings("unused")
public class SendMailActions implements ThingActions {

    private final Logger logger = LoggerFactory.getLogger(SendMailActions.class);

    private @Nullable SMTPHandler handler;

    // plain text actions

    @RuleAction(label = "@text/sendMessageActionLabel", description = "@text/sendMessageActionDescription")
    public @ActionOutput(name = "success", type = "java.lang.Boolean") Boolean sendMail(
            @ActionInput(name = "recipient") @Nullable String recipient,
            @ActionInput(name = "subject") @Nullable String subject,
            @ActionInput(name = "text") @Nullable String text) {
        return sendComplexMail(recipient, subject, text, List.of(), null);
    }

    @RuleAction(label = "@text/sendAttachmentMessageActionLabel", description = "@text/sendAttachmentMessageActionDescription")
    public @ActionOutput(name = "success", type = "java.lang.Boolean") Boolean sendMailWithAttachment(
            @ActionInput(name = "recipient") @Nullable String recipient,
            @ActionInput(name = "subject") @Nullable String subject, @ActionInput(name = "text") @Nullable String text,
            @ActionInput(name = "url") @Nullable String urlString) {
        List<String> urlList = new ArrayList<>();
        if (urlString != null) {
            urlList.add(urlString);
        }
        return sendComplexMail(recipient, subject, text, urlList, null);
    }

    @RuleAction(label = "@text/sendAttachmentsMessageActionLabel", description = "@text/sendAttachmentsMessageActionDescription")
    public @ActionOutput(name = "success", type = "java.lang.Boolean") Boolean sendMailWithAttachments(
            @ActionInput(name = "recipient") @Nullable String recipient,
            @ActionInput(name = "subject") @Nullable String subject, @ActionInput(name = "text") @Nullable String text,
            @ActionInput(name = "urlList") @Nullable List<String> urlStringList) {
        return sendComplexMail(recipient, subject, text, urlStringList, null);
    }

    @RuleAction(label = "@text/sendComplexMessageActionLabel", description = "@text/sendComplexMessageActionDescription")
    public @ActionOutput(name = "success", type = "java.lang.Boolean") Boolean sendComplexMail(
            @ActionInput(name = "recipient") @Nullable String recipient,
            @ActionInput(name = "subject") @Nullable String subject, @ActionInput(name = "text") @Nullable String text,
            @ActionInput(name = "urlList") @Nullable List<String> urlStringList,
            @ActionInput(name = "headers") @Nullable Map<String, String> headers) {
        if (recipient == null) {
            logger.warn("Cannot send mail as recipient is missing.");
            return false;
        }

        try {
            MailBuilder builder = new MailBuilder(recipient);

            if (subject != null && !subject.isEmpty()) {
                builder.withSubject(subject);
            }
            if (text != null && !text.isEmpty()) {
                builder.withText(text);
            }
            if (urlStringList != null) {
                for (String urlString : urlStringList) {
                    builder.withURLAttachment(urlString);
                }
            }

            if (headers != null) {
                headers.forEach(builder::withHeader);
            }

            final SMTPHandler handler = this.handler;
            if (handler == null) {
                logger.info("Handler is null, cannot send mail.");
                return false;
            } else {
                return handler.sendMail(builder.build());
            }
        } catch (AddressException | MalformedURLException | EmailException e) {
            logger.warn("Could not send mail: {}", e.getMessage());
            return false;
        }
    }

    public static boolean sendMail(ThingActions actions, @Nullable String recipient, @Nullable String subject,
            @Nullable String text) {
        return SendMailActions.sendComplexMail(actions, recipient, subject, text, List.of(), null);
    }

    public static boolean sendMailWithAttachment(ThingActions actions, @Nullable String recipient,
            @Nullable String subject, @Nullable String text, @Nullable String urlString) {
        List<String> urlList = new ArrayList<>();
        if (urlString != null) {
            urlList.add(urlString);
        }
        return SendMailActions.sendComplexMail(actions, recipient, subject, text, urlList, null);
    }

    @Deprecated
    public static boolean sendMail(ThingActions actions, @Nullable String recipient, @Nullable String subject,
            @Nullable String text, @Nullable String urlString) {
        return SendMailActions.sendMailWithAttachment(actions, recipient, subject, text, urlString);
    }

    public static boolean sendMailWithAttachments(ThingActions actions, @Nullable String recipient,
            @Nullable String subject, @Nullable String text, @Nullable List<String> urlStringList) {
        return SendMailActions.sendComplexMail(actions, recipient, subject, text, urlStringList, null);
    }

    @Deprecated
    public static boolean sendMail(ThingActions actions, @Nullable String recipient, @Nullable String subject,
            @Nullable String text, @Nullable List<String> urlStringList) {
        return SendMailActions.sendMailWithAttachments(actions, recipient, subject, text, urlStringList);
    }

    public static boolean sendComplexMail(ThingActions actions, @Nullable String recipient, @Nullable String subject,
            @Nullable String text, @Nullable List<String> urlStringList, @Nullable Map<String, String> headers) {
        return ((SendMailActions) actions).sendComplexMail(recipient, subject, text, urlStringList, headers);
    }

    // HTML actions

    @RuleAction(label = "@text/sendHTMLMessageActionLabel", description = "@text/sendHTMLMessageActionDescription")
    public @ActionOutput(name = "success", type = "java.lang.Boolean") Boolean sendHtmlMail(
            @ActionInput(name = "recipient") @Nullable String recipient,
            @ActionInput(name = "subject") @Nullable String subject,
            @ActionInput(name = "html") @Nullable String html) {
        return sendHtmlMailWithAttachments(recipient, subject, html, List.of());
    }

    @RuleAction(label = "@text/sendHTMLAttachmentMessageActionLabel", description = "@text/sendHTMLAttachmentMessageActionDescription")
    public @ActionOutput(name = "success", type = "java.lang.Boolean") Boolean sendHtmlMailWithAttachment(
            @ActionInput(name = "recipient") @Nullable String recipient,
            @ActionInput(name = "subject") @Nullable String subject, @ActionInput(name = "html") @Nullable String html,
            @ActionInput(name = "url") @Nullable String urlString) {
        List<String> urlList = new ArrayList<>();
        if (urlString != null) {
            urlList.add(urlString);
        }
        return sendHtmlMailWithAttachments(recipient, subject, html, urlList);
    }

    @RuleAction(label = "@text/sendHTMLAttachmentsMessageActionLabel", description = "@text/sendHTMLAttachmentsMessageActionDescription")
    public @ActionOutput(name = "success", type = "java.lang.Boolean") Boolean sendHtmlMailWithAttachments(
            @ActionInput(name = "recipient") @Nullable String recipient,
            @ActionInput(name = "subject") @Nullable String subject, @ActionInput(name = "html") @Nullable String html,
            @ActionInput(name = "urlList") @Nullable List<String> urlStringList) {
        return sendComplexHtmlMail(recipient, subject, html, urlStringList, null);
    }

    @RuleAction(label = "@text/sendComplexHTMLMessageActionLabel", description = "@text/sendComplexHTMLMessageActionDescription")
    public @ActionOutput(name = "success", type = "java.lang.Boolean") Boolean sendComplexHtmlMail(
            @ActionInput(name = "recipient") @Nullable String recipient,
            @ActionInput(name = "subject") @Nullable String subject, @ActionInput(name = "html") @Nullable String html,
            @ActionInput(name = "urlList") @Nullable List<String> urlStringList,
            @ActionInput(name = "headers") @Nullable Map<String, String> headers) {
        if (recipient == null) {
            logger.warn("Cannot send mail as recipient is missing.");
            return false;
        }

        try {
            MailBuilder builder = new MailBuilder(recipient);

            if (subject != null && !subject.isEmpty()) {
                builder.withSubject(subject);
            }
            if (html != null && !html.isEmpty()) {
                builder.withHtml(html);
            }
            if (urlStringList != null) {
                for (String urlString : urlStringList) {
                    builder.withURLAttachment(urlString);
                }
            }

            if (headers != null) {
                headers.forEach(builder::withHeader);
            }

            final SMTPHandler handler = this.handler;
            if (handler == null) {
                logger.warn("Handler is null, cannot send mail.");
                return false;
            } else {
                return handler.sendMail(builder.build());
            }
        } catch (AddressException | MalformedURLException | EmailException e) {
            logger.warn("Could not send mail: {}", e.getMessage());
            return false;
        }
    }

    public static boolean sendHtmlMail(ThingActions actions, @Nullable String recipient, @Nullable String subject,
            @Nullable String html) {
        return SendMailActions.sendComplexHtmlMail(actions, recipient, subject, html, List.of(), null);
    }

    public static boolean sendHtmlMailWithAttachment(ThingActions actions, @Nullable String recipient,
            @Nullable String subject, @Nullable String html, @Nullable String urlString) {
        List<String> urlList = new ArrayList<>();
        if (urlString != null) {
            urlList.add(urlString);
        }
        return SendMailActions.sendComplexHtmlMail(actions, recipient, subject, html, urlList, null);
    }

    @Deprecated
    public static boolean sendHtmlMail(ThingActions actions, @Nullable String recipient, @Nullable String subject,
            @Nullable String html, @Nullable String urlString) {
        return SendMailActions.sendHtmlMailWithAttachment(actions, recipient, subject, html, urlString);
    }

    public static boolean sendHtmlMailWithAttachments(ThingActions actions, @Nullable String recipient,
            @Nullable String subject, @Nullable String html, @Nullable List<String> urlStringList) {
        return SendMailActions.sendComplexHtmlMail(actions, recipient, subject, html, urlStringList, null);
    }

    @Deprecated
    public static boolean sendHtmlMail(ThingActions actions, @Nullable String recipient, @Nullable String subject,
            @Nullable String html, @Nullable List<String> urlStringList) {
        return SendMailActions.sendHtmlMailWithAttachments(actions, recipient, subject, html, urlStringList);
    }

    public static boolean sendComplexHtmlMail(ThingActions actions, @Nullable String recipient,
            @Nullable String subject, @Nullable String html, @Nullable List<String> urlStringList,
            @Nullable Map<String, String> headers) {
        return ((SendMailActions) actions).sendComplexHtmlMail(recipient, subject, html, urlStringList, headers);
    }

    @Override
    public void setThingHandler(@Nullable ThingHandler handler) {
        if (handler instanceof SMTPHandler) {
            this.handler = (SMTPHandler) handler;
        }
    }

    @Override
    public @Nullable ThingHandler getThingHandler() {
        return handler;
    }
}
