/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.transport.mailets;

import static org.assertj.core.api.Assertions.assertThat;

import javax.mail.internet.AddressException;

import org.apache.mailet.MailAddress;
import org.apache.mailet.base.MailAddressFixture;
import org.apache.mailet.base.test.FakeMail;
import org.apache.mailet.base.test.FakeMailetConfig;
import org.apache.mailet.base.test.MimeMessageBuilder;
import org.junit.Before;
import org.junit.Test;

public class UseHeaderRecipientsTest {

    private UseHeaderRecipients testee;
    
    @Before
    public void setUp() throws Exception {
        testee = new UseHeaderRecipients();
        testee.init(FakeMailetConfig.builder().build());
    }

    @Test
    public void serviceShouldSetMimeMessageRecipients() throws Exception {
    	
    	String RCPT_1 = "abc1@apache1.org";
    	String RCPT_2 = "abc2@apache2.org";
    	
    	FakeMail fakeMail = FakeMail.builder()
    			.recipients(MailAddressFixture.ANY_AT_JAMES, MailAddressFixture.ANY_AT_JAMES2)
    				.mimeMessage(MimeMessageBuilder.mimeMessageBuilder()
    					.addToRecipient(RCPT_1, RCPT_2)
    					.build())
    				.build();
    	
    	testee.service(fakeMail);
    	
    	assertThat(fakeMail.getRecipients())
    		.containsOnly(new MailAddress(RCPT_1), new MailAddress(RCPT_2));
    }

    @Test
    public void serviceShouldSetToCcAndBccSpecifiedInTheMimeMessage() throws Exception {
    	
    	String RCPT_1 = "abc1@apache1.org";
    	String RCPT_2 = "abc2@apache2.org";
    	String RCPT_3 = "abc3@apache3.org";
    	
    	FakeMail fakeMail = FakeMail.builder()
    			.recipients(MailAddressFixture.ANY_AT_JAMES)
    				.mimeMessage(MimeMessageBuilder.mimeMessageBuilder()
    					.addToRecipient(RCPT_1)
    					.addCcRecipient(RCPT_2)
    					.addBccRecipient(RCPT_3)
    					.build())
    				.build();
    	
    	testee.service(fakeMail);
    	
    	assertThat(fakeMail.getRecipients())
    		.containsOnly(new MailAddress(RCPT_1), new MailAddress(RCPT_2), new MailAddress(RCPT_3));
    }

    @Test
    public void serviceShouldSetEmptyRecipientWhenNoRecipientsInTheMimeMessage() throws Exception {
    	
    	FakeMail fakeMail = FakeMail.builder()
    			.recipients(MailAddressFixture.ANY_AT_JAMES)
    				.mimeMessage(MimeMessageBuilder.mimeMessageBuilder()
    					.build())
    				.build();
    	
    	testee.service(fakeMail);
    	
    	assertThat(fakeMail.getRecipients())
    		.isEmpty();
    }

    @Test (expected = AddressException.class)
    public void serviceShouldThrowOnInvalidMailAddress() throws Exception {
    	
    	String RCPT_1 = "abcd";
    	
    	FakeMail fakeMail = FakeMail.builder()
    			.recipients(new MailAddress(RCPT_1))
    				.build();
    	
    	testee.service(fakeMail);
    }

    @Test
    public void serviceShouldSupportAddressList() throws Exception {

    	String RCPT_1 = "abc1@apache1.org";
    	String RCPT_2 = "abc2@apache2.org";
    	
    	FakeMail fakeMail = FakeMail.builder()
    			.recipients()
    				.mimeMessage(MimeMessageBuilder.mimeMessageBuilder()
    					.addToRecipient(RCPT_1, RCPT_2)
    					.build())
    				.build();
    	
    	testee.service(fakeMail);
    	
    	assertThat(fakeMail.getRecipients())
    		.containsOnly(new MailAddress(RCPT_1), new MailAddress(RCPT_2));
    }

    @Test
    public void serviceShouldSupportMailboxes() throws Exception {   	
    	String RCPT_1 = "abc1@apache1.org";
    	
    	FakeMail fakeMail = FakeMail.builder()
    			.recipients()
    				.mimeMessage(MimeMessageBuilder.mimeMessageBuilder()
    					.addToRecipient("APACHE" + "<" + RCPT_1 + ">")
    					.build())
    				.build();
    	
    	testee.service(fakeMail);
    	
    	assertThat(fakeMail.getRecipients())
    		.containsOnly(new MailAddress(RCPT_1));
    }
}
