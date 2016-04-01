#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
This script is intended to run within the docker container.

Test data available to download from:
https://s3-eu-west-1.amazonaws.com/wtsi-pancancer/testdata/HCC1143_ds.tar
"""
from __future__ import print_function

import argparse
import glob
import os
import re
import shlex
import subprocess
import sys


def collect_args():
    descr = 'SeqWare-based Variant Calling Workflow from Sanger'
    parser = argparse.ArgumentParser(
        description=descr
    )
    parser.add_argument("--tumor",
                        type=str,
                        required=True,
                        nargs="+",
                        help="tumor BAM input")
    parser.add_argument("--normal",
                        type=str,
                        required=True,
                        help="matched normal BAM input")
    parser.add_argument("--output-dir",
                        type=str,
                        default="/output/",
                        help="directory in which to store the outputs of the \
                        workflow.")
    parser.add_argument("--output-file-basename",
                        dest="output_file_basename",
                        type=str,
                        help="all primary output files with be named following \
                        the convention: \
                        <output_file_basename>.somatic.<output_type>.tar.gz \
                        where output type is one of: [snv_mnv, cnv, sv, indel, \
                        imputeCounts, genotype, verifyBamId]. \
                        Otherwise sample files will be named automatically \
                        following the pattern: \
                        <SM>.<workflowName>.<dateString>.somatic.<output_type>.tar.gz \
                        where SM is extracted from the @RG line in the BAM header.")
    parser.add_argument("--refFrom",
                        type=str,
                        required=True,
                        help="reference file archive for CGP-Somatic-Core workflow. \
                        Available to download from: https://s3-eu-west-1.amazonaws.com/wtsi-pancancer/reference/GRCh37d5_CGP_refBundle.tar.gz ")
    parser.add_argument("--bbFrom",
                        type=str,
                        required=True,
                        help="battenberg reference file archive for CGP-Somatic-Core workflow. \
                        Available to download from: https://s3-eu-west-1.amazonaws.com/wtsi-pancancer/reference/GRCh37d5_battenberg.tar.gz")
    parser.add_argument("--keep-all-seqware-intermediate-output-files",
                        dest='keep_all_seqware_output_files',
                        default=False,
                        action="store_true",
                        help=argparse.SUPPRESS)
    return parser


def write_ini(args, ini_outdir):
    output_dir = os.path.abspath(args.output_dir).split("/")[-1]
    output_prefix = re.sub(output_dir, "", os.path.abspath(args.output_dir))

    if os.path.isfile(os.path.abspath(args.refFrom)):
        refFrom = os.path.abspath(args.refFrom)
    elif re.match("^http", args.refFrom):
        refFrom = args.refFrom
    else:
        raise Exception("refFrom must be a local file or a valid URL")

    if os.path.isfile(os.path.abspath(args.bbFrom)):
        bbFrom = os.path.abspath(args.bbFrom)
    elif re.match("^http", args.bbFrom):
        bbFrom = args.bbFrom
    else:
        raise Exception("bbFrom must be a local file or a valid URL")

    # based on workflow/config/CgpSomaticCore.ini
    # set up like this to make it easy to parameterize addtional settings
    # in the future
    ini_parts = ["refFrom={0}".format(refFrom),
                 "bbFrom={0}".format(bbFrom),
                 # input files
                 "tumourAliquotIds={0}".format(""),
                 "tumourAnalysisIds={0}".format(""),
                 "tumourBams={0}".format(":".join(args.tumor)),
                 "controlAnalysisId={0}".format(""),
                 "controlBam={0}".format(args.normal),
                 # output dir setup
                 "output_dir={0}".format(output_dir),
                 "output_prefix={0}".format(output_prefix),
                 # clean up
                 "cleanup={0}".format("false"),
                 "cleanupBams={0}".format("false"),
                 # basic setup
                 "coresAddressable={0}".format("24"),
                 "memHostMbAvailable={0}".format("108000"),
                 "study-refname-override={0}".format(""),
                 "analysis-center-override={0}".format(""),
                 "assembly={0}".format("GRCh37"),
                 "species={0}".format("human"),
                 "seqType={0}".format("WGS"),
                 "gender={0}".format("L"),
                 "refExclude={0}".format("MT,GL%,hs37d5,NC_007605"),
                 # GENERIC
                 "memWorkflowOverhead={0}".format("3000"),
                 "memMarkTime={0}".format("4000"),
                 "memGenotype={0}".format("4000"),
                 "memContam={0}".format("4000"),
                 "memQcMetrics={0}".format("4000"),
                 "memGetTbi={0}".format("4000"),
                 "memGenerateBasFile={0}".format("4000"),
                 "memPackageResults={0}".format("4000"),
                 # QC
                 "contamDownSampOneIn={0}".format("25"),
                 # BATTENBERG
                 "memUnpack={0}".format("4000"),
                 "memBbMerge={0}".format("4000"),
                 # ASCAT
                 "memAlleleCount={0}".format("4000"),
                 "memAscat={0}".format("8000"),
                 "memAscatFinalise={0}".format("4000"),
                 # PINDEL
                 "memPindelInput={0}".format("7000"),
                 "memPindelPerThread={0}".format("8000"),
                 "memPindelVcf={0}".format("8000"),
                 "memPindelMerge={0}".format("6000"),
                 "memPindelFlag={0}".format("8000"),
                 # BRASS
                 "memBrassInput={0}".format("6000"),
                 # new
                 "memBrassCoverPerThread={0}".format("2000"),
                 # new
                 "memBrassCoverMerge={0}".format("500"),
                 "memBrassGroup={0}".format("4500"),
                 # new group, isize and normcn can run in parallel
                 "memBrassIsize={0}".format("2000"),
                 "memBrassNormCn={0}".format("4000"),
                 "memBrassFilter={0}".format("4500"),
                 "memBrassSplit={0}".format("4000"),
                 "memBrassAssemblePerThread={0}".format("4000"),
                 "memBrassGrass={0}".format("4000"),
                 "memBrassTabix={0}".format("4000"),
                 # CAVEMAN
                 "memCaveCnPrep={0}".format("4000"),
                 "memCavemanSetup={0}".format("4000"),
                 "memCavemanSplit={0}".format("4000"),
                 "memCavemanSplitConcat={0}".format("4000"),
                 "memCavemanMstepPerThread={0}".format("3000"),
                 "memCavemanMerge={0}".format("4000"),
                 "memCavemanEstepPerThread={0}".format("3000"),
                 "memCavemanMergeResults={0}".format("4000"),
                 "memCavemanAddIds={0}".format("4000"),
                 "memCavemanFlag={0}".format("5000"),
                 "memCavemanTbiClean={0}".format("4000")]

    ini = "\n".join(ini_parts)
    ini_filepath = os.path.join(ini_outdir, "workflow.ini")
    with open(ini_filepath, 'wb') as f:
        f.write(ini)
    return ini_filepath

def execute(cmd):
    print("RUNNING...\n", cmd, "\n")
    process = subprocess.Popen(shlex.split(cmd),
                               shell=False,
                               stdout=subprocess.PIPE,
                               stderr=subprocess.PIPE)

    while True:
        nextline = process.stdout.readline()
        if nextline == '' and process.poll() is not None:
            break
        sys.stdout.write(nextline)
        sys.stdout.flush()

    stderr = process.communicate()[1]
    if stderr is not None:
        print(stderr)
    if process.returncode != 0:
        print("[WARNING] command: {0} exited with code: {1}".format(
            cmd, process.returncode
        ))
    return process.returncode


def main():
    parser = collect_args()
    args = parser.parse_args()

    workflow_version = "0.0.0"
    workflow_bundle = "Workflow_Bundle_CgpSomaticCore"
    seqware_bundle_dir = "".join(
        ["/home/seqware/CGP-Somatic-Docker/target/",
         workflow_bundle,
         "_",
         workflow_version,
         "_SeqWare_1.1.1/"]
    )

    output_dir = os.path.abspath(args.output_dir)
    if not os.path.isdir(output_dir):
        # Make the output directory if it does not exist
        execute("mkdir -p {0}".format(output_dir))

    # WRITE WORKFLOW INI
    ini_file = write_ini(args, output_dir)

    # RUN WORKFLOW
    cmd_parts = ["seqware bundle launch",
                 "--dir {0}".format(seqware_bundle_dir),
                 "--engine whitestar-parallel",
                 "--ini {0}".format(ini_file),
                 "--no-metadata"]
    cmd = " ".join(cmd_parts)
    execute(cmd)

    if args.output_file_basename is not None:
        # find all primary output file archives
        output_files = glob.glob(
            os.path.join(output_dir, "*.somatic.*.tar.gz")
        )

        for f in output_files:
            new_f = [args.output_file_basename]
            f_base = os.path.basename(f).split(".")
            # extract out ["somatic", <output_type>, "tar", "gz"] and append to
            # new file basename
            new_f += f_base[-4:]

            # rename file
            execute("mv {0} {1}".format(
                f, os.path.join(output_dir, ".".join(new_f))
            ))

    if (args.keep_all_seqware_output_files):
        # find seqware tmp output path; it contains generated scripts w/
        # stdout stderr for each step
        run_info_output_path = glob.glob("/datastore/oozie-*")[0]

        # move all files to the output directory
        execute("mv {0}/* {1}/".format(
            run_info_output_path, output_dir
        ))


if __name__ == "__main__":
    main()
