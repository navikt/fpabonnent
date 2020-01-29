#!/usr/bin/env bash

echo "Running  $0"

if test -f /config/oracle/jdbc_url;
then
   export  DEFAULTDS_URL=$(/config/oracle/jdbc_url)
   echo "Setting DEFAULTDS_URL to $DEFAULTDS_URL"   
fi

if test -f /secrets/oracle/username;
then
   export  DEFAULTDS_USERNAME= $(cat /secrets/oracle/username)
   echo "Setting DEFAULTDS_USERNAME to $DEFAULTDS_USERNAME"   
fi

if test -f /secrets/oracle/password;
then
   export  DEFAULTDS_PASSWORD=$(cat /secrets/oracle/password)
   echo "Setting DEFAULTDS_PASSWORD"   
fi

if test -f /secrets/serviceuser/fpabonnent/username;
then
   export  SYSTEMBRUKER_USERNAME=$(cat /secrets/serviceuser/fpabonnent/username)
   echo "Setting SYSTEMBRUKER_USERNAME to $SYSTEMBRUKER_USERNAME"   
fi

if test -f /secrets/serviceuser/fpabonnent/password;
then
   export  SYSTEMBRUKER_USERNAME=$(cat /secrets/serviceuser/fpabonnent/password)
   echo "Setting SYSTEMBRUKER_PASSWORD"   
fi