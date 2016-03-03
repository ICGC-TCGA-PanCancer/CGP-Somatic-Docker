#!/usr/bin/env python

from __future__ import print_function

import argparse
import logging
import os
import subprocess


def collect_args():
    descr = 'SeqWare-based Variant Calling Workflow from Sanger'
    parser = argparse.ArgumentParser(
        description=descr
    )
    parser.add_argument("--tumor",
                        type=str,
                        required=True,
                        help="tumor BAM input")
    parser.add_argument("--normal",
                        type=str,
                        required=True,
                        help="matched normal BAM input")
    parser.add_argument("--reference-gz",
                        type=str,
                        required=True,
                        help="gzipped reference genome fasta (GR37)")
    parser.add_argument("--reference-fai",
                        type=str,
                        required=True,
                        help="reference genome fasta index")
    parser.add_argument("--reference-",
                        type=str,
                        required=True,
                        help="")
    return parser


def link_references(args, dest="/home/seqware/Seqware-CGP-SomaticCore/target/Workflow_Bundle_CgpSomaticCore_1.0.8_SeqWare_1.1.0/Workflow_Bundle_CgpSomaticCore/1.0.8/data"):
    pass


def write_ini(args):
    pass


def execute(cmd):
    logging.info("RUNNING: %s" % (cmd))
    print("\nRUNNING...\n", cmd, "\n")
    p = subprocess.Popen(cmd, shell=True, stdout=subprocess.PIPE,
                         stderr=subprocess.PIPE)
    stdout, stderr = p.communicate()
    if stderr is not None:
        print(stderr)
    if stdout is not None:
        print(stdout)
    return p.returncode


def main():
    parser = collect_args()
    args = parser.parse_args()


# TODO: Move this to a template file, maybe mustache?
base_ini = """# the output directory is a convention used in many workflows to specify a relative output path
output_dir=seqware-results
# the output_prefix is a convention used to specify the root of the absolute output path or an S3 bucket name
# you should pick a path that is available on all cluster nodes and can be written by your user
output_prefix=./
# cleanup true will remove just the input BAMs if not uploading and the full output directory if uploading
# false there will be no cleanup which is useful for debugging
cleanup=false

# START NEW ITEMS FOR 1.0.6
gnos_retries=3
gnos_timeout_min=20
downloadBamsFromS3=false
S3DownloadKey=slkdfjslkdj
S3DownloadSecretKey=lkdjflskdjflskdj
# comma separated
tumorS3Urls=s3://bucket/path/7723a85b59ebce340fe43fc1df504b35.bam
normalS3Url=s3://bucket/path/8f957ddae66343269cb9b854c02eee2f.bam
# END NEW ITEMS FOR 1.0.6

# these are just used for tracking
donor_id=unknown
project_code=unknown

# memory for upload to SFTP/S3
duckJobMem=16000

# tracking instance types
vm_instance_type=unknown
vm_instance_cores=unknown
vm_instance_mem_gb=unknown
vm_location_code=unknown

# cleanup
cleanupBams=false

# archive tarball
saveUploadArchive=true
uploadArchivePath=./seqware-results/upload_archive/

# options for tarball to SFTP
SFTPUploadArchive=true
# can be overwrite or skip, see https://trac.cyberduck.io/wiki/help/en/howto/cli
SFTPUploadArchiveMode=overwrite
SFTPUploadArchiveUsername=boconnor
SFTPUploadArchivePassword=klsdfskdjfskjd
SFTPUploadArchiveServer=10.1.1.13
SFTPUploadArchivePath=/upload/path/directory/

# options for tarball to S3
S3UploadArchive=true
S3UploadArchiveMode=overwrite
S3UploadArchiveBucketURL=s3://bucketname/uploads/
S3UploadArchiveKey=slkdfjslkdj
S3UploadArchiveSecretKey=lkdjflskdjflskdj

# send the files not a tarball
SFTPUploadFiles=true
# can be overwrite or skip, see https://trac.cyberduck.io/wiki/help/en/howto/cli
SFTPUploadMode=overwrite
SFTPUploadUsername=boconnor
SFTPUploadPassword=klsdfskdjfskjd
SFTPUploadServer=10.1.1.13
SFTPUploadPath=/upload/path/directory/

S3UploadFiles=true
S3UploadFileMode=overwrite
S3UploadBucketURL=s3://bucketname/uploads/
S3UploadKey=slkdfjslkdj
S3UploadSecretKey=lkdjflskdjflskdj

# synapse upload
# I DO NOT recommend you use this, it has not been tested well
SynapseUpload=true
SynapseUploadSFTPUsername=boconnor
SynapseUploadSFTPPassword=klsdfskdjfskjd
SynapseUploadUsername=boconnor
SynapseUploadPassword=klsdfskdjfskjd
SynapseUploadURL=sftp://tcgaftps.nci.nih.gov/tcgapancan/pancan/variant_calling_pilot_64/OICR_Sanger_Core
SynapseUploadParent=syn3155834

# if set, these trigger an upload of the input bam file to a GNOS repository
# I DO NOT recommend you use this, this hasn't been tested and is likely to have bugs/issues
bamUploadServer=
bamUploadPemFile=
bamUploadStudyRefnameOverride=
bamUploadAnalysisCenterOverride=
bamUploadScriptJobMem=10000
bamUploadScriptJobSlots=1

# if set to true GNOS is still used for downloading metadata but the input bam file paths are assumed to be local file paths
# specifically tumourBams is a colon-delimited list of full file paths and controlBam is a full file path
localFileMode=false
# if localFileMode is true then you need to give the path where the decider downloads all the XML
localXMLMetadataPath=path_to_decider_download_dir_for_xml

# another option for upload
skip-validate=false

# when localFileMode=true, if set this causes the bam file paths to be modified so they are <localBamFilePathPrefix>/<analysis_id>/<bam_file>, also affects the bai path implicitly. This is done so you can continue working with the decider.
localBamFilePathPrefix=

# basic setup
coresAddressable=32
memHostMbAvailable=240000
tabixSrvUri=http://10.89.9.50/

# Use public pulldown data bundled with workflow instead of full genome scale data from GNOS
#testMode=true

pemFile=/home/ubuntu/.gnos/gnos.pem
gnosServer=https://gtrepo-ebi.annailabs.com
## comment out upload server to block vcfUpload
uploadServer=https://gtrepo-ebi.annailabs.com
uploadPemFile=/home/ubuntu/.gnos/gnos.pem

study-refname-override=icgc_pancancer_vcf_test
#analysis-center-override=
#center-override=
#ref-center-override=
#upload-test=true
#upload-skip=true

assembly=GRCh37
species=human
seqType=WGS
gender=L

# PD4116a 30x vs PD4116b 30x (BRCA-UK::CGP_donor_1199138)
tumourAliquotIds=f393bb07-270c-2c93-e040-11ac0d484533
tumourAnalysisIds=ef26d046-e88a-4f21-a232-16ccb43637f2
tumourBams=7723a85b59ebce340fe43fc1df504b35.bam
controlAnalysisId=1b9215ab-3634-4108-9db7-7e63139ef7e9
controlBam=8f957ddae66343269cb9b854c02eee2f.bam

refExclude=MT,GL%,hs37d5,NC_007605

# GNOS
memBasFileGet=4000
memGnosDownload=14000
memUpload=14000

# GENERIC
memWorkflowOverhead=3000
memPackageResults=4000
memMarkTime=4000
memGenotype=4000
memContam=4000
memQcMetrics=4000
memGetTbi=4000

contamDownSampOneIn=25

#PICNIC
memPicnicCounts=4000
memPicnicMerge=4000

#BATTENBERG
memUnpack=4000
memBbMerge=4000

# ASCAT
memAlleleCount=4000
memAscat=8000
memAscatFinalise=4000

# PINDEL
memPindelInput=7000
memPindelPerThread=8000
memPindelVcf=8000
memPindelMerge=6000
memPindelFlag=8000

# BRASS
memBrassInput=6000
memBrassGroup=4500
memBrassFilter=4500
memBrassSplit=4000
memBrassAssemblePerThread=4000
memBrassGrass=4000
memBrassTabix=4000

# CAVEMAN
memCaveCnPrep=4000
memCavemanSetup=4000
memCavemanSplit=4000
memCavemanSplitConcat=4000
memCavemanMstepPerThread=3000
memCavemanMerge=4000
memCavemanEstepPerThread=3000
memCavemanMergeResults=4000
memCavemanAddIds=4000
memCavemanFlag=5000
memCavemanTbiClean=4000
"""

if __name__ == "__main__":
    main()
