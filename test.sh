#! /usr/bin/env bash

c=0
n=0
until [ $n -ge 2 ]
do
    n=$[$n+1]
    echo $n test starting...
    ./gradlew --info --stacktrace clean :app:connectedAndroidTest && break      # substitute your command here
    c=$?
    echo $n test errored.
    adb reboot
    sleep 50
    circle-android wait-for-boot
done

echo tested $n times.

exit $c
