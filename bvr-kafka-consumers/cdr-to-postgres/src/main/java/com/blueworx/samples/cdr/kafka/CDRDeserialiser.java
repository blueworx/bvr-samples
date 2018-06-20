/*----------------------------------------------------*/
/* Licensed Materials - Property of Blueworx          */
/*                                                    */
/* SAMPLE                                             */
/*                                                    */
/* (c) Copyright Blueworx. 2018. All Rights Reserved. */
/*----------------------------------------------------*/

package com.blueworx.samples.cdr.kafka;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;

import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.blueworx.samples.cdr.kafka.CallDetailRecord;
import com.blueworx.samples.cdr.kafka.MRCPCDR;
import com.blueworx.samples.cdr.kafka.XMLCDR;

public class CDRDeserialiser implements Deserializer<CallDetailRecord> {

    public static final String _copyright = "Licensed Materials - Property of Blueworx. Copyright (c) Blueworx. 2018 All Rights Reserved. ";

    private ObjectMapper mapper = new ObjectMapper();

	  @Override
    public void configure(Map configs, boolean isKey) {
  	    // nothing to configure
    }

    @Override
    public CallDetailRecord deserialize(String topic, byte[] data) {
        CallDetailRecord cdr = null;
        try {
            cdr = mapper.readValue(data, CallDetailRecord.class);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return cdr;
    }

    @Override
    public void close() {
	      // nothing to close
    }
}
