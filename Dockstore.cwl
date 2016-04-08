#!/usr/bin/env cwl-runner

class: CommandLineTool
id: "Seqware-Sanger-Somatic-Workflow"
label: "Seqware-Sanger-Somatic-Workflow"

description: |
    The Sanger's Cancer Genome Project core somatic calling workflow from 
    the ICGC PanCancer Analysis of Whole Genomes (PCAWG) project.
    For more information see the PCAWG project [page](https://dcc.icgc.org/pcawg) and our GitHub
    [page](https://github.com/ICGC-TCGA-PanCancer) for our code including the source for
    [this workflow](https://github.com/ICGC-TCGA-PanCancer/CGP-Somatic-Docker).
    ```
    Usage:
    # fetch CWL
    $> dockstore cwl --entry quay.io/pancancer/pcawg-sanger-cgp-workflow:2.0.0 > Dockstore.cwl
    # make a runtime JSON template and edit it
    $> dockstore convert cwl2json --cwl Dockstore.cwl > Dockstore.json
    # run it locally with the Dockstore CLI
    $> dockstore launch --entry quay.io/pancancer/pcawg-sanger-cgp-workflow:2.0.0 \
        --json Dockstore.json
    ```

dct:creator:
  "@id": "http://sanger.ac.uk/..."
  foaf:name: "Keiran Raine"
  foaf:mbox: "mailto:keiranmraine@gmail.com"

requirements:
  - class: ExpressionEngineRequirement
    id: "#node-engine"
    requirements:
    - class: DockerRequirement
      dockerPull: commonworkflowlanguage/nodejs-engine
    engineCommand: cwlNodeEngine.js
  - class: DockerRequirement
    dockerPull: quay.io/pancancer/pcawg-sanger-cgp-workflow:2.0.0

inputs:
  - id: "#tumor"
    type: File
    inputBinding:
      position: 1
      prefix: "--tumor"
    secondaryFiles:
      - .bai 

  - id: "#normal"
    type: File
    inputBinding:
      position: 2
      prefix: "--normal"
    secondaryFiles:
      - .bai 

  - id: "#refFrom"
    type: File
    inputBinding:
      position: 3
      prefix: "--refFrom"

  - id: "#bbFrom"
    type: File
    inputBinding:
      position: 4
      prefix: "--bbFrom"

outputs:
  - id: "#somatic_snv_mnv_tar_gz"
    type: File
    outputBinding:
      glob: "*.somatic.snv_mnv.tar.gz"
  - id: "#somatic_cnv_tar_gz"
    type: File
    outputBinding:
      glob: "*.somatic.cnv.tar.gz"
  - id: "#somatic_sv_tar_gz"
    type: File
    outputBinding:
      glob: "*.somatic.sv.tar.gz"
  - id: "#somatic_indel_tar_gz"
    type: File
    outputBinding:
      glob: "*.somatic.indel.tar.gz"
  - id: "#somatic_imputeCounts_tar_gz"
    type: File
    outputBinding:
      glob: "*.somatic.imputeCounts.tar.gz"
  - id: "#somatic_genotype_tar_gz"
    type: File
    outputBinding:
      glob: "*.somatic.genotype.tar.gz"
  - id: "#somatic_verifyBamId_tar_gz"
    type: File
    outputBinding:
      glob: "*.somatic.verifyBamId.tar.gz"

baseCommand: ["python", "/home/seqware/CGP-Somatic-Docker/scripts/run_seqware_workflow.py"]
