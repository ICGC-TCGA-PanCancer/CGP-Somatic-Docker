# README - Extras

Contains a simple script for generating ini files and submission commands based
on a TSV from the official PCAWG Data Freeze Train 2.0 Pilot 58.  This is just
a quick and dirty way to launch workflows on BioNimbus before the updated
decider is ready.

See https://docs.google.com/spreadsheets/d/1fM3j5eJB42iVIhTTqtcgddalUb8HJEY_CZ2x5bdPeCg/edit#gid=886811499

This tool is designed to work with the 1.0.1 version of the workflow.

## Dependencies

    sudo apt-get install libtemplate-perl

## Example

    # 1.0.0
    perl generate_ini.pl --tsv data/PCAWG_Data_Freeze_Train_2.0_Pilot-58.tsv --output-dir /glusterfs/netapp/homes1/BOCONNOR/workflow-dev/20141125b_test_runs --defaults config/settings.conf --template template/workflow_config.1.0.0.ini.tt 
    # 1.0.1
    perl generate_ini.pl --tsv data/PCAWG_Data_Freeze_Train_2.0_Pilot-58.tsv --output-dir /glusterfs/netapp/homes1/BOCONNOR/workflow-dev/20141202_test_runs --defaults config/settings.conf --template template/workflow_config.1.0.1.ini.tt

## TODO

* support tcga_pancancer_vcf_test and icgc_pancancer_vcf_test study names based on what repo samples go to
