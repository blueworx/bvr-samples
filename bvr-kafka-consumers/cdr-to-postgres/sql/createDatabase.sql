/*----------------------------------------------------*/
/* Licensed Materials - Property of Blueworx          */
/*                                                    */
/* SAMPLE                                             */
/*                                                    */
/* (c) Copyright Blueworx. 2018. All Rights Reserved. */
/*----------------------------------------------------*/

-- Create samples user ID
CREATE ROLE bwsamples;
ALTER ROLE bwsamples LOGIN NOBYPASSRLS PASSWORD 'md5bfd4102f654bb7059d11950eccb7b522';

-- Create bw_sample_cdr database
CREATE DATABASE bw_sample_cdr WITH TEMPLATE = template0 OWNER = bwsamples;

-- Connect to bw_sample_cdr database
\connect bw_sample_cdr

-- Create the CallRecords table to hold Call Detail Records
CREATE TABLE "CallRecords" (
  callIndexFull character varying(50) NOT NULL,
  callIndex character varying(16) NOT NULL,
  accountID character varying(10) NOT NULL,
  callStart timestamp(0) NOT NULL,
  callEnd timestamp(0) NOT NULL,
  duration integer NOT NULL,
  direction character varying(10) NOT NULL,
  fromAddress character varying(256) NOT NULL,
  toAddress character varying(256) NOT NULL,
  ani character varying(30) NOT NULL,
  dnis character varying(30) NOT NULL,
  terminationReason character varying(50) NOT NULL,
  ttsSessionDuration integer NOT NULL,
  ttsStreamDuration integer NOT NULL,
  asrSessionDuration integer NOT NULL,
  asrStreamDuration integer NOT NULL,
  applicationID character varying(10) NOT NULL,
  bvrMachine character varying(50) NOT NULL,
  xmlInitial character varying(256) NOT NULL,
  xmlTerminate character varying(256) NOT NULL
);

-- Create the ConferenceRecords table to hold Conference Detail Records
CREATE TABLE "ConferenceRecords" (
  conferenceId character varying(50) NOT NULL,
  duration integer NOT NULL
);

-- Create the ConferenceMembers table to hold the Call to Conference Detail Record links
CREATE TABLE "ConferenceMembers" (
  conferenceId character varying(50) NOT NULL,
  callIndexFull character varying(50) NOT NULL,
  callIndex character varying(16) NOT NULL,
  participantStart timestamp(0) NOT NULL,
  participantEnd timestamp(0) NOT NULL
);

-- Alter the ownership of all tables to the newly created bwsamples ID
ALTER TABLE "CallRecords" OWNER TO bwsamples;
ALTER TABLE "ConferenceRecords" OWNER TO bwsamples;
ALTER TABLE "ConferenceMembers" OWNER TO bwsamples;

-- Finished
