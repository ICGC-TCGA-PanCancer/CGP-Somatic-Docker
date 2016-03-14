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
  - id: "#tumor"
    type:
      type: File
    inputBinding:
      position: 1
      prefix: "--tumor"
    secondaryFiles:
      - .bai 

  - id: "#normal"
    type:
      type: File
    inputBinding:
      position: 2
      prefix: "--normal"
    secondaryFiles:
      - .bai 

  - id: "#refFrom"
    type:
      type: File
    inputBinding:
      position: 3
      prefix: "--refFrom"

  - id: "#bbFrom"
    type:
      type: File
    inputBinding:
      position: 4
      prefix: "--bbFrom"

outputs:
  - id: "#vcf"
    type: array
    items: File
    outputBinding:
      glob: ["*.vcf"]

baseCommand: ["python", "/home/seqware/CGP-Somatic-Docker/scripts/run_seqware_workflow.py"]
