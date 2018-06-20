/*----------------------------------------------------*/
/* Licensed Materials - Property of Blueworx          */
/*                                                    */
/* SAMPLE                                             */
/*                                                    */
/* (c) Copyright Blueworx. 2018. All Rights Reserved. */
/*----------------------------------------------------*/

package com.blueworx.samples.cdr.kafka;

public class MRCPCDR {

	public static final String _copyright = "Licensed Materials - Property of Blueworx. Copyright (c) Blueworx. 2018 All Rights Reserved. ";

    private int sessionDuration;
    private int streamDuration;

    public int getSessionDuration() {
        return sessionDuration;
    }

    public void setSessionDuration(int sessionDuration) {
        this.sessionDuration = sessionDuration;
    }

    public int getStreamDuration() {
        return streamDuration;
    }

    public void setStreamDuration(int streamDuration) {
        this.streamDuration = streamDuration;
    }
}
