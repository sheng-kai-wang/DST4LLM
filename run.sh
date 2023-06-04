#!/bin/bash

docker run -d -v /etc/localtime:/etc/localtime:ro --name chatops4msa-dst4llm chatops4msa/chatops4msa-dst4llm:v1