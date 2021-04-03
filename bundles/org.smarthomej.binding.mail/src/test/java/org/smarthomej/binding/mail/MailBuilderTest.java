/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
 * Copyright (c) 2021 Contributors to the SmartHome/J project
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
package org.smarthomej.binding.mail;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.MalformedURLException;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;

import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.apache.commons.mail.MultiPartEmail;
import org.apache.commons.mail.SimpleEmail;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.smarthomej.binding.mail.internal.MailBuilder;

/**
 * The {@link MailBuilderTest} class defines tests for the {@link MailBuilder} class
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class MailBuilderTest {

    private static final String TEST_STRING = "test";
    private static final String TEST_EMAIL = "foo@bar.zinga";

    @Test
    public void illegalToAddressThrowsException() {
        assertThrows(AddressException.class, () -> new MailBuilder("foo bar.zinga"));
    }

    @Test
    public void illegalFromAddressThrowsException() {
        assertThrows(EmailException.class, () -> new MailBuilder("TEST_EMAIL").withSender("foo bar.zinga").build());
    }

    @Test
    public void illegalURLThrowsException() {
        assertThrows(MalformedURLException.class,
                () -> new MailBuilder("TEST_EMAIL").withURLAttachment("foo bar.zinga"));
    }

    @Test
    public void withTextOnlyReturnsSimpleEmail() throws AddressException, EmailException {
        MailBuilder builder = new MailBuilder(TEST_EMAIL);
        Email mail = builder.withText("boo").build();
        assertThat(mail, instanceOf(SimpleEmail.class));
    }

    @Test
    public void withURLAttachmentReturnsMultiPartEmail()
            throws AddressException, EmailException, MalformedURLException {
        MailBuilder builder = new MailBuilder(TEST_EMAIL);
        Email mail = builder.withText("boo").withURLAttachment("http://www.openhab.org").build();
        assertThat(mail, instanceOf(MultiPartEmail.class));
    }

    @Test
    public void withHtmlReturnsHtmlEmail() throws AddressException, EmailException {
        MailBuilder builder = new MailBuilder(TEST_EMAIL);
        Email mail = builder.withHtml("<html>test</html>").build();
        assertThat(mail, instanceOf(HtmlEmail.class));
    }

    @Test
    public void fieldsSetInMail() throws EmailException, MessagingException, IOException {
        MailBuilder builder = new MailBuilder(TEST_EMAIL);

        assertEquals("(no subject)", builder.build().getSubject());
        assertEquals(TEST_STRING, builder.withSubject(TEST_STRING).build().getSubject());

        assertEquals(TEST_EMAIL, builder.withSender(TEST_EMAIL).build().getFromAddress().getAddress());

        assertEquals(TEST_EMAIL, builder.build().getToAddresses().get(0).getAddress());
        assertEquals(2, builder.withRecipients(TEST_EMAIL).build().getToAddresses().size());
    }
}
