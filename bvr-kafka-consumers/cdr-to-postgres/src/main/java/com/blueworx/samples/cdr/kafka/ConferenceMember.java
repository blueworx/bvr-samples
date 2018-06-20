/*----------------------------------------------------*/
/* Licensed Materials - Property of Blueworx          */
/*                                                    */
/* SAMPLE                                             */
/*                                                    */
/* (c) Copyright Blueworx. 2018. All Rights Reserved. */
/*----------------------------------------------------*/

package com.blueworx.samples.cdr.kafka;

public class ConferenceMember {

    public static final String _copyright = "Licensed Materials - Property of Blueworx. Copyright (c) Blueworx. 2018 All Rights Reserved. ";

    private String callIndex;
    private String callIndexFull;
    private String start;
    private String end;

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

    @Override
    public String toString() {
    	StringBuilder sb = new StringBuilder();

        sb.append("Call Index Full (Unique): " + getCallIndexFull() + "\n");
        sb.append("Call Index: " + getCallIndex() + "\n");
        sb.append("Start: " + getStart() + "\n");
        sb.append("End: " + getEnd() + "\n");

        return sb.toString();
    }
}
