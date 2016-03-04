#! /bin/bash
set -eu
# If /datastore/work.ini exists then it will be executed
# this should be done by the docker mount -v option
# if not found will default to the in-build version that will attempt to run the test data
#  - If test data is not found it will attempt to download it

ini='/home/seqware/CGP-Somatic-Docker/workflow/config/CgpSomaticCore.ini'
user_ini='/datastore/work.ini'

if [ -f $user_ini ]; then
  echo "PREP: Found user defined ini file at $user_ini";
  ini=$user_ini
else
  echo -e "PREP: No user defined ini file at $user_ini\n\tRunning test config: $ini";
  if [ ! -d "/datastore/HCC1143_ds" ]; then
    echo "PREP: test data not found, downloading...";
    curl -sSL https://s3-eu-west-1.amazonaws.com/wtsi-pancancer/testdata/HCC1143_ds.tar | tar -C /datastore -x
  fi
fi

mvn clean install

target_path=`find /home/seqware/CGP-Somatic-Docker/target -type d -name 'Workflow_Bundle_CgpSomaticCore_*SeqWare_*'`

/home/seqware/bin/seqware bundle launch \
  --dir $target_path \
  --ini $ini \
  --no-metadata \
  --engine whitestar-parallel
