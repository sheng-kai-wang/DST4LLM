#!/bin/bash

mvn clean install -Dmaven.test.skip=true
cp ./target/*.jar app.jar

docker build . -t chatops4msa/chatops4msa-dst4llm:v1