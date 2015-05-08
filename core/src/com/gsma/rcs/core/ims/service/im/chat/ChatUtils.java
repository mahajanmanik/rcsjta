/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * NOTE: This file has been modified by Sony Mobile Communications Inc.
 * Modifications are licensed under the License.
 ******************************************************************************/

package com.gsma.rcs.core.ims.service.im.chat;

import static com.gsma.rcs.utils.StringUtils.UTF8;
import static com.gsma.rcs.utils.StringUtils.UTF8_STR;

import com.gsma.rcs.core.ims.ImsModule;
import com.gsma.rcs.core.ims.network.sip.FeatureTags;
import com.gsma.rcs.core.ims.network.sip.Multipart;
import com.gsma.rcs.core.ims.network.sip.SipUtils;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.service.im.chat.cpim.CpimMessage;
import com.gsma.rcs.core.ims.service.im.chat.cpim.CpimParser;
import com.gsma.rcs.core.ims.service.im.chat.geoloc.GeolocInfoDocument;
import com.gsma.rcs.core.ims.service.im.chat.geoloc.GeolocInfoParser;
import com.gsma.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.gsma.rcs.core.ims.service.im.chat.imdn.ImdnParser;
import com.gsma.rcs.core.ims.service.im.chat.imdn.ImdnUtils;
import com.gsma.rcs.core.ims.service.im.chat.iscomposing.IsComposingInfo;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileTransferUtils;
import com.gsma.rcs.core.ims.service.im.filetransfer.http.FileTransferHttpInfoDocument;
import com.gsma.rcs.core.ims.service.im.filetransfer.http.FileTransferHttpResumeInfo;
import com.gsma.rcs.core.ims.service.im.filetransfer.http.FileTransferHttpResumeInfoParser;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.ContactUtil.PhoneNumber;
import com.gsma.rcs.utils.DateUtils;
import com.gsma.rcs.utils.IdGenerator;
import com.gsma.rcs.utils.PhoneUtils;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.Geoloc;
import com.gsma.services.rcs.chat.ChatLog.Message.MimeType;
import com.gsma.services.rcs.chat.GroupChat.ParticipantStatus;
import com.gsma.services.rcs.contact.ContactId;

import android.text.TextUtils;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax2.sip.header.ContactHeader;
import javax2.sip.header.ExtensionHeader;

/**
 * Chat utility functions
 * 
 * @author jexa7410
 */
public class ChatUtils {
    /**
     * Anonymous URI
     */
    public final static String ANOMYNOUS_URI = "sip:anonymous@anonymous.invalid";

    /**
     * Contribution ID header
     */
    public static final String HEADER_CONTRIBUTION_ID = "Contribution-ID";

    /**
     * CRLF constant
     */
    private static final String CRLF = "\r\n";

    private static final Logger sLogger = Logger.getLogger(ChatUtils.class.getName());

    /**
     * Get supported feature tags for a group chat
     * 
     * @param rcsSettings
     * @return List of tags
     */
    public static List<String> getSupportedFeatureTagsForGroupChat(RcsSettings rcsSettings) {
        List<String> tags = new ArrayList<String>();
        tags.add(FeatureTags.FEATURE_OMA_IM);

        List<String> additionalRcseTags = new ArrayList<String>();
        if (rcsSettings.isGeoLocationPushSupported()) {
            additionalRcseTags.add(FeatureTags.FEATURE_RCSE_GEOLOCATION_PUSH);
        }
        if (rcsSettings.isFileTransferSupported()) {
            additionalRcseTags.add(FeatureTags.FEATURE_RCSE_FT);
        }
        if (rcsSettings.isFileTransferHttpSupported()) {
            additionalRcseTags.add(FeatureTags.FEATURE_RCSE_FT_HTTP);
        }
        if (rcsSettings.isFileTransferStoreForwardSupported()) {
            additionalRcseTags.add(FeatureTags.FEATURE_RCSE_FT_SF);
        }
        if (!additionalRcseTags.isEmpty()) {
            tags.add(FeatureTags.FEATURE_RCSE + "=\"" + TextUtils.join(",", additionalRcseTags)
                    + "\"");
        }

        return tags;
    }

    /**
     * Get Accept-Contact tags for a group chat
     * 
     * @return List of tags
     */
    public static List<String> getAcceptContactTagsForGroupChat() {
        List<String> tags = new ArrayList<String>();
        tags.add(FeatureTags.FEATURE_OMA_IM);
        return tags;
    }

    /**
     * Get supported feature tags for a chat
     * 
     * @param rcsSettings
     * @return List of tags
     */
    public static List<String> getSupportedFeatureTagsForChat(RcsSettings rcsSettings) {
        List<String> tags = new ArrayList<String>();
        tags.add(FeatureTags.FEATURE_OMA_IM);

        List<String> additionalRcseTags = new ArrayList<String>();
        if (rcsSettings.isGeoLocationPushSupported()) {
            additionalRcseTags.add(FeatureTags.FEATURE_RCSE_GEOLOCATION_PUSH);
        }
        if (rcsSettings.isFileTransferSupported()) {
            additionalRcseTags.add(FeatureTags.FEATURE_RCSE_FT);
        }
        if (rcsSettings.isFileTransferHttpSupported()) {
            additionalRcseTags.add(FeatureTags.FEATURE_RCSE_FT_HTTP);
        }
        if (rcsSettings.isFileTransferStoreForwardSupported()) {
            additionalRcseTags.add(FeatureTags.FEATURE_RCSE_FT_SF);
        }
        if (!additionalRcseTags.isEmpty()) {
            tags.add(FeatureTags.FEATURE_RCSE + "=\"" + TextUtils.join(",", additionalRcseTags)
                    + "\"");
        }

        return tags;
    }

    /**
     * Get contribution ID
     * 
     * @param request
     * @return String
     */
    public static String getContributionId(SipRequest request) {
        ExtensionHeader contribHeader = (ExtensionHeader) request.getHeader(HEADER_CONTRIBUTION_ID);
        if (contribHeader != null) {
            return contribHeader.getValue();
        } else {
            return null;
        }
    }

    /**
     * Is a group chat session invitation
     * 
     * @param request Request
     * @return Boolean
     */
    public static boolean isGroupChatInvitation(SipRequest request) {
        ContactHeader contactHeader = (ContactHeader) request.getHeader(ContactHeader.NAME);
        String param = contactHeader.getParameter("isfocus");
        return param != null;
    }

    /**
     * Get referred identity as a ContactId
     * 
     * @param request SIP request
     * @return ContactId
     */
    public static ContactId getReferredIdentityAsContactId(SipRequest request) {
        /* First use the Referred-By header */
        String referredBy = SipUtils.getReferredByHeader(request);
        PhoneNumber number = ContactUtil.getValidPhoneNumberFromUri(referredBy);
        if (number == null) {
            /* Use the Asserted-Identity header if parsing of Referred-By header failed */
            String assertedId = SipUtils.getAssertedIdentity(request);
            number = ContactUtil.getValidPhoneNumberFromUri(assertedId);
            if (number == null) {
                return null;
            }
        }
        return ContactUtil.createContactIdFromValidatedData(number);
    }

    /**
     * Get referred identity as a contact URI
     * 
     * @param request SIP request
     * @return SIP URI
     */
    public static String getReferredIdentityAsContactUri(SipRequest request) {
        String referredBy = SipUtils.getReferredByHeader(request);
        if (referredBy != null) {
            // Use the Referred-By header
            return referredBy;
        }
        // Use the Asserted-Identity header
        return SipUtils.getAssertedIdentity(request);
    }

    /**
     * Is a plain text type
     * 
     * @param mime MIME type
     * @return Boolean
     */
    public static boolean isTextPlainType(String mime) {
        return mime != null && mime.toLowerCase().startsWith(MimeType.TEXT_MESSAGE);
    }

    /**
     * Is a composing event type
     * 
     * @param mime MIME type
     * @return Boolean
     */
    public static boolean isApplicationIsComposingType(String mime) {
        return mime != null && mime.toLowerCase().startsWith(IsComposingInfo.MIME_TYPE);
    }

    /**
     * Is a CPIM message type
     * 
     * @param mime MIME type
     * @return Boolean
     */
    public static boolean isMessageCpimType(String mime) {
        return mime != null && mime.toLowerCase().startsWith(CpimMessage.MIME_TYPE);
    }

    /**
     * Is an IMDN message type
     * 
     * @param mime MIME type
     * @return Boolean
     */
    public static boolean isMessageImdnType(String mime) {
        return mime != null && mime.toLowerCase().startsWith(ImdnDocument.MIME_TYPE);
    }

    /**
     * Is a geolocation event type
     * 
     * @param mime MIME type
     * @return Boolean
     */
    public static boolean isGeolocType(String mime) {
        return mime != null && mime.toLowerCase().startsWith(GeolocInfoDocument.MIME_TYPE);
    }

    /**
     * Generate resource-list for a chat session
     * 
     * @param participants Set of participants
     * @return XML document
     */
    public static String generateChatResourceList(Set<ContactId> participants) {
        StringBuilder resources = new StringBuilder("<?xml version=\"1.0\" encoding=\"")
                .append(UTF8_STR).append("\"?>").append(CRLF)
                .append("<resource-lists xmlns=\"urn:ietf:params:xml:ns:resource-lists\" ")
                .append("xmlns:cp=\"urn:ietf:params:xml:ns:copycontrol\">").append("<list>")
                .append(CRLF);
        for (ContactId contact : participants) {
            resources.append(" <entry uri=\"").append(PhoneUtils.formatContactIdToUri(contact))
                    .append("\" cp:copyControl=\"to\"/>").append(CRLF);
        }
        return resources.append("</list></resource-lists>").toString();
    }

    /**
     * Get participants from contacts
     * 
     * @param contacts contacts
     * @param status status
     * @return participants
     */
    public static Map<ContactId, ParticipantStatus> getParticipants(Set<ContactId> contacts,
            ParticipantStatus status) {
        Map<ContactId, ParticipantStatus> participants = new HashMap<ContactId, ParticipantStatus>();

        for (ContactId contact : contacts) {
            participants.put(contact, status);
        }
        return participants;
    }

    /**
     * Is IMDN service
     * 
     * @param request Request
     * @return Boolean
     */
    public static boolean isImdnService(SipRequest request) {
        String content = request.getContent();
        String contentType = request.getContentType();
        if ((content != null) && (content.contains(ImdnDocument.IMDN_NAMESPACE))
                && (contentType != null) && (contentType.equalsIgnoreCase(CpimMessage.MIME_TYPE))) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Is IMDN notification "delivered" requested
     * 
     * @param request Request
     * @return Boolean
     */
    public static boolean isImdnDeliveredRequested(SipRequest request) {
        boolean result = false;
        try {
            // Read ID from multipart content
            String content = request.getContent();
            int index = content.indexOf(ImdnUtils.HEADER_IMDN_DISPO_NOTIF);
            if (index != -1) {
                index = index + ImdnUtils.HEADER_IMDN_DISPO_NOTIF.length() + 1;
                String part = content.substring(index);
                String notif = part.substring(0, part.indexOf(CRLF));
                if (notif.indexOf(ImdnDocument.POSITIVE_DELIVERY) != -1) {
                    result = true;
                }
            }
        } catch (Exception e) {
            result = false;
        }
        return result;
    }

    /**
     * Is IMDN notification "displayed" requested
     * 
     * @param request Request
     * @return Boolean
     */
    public static boolean isImdnDisplayedRequested(SipRequest request) {
        boolean result = false;
        try {
            // Read ID from multipart content
            String content = request.getContent();
            int index = content.indexOf(ImdnUtils.HEADER_IMDN_DISPO_NOTIF);
            if (index != -1) {
                index = index + ImdnUtils.HEADER_IMDN_DISPO_NOTIF.length() + 1;
                String part = content.substring(index);
                String notif = part.substring(0, part.indexOf(CRLF));
                if (notif.indexOf(ImdnDocument.DISPLAY) != -1) {
                    result = true;
                }
            }
        } catch (Exception e) {
            result = false;
        }
        return result;
    }

    /**
     * Returns the message ID from a SIP request
     * 
     * @param request Request
     * @return Message ID
     */
    public static String getMessageId(SipRequest request) {
        String result = null;
        try {
            // Read ID from multipart content
            String content = request.getContent();
            int index = content.indexOf(ImdnUtils.HEADER_IMDN_MSG_ID);
            if (index != -1) {
                index = index + ImdnUtils.HEADER_IMDN_MSG_ID.length() + 1;
                String part = content.substring(index);
                String msgId = part.substring(0, part.indexOf(CRLF));
                result = msgId.trim();
            }
        } catch (Exception e) {
            result = null;
        }
        return result;
    }

    private static String addUriDelimiters(String uri) {
        return new StringBuilder(PhoneUtils.URI_START_DELIMITER).append(uri)
                .append(PhoneUtils.URI_END_DELIMITER).toString();
    }

    /**
     * Format to a SIP-URI for CPIM message
     * 
     * @param input Input
     * @return SIP-URI
     */
    private static String formatCpimSipUri(String input) {
        input = input.trim();

        if (input.startsWith(PhoneUtils.URI_START_DELIMITER)) {
            /* Already a SIP-URI format */
            return input;
        }

        /* It's already a SIP address with display name */
        if (input.startsWith("\"")) {
            return input;
        }

        if (input.startsWith(PhoneUtils.SIP_URI_HEADER)
                || input.startsWith(PhoneUtils.TEL_URI_HEADER)) {
            /* It is already a SIP or TEL URI: just add URI delimiters */
            return addUriDelimiters(input);
        }

        PhoneNumber validatedNumber = ContactUtil.getValidPhoneNumberFromUri(input);
        if (validatedNumber == null) {
            /* It's not a valid phone number: just add URI delimiters */
            return addUriDelimiters(input);
        }

        /* It's a valid phone number, format it to URI and add delimiters */
        ContactId contact = ContactUtil.createContactIdFromValidatedData(validatedNumber);
        return addUriDelimiters(PhoneUtils.formatContactIdToUri(contact));
    }

    /**
     * Build a CPIM message
     * 
     * @param from From
     * @param to To
     * @param content Content
     * @param contentType Content type
     * @param timestampSent Timestamp sent in payload for CPIM DateTimes
     * @return String
     */
    public static String buildCpimMessage(String from, String to, String content,
            String contentType, long timestampSent) {
        return new StringBuilder(CpimMessage.HEADER_FROM).append(": ")
                .append(formatCpimSipUri(from)).append(CRLF).append(CpimMessage.HEADER_TO)
                .append(": ").append(formatCpimSipUri(to)).append(CRLF)
                .append(CpimMessage.HEADER_DATETIME).append(": ")
                .append(DateUtils.encodeDate(timestampSent)).append(CRLF).append(CRLF)
                .append(CpimMessage.HEADER_CONTENT_TYPE).append(": ").append(contentType)
                .append(";charset=").append(UTF8_STR).append(CRLF).append(CRLF).append(content)
                .toString();
    }

    /**
     * Build a CPIM message with full IMDN headers
     * 
     * @param from From URI
     * @param to To URI
     * @param messageId Message ID
     * @param content Content
     * @param contentType Content type
     * @param timestampSent Timestamp sent in payload for CPIM DateTime
     * @return String
     */
    public static String buildCpimMessageWithImdn(String from, String to, String messageId,
            String content, String contentType, long timestampSent) {
        return new StringBuilder(CpimMessage.HEADER_FROM).append(": ")
                .append(formatCpimSipUri(from)).append(CRLF).append(CpimMessage.HEADER_TO)
                .append(": ").append(formatCpimSipUri(to)).append(CRLF)
                .append(CpimMessage.HEADER_NS).append(": ").append(ImdnDocument.IMDN_NAMESPACE)
                .append(CRLF).append(ImdnUtils.HEADER_IMDN_MSG_ID).append(": ").append(messageId)
                .append(CRLF).append(CpimMessage.HEADER_DATETIME).append(": ")
                .append(DateUtils.encodeDate(timestampSent)).append(CRLF)
                .append(ImdnUtils.HEADER_IMDN_DISPO_NOTIF).append(": ")
                .append(ImdnDocument.POSITIVE_DELIVERY).append(", ").append(ImdnDocument.DISPLAY)
                .append(CRLF).append(CRLF).append(CpimMessage.HEADER_CONTENT_TYPE).append(": ")
                .append(contentType).append(";charset=").append(UTF8_STR).append(CRLF)
                .append(CpimMessage.HEADER_CONTENT_LENGTH).append(": ")
                .append(content.getBytes(UTF8).length).append(CRLF).append(CRLF).append(content)
                .toString();
    }

    /**
     * Build a CPIM message with IMDN delivered header
     * 
     * @param from From URI
     * @param to To URI
     * @param messageId Message ID
     * @param content Content
     * @param contentType Content type
     * @param timestampSent Timestamp sent in payload for CPIM DateTime
     * @return String
     */
    public static String buildCpimMessageWithoutDisplayedImdn(String from, String to,
            String messageId, String content, String contentType, long timestampSent) {
        return new StringBuilder(CpimMessage.HEADER_FROM).append(": ")
                .append(formatCpimSipUri(from)).append(CRLF).append(CpimMessage.HEADER_TO)
                .append(": ").append(formatCpimSipUri(to)).append(CRLF)
                .append(CpimMessage.HEADER_NS).append(": ").append(ImdnDocument.IMDN_NAMESPACE)
                .append(CRLF).append(ImdnUtils.HEADER_IMDN_MSG_ID).append(": ").append(messageId)
                .append(CRLF).append(CpimMessage.HEADER_DATETIME).append(": ")
                .append(DateUtils.encodeDate(timestampSent)).append(CRLF)
                .append(ImdnUtils.HEADER_IMDN_DISPO_NOTIF).append(": ")
                .append(ImdnDocument.POSITIVE_DELIVERY).append(CRLF).append(CRLF)
                .append(CpimMessage.HEADER_CONTENT_TYPE).append(": ").append(contentType)
                .append(";charset=").append(UTF8_STR).append(CRLF)
                .append(CpimMessage.HEADER_CONTENT_LENGTH).append(": ")
                .append(content.getBytes(UTF8).length).append(CRLF).append(CRLF).append(content)
                .toString();
    }

    /**
     * Build a CPIM delivery report
     * 
     * @param from From
     * @param to To
     * @param imdn IMDN report
     * @param timestampSent Timestamp sent in payload for CPIM DateTime
     * @return String
     */
    public static String buildCpimDeliveryReport(String from, String to, String imdn,
            long timestampSent) {
        return new StringBuilder(CpimMessage.HEADER_FROM).append(": ")
                .append(formatCpimSipUri(from)).append(CRLF).append(CpimMessage.HEADER_TO)
                .append(": ").append(formatCpimSipUri(to)).append(CRLF)
                .append(CpimMessage.HEADER_NS).append(": ").append(ImdnDocument.IMDN_NAMESPACE)
                .append(CRLF).append(ImdnUtils.HEADER_IMDN_MSG_ID).append(": ")
                .append(IdGenerator.generateMessageID()).append(CRLF)
                .append(CpimMessage.HEADER_DATETIME).append(": ")
                .append(DateUtils.encodeDate(timestampSent)).append(CRLF).append(CRLF)
                .append(CpimMessage.HEADER_CONTENT_TYPE).append(": ")
                .append(ImdnDocument.MIME_TYPE).append(CRLF)
                .append(CpimMessage.HEADER_CONTENT_DISPOSITION).append(": ")
                .append(ImdnDocument.NOTIFICATION).append(CRLF)
                .append(CpimMessage.HEADER_CONTENT_LENGTH).append(": ")
                .append(imdn.getBytes(UTF8).length).append(CRLF).append(CRLF).append(imdn)
                .toString();
    }

    /**
     * Parse a CPIM delivery report
     * 
     * @param cpim CPIM document
     * @return IMDN document
     * @throws Exception
     */
    public static ImdnDocument parseCpimDeliveryReport(String cpim) throws Exception {
        ImdnDocument imdn = null;
        // Parse CPIM document
        CpimParser cpimParser = new CpimParser(cpim);
        CpimMessage cpimMsg = cpimParser.getCpimMessage();
        if (cpimMsg != null) {
            // Check if the content is a IMDN message
            String contentType = cpimMsg.getContentType();
            if ((contentType != null) && isMessageImdnType(contentType)) {
                // Parse the IMDN document
                imdn = parseDeliveryReport(cpimMsg.getMessageContent());
            }
        }
        return imdn;
    }

    /**
     * Parse a delivery report
     * 
     * @param xml XML document
     * @return IMDN document
     * @throws Exception
     */
    public static ImdnDocument parseDeliveryReport(String xml) throws Exception {
        InputSource input = new InputSource(new ByteArrayInputStream(xml.getBytes()));
        ImdnParser parser = new ImdnParser(input);
        return parser.getImdnDocument();
    }

    /**
     * Build a delivery report
     * 
     * @param msgId Message ID
     * @param status Status
     * @param timestamp Timestamp sent in payload for IMDN datetime
     * @return XML document
     */
    public static String buildImdnDeliveryReport(String msgId, String status, long timestamp) {
        String method;
        if (status.equals(ImdnDocument.DELIVERY_STATUS_DISPLAYED)) {
            method = "display-notification";
        } else if (status.equals(ImdnDocument.DELIVERY_STATUS_DELIVERED)) {
            method = "delivery-notification";
        } else {
            method = "processing-notification";
        }

        return new StringBuilder("<?xml version=\"1.0\" encoding=\"").append(UTF8_STR)
                .append("\"?>").append(CRLF).append("<imdn xmlns=\"urn:ietf:params:xml:ns:imdn\">")
                .append(CRLF).append("<message-id>").append(msgId).append("</message-id>")
                .append(CRLF).append("<datetime>").append(DateUtils.encodeDate(timestamp))
                .append("</datetime>").append(CRLF).append("<").append(method).append("><status><")
                .append(status).append("/></status></").append(method).append(">").append(CRLF)
                .append("</imdn>").toString();
    }

    /**
     * Build a geoloc document
     * 
     * @param geoloc Geolocation
     * @param contact Contact
     * @param msgId Message ID
     * @param timestamp Local timestamp
     * @return XML document
     */
    public static String buildGeolocDocument(Geoloc geoloc, String contact, String msgId,
            long timestamp) {
        String expire = DateUtils.encodeDate(geoloc.getExpiration());
        return new StringBuilder("<?xml version=\"1.0\" encoding=\"").append(UTF8_STR)
                .append("\"?>").append(CRLF)
                .append("<rcsenvelope xmlns=\"urn:gsma:params:xml:ns:rcs:rcs:geolocation\"")
                .append(" xmlns:rpid=\"urn:ietf:params:xml:ns:pidf:rpid\"")
                .append(" xmlns:gp=\"urn:ietf:params:xml:ns:pidf:geopriv10\"")
                .append(" xmlns:gml=\"http://www.opengis.net/gml\"")
                .append(" xmlns:gs=\"http://www.opengis.net/pidflo/1.0\"").append(" entity=\"")
                .append(contact).append("\">").append(CRLF).append("<rcspushlocation id=\"")
                .append(msgId).append("\" label=\"").append(geoloc.getLabel()).append("\" >")
                .append("<rpid:place-type rpid:until=\"").append(expire).append("\">")
                .append("</rpid:place-type>").append(CRLF)
                .append("<rpid:time-offset rpid:until=\"").append(expire)
                .append("\"></rpid:time-offset>").append(CRLF).append("<gp:geopriv>").append(CRLF)
                .append("<gp:location-info>").append(CRLF)
                .append("<gs:Circle srsName=\"urn:ogc:def:crs:EPSG::4326\">").append(CRLF)
                .append("<gml:pos>").append(geoloc.getLatitude()).append(" ")
                .append(geoloc.getLongitude()).append("</gml:pos>").append(CRLF)
                .append("<gs:radius uom=\"urn:ogc:def:uom:EPSG::9001\">")
                .append(geoloc.getAccuracy()).append("</gs:radius>").append(CRLF)
                .append("</gs:Circle>").append(CRLF).append("</gp:location-info>").append(CRLF)
                .append("<gp:usage-rules>").append(CRLF).append("<gp:retention-expiry>")
                .append(expire).append("</gp:retention-expiry>").append(CRLF)
                .append("</gp:usage-rules>").append(CRLF).append("</gp:geopriv>").append(CRLF)
                .append("<timestamp>").append(DateUtils.encodeDate(timestamp))
                .append("</timestamp>").append(CRLF).append("</rcspushlocation>").append(CRLF)
                .append("</rcsenvelope>").append(CRLF).toString();
    }

    /**
     * Parse a geoloc document
     * 
     * @param xml XML document
     * @return Geolocation
     * @throws IllegalArgumentException
     */
    public static Geoloc parseGeolocDocument(String xml) {
        try {
            InputSource geolocInput = new InputSource(new ByteArrayInputStream(xml.getBytes(UTF8)));
            GeolocInfoParser geolocParser = new GeolocInfoParser(geolocInput);
            GeolocInfoDocument geolocDocument = geolocParser.getGeoLocInfo();
            if (geolocDocument == null) {
                throw new IllegalArgumentException("Unable to parse geoloc document!");
            }
            Geoloc geoloc = new Geoloc(geolocDocument.getLabel(), geolocDocument.getLatitude(),
                    geolocDocument.getLongitude(), geolocDocument.getExpiration(),
                    geolocDocument.getRadius());
            return geoloc;
        } catch (ParserConfigurationException e) {
            throw new IllegalArgumentException("Unable to parse geoloc document!", e);
        } catch (SAXException e) {
            throw new IllegalArgumentException("Unable to parse geoloc document!", e);
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to parse geoloc document!", e);
        }
    }

    /**
     * Parse a file transfer resume info
     * 
     * @param xml XML document
     * @return File transfer resume info
     */
    public static FileTransferHttpResumeInfo parseFileTransferHttpResumeInfo(byte[] xml) {
        try {
            InputSource ftHttpInput = new InputSource(new ByteArrayInputStream(xml));
            FileTransferHttpResumeInfoParser ftHttpParser = new FileTransferHttpResumeInfoParser(
                    ftHttpInput);
            return ftHttpParser.getResumeInfo();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Create a text message
     * 
     * @param remote Remote contact identifier
     * @param msg Text message
     * @param timestamp Local timestamp
     * @param timestampSent Timestamp sent in payload
     * @return Text message
     */
    public static ChatMessage createTextMessage(ContactId remote, String msg, long timestamp,
            long timestampSent) {
        String msgId = IdGenerator.generateMessageID();
        return new ChatMessage(msgId, remote, msg, MimeType.TEXT_MESSAGE, timestamp, timestampSent,
                null);
    }

    /**
     * Create a file transfer message
     * 
     * @param remote Remote contact identifier
     * @param fileInfo File XML description
     * @param imdn IMDN flag
     * @param msgId Message ID
     * @param timestamp The local timestamp for the file transfer
     * @param timestampSent The timestamp sent in payload for the file transfer
     * @return File message
     */
    public static ChatMessage createFileTransferMessage(ContactId remote, String fileInfo,
            boolean imdn, String msgId, long timestamp, long timestampSent) {
        return new ChatMessage(msgId, remote, fileInfo, FileTransferHttpInfoDocument.MIME_TYPE,
                timestamp, timestampSent, null);
    }

    /**
     * Create a geoloc message
     * 
     * @param remote Remote contact
     * @param geoloc Geolocation
     * @param timestamp Local timestamp
     * @param timestampSent Timestamp sent in payload
     * @return Geolocation message
     */
    public static ChatMessage createGeolocMessage(ContactId remote, Geoloc geoloc, long timestamp,
            long timestampSent) {
        String msgId = IdGenerator.generateMessageID();
        String geolocContent = buildGeolocDocument(geoloc,
                ImsModule.IMS_USER_PROFILE.getPublicUri(), msgId, timestamp);
        return new ChatMessage(msgId, remote, geolocContent, GeolocInfoDocument.MIME_TYPE,
                timestamp, timestampSent, null);
    }

    /**
     * Get the first message
     * 
     * @param invite Request
     * @param timestamp Local timestamp
     * @return First message
     */
    public static ChatMessage getFirstMessage(SipRequest invite, long timestamp) {
        ChatMessage msg = getFirstMessageFromCpim(invite, timestamp);
        if (msg != null) {
            return msg;
        } else {
            return getFirstMessageFromSubject(invite, timestamp);
        }
    }

    public static boolean isContainingFirstMessage(SipRequest invite) {
        if (invite.getContent() != null) {
            return true;
        }
        if (invite.getSubject().length() > 0) {
            return true;
        }
        return false;
    }

    /**
     * Get the subject
     * 
     * @param invite Request
     * @return String
     */
    public static String getSubject(SipRequest invite) {
        return invite.getSubject();
    }

    /**
     * Get the first message from CPIM content
     * 
     * @param invite Request
     * @param timestamp Local timestamp
     * @return First message
     */
    private static ChatMessage getFirstMessageFromCpim(SipRequest invite, long timestamp) {
        CpimMessage cpimMsg = extractCpimMessage(invite);
        if (cpimMsg == null) {
            return null;
        }
        ContactId remote = getReferredIdentityAsContactId(invite);
        if (remote == null) {
            if (sLogger.isActivated()) {
                sLogger.warn("getFirstMessageFromCpim: cannot parse contact");
            }
            return null;
        }
        String msgId = getMessageId(invite);
        String content = cpimMsg.getMessageContent();
        long timestampSent = cpimMsg.getTimestampSent();
        String mime = cpimMsg.getContentType();
        if (msgId == null || content == null || mime == null) {
            return null;
        }
        if (isGeolocType(mime)) {
            return new ChatMessage(msgId, remote, content, GeolocInfoDocument.MIME_TYPE, timestamp,
                    timestampSent, null);
        } else if (FileTransferUtils.isFileTransferHttpType(mime)) {
            return new ChatMessage(msgId, remote, content, FileTransferHttpInfoDocument.MIME_TYPE,
                    timestamp, timestampSent, null);
        } else if (isTextPlainType(mime)) {
            return new ChatMessage(msgId, remote, content, MimeType.TEXT_MESSAGE, timestamp,
                    timestampSent, null);
        }
        sLogger.warn(new StringBuilder("Unknown MIME-type in first message; msgId=").append(msgId)
                .append(", mime='").append(mime).append("'.").toString());
        return null;
    }

    /**
     * Get the first message from the Subject header
     * 
     * @param invite Request
     * @param timestamp Local timestamp
     * @return First message
     */
    private static ChatMessage getFirstMessageFromSubject(SipRequest invite, long timestamp) {
        String subject = invite.getSubject();
        if (TextUtils.isEmpty(subject)) {
            return null;
        }
        ContactId remote = getReferredIdentityAsContactId(invite);
        if (remote == null) {
            if (sLogger.isActivated()) {
                sLogger.warn("getFirstMessageFromSubject: cannot parse contact");
            }
            return null;
        }
        /**
         * Since in subject, there is no DateTime or datetime included, then we need to fake that by
         * using the local timestamp even if this is not the real proper timestamp from the remote
         * side in this case.
         */
        return new ChatMessage(IdGenerator.generateMessageID(), remote, subject,
                MimeType.TEXT_MESSAGE, timestamp, timestamp, null);
    }

    /**
     * Extract CPIM message from incoming INVITE request
     * 
     * @param request Request
     * @return Boolean
     */
    public static CpimMessage extractCpimMessage(SipRequest request) {
        CpimMessage message = null;
        try {
            // Extract message from content/CPIM
            String content = request.getContent();
            String boundary = request.getBoundaryContentType();
            Multipart multi = new Multipart(content, boundary);
            if (multi.isMultipart()) {
                String cpimPart = multi.getPart(CpimMessage.MIME_TYPE);
                if (cpimPart != null) {
                    // CPIM part
                    message = new CpimParser(cpimPart.getBytes(UTF8)).getCpimMessage();
                }
            }
        } catch (Exception e) {
            message = null;
        }
        return message;
    }

    /**
     * Get participants from 'resource-list' present in XML document and include the 'remote' as
     * participant.
     * 
     * @param request Request
     * @param status Status to assign to created participants
     * @return Participants based on contacts in the request
     */
    public static Map<ContactId, ParticipantStatus> getParticipants(SipRequest request,
            ParticipantStatus status) {
        Map<ContactId, ParticipantStatus> participants = new HashMap<ContactId, ParticipantStatus>();
        String content = request.getContent();
        String boundary = request.getBoundaryContentType();
        Multipart multi = new Multipart(content, boundary);
        if (multi.isMultipart()) {

            String listPart = multi.getPart("application/resource-lists+xml");
            if (listPart != null) {

                participants = ParticipantInfoUtils.parseResourceList(listPart, status);
                ContactId remote = getReferredIdentityAsContactId(request);
                if (remote == null) {
                    if (sLogger.isActivated()) {
                        sLogger.warn("getParticipants: cannot parse contact");
                    }
                } else {
                    /* Include remote contact if format if correct */
                    participants.put(remote, status);
                }
            }
        }
        return participants;
    }

    /**
     * Is request is for FToHTTP
     * 
     * @param request SIP request
     * @return true if FToHTTP
     */
    public static boolean isFileTransferOverHttp(SipRequest request) {
        CpimMessage message = extractCpimMessage(request);
        if (message != null
                && message.getContentType().startsWith(FileTransferHttpInfoDocument.MIME_TYPE)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Generate persisted MIME-type from network pay-load
     * 
     * @param networkMimeType Network pay-load MIME-type
     * @return API MIME-type
     */
    public static String networkMimeTypeToApiMimeType(String networkMimeType) {
        /*
         * Geolocation chat messages does not have the same mimetype in the payload as in the TAPI.
         * Text chat messages do.
         */
        if (isGeolocType(networkMimeType)) {
            return MimeType.GEOLOC_MESSAGE;
        }
        return networkMimeType;
    }

    /**
     * Generate persisted content from network pay-load
     * 
     * @param msg
     * @return Persisted content
     */
    public static String networkContentToPersistedContent(ChatMessage msg) {
        /*
         * Geolocation chat messages does not have the same mimetype in the payload as in the TAPI.
         * Text chat messages do.
         */
        if (isGeolocType(msg.getMimeType())) {
            Geoloc geoloc = parseGeolocDocument(msg.getContent());
            return geoloc.toString();
        }
        return msg.getContent();
    }

    /**
     * Create a chat message
     * 
     * @param msgId
     * @param apiMimeType
     * @param content
     * @param contact
     * @param displayName
     * @param timestamp
     * @param timestampSent
     * @return ChatMessgae
     */
    public static ChatMessage createChatMessage(String msgId, String apiMimeType, String content,
            ContactId contact, String displayName, long timestamp, long timestampSent) {
        if (MimeType.TEXT_MESSAGE.equals(apiMimeType)) {
            return new ChatMessage(msgId, contact, content, MimeType.TEXT_MESSAGE, timestamp,
                    timestampSent, displayName);
        } else if (MimeType.GEOLOC_MESSAGE.equals(apiMimeType)) {
            return new ChatMessage(msgId, contact, content, GeolocInfoDocument.MIME_TYPE,
                    timestamp, timestampSent, displayName);
        }
        throw new IllegalArgumentException(
                "Unable to create message, Invalid mimetype ".concat(apiMimeType));
    }

}