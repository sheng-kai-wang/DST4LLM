#!/bin/bash

mvn clean install -Dmaven.test.skip=true
cp ./target/*.jar app.jar

docker build . -t dst4llm:v1