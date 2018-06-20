/*----------------------------------------------------*/
/* Licensed Materials - Property of Blueworx          */
/*                                                    */
/* SAMPLE                                             */
/*                                                    */
/* (c) Copyright Blueworx. 2018. All Rights Reserved. */
/*----------------------------------------------------*/

package com.blueworx.samples.cdr.kafka;

import java.util.Calendar;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import com.blueworx.samples.cdr.kafka.MRCPCDR;
import com.blueworx.samples.cdr.kafka.XMLCDR;

public class CallDetailRecord {

    public static final String _copyright = "Licensed Materials - Property of Blueworx. Copyright (c) Blueworx. 2018 All Rights Reserved. ";

    private String callIndexFull;       // Call
    private String callIndex;           // Call
    private String bvrMachine;          // Call
    private String type;                // Call and Conference
    private String accountID;           // Call
    private String start;               // Call
    private String end;                 // Call
    private int duration;               // Call and Conference
    private String applicationID;       // Call
    private MRCPCDR tts;                // Call
    private MRCPCDR asr;                // Call
    private XMLCDR xml;                 // Call
    private String direction;           // Call
    private String from;                // Call
    private String to;                  // Call
    private String ani;                 // Call
    private String dnis;                // Call
    private String terminationReason;   // Call
    private String conferenceId;        // Call and Conference
    private ConferenceMember[] members; // Conference

    public String getStart() {
    	return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public String getEnd() {
        return end;
    }

    public void setEnd(String end) {
        this.end = end;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public XMLCDR getXml() {
        return xml;
    }

    public void setXml(XMLCDR xml) {
        this.xml = xml;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getTerminationReason() {
        return terminationReason;
    }

    public void setTerminationReason(String terminationReason) {
        this.terminationReason = terminationReason;
    }

    public String getCallIndex() {
        return callIndex;
    }

    public void setCallIndex(String callIndex) {
        this.callIndex = callIndex;
    }

    public String getCallIndexFull() {
        return callIndexFull;
    }

    public void setCallIndexFull(String callIndexFull) {
        this.callIndexFull = callIndexFull;
    }

    public String getAccountID() {
        return accountID;
    }

    public void setAccountID(String accountID) {
        this.accountID = accountID;
    }

    public String getApplicationID() {
        return applicationID;
    }

    public void setApplicationID(String applicationID) {
        this.applicationID = applicationID;
    }

    public MRCPCDR getTts() {
        return tts;
    }

    public void setTts(MRCPCDR tts) {
        this.tts = tts;
    }

    public MRCPCDR getAsr() {
        return asr;
    }

    public void setAsr(MRCPCDR asr) {
        this.asr = asr;
    }

    public String getBvrMachine() {
        return bvrMachine;
    }

    public void setBvrMachine(String bvrMachine) {
        this.bvrMachine = bvrMachine;
    }

    public String getConferenceId() {
        return conferenceId;
    }

    public void setConferenceId(String conferenceId) {
        this.conferenceId = conferenceId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAni() {
        return ani;
    }

    public void setAni(String ani) {
    	this.ani = ani;
    }

    public String getDnis() {
        return dnis;
    }

    public void setDnis(String dnis) {
    	this.dnis = dnis;
    }

    public ConferenceMember[] getMembers() {
        return members;
    }

    public void setMembers(ConferenceMember[] members) {
        this.members = members;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        if (type.equals("Call")) {
            sb.append("Type: " + getType() + "\n");
            sb.append("Call Index Full (Unique): " + getCallIndexFull() + "\n");
            sb.append("Call Index: " + getCallIndex() + "\n");
            sb.append("Account ID: " + getAccountID() + "\n");
            sb.append("Start: " + getStart() + "\n");
            sb.append("End: " + getEnd() + "\n");
            sb.append("Duration: " + getDuration() + "\n");
            sb.append("Direction: " + getDirection() + "\n");
            sb.append("From: " + getFrom() + "\n");
            sb.append("To: " + getTo() + "\n");
            sb.append("ANI: " + getAni() + "\n");
            sb.append("DNIS: " + getDnis() + "\n");
            sb.append("Termination Reason: " + getTerminationReason() + "\n");
            sb.append("Application ID: " + getApplicationID() + "\n");
            sb.append("BVR Machine: " + getBvrMachine() + "\n");
            sb.append("Conference ID: " + getConferenceId() + "\n");
            if (getTts() != null) {
            	sb.append("TTS - Session Duration: " + getTts().getSessionDuration() + "\n");
            	sb.append("TTS - Stream Duration: " + getTts().getStreamDuration() + "\n");
            }
            if (getAsr() != null) {
            	sb.append("ASR - Session Duration: " + getAsr().getSessionDuration() + "\n");
            	sb.append("ASR - Stream Duration: " + getAsr().getStreamDuration() + "\n");
            }
            if (getXml() != null) {
            	sb.append("XML - Initial: " + getXml().getInitial() + "\n");
            	sb.append("XML - Terminate: " + getXml().getTerminate() + "\n");
            }
        } else if (type.equals("Conference")) {
            sb.append("Type: " + getType() + "\n");
            sb.append("Conference ID: " + getConferenceId() + "\n");
            sb.append("Duration: " + getDuration() + "\n");
            for (ConferenceMember member : members) {
                member.toString();
            }
        }

        return sb.toString();
    }
}
