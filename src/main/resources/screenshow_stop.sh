#!/bin/bash

pid=`ps aux | \
  grep screenshow-1.0 | \
  grep -v grep | awk '{print $2}'`

kill -9 ${pid}