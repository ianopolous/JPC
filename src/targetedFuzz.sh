#!/bin/bash
#find all unique instruction encodings used during the boot of a target, sorted rarest first
java -jar JPCApplication.jar -config $1 -log-disam | sort | uniq -d -c | sort -n > $1.disam
java -jar Tools.jar -testgen $1.disam