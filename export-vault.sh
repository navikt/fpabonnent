#!/usr/bin/env bash

echo "Kjører  $0"

if test -f /config/oracle/jdbc_url;
then
   export  DEFAULTDS_URL=$(cat /config/oracle/jdbc_url)
   echo "Setter DEFAULTDS_URL to $DEFAULTDS_URL"   
fi

if test -f /secrets/oracle/username;
then
   export  DEFAULTDS_USERNAME= $(cat /secrets/oracle/username)
   echo "Setter DEFAULTDS_USERNAME to $DEFAULTDS_USERNAME"   
fi

if test -f /secrets/oracle/password;
then
   export  DEFAULTDS_PASSWORD=$(cat /secrets/oracle/password)
   echo "Setter DEFAULTDS_PASSWORD"   
fi

if test -f /secrets/serviceuser/fpabonnent/username;
then
   export  SYSTEMBRUKER_USERNAME=$(cat /secrets/serviceuser/fpabonnent/username)
   echo "Setter SYSTEMBRUKER_USERNAME to $SYSTEMBRUKER_USERNAME"   
fi

if test -f /secrets/serviceuser/fpabonnent/password;
then
   export  SYSTEMBRUKER_USERNAME=$(cat /secrets/serviceuser/fpabonnent/password)
   echo "Setter SYSTEMBRUKER_PASSWORD"   
fi