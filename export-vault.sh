#!/usr/bin/env bash

if test -f /config/oracle/jdbc_url;
then
   export  DEFAULTDS_URL=$(cat /config/oracle/jdbc_url)
   echo "Setter DEFAULTDS_URL til $DEFAULTDS_URL"   
fi

if test -f /secrets/oracle/username;
then
   export  DEFAULTDS_USERNAME=$(cat /secrets/oracle/username)
   echo "Setter DEFAULTDS_USERNAME til $DEFAULTDS_USERNAME"   
fi

if test -f /secrets/oracle/password;
then
   export  DEFAULTDS_PASSWORD=$(cat /secrets/oracle/password)
   echo "Setter DEFAULTDS_PASSWORD"   
fi

if test -f /secrets/serviceuser/username;
then
   export  SYSTEMBRUKER_USERNAME=$(cat /secrets/serviceuser/username)
   echo "Setter SYSTEMBRUKER_USERNAME til $SYSTEMBRUKER_USERNAME"   
fi

if test -f /secrets/serviceuser/password;
then
   export  SYSTEMBRUKER_PASSWORD=$(cat /secrets/serviceuser/password)
   echo "Setter SYSTEMBRUKER_PASSWORD"   
fi