#next

* Modified cgpPindel_pindel to use threaded approach like caveman\[em\]step.

# 1.0.3

* using Adam's wrapper (modified) to monitor gtdownload/gtupload

# 1.0.2

* creating additional config files with different memory profiles
* Artifact addition:
    * cgpNgsQc - genotype and contamination analysis
* Artifact updated to fix the following:
    * BRASS - prevent memory issues cause by high-depth regions.
    * BRASS - Fix issue due to absence of insert size in BAM headers - use \*.bas instead.
    * PCAP-core - xml_to_bas.pl, resolve read_group_id clashes via platform_unit where possible.

# 1.0.1

* Modified CaVEMan_mstep, CaVEMan_estep and BRASS_assemble steps to use pooled cpu/memory to reduce SeqWare per-process overhead.

# 1.0.0
