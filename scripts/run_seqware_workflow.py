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
import subprocess
import sys
import multiprocessing


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
                        required=True,
                        help="directory in which to store the outputs of the \
                        workflow.")
    parser.add_argument("--run-id",
                        dest="run_id",
                        type=str,
                        help="all primary output files with be named following \
                        the convention: \
                        <run_id>.<workflowName>.<dateString>.somatic.<output_type>.tar.gz \
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
                        Available to download from: \
                        https://s3-eu-west-1.amazonaws.com/wtsi-pancancer/reference/GRCh37d5_battenberg.tar.gz")
    parser.add_argument("--keep-all-seqware-intermediate-output-files",
                        dest='keep_all_seqware_output_files',
                        default=False,
                        action="store_true",
                        help=argparse.SUPPRESS)
    parser.add_argument("--coreNum",
                        type=int,
                        default=multiprocessing.cpu_count(),
                        help="number of CPU cores to use"
                        )
    parser.add_argument("--memGB",
                        type=int,
                        default=int((os.sysconf('SC_PAGE_SIZE') * os.sysconf('SC_PHYS_PAGES') * 0.85)/(1024.**3)),
                        help="maximum RAM in GB to use"
                        )
    return parser


def write_ini(args):
    outdir = os.path.abspath(args.output_dir)
    output_dir = outdir.split("/")[-1]
    output_prefix = re.sub(output_dir, "", outdir)

    # check refFrom
    if os.path.isfile(os.path.abspath(args.refFrom)):
        refFrom = os.path.abspath(args.refFrom)
    elif re.match("^http", args.refFrom):
        refFrom = args.refFrom
    else:
        raise Exception("refFrom must be a local file or a valid URL")

    # check bbFrom
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
                 # "coresAddressable={0}".format("24"),
                 "coresAddressable={0}".format(args.coreNum),
                 # "memHostMbAvailable={0}".format("108000"),
                 "memHostMbAvailable={0}".format(args.memGB * 1024),
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
    ini_filepath = os.path.join(outdir, "workflow.ini")
    with open(ini_filepath, 'wb') as f:
        f.write(ini)
    return ini_filepath


def execute(cmd):
    print("RUNNING...\n", cmd, "\n")
    process = subprocess.Popen(cmd,
                               shell=True,
                               stdout=subprocess.PIPE,
                               stderr=subprocess.PIPE)

    while True:
        nextline = process.stdout.readline()
        if nextline == '' and process.poll() is not None:
            break
        sys.stdout.write(nextline)
        sys.stdout.flush()

    stderr = process.communicate()[1]
    if process.returncode != 0:
        print(
            "[ERROR] command:", cmd, "exited with code:", process.returncode,
            file=sys.stderr
        )
        print(stderr, file=sys.stderr)
        raise RuntimeError
    else:
        if stderr is not None:
            print("----------------------------------------------------------",
                  file=sys.stderr)
            print("stderr for '", cmd, "' was:", file=sys.stderr)
            print("----------------------------------------------------------",
                  file=sys.stderr)
            print(stderr, file=sys.stderr)
    return process.returncode


def main():
    parser = collect_args()
    args = parser.parse_args()
    output_dir = os.path.abspath(args.output_dir)

    os.environ['TMPDIR'] = "/tmp"
    # this is necessary because SeqWare will self-install to $HOME dir,
    # we make sure $HOME is same as output_dir. $HOME may not be the same
    # as output_dir if this script is run using sudo
    os.environ['HOME'] = output_dir
    execute("env")
    execute("whoami")

    workflow_version = "0.0.0"
    workflow_bundle = "Workflow_Bundle_CgpSomaticCore"
    seqware_bundle_dir = "".join(
        ["/home/seqware/CGP-Somatic-Docker/target/",
         workflow_bundle,
         "_",
         workflow_version,
         "_SeqWare_1.1.1/"]
    )

    if not os.path.isdir(output_dir):
        # Make the output directory if it does not exist
        execute("mkdir -p {0}".format(output_dir))

    # RUN WORKFLOW
    # workaround for docker permissions for cwltool
    execute("gosu root mkdir -p %s/.seqware" % output_dir)
    execute("gosu root chown -R seqware %s" % output_dir)
    execute("gosu root cp /home/seqware/.seqware/settings %s/.seqware" % output_dir)
    execute("gosu root chmod a+wrx %s/.seqware/settings" % output_dir)
    execute("perl -pi -e 's/wrench.res/seqwaremaven/g' /home/seqware/bin/seqware")
    execute("echo \"options(bitmapType='cairo')\" > %s/.Rprofile" % output_dir)

    # WRITE WORKFLOW INI
    ini_file = write_ini(args)

    cmd_parts = ["seqware bundle launch",
                 "--dir {0}".format(seqware_bundle_dir),
                 "--engine whitestar-parallel",
                 "--ini {0}".format(ini_file),
                 "--no-metadata"]
    cmd = " ".join(cmd_parts)
    execute(cmd)

    # get the filename prefix
    genotype_tar = glob.glob(os.path.join(output_dir, "*.somatic.genotype.tar.gz"))
    file_base = os.path.basename(genotype_tar[0]).split('.')
    prefix = '.'.join(file_base[0:3])

    # tar gzip *.bam.bas files and generate md5
    execute("cd {0} && tar -cvzf {1}.bas.tar.gz *.bam.bas".format(output_dir, prefix))
    execute("cat {0}/{1}.bas.tar.gz | md5sum | cut -b 1-33 > {0}/{1}.bas.tar.gz.md5".format(output_dir, prefix))

    # tar gzip timing_metrics json files and generate md5
    execute("tar -cvzf {0}/{1}.timing_metrics.tar.gz -C {0} process_metrics.json".format(output_dir, prefix))
    execute("cat {0}/{1}.timing_metrics.tar.gz | md5sum | cut -b 1-33 > {0}/{1}.timing_metrics.tar.gz.md5".format(output_dir, prefix))

    # tar gzip qc_metrics json files and generate md5
    execute("tar -cvzf {0}/{1}.qc_metrics.tar.gz -C {0} qc_metrics.json".format(output_dir, prefix))
    execute("cat {0}/{1}.qc_metrics.tar.gz | md5sum | cut -b 1-33 > {0}/{1}.qc_metrics.tar.gz.md5".format(output_dir, prefix))

    if args.run_id is not None:
        # find all output file archives
        output_files = glob.glob(
            os.path.join(output_dir, "*.*")
        )

        for f in output_files:
            new_f = [args.run_id]
            f_base = os.path.basename(f).split(".")
            new_f += f_base[1:]

            # rename file
            execute("gosu root mv {0} {1}".format(
                f, os.path.join(output_dir, ".".join(new_f))
            ))

    if (args.keep_all_seqware_output_files):
        # find seqware tmp output path; it contains generated scripts w/
        # stdout stderr for each step
        run_info_output_path = glob.glob("/datastore/oozie-*")[0]

        # move all files to the output directory
        execute("gosu root mv {0}/* {1}/".format(
            run_info_output_path, output_dir
        ))

if __name__ == "__main__":
    main()
