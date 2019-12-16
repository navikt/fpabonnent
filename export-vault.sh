#!/usr/bin/env bash

export DEFAULTDS_USERNAME=$(cat /secrets/oracle/username)
export DEFAULTDS_PASSWORD=$(cat /secrets/oracle/password)
export SYSTEMBRUKER_USERNAME=$(cat /secrets/serviceuser/fpabonnent/username)
export SYSTEMBRUKER_PASSWORD=$(cat /secrets/serviceuser/fpabonnent/password)
