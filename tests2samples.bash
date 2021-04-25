#!/bin/bash

find ../java-concolic/tests -mindepth 1 -maxdepth 1 -type d | while read f; do
    name=$(basename $f)
    cp "$f/Main.java" "samples/$name"
done
