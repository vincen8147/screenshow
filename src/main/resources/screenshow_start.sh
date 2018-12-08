#!/bin/bash
JAVA_HOME=/usr/lib/jvm/jdk-8-oracle-arm32-vfp-hflt

${JAVA_HOME}/bin/java -jar \
 /opt/screenshow/target/screenshow-1.0-SNAPSHOT.jar \
 ${SCREENSHOW_GOOGLE_EMAIL} \
 ${SCREENSHOW_GOOGLE_APP_ID} &

