#!/usr/bin/env cwl-runner

class: CommandLineTool
id: Seqware-Sanger-Somatic-Workflow
label: Seqware-Sanger-Somatic-Workflow
dct:creator:
  '@id': http://sanger.ac.uk/...
  foaf:name: Keiran Raine
  foaf:mbox: mailto:keiranmraine@gmail.com
dct:contributor:
  foaf:name: Brian O'Connor
  foaf:mbox: mailto:broconno@ucsc.edu

dct:contributor:
  foaf:name: Denis Yuen
  foaf:mbox: mailto:denis.yuen@oicr.on.ca

requirements:
- class: DockerRequirement
  dockerPull: quay.io/pancancer/pcawg-sanger-cgp-workflow:2.0.3

cwlVersion: v1.0

inputs:
  tumor:
    type: File
    inputBinding:
      position: 1
      prefix: --tumor
    secondaryFiles:
    - .bai

  refFrom:
    type: File
    inputBinding:
      position: 3
      prefix: --refFrom
  bbFrom:
    type: File
    inputBinding:
      position: 4
      prefix: --bbFrom
  normal:
    type: File
    inputBinding:
      position: 2
      prefix: --normal
    secondaryFiles:
    - .bai

outputs:
  somatic_sv_tar_gz:
    type: File
    outputBinding:
      glob: '*.somatic.sv.tar.gz'
  somatic_snv_mnv_tar_gz:
    type: File
    outputBinding:
      glob: '*.somatic.snv_mnv.tar.gz'
  somatic_verifyBamId_tar_gz:
    type: File
    outputBinding:
      glob: '*.somatic.verifyBamId.tar.gz'
  somatic_indel_tar_gz:
    type: File
    outputBinding:
      glob: '*.somatic.indel.tar.gz'
  somatic_genotype_tar_gz:
    type: File
    outputBinding:
      glob: '*.somatic.genotype.tar.gz'
  somatic_cnv_tar_gz:
    type: File
    outputBinding:
      glob: '*.somatic.cnv.tar.gz'
  somatic_imputeCounts_tar_gz:
    type: File
    outputBinding:
      glob: '*.somatic.imputeCounts.tar.gz'
baseCommand: [/start.sh, python, /home/seqware/CGP-Somatic-Docker/scripts/run_seqware_workflow.py]
doc: |
    PCAWG Sanger variant calling workflow is developed by Wellcome Trust Sanger Institute (http://www.sanger.ac.uk/), it consists of software components calling somatic substitutions, indels and structural variants using uniformly aligned tumour / normal WGS sequences. The workflow has been dockerized and packaged using CWL workflow language, the source code is available on GitHub at: https://github.com/ICGC-TCGA-PanCancer/CGP-Somatic-Docker. The workflow is also registered in Dockstore at: https://dockstore.org/containers/quay.io/pancancer/pcawg-sanger-cgp-workflow 
    
    
    ## Run the workflow with your own data
    
    ### Prepare compute environment and install software packages
    The workflow has been tested in Ubuntu 16.04 Linux environment with the following hardware and software settings.
    
    1. Hardware requirement (assuming X30 coverage whole genome sequence)
    - CPU core: 16
    - Memory: 64GB
    - Disk space: 1TB
    
    2. Software installation
    - Docker (1.12.6): follow instructions to install Docker https://docs.docker.com/engine/installation
    - CWL tool
    ```
    pip install cwltool==1.0.20170217172322
    ```
    
    ### Prepare input data
    1. Input aligned tumor / normal BAM files
    
    The workflow uses a pair of aligned BAM files as input, one BAM for tumor, the other for normal, both from the same donor. Here we assume file names are `tumor_sample.bam` and `normal_sample.bam`, and both files are under `bams` subfolder.
    
    2. Reference data files
    
    The workflow also uses two precompiled reference files as input, they can be downloaded from the ICGC Data Portal at [https://dcc.icgc.org/releases/PCAWG/reference_data/pcawg-sanger](https://dcc.icgc.org/releases/PCAWG/reference_data/pcawg-sanger). We assume the two reference files are under `reference` subfolder. 
    
    3. Job JSON file for CWL
    
    Finally, we need to prepare a JSON file with input, reference and output files specified. Please replace the `tumor` and `normal` parameters with your real BAM file names. Parameters for output are file name suffixes, usually don't need to be changed.
    
    Name the JSON file: `pcawg-sanger-variant-caller.job.json`
    ```
    {
      "tumor":
      {
        "path":"bams/tumor_sample.bam",
        "class":"File"
      },
      "normal":
      {
        "path":"bams/normal_sample.bam",
        "class":"File"
      },
      "refFrom":
      {
        "path":"reference/GRCh37d5_CGP_refBundle.tar.gz",
        "class":"File"
      },
      "bbFrom":
      {
        "path":"reference/GRCh37d5_battenberg.tar.gz",
        "class":"File"
      },
      "somatic_snv_mnv_tar_gz":
      {
        "path":"somatic_snv_mnv_tar_gz",
        "class":"File"
      },
      "somatic_cnv_tar_gz":
      {
        "path":"somatic_cnv_tar_gz",
        "class":"File"
      },
      "somatic_sv_tar_gz":
      {
        "path":"somatic_sv_tar_gz",
        "class":"File"
      },
      "somatic_indel_tar_gz":
      {
        "path":"somatic_indel_tar_gz",
        "class":"File"
      },
      "somatic_imputeCounts_tar_gz":
      {
        "path":"somatic_imputeCounts_tar_gz",
        "class":"File"
      },
      "somatic_genotype_tar_gz":
      {
        "path":"somatic_genotype_tar_gz",
        "class":"File"
      },
      "somatic_verifyBamId_tar_gz":
      {
        "path":"somatic_verifyBamId_tar_gz",
        "class":"File"
      }
    }
    ```
