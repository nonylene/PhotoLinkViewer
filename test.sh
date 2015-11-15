#! /usr/bin/env bash

c=0
n=0
port=5554
until [ $n -ge 2 ]
do
    case $CIRCLE_NODE_INDEX in 0) export ANDROID_VERSION=22 ;; 1) export ANDROID_VERSION=22 ;; 2) export ANDROID_VERSION=22 ;; 3) ANDROID_VERSION=22 ;; esac
    nohup bash -c "$ANDROID_HOME/tools/emulator -avd test$ANDROID_VERSION -no-skin -no-boot-anim -no-audio -no-window -port $port &"
    circle-android wait-for-boot
    sleep 30
    n=$[$n+1]
    echo $n test starting...
    ./gradlew --info --stacktrace clean :app:connectedAndroidTest && break      # substitute your command here
    c=$?
    echo $n test errored.
    adb emu kill
    echo kill | nc -w 2 localhost $port
    port=$[$port+2]
    sleep 10
done

echo tested $n times.

exit $c
