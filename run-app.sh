#!/usr/bin/env bash
export JAVA_PROGRAM_ARGS=`echo "$@"`
mvn exec:java -Dexec.mainClass="com.coursecoder.training.CurrencyConverter" -Dexec.args="$JAVA_PROGRAM_ARGS"