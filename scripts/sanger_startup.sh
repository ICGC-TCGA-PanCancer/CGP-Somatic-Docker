#! /bin/bash

echo "Downloading refdata..."

# Tip: if you mount a volume to DEST_DIR that already contains all of the reference data,  
# you won't have to do a full reference data download.
DEST_DIR=/refdata/data

perl /home/seqware/Seqware-CGP-SomaticCore/workflow/scripts/download_ref.pl  $DEST_DIR
ln -s $DEST_DIR /home/seqware/Seqware-CGP-SomaticCore/target/Workflow_Bundle_CgpSomaticCore_1.0.8_SeqWare_1.1.0/Workflow_Bundle_CgpSomaticCore/1.0.8/data

exec $@
