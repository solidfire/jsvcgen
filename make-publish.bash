#!/usr/bin/env bash
sbt '; + clean; + test; + publishSigned'
echo copy assembly to sdk-java
cp jsvcgen-assembly/target/jsvcgen-assembly-*-SNAPSHOT.jar ../solidfire-sdk-java
echo copy assembly to sdk-python
cp jsvcgen-assembly/target/jsvcgen-assembly-*-SNAPSHOT.jar ../solidfire-sdk-python
echo copy assembly to sdk-dotnet
cp jsvcgen-assembly/target/jsvcgen-assembly-*-SNAPSHOT.jar ../solidfire-sdk-dotnet
