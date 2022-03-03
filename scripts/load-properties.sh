#!/usr/bin/env bash

function getProperty {
  cat gradle.properties | grep -w "$1" | cut -d '=' -f 2 | tr -d '\n' | xargs echo -n
}