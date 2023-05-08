#!/bin/bash

mvn clean install -Dmaven.test.skip=true
cp ./target/MsdoBot-LLM-Lab-0.0.1-SNAPSHOT.jar app.jar

docker build . -t msdobot/msdobot-llm-lab:v1