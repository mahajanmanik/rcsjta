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

package com.gsma.rcs.core.ims.service.sip.streaming;

import com.gsma.rcs.core.ims.protocol.sip.SipException;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.protocol.sip.SipResponse;
import com.gsma.rcs.core.ims.protocol.sip.SipTransactionContext;
import com.gsma.rcs.core.ims.service.ImsService;
import com.gsma.rcs.core.ims.service.ImsSessionListener;
import com.gsma.rcs.core.ims.service.SessionTimerManager;
import com.gsma.rcs.core.ims.service.sip.GenericSipSession;
import com.gsma.rcs.core.ims.service.sip.SipSessionError;
import com.gsma.rcs.core.ims.service.sip.SipSessionListener;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

import android.content.Intent;

import java.io.IOException;
import java.util.Collection;

/**
 * Terminating SIP RTP session
 * 
 * @author jexa7410
 */
public class TerminatingSipRtpSession extends GenericSipRtpSession {

    private static final Logger sLogger = Logger.getLogger(TerminatingSipRtpSession.class
            .getSimpleName());

    private final Intent mSessionInvite;

    /**
     * Constructor
     * 
     * @param parent IMS service
     * @param invite Initial INVITE request
     * @param contact
     * @param sessionInvite
     * @param rcsSettings
     * @param timestamp Local timestamp for the session
     * @param contactManager
     */
    public TerminatingSipRtpSession(ImsService parent, SipRequest invite, ContactId contact,
            Intent sessionInvite, RcsSettings rcsSettings, long timestamp,
            ContactManager contactManager) {
        super(parent, contact, GenericSipSession.getIariFeatureTag(invite.getFeatureTags()),
                rcsSettings, timestamp, contactManager);

        mSessionInvite = sessionInvite;
        // Create dialog path
        createTerminatingDialogPath(invite);
    }

    /**
     * Background processing
     */
    public void run() {
        boolean logActivated = sLogger.isActivated();
        if (logActivated) {
            sLogger.info("Initiate a new RTP session as terminating");
        }
        try {
            send180Ringing(getDialogPath().getInvite(), getDialogPath().getLocalTag());

            Collection<ImsSessionListener> listeners = getListeners();
            ContactId contact = getRemoteContact();
            for (ImsSessionListener listener : listeners) {
                ((SipSessionListener) listener).handleSessionInvited(contact, mSessionInvite);
            }

            InvitationStatus answer = waitInvitationAnswer();
            switch (answer) {
                case INVITATION_REJECTED:
                    if (logActivated) {
                        sLogger.debug("Session has been rejected by user");
                    }

                    removeSession();

                    for (ImsSessionListener listener : listeners) {
                        listener.handleSessionRejected(contact,
                                TerminationReason.TERMINATION_BY_USER);
                    }
                    return;

                case INVITATION_TIMEOUT:
                    if (logActivated) {
                        sLogger.debug("Session has been rejected on timeout");
                    }

                    // Ringing period timeout
                    send486Busy(getDialogPath().getInvite(), getDialogPath().getLocalTag());

                    removeSession();

                    for (ImsSessionListener listener : listeners) {
                        listener.handleSessionRejected(contact,
                                TerminationReason.TERMINATION_BY_TIMEOUT);
                    }
                    return;

                case INVITATION_REJECTED_BY_SYSTEM:
                    if (logActivated) {
                        sLogger.debug("Session has been aborted by system");
                    }
                    removeSession();
                    return;

                case INVITATION_CANCELED:
                    if (logActivated) {
                        sLogger.debug("Session has been canceled");
                    }

                    removeSession();

                    for (ImsSessionListener listener : listeners) {
                        listener.handleSessionRejected(contact,
                                TerminationReason.TERMINATION_BY_REMOTE);
                    }
                    return;

                case INVITATION_ACCEPTED:
                    setSessionAccepted();

                    for (ImsSessionListener listener : listeners) {
                        listener.handleSessionAccepted(contact);
                    }
                    break;

                case INVITATION_DELETED:
                    if (logActivated) {
                        sLogger.debug("Session has been deleted");
                    }
                    removeSession();
                    return;

                default:
                    if (logActivated) {
                        sLogger.debug("Unknown invitation answer in run; answer=".concat(String
                                .valueOf(answer)));
                    }
                    return;
            }

            // Build SDP part
            String sdp = generateSdp();

            // Set the local SDP part in the dialog path
            getDialogPath().setLocalContent(sdp);

            // Test if the session should be interrupted
            if (isInterrupted()) {
                if (logActivated) {
                    sLogger.debug("Session has been interrupted: end of processing");
                }
                return;
            }

            // Test if the session should be interrupted
            if (isInterrupted()) {
                if (logActivated) {
                    sLogger.debug("Session has been interrupted: end of processing");
                }
                return;
            }

            // Prepare Media Session
            prepareMediaSession();

            // Create a 200 OK response
            if (logActivated) {
                sLogger.info("Send 200 OK");
            }
            SipResponse resp = create200OKResponse();

            // The signalisation is established
            getDialogPath().sigEstablished();

            // Send response
            SipTransactionContext ctx = getImsService().getImsModule().getSipManager()
                    .sendSipMessageAndWait(resp);

            // Analyze the received response
            if (ctx.isSipAck()) {
                // ACK received
                if (logActivated) {
                    sLogger.info("ACK request received");
                }

                // The session is established
                getDialogPath().sessionEstablished();

                // Start Media Session
                startMediaSession();

                // Start session timer
                if (getSessionTimerManager().isSessionTimerActivated(resp)) {
                    getSessionTimerManager().start(SessionTimerManager.UAS_ROLE,
                            getDialogPath().getSessionExpireTime());
                }

                // Notify listeners
                for (int j = 0; j < getListeners().size(); j++) {
                    getListeners().get(j).handleSessionStarted(contact);
                }
            } else {
                if (logActivated) {
                    sLogger.debug("No ACK received for INVITE");
                }

                // No response received: timeout
                handleError(new SipSessionError(SipSessionError.SESSION_INITIATION_FAILED));
            }
        } catch (IOException e) {
            handleError(new SipSessionError(SipSessionError.MEDIA_FAILED, e));
        } catch (SipException e) {
            sLogger.error(
                    new StringBuilder("Session initiation has failed for CallId=")
                            .append(getDialogPath().getCallId()).append(" ContactId=")
                            .append(getRemoteContact()).toString(), e);
            handleError(new SipSessionError(SipSessionError.MEDIA_FAILED, e));
        } catch (RuntimeException e) {
            /**
             * Intentionally catch runtime exceptions as else it will abruptly end the thread and
             * eventually bring the whole system down, which is not intended.
             */
            sLogger.error(
                    new StringBuilder("Session initiation has failed for CallId=")
                            .append(getDialogPath().getCallId()).append(" ContactId=")
                            .append(getRemoteContact()).toString(), e);
            handleError(new SipSessionError(SipSessionError.MEDIA_FAILED, e));
        }
    }

    @Override
    public boolean isInitiatedByRemote() {
        return true;
    }

}