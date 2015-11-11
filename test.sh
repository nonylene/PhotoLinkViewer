#! /usr/bin/env bash

c=0
n=0
until [ $n -ge 2 ]
do
    ./gradlew --info --stacktrace clean :app:connectedAndroidTest && break      # substitute your command here
    c=$?
    n=$[$n+1]
    sleep 15
done

exit $c
