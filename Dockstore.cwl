#!/usr/bin/env cwl-runner

class: CommandLineTool

description: |
   Sanger placeholder text.

dct:creator:
  "@id": "http://sanger.ac.uk/...
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
    dockerPull: quay.io/TBD

inputs:
  - id: "#refdata_1"
    type:
      type: array
      items: File
    inputBinding:
      position: 1
      prefix: "--file"

  - id: "#refdata_2"
    type:
      type: array
      items: File
    inputBinding:
      position: 1
      prefix: "--file"

  - id: "#refdata_3"
    type:
      type: array
      items: File
    inputBinding:
      position: 1
      prefix: "--file"

  - id: "#reads"
    type:
      type: array
      items: File
    inputBinding:
      position: 1
      prefix: "--file"

outputs:
  - id: "#vcf"
    type: array
    items: File
    outputBinding:
      glob: ["*.vcf"]

baseCommand: ["perl", "/home/seqware/CGP-Somatic-Docker/scripts/run_workflow.sh"]
