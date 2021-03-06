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

package org.apache.james.jmap.methods.integration.cucumber;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.mail.Flags;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.client.utils.URIBuilder;
import org.apache.james.jmap.api.access.AccessToken;
import org.apache.james.jmap.model.AttachmentAccessToken;
import org.apache.james.mailbox.model.MailboxConstants;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mime4j.codec.DecoderUtil;
import org.apache.james.modules.MailboxProbeImpl;

import com.google.common.base.CharMatcher;
import com.google.common.base.Charsets;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import cucumber.runtime.java.guice.ScenarioScoped;

@ScenarioScoped
public class DownloadStepdefs {

    private static final String ONE_ATTACHMENT_EML_ATTACHEMENT_BLOB_ID = "4000c5145f633410b80be368c44e1c394bff9437";
    private static final String EXPIRED_ATTACHMENT_TOKEN = "usera@domain.tld_"
            + "2016-06-29T13:41:22.124Z_"
            + "DiZa0O14MjLWrAA8P6MG35Gt5CBp7mt5U1EH/M++rIoZK7nlGJ4dPW0dvZD7h4m3o5b/Yd8DXU5x2x4+s0HOOKzD7X0RMlsU7JHJMNLvTvRGWF/C+MUyC8Zce7DtnRVPEQX2uAZhL2PBABV07Vpa8kH+NxoS9CL955Bc1Obr4G+KN2JorADlocFQA6ElXryF5YS/HPZSvq1MTC6aJIP0ku8WRpRnbwgwJnn26YpcHXcJjbkCBtd9/BhlMV6xNd2hTBkfZmYdoNo+UKBaXWzLxAlbLuxjpxwvDNJfOEyWFPgHDoRvzP+G7KzhVWjanHAHrhF0GilEa/MKpOI1qHBSwA==";
    private static final String INVALID_ATTACHMENT_TOKEN = "usera@domain.tld_"
            + "2015-06-29T13:41:22.124Z_"
            + "DiZa0O14MjLWrAA8P6MG35Gt5CBp7mt5U1EH/M++rIoZK7nlGJ4dPW0dvZD7h4m3o5b/Yd8DXU5x2x4+s0HOOKzD7X0RMlsU7JHJMNLvTvRGWF/C+MUyC8Zce7DtnRVPEQX2uAZhL2PBABV07Vpa8kH+NxoS9CL955Bc1Obr4G+KN2JorADlocFQA6ElXryF5YS/HPZSvq1MTC6aJIP0ku8WRpRnbwgwJnn26YpcHXcJjbkCBtd9/BhlMV6xNd2hTBkfZmYdoNo+UKBaXWzLxAlbLuxjpxwvDNJfOEyWFPgHDoRvzP+G7KzhVWjanHAHrhF0GilEa/MKpOI1qHBSwA==";
    private static final String UTF8_CONTENT_DIPOSITION_START = "Content-Disposition: attachment; filename*=\"";

    private final UserStepdefs userStepdefs;
    private final MainStepdefs mainStepdefs;
    private HttpResponse response;
    private Multimap<String, String> attachmentsByMessageId;
    private Map<String, String> blobIdByAttachmentId;
    private Map<AttachmentAccessTokenKey, AttachmentAccessToken> attachmentAccessTokens;

    @Inject
    private DownloadStepdefs(MainStepdefs mainStepdefs, UserStepdefs userStepdefs) {
        this.mainStepdefs = mainStepdefs;
        this.userStepdefs = userStepdefs;
        this.attachmentsByMessageId = ArrayListMultimap.create();
        this.blobIdByAttachmentId = new HashMap<>();
        this.attachmentAccessTokens = new HashMap<>();
    }
    
    @Given("^\"([^\"]*)\" mailbox \"([^\"]*)\" contains a message \"([^\"]*)\" with an attachment \"([^\"]*)\"$")
    public void appendMessageWithAttachmentToMailbox(String user, String mailbox, String messageId, String attachmentId) throws Throwable {
        MailboxPath mailboxPath = new MailboxPath(MailboxConstants.USER_NAMESPACE, user, mailbox);

        mainStepdefs.jmapServer.getProbe(MailboxProbeImpl.class).appendMessage(user, mailboxPath,
                ClassLoader.getSystemResourceAsStream("eml/oneAttachment.eml"), new Date(), false, new Flags());
        
        attachmentsByMessageId.put(messageId, attachmentId);
        blobIdByAttachmentId.put(attachmentId, "4000c5145f633410b80be368c44e1c394bff9437");
    }

    @Given("^\"([^\"]*)\" mailbox \"([^\"]*)\" contains a message \"([^\"]*)\" with an inlined attachment \"([^\"]*)\"$")
    public void appendMessageWithInlinedAttachmentToMailbox(String user, String mailbox, String messageId, String attachmentId) throws Throwable {
        MailboxPath mailboxPath = new MailboxPath(MailboxConstants.USER_NAMESPACE, user, mailbox);

        mainStepdefs.jmapServer.getProbe(MailboxProbeImpl.class).appendMessage(user, mailboxPath,
                ClassLoader.getSystemResourceAsStream("eml/oneInlinedImage.eml"), new Date(), false, new Flags());
        
        attachmentsByMessageId.put(messageId, attachmentId);
        // TODO
        //blobIdByAttachmentId.put(attachmentId, "<correctComputedBlobId>");
    }

    @When("^\"([^\"]*)\" checks for the availability of the attachment endpoint$")
    public void optionDownload(String username) throws Throwable {
        AccessToken accessToken = userStepdefs.tokenByUser.get(username);
        URI target = mainStepdefs.baseUri().setPath("/download/" + ONE_ATTACHMENT_EML_ATTACHEMENT_BLOB_ID).build();
        Request request = Request.Options(target);
        if (accessToken != null) {
            request.addHeader("Authorization", accessToken.serialize());
        }
        response = request.execute().returnResponse();
    }

    @When("^\"([^\"]*)\" downloads \"([^\"]*)\"$")
    public void downloads(String username, String attachmentId) throws Throwable {
        String blobId = blobIdByAttachmentId.get(attachmentId);
        URIBuilder uriBuilder = mainStepdefs.baseUri().setPath("/download/" + blobId);
        response = authenticatedDownloadRequest(uriBuilder, blobId, username).execute().returnResponse();
    }

    private Request authenticatedDownloadRequest(URIBuilder uriBuilder, String blobId, String username) throws URISyntaxException {
        AccessToken accessToken = userStepdefs.tokenByUser.get(username);
        AttachmentAccessTokenKey key = new AttachmentAccessTokenKey(username, blobId);
        if (attachmentAccessTokens.containsKey(key)) {
            uriBuilder.addParameter("access_token", attachmentAccessTokens.get(key).serialize());
        }
        Request request = Request.Get(uriBuilder.build());
        if (accessToken != null) {
            request.addHeader("Authorization", accessToken.serialize());
        }
        return request;
    }

    @When("^\"([^\"]*)\" is trusted for attachment \"([^\"]*)\"$")
    public void attachmentAccessTokenFor(String username, String attachmentId) throws Throwable {
        userStepdefs.connectUser(username);
        trustForBlobId(blobIdByAttachmentId.get(attachmentId), username);
    }

    private static class AttachmentAccessTokenKey {

        private String username;
        private String blobId;

        public AttachmentAccessTokenKey(String username, String blobId) {
            this.username = username;
            this.blobId = blobId;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof AttachmentAccessTokenKey) {
                AttachmentAccessTokenKey other = (AttachmentAccessTokenKey) obj;
                return Objects.equal(username, other.username)
                    && Objects.equal(blobId, other.blobId);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(username, blobId);
        }

        @Override
        public String toString() {
            return MoreObjects
                    .toStringHelper(this)
                    .add("username", username)
                    .add("blobId", blobId)
                    .toString();
        }
    }

    private void trustForBlobId(String blobId, String username) throws Exception {
        Response tokenGenerationResponse = Request.Post(mainStepdefs.baseUri().setPath("/download/" + blobId).build())
            .addHeader("Authorization", userStepdefs.tokenByUser.get(username).serialize())
            .execute();
        String serializedAttachmentAccessToken = tokenGenerationResponse.returnContent().asString();
        attachmentAccessTokens.put(
                new AttachmentAccessTokenKey(username, blobId),
                AttachmentAccessToken.from(
                    serializedAttachmentAccessToken,
                    blobId));
    }

    @When("^\"([^\"]*)\" downloads \"([^\"]*)\" with a valid authentication token but a bad blobId$")
    public void downloadsWithValidToken(String username, String attachmentId) throws Throwable {
        URIBuilder uriBuilder = mainStepdefs.baseUri().setPath("/download/badblobId");
        response = Request.Get(uriBuilder.build())
            .addHeader("Authorization", userStepdefs.tokenByUser.get(username).serialize())
            .execute()
            .returnResponse();
    }

    @When("^\"([^\"]*)\" downloads \"([^\"]*)\" without any authentication token$")
    public void getDownloadWithoutToken(String username, String attachmentId) throws Exception {
        String blobId = blobIdByAttachmentId.get(attachmentId);
        response = Request.Get(mainStepdefs.baseUri().setPath("/download/" + blobId).build())
            .execute()
            .returnResponse();
    }

    @When("^\"([^\"]*)\" downloads \"([^\"]*)\" with an empty authentication token$")
    public void getDownloadWithEmptyToken(String username, String attachmentId) throws Exception {
        String blobId = blobIdByAttachmentId.get(attachmentId);
        response = Request.Get(
                mainStepdefs.baseUri()
                    .setPath("/download/" + blobId)
                    .addParameter("access_token", "")
                    .build())
                .execute()
                .returnResponse();
    }

    @When("^\"([^\"]*)\" downloads \"([^\"]*)\" with a bad authentication token$")
    public void getDownloadWithBadToken(String username, String attachmentId) throws Exception {
        String blobId = blobIdByAttachmentId.get(attachmentId);
        response = Request.Get(
                mainStepdefs.baseUri()
                    .setPath("/download/" + blobId)
                    .addParameter("access_token", "bad")
                    .build())
                .execute()
                .returnResponse();
    }

    @When("^\"([^\"]*)\" downloads \"([^\"]*)\" with an invalid authentication token$")
    public void getDownloadWithUnknownToken(String username, String attachmentId) throws Exception {
        String blobId = blobIdByAttachmentId.get(attachmentId);
        response = Request.Get(
                mainStepdefs.baseUri()
                    .setPath("/download/" + blobId)
                    .addParameter("access_token", INVALID_ATTACHMENT_TOKEN)
                    .build())
                .execute()
                .returnResponse();
    }

    @When("^\"([^\"]*)\" downloads \"([^\"]*)\" without blobId parameter$")
    public void getDownloadWithoutBlobId(String username, String attachmentId) throws Throwable {
        String blobId = blobIdByAttachmentId.get(attachmentId);

        URIBuilder uriBuilder = mainStepdefs.baseUri().setPath("/download/");
        trustForBlobId(blobId, username);
        AttachmentAccessTokenKey key = new AttachmentAccessTokenKey(username, blobId);
        uriBuilder.addParameter("access_token", attachmentAccessTokens.get(key).serialize());
        response = Request.Get(uriBuilder.build())
            .execute()
            .returnResponse();
    }

    @When("^\"([^\"]*)\" downloads \"([^\"]*)\" with wrong blobId$")
    public void getDownloadWithWrongBlobId(String username, String attachmentId) throws Throwable {
        String blobId = blobIdByAttachmentId.get(attachmentId);

        URIBuilder uriBuilder = mainStepdefs.baseUri().setPath("/download/badbadbadbadbadbadbadbadbadbadbadbadbadb");
        trustForBlobId(blobId, username);
        AttachmentAccessTokenKey key = new AttachmentAccessTokenKey(username, blobId);
        uriBuilder.addParameter("access_token", attachmentAccessTokens.get(key).serialize());
        response = Request.Get(uriBuilder.build())
            .execute()
            .returnResponse();
    }

    @When("^\"([^\"]*)\" asks for a token for attachment \"([^\"]*)\"$")
    public void postDownload(String username, String attachmentId) throws Throwable {
        String blobId = blobIdByAttachmentId.get(attachmentId);
        AccessToken accessToken = userStepdefs.tokenByUser.get(username);
        response = Request.Post(mainStepdefs.baseUri().setPath("/download/" + blobId).build())
                .addHeader("Authorization", accessToken.serialize())
                .execute()
                .returnResponse();
    }

    @When("^\"([^\"]*)\" downloads \"([^\"]*)\" with \"([^\"]*)\" name$")
    public void downloadsWithName(String username, String attachmentId, String name) throws Exception {
        String blobId = blobIdByAttachmentId.get(attachmentId);
        URIBuilder uriBuilder = mainStepdefs.baseUri().setPath("/download/" + blobId + "/" + name);
        response = authenticatedDownloadRequest(uriBuilder, blobId, username)
                .execute()
                .returnResponse();
    }

    @When("^\"([^\"]*)\" downloads \"([^\"]*)\" with an expired token$")
    public void getDownloadWithExpiredToken(String username, String attachmentId) throws Exception {
        String blobId = blobIdByAttachmentId.get(attachmentId);
        response = Request.Get(mainStepdefs.baseUri().setPath("/download/" + blobId)
                .addParameter("access_token", EXPIRED_ATTACHMENT_TOKEN)
                .build())
            .execute()
            .returnResponse();
    }

    @Then("^the user should be authorized$")
    public void httpStatusDifferentFromUnauthorized() throws IOException {
        assertThat(response.getStatusLine().getStatusCode()).isIn(200, 404);
    }

    @Then("^the user should not be authorized$")
    public void httpUnauthorizedStatus() throws IOException {
        assertThat(response.getStatusLine().getStatusCode()).isEqualTo(401);
    }

    @Then("^the user should receive a bad request response$")
    public void httpBadRequestStatus() throws IOException {
        assertThat(response.getStatusLine().getStatusCode()).isEqualTo(400);
    }

    @Then("^the user should receive that attachment$")
    public void httpOkStatusAndExpectedContent() throws IOException {
        assertThat(response.getStatusLine().getStatusCode()).isEqualTo(200);
        assertThat(IOUtils.toString(response.getEntity().getContent(), Charsets.UTF_8)).isNotEmpty();
    }

    @Then("^the user should receive a not found response$")
    public void httpNotFoundStatus() throws IOException {
        assertThat(response.getStatusLine().getStatusCode()).isEqualTo(404);
    }

    @Then("^the user should receive an attachment access token$")
    public void accessTokenResponse() throws Throwable {
        assertThat(response.getStatusLine().getStatusCode()).isEqualTo(200);
        assertThat(response.getHeaders("Content-Type")).extracting(Header::toString).containsExactly("Content-Type: text/plain");
        assertThat(IOUtils.toString(response.getEntity().getContent(), Charsets.UTF_8)).isNotEmpty();
    }

    @Then("^the attachment is named \"([^\"]*)\"$")
    public void assertContentDisposition(String name) throws IOException {
        if (!CharMatcher.ASCII.matchesAllOf(name)) {
            assertEncodedFilenameMatches(name);
        } else {
            assertThat(response.getFirstHeader("Content-Disposition").getValue()).isEqualTo("attachment; filename=\"" + name + "\"");
        }
    }

    @Then("^the attachment size is (\\d+)$")
    public void assertContentLength(int size) throws IOException {
        assertThat(response.getFirstHeader("Content-Length").getValue()).isEqualTo(String.valueOf(size));
    }

    private void assertEncodedFilenameMatches(String name) {
        String contentDispositionHeader = response.getHeaders("Content-Disposition")[0].toString();
        assertThat(contentDispositionHeader).startsWith(UTF8_CONTENT_DIPOSITION_START);

        String expectedFilename = decode(extractFilename(contentDispositionHeader));
        assertThat(name).isEqualTo(expectedFilename);
    }

    private String extractFilename(String contentDispositionHeader) {
        return contentDispositionHeader.substring(UTF8_CONTENT_DIPOSITION_START.length(), 
                contentDispositionHeader.length() - 1);
    }

    private String decode(String name) {
        return DecoderUtil.decodeEncodedWords(name, Charsets.UTF_8);
    }
}
