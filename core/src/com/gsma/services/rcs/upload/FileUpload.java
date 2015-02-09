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

package com.gsma.services.rcs.upload;

import com.gsma.services.rcs.RcsServiceException;

import android.net.Uri;
import android.util.SparseArray;

/**
 * File upload
 * 
 * @author Jean-Marc AUFFRET
 */
public class FileUpload {

    /**
     * File upload state
     */
    public enum State {

        /**
         * Inactive state
         */
        INACTIVE(0),

        /**
         * Upload is started
         */
        STARTED(1),

        /**
         * Upload has been aborted
         */
        ABORTED(2),

        /**
         * Upload has failed
         */
        FAILED(3),

        /**
         * File has been transferred with success
         */
        TRANSFERRED(4);

        private final int mValue;

        private static SparseArray<State> mValueToEnum = new SparseArray<State>();
        static {
            for (State entry : State.values()) {
                mValueToEnum.put(entry.toInt(), entry);
            }
        }

        private State(int value) {
            mValue = value;
        }

        public final int toInt() {
            return mValue;
        }

        public static final State valueOf(int value) {
            State entry = mValueToEnum.get(value);
            if (entry != null) {
                return entry;
            }
            throw new IllegalArgumentException(new StringBuilder("No enum const class ")
                    .append(State.class.getName()).append(".").append(value).append("!").toString());
        }
    }

    /**
     * File upload interface
     */
    private IFileUpload uploadInf;

    /**
     * Constructor
     * 
     * @param uploadInf Upload interface
     */
    /* package private */FileUpload(IFileUpload uploadInf) {
        this.uploadInf = uploadInf;
    }

    /**
     * Returns the upload ID of the upload
     * 
     * @return Upload ID
     * @throws RcsServiceException
     */
    public String getUploadId() throws RcsServiceException {
        try {
            return uploadInf.getUploadId();
        } catch (Exception e) {
            throw new RcsServiceException(e.getMessage());
        }
    }

    /**
     * Returns the URI of the file to be uploaded
     * 
     * @return Uri
     * @throws RcsServiceException
     */
    public Uri getFile() throws RcsServiceException {
        try {
            return uploadInf.getFile();
        } catch (Exception e) {
            throw new RcsServiceException(e.getMessage());
        }
    }

    /**
     * Returns info related to the uploaded file on the content server
     * 
     * @return Upload info or null if not yet upload or in case of error
     * @see FileUploadInfo
     * @throws RcsServiceException
     */
    public FileUploadInfo getUploadInfo() throws RcsServiceException {
        try {
            return uploadInf.getUploadInfo();
        } catch (Exception e) {
            throw new RcsServiceException(e.getMessage());
        }
    }

    /**
     * Returns the state of the upload
     * 
     * @return State
     * @see FileUpload.State
     * @throws RcsServiceException
     */
    public State getState() throws RcsServiceException {
        try {
            return State.valueOf(uploadInf.getState());
        } catch (Exception e) {
            throw new RcsServiceException(e.getMessage());
        }
    }

    /**
     * Aborts the upload
     * 
     * @throws RcsServiceException
     */
    public void abortUpload() throws RcsServiceException {
        try {
            uploadInf.abortUpload();
        } catch (Exception e) {
            throw new RcsServiceException(e.getMessage());
        }
    }
}
