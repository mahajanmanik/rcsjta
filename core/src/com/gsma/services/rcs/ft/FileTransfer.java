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
package com.gsma.services.rcs.ft;

import android.net.Uri;

import com.gsma.services.rcs.JoynServiceException;
import com.gsma.services.rcs.contacts.ContactId;

/**
 * File transfer
 * 
 * @author Jean-Marc AUFFRET
 */
public class FileTransfer {

    /**
     * File transfer state
     */
    public static class State {
    	/**
    	 * Unknown state
    	 */
    	public final static int UNKNOWN = 0;

    	/**
    	 * File transfer invitation received
    	 */
    	public final static int INVITED = 1;
    	
    	/**
    	 * File transfer invitation sent
    	 */
    	public final static int INITIATED = 2;
    	
    	/**
    	 * File transfer is started
    	 */
    	public final static int STARTED = 3;
    	
    	/**
    	 * File transfer has been transferred with success 
    	 */
    	public final static int TRANSFERRED = 4;
    	
    	/**
    	 * File transfer has been aborted 
    	 */
    	public final static int ABORTED = 5;
    	
    	/**
    	 * File transfer has failed
    	 */
    	public final static int FAILED = 6;

    	/**
    	 * File transfer is paused
    	 */
    	public final static int PAUSED = 7;

    	/**
    	 * File transfer is rejected
    	 */
    	public final static int REJECTED = 8;

    	/**
    	 * Accepting, waiting for reply
    	 */
    	public final static int ACCEPTING = 9;
    	
    	/**
    	 * File transfer has been delivered
    	 */
    	public final static int DELIVERED = 10;

    	/**
    	 * File transfer has been displayed or opened
    	 */
    	public final static int DISPLAYED = 11;

    	/**
    	 * File transfer has been queued
    	 */
    	public final static int QUEUED = 12;
    	
    	private State() {
        }    	
    }

    /**
     * File transfer reason code
     */
    public static class ReasonCode {
        /**
         * No specific reason code specified.
         */
        public final static int UNSPECIFIED = 0;

        /**
         * File transfer is aborted by local user.
         */
        public final static int ABORTED_BY_USER = 1;

        /**
         * File transfer is aborted by remote user..
         */
        public final static int ABORTED_BY_REMOTE = 2;

        /**
         * File transfer is aborted by system.
         */
        public final static int ABORTED_BY_SYSTEM = 3;

        /**
         * file transfer is aborted because already taken by the secondary device.
         */
        public final static int ABORTED_BY_SECONDARY_DEVICE = 4;

        /**
         * File transfer has been rejected due to time out.
         */
        public final static int REJECTED_TIME_OUT = 5;

        /**
         * Incoming file transfer was rejected as it was detected as spam.
         */
        public final static int REJECTED_SPAM = 6;

        /**
         * Incoming file transfer was rejected as is cannot be received due to lack of local storage space.
         */
        public final static int REJECTED_LOW_SPACE = 7;

        /**
         * Incoming transfer was rejected as it was too big to be received.
         */
        public final static int REJECTED_MAX_SIZE = 8;

        /**
         * Incoming file transfer was rejected as there was too many file transfers ongoing.
         */
        public final static int REJECTED_MAX_FILE_TRANSFERS = 9;

        /**
         * File transfer invitation was rejected by local user.
         */
        public final static int REJECTED_BY_USER = 10;

        /**
         * File transfer invitation was rejected by remote.
         */
        public final static int REJECTED_BY_REMOTE = 11;

        /**
         * File transfer was paused by system.
         */
        public final static int PAUSED_BY_SYSTEM = 12;

        /**
         * File transfer was paused by user.
         */
        public final static int PAUSED_BY_USER = 13;

        /**
         * File transfer initiation failed.
         */
        public final static int FAILED_INITIATION = 14;

        /**
         * The transferring of the file contents (data) from/to remote side failed.
         */
        public final static int FAILED_DATA_TRANSFER = 15;

        /**
         * Saving of the incoming file transfer failed.
         */
        public final static int FAILED_SAVING = 16;

        /**
         * Delivering of the file transfer invitation failed.
         */
        public final static int FAILED_DELIVERY = 17;

        /**
         * Displaying of the file transfer invitation failed.
         */
        public final static int FAILED_DISPLAY = 18;

        /**
         * File transfer not allowed to be sent.
         */
        public final static int FAILED_NOT_ALLOWED_TO_SEND = 19;
    }
    
    /**
     * File transfer error
     */
    public static class Error {
    	/**
    	 * Transfer has failed
    	 */
    	public final static int TRANSFER_FAILED = 0;
    	
    	/**
    	 * Transfer invitation has been declined by remote
    	 */
    	public final static int INVITATION_DECLINED = 1;

    	/**
    	 * File saving has failed 
       	 */
    	public final static int SAVING_FAILED = 2;
    	
        private Error() {
        }    	
    }

    /**
     * File transfer interface
     */
    private IFileTransfer transferInf;
    
    /**
     * Constructor
     * 
     * @param transferIntf File transfer interface
     * @hide
     */
    public FileTransfer(IFileTransfer transferIntf) {
    	this.transferInf = transferIntf;
    }

	/**
	 * Returns the chat ID if this file transfer is a group file transfer
	 *
	 * @return Chat ID
	 * @throws JoynServiceException
	 */
	public String getChatId() throws JoynServiceException {
		try {
			return transferInf.getChatId();
		} catch (Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}
    	
    /**
	 * Returns the file transfer ID of the file transfer
	 * 
	 * @return Transfer ID
	 * @throws JoynServiceException
	 */
	public String getTransferId() throws JoynServiceException {
		try {
			return transferInf.getTransferId();
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}
	
	/**
	 * Returns the remote contact identifier
	 * 
	 * @return ContactId
	 * @throws JoynServiceException
	 */
	public ContactId getRemoteContact() throws JoynServiceException {
		try {
			return transferInf.getRemoteContact();
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}
	
	/**
     * Returns the complete filename including the path of the file to be transferred
     *
     * @return Filename
	 * @throws JoynServiceException
     */
	public String getFileName() throws JoynServiceException {
		try {
			return transferInf.getFileName();
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}

	/**
     * Returns the size of the file to be transferred
     *
     * @return Size in bytes
	 * @throws JoynServiceException
     */
	public long getFileSize() throws JoynServiceException {
		try {
			return transferInf.getFileSize();
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}	

    /**
     * Returns the MIME type of the file to be transferred
     * 
     * @return Type
	 * @throws JoynServiceException
     */
    public String getFileType() throws JoynServiceException {
		try {
			return transferInf.getFileType();
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
    }
    
	/**
	 * Returns the Uri of the file icon
	 * 
	 * @return the Uri of the file icon or thumbnail
	 * @throws JoynServiceException
	 */
	public Uri getFileIcon() throws JoynServiceException {
		try {
			return transferInf.getFileIcon();
		} catch (Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}

	/**
	 * Returns the Uri of the file
	 *
	 * @return Uri of file
	 * @throws JoynServiceException
	 */
	public Uri getFile() throws JoynServiceException {
		try {
			return transferInf.getFile();
		} catch (Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}

	/**
	 * Returns the state of the file transfer
	 * 
	 * @return State
	 * @see FileTransfer.State
	 * @throws JoynServiceException
	 */
	public int getState() throws JoynServiceException {
		try {
			return transferInf.getState();
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}		
		
	/**
	 * Returns the direction of the transfer (incoming or outgoing)
	 * 
	 * @return Direction
	 * @see Direction
	 * @throws JoynServiceException
	 */
	public int getDirection() throws JoynServiceException {
		try {
			return transferInf.getDirection();
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}
	
	/**
	 * Accepts file transfer invitation
	 * 
	 * @throws JoynServiceException
	 */
	public void acceptInvitation() throws JoynServiceException {
		try {
			transferInf.acceptInvitation();
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}
	
	/**
	 * Rejects file transfer invitation
	 * 
	 * @throws JoynServiceException
	 */
	public void rejectInvitation() throws JoynServiceException {
		try {
			transferInf.rejectInvitation();
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}

	/**
	 * Aborts the file transfer
	 * 
	 * @throws JoynServiceException
	 */
	public void abortTransfer() throws JoynServiceException {
		try {
			transferInf.abortTransfer();
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}
	
	/**
	 * Pauses the file transfer
	 * 
	 * @throws JoynServiceException
	 */
	public void pauseTransfer() throws JoynServiceException {
		try {
			transferInf.pauseTransfer();
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}
	
	/**
	 * Resumes the file transfer
	 * 
	 * @throws JoynServiceException
	 */
	public void resumeTransfer() throws JoynServiceException {
		try {
			transferInf.resumeTransfer();
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}
}
