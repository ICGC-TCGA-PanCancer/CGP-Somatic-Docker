#!/usr/bin/env bash

set -o errexit
set -o pipefail
set -x

gosu root chmod a+wrx /tmp
gosu root chmod a+wrx /var/spool/cwl
gosu root bash -c "$*"
#allow cwltool to pick up the results created by seqware
gosu root chmod -R a+wrx  /var/spool/cwl
