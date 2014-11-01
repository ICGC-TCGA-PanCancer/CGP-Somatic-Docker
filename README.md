SeqWare-CGP-SomaticCore
=======================

Seqware workflow for Cancer Genome Project core somatic calling pipeline

## Additional packages for base instance

The following are the packages needed for Ubuntu 12.04:

    sudo apt-get install g++
    sudo apt-get install pkg-config
    sudo apt-get install libncurses5-dev
    sudo apt-get install libgd2-xpm-dev
    sudo apt-get install libboost-all-dev
    sudo apt-get install libpstreams-dev
    sudo apt-get install libglib2.0-dev
    sudo apt-get install r-base
    sudo apt-get install r-base-core
    sudo apt-get install r-cran-rcolorbrewer
    sudo apt-get install dh-autoreconf
    sudo apt-get install zlib1g-dev
    sudo apt-get install zlib1g-dev 
    sudo apt-get install libncurses5-dev
    sudo apt-get install libgd2-xpm-dev 
    sudo apt-get install r-cran-rcolorbrewer
    # for the vcf-uploader
    sudo apt-get install libxml-dom-perl libxml-xpath-perl libjson-perl libxml-libxml-perl

## Building CGP codebase

You need to build and install the following in this order:

* [PCAP v1.2.2](https://github.com/ICGC-TCGA-PanCancer/PCAP-core/archive/v1.2.2.tar.gz)
* [cgpVcf v1.2.2](https://github.com/cancerit/cgpVcf/archive/v1.2.2.tar.gz)
* [alleleCount v1.2.1](https://github.com/cancerit/alleleCount/archive/v1.2.1.tar.gz)
* [ascatNgs v1.4.1](https://github.com/cancerit/ascatNgs/archive/v1.4.1.tar.gz)
* [cgpPindel v1.1.1](https://github.com/cancerit/cgpPindel/archive/v1.1.1.tar.gz)
* [cgpCaVEManWrapper v1.2.0](https://github.com/cancerit/cgpCaVEManWrapper/archive/v1.2.0.tar.gz)
* [cgpCaVEManPostProcessing v1.0.2](https://github.com/cancerit/cgpCaVEManPostProcessing/archive/v1.0.2.tar.gz)
* [BRASS v2.0.0](https://github.com/cancerit/BRASS/archive/v2.0.0.tar.gz)
* [VAGrENT v2.0.0](https://github.com/cancerit/VAGrENT/archive/v2.0.0.tar.gz)
* [grass v1.0.1](https://github.com/cancerit/grass/archive/v1.0.1.tar.gz)

All of these packages have the same installation method.  For installation within this workflow:

    wget <package>
    tar zxf vX.X.X.tar.gz
    cd <package>
    ./setup.pl ../SeqWare-CGP-SomaticCore/workflow/bin/opt

Note, PCAP uses a "setup.sh" and not "setup.pl".

Note, the workflow references velveth but it compiles/installs as velvet95h. You need to do something similar to:

    seqware@master:/mnt/SeqWare-CGP-SomaticCore/workflow/bin/opt/bin$ cp velvet95g velvetg
    seqware@master:/mnt/SeqWare-CGP-SomaticCore/workflow/bin/opt/bin$ cp velvet95h velveth

Once brass is publicly available I will add a script to automate this fully.

## Host currently needs reconf for SGE

    sudo qconf -aattr queue slots "[master=`nproc`]" main.q
    sudo qconf -mattr queue load_thresholds "np_load_avg=`nproc`" main.q
    sudo qconf -rattr exechost complex_values h_vmem=`free -b|grep Mem | cut -d" " -f5` master

## Add following to ~seqware/.bash_profile

    export OOZIE_URL=http://master:11000/oozie
