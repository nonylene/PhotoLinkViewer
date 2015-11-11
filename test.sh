#! /usr/bin/env bash

c=0
n=0
until [ $n -ge 2 ]
do
    n=$[$n+1]
    echo $n test starting...
    nohup bash -c "case $CIRCLE_NODE_INDEX in 0) $ANDROID_HOME/tools/emulator -avd test22 -no-skin -no-boot-anim -no-audio -no-window ;; 1) $ANDROID_HOME/tools/emulator -avd test22 -no-skin -no-boot-anim -no-audio -no-window ;; 2) $ANDROID_HOME/tools/emulator -avd test22 -no-skin -no-boot-anim -no-audio -no-window ;; 3) $ANDROID_HOME/tools/emulator -avd test22 -no-skin -no-boot-anim -no-audio -no-window ;; esac &"
    circle-android wait-for-boot
    sleep 30
    ./gradlew --info --stacktrace clean :app:connectedAndroidTest && break      # substitute your command here
    c=$?
    echo $n test errored.
    adb emu kill
    sleep 30
done

echo tested $n times.

exit $c
