#!/usr/bin/env python
# -*- coding: utf-8 -*-

from __future__ import print_function

import argparse
import glob
import logging
import os
import re
import subprocess
import tarfile

# set global variable for workflow version
workflow_version = "1.0.8"
global workflow_version


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
    parser.add_argument("--reference-tarball",
                        type=str,
                        required=True,
                        help="reference file archive for CGP-Somatic-Core workflow")
    return parser


def link_references(reference_tar_gz):
    dest = "".join(
        ["/home/seqware/Seqware-CGP-SomaticCore/target/Workflow_Bundle_CgpSomaticCore_",
         workflow_version,
         "_SeqWare_1.1.0/Workflow_Bundle_CgpSomaticCore/",
         workflow_version,
         "/data"])

    extracted_refs = untar(os.path.abspath(reference_tar_gz))

    if not os.path.isdir(dest):
        os.makedirs(dest, exist_ok=True)

    # symlink extracted reference files to dest and
    # maintain dir stucture from tar archive
    execute("ln -s {0} {1}".format(os.path.join(extracted_refs, "*"), dest))


def write_ini(args):
    pass


def untar(fname, dest):
    if (fname.endswith("tar.gz")):
        tar = tarfile.open(fname)
        if not os.path.isdir(dest):
            os.makedirs(dest, exist_ok=True)
        tar.extractall(path=dest)
        tar.close()
        output_dir = os.path.join(
            dest, re.sub("\.tar\.gz", "", os.path.basename(fname)))
        print("Extracted in: {0}".format(output_dir))
        return output_dir
    else:
        raise Exception("Not a tar.gz file: {0}".format(fname))


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

    # PUT INPUT AND REF FILE IN THE RIGHT PLACE
    link_references(args)

    # WRITE WORKFLOW INI
    write_ini(args)

    # RUN WORKFLOW
    cmd_parts = ["seqware bundle launch",
                 "--dir /home/seqware/Seqware-BWA-Workflow/target/Workflow_Bundle_BWA_{0}_SeqWare_1.1.1".format(workflow_version),
                 "--engine whitestar-parallel",
                 "--ini workflow.ini",
                 "--no-metadata"]
    cmd = " ".join(cmd_parts)
    execute(cmd)

    # FIND OUTPUT
    path = os.path.dirname(glob.glob("/datastore/oozie-")[0])
    results_dir = os.path.join("/datastore/", path, "data")

    # MOVE THESE TO THE RIGHT PLACE
    # system("mv /datastore/$path/data/merged_output.bam* $cwd")
    # system("mv /datastore/$path/data/merged_output.unmapped.bam* $cwd")


# based on workflow/config/CgpSomaticCore.ini
base_ini = """
# the output directory is a convention used in many workflows to specify a relative output path
output_dir=seqware-results
# the output_prefix is a convention used to specify the root of the absolute output path or an S3 bucket name
# you should pick a path that is available on all cluster nodes and can be written by your user
output_prefix=/datastore/
# cleanup true will remove just the input BAMs if not uploading and the full output directory if uploading
# false there will be no cleanup which is useful for debugging
cleanup=false

# cleanup
cleanupBams=false

# basic setup
coresAddressable=32
memHostMbAvailable=212000

study-refname-override=
analysis-center-override=

assembly=GRCh37
species=human
seqType=WGS
gender=L

# ref files
refFrom=
bbFrom=

# input files
tumourAliquotIds=
tumourAnalysisIds=
tumourBams=

controlAnalysisId=
controlBam=

refExclude=MT,GL%,hs37d5,NC_007605

# GENERIC
memWorkflowOverhead=3000
memMarkTime=4000
memGenotype=4000
memContam=4000
memQcMetrics=4000
memGetTbi=4000
memGenerateBasFile=4000
memPackageResults=4000

# QC
contamDownSampOneIn=25

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
#new
memBrassCoverPerThread=2000
# new
memBrassCoverMerge=500
memBrassGroup=4500
# new group, isize and normcn can run in parallel
memBrassIsize=2000
# new group, isize and normcn can run in parallel
memBrassNormCn=4000
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
