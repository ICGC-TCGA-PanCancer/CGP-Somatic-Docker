#! /bin/bash
set -x
# First argument should be path to INI file.
# Example: bash run_sanger.sh `pwd`/test.ini
# If not defined the default ini in target will be used

target_path="/home/seqware/Seqware-CGP-SomaticCore/target/Workflow_Bundle_CgpSomaticCore_0.0.0_SeqWare_1.1.1"
ini="/home/seqware/Seqware-CGP-SomaticCore/workflow/config/CgpSomaticCore.ini"

if [[ $# -eq 0 ]]; then
  echo -e "\t !!! Using default ini file as no params provided !!!";
elif [ "$#" -ne 1 ]; then
  echo "More than 1 argument provided, usage is:"
  echo -e "\tUSAGE: ./run_sanger.sh [<inifile>]"
  exit 1;
else
  ini=$1
fi

/home/seqware/bin/seqware bundle launch \
  --dir $target_path \
  --ini $ini \
  --no-metadata \
  --engine whitestar-parallel
