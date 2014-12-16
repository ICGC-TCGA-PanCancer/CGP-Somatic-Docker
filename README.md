# SeqWare-CGP-SomaticCore Workflow

A Seqware workflow derived from Sanger's Cancer Genome Project core somatic calling pipeline. This is used by the ICGC/TCGA PanCancer project. See http://pancancer.info for more information. This workflow has been designed to work with [SeqWare 1.1.0](http://seqware.io) VMs setup by [Bindle 2.0](https://github.com/CloudBindle/Bindle).

## Authors

Email Brian if you have questions.

* Keiran Raine
* Brian O'Connor <boconnor@oicr.on.ca>

## Environment Requirements

This workflow assumes you are working on a VM (running on Amazon, in VirtualBox, etc) that is running SeqWare 1.1.0.  You can find pre-fab AMIs and OVA files on our project site http://seqware.io but 1.1.0 is generally bleeding edge and we may not have posted VMs yet.  If that's the case, please take a look at our internal documentation from the PanCancer staff which describe how to build an up-to-date SeqWare 1.1.0 VM using Bindle 2.0, our provisioner.  See: https://github.com/ICGC-TCGA-PanCancer/pancancer-info/tree/develop/docs

Again, this is a bit complex but in the future we will distributed pre-created VMs as OVA files or AMIs that should make this process go much more smoothly.

### Hardware Requirements

This workflow recommends:

* 16-32 cores (make sure you set the coresAddressable variable correctly in the ini file)
* 4.5G per core, so, ideally 72GB+ for 16 cores, for 32 cores 144GB+, on Amazon we recommend r3.8xlarge or r3.4xlarge
* 1TB of local disk space

### Other Requirements

This workflow requires an HTTP server to serve tabix-indexed data files, the location is controlled by the tabixSrvUri variable in the ini file

## Additional Packages for Base Instance

The directions above at our PanCancer SOP docs site will build a base SeqWare 1.1.0 box.  Over time we wil add these packages to the base configuration but for now you will want to install the following on your SeqWare 1.1.0 VM once Bindle 2.0 has created it.

The following are the packages needed for Ubuntu 12.04:

    # added due to some issues during setup of the new HVM host
    sudo apt-get autoclean
    sudo apt-get update
    sudo apt-get upgrade
    sudo apt-get install g++ pkg-config dh-autoreconf
    sudo apt-get install libncurses5-dev
    sudo apt-get install libgd2-xpm-dev
    sudo apt-get install libboost-all-dev
    sudo apt-get install libpstreams-dev
    sudo apt-get install libglib2.0-dev
    sudo apt-get install zlib1g-dev
    # for the vcf-uploader
    sudo apt-get install libxml-dom-perl libxml-xpath-perl libjson-perl libxml-libxml-perl

These packages are needed for execution, **even when pulling from Artifactory**:

    sudo apt-get install r-base r-base-core r-cran-rcolorbrewer

All of the above on one line to make it easy to cut and paste into a terminal:

    sudo apt-get autoclean; sudo apt-get update; sudo apt-get -y upgrade; sudo apt-get -y install libxml-dom-perl libxml-xpath-perl libjson-perl libxml-libxml-perl zlib1g-dev libglib2.0-dev libpstreams-dev libboost-all-dev libgd2-xpm-dev libncurses5-dev g++ pkg-config dh-autoreconf r-base r-base-core r-cran-rcolorbrewer

Note: on BioNimbus I ran into an issue with r-cran-rcolorbrewer not being up to date with R 3.x.  See http://stackoverflow.com/questions/16503554/r-3-0-0-update-has-left-loads-of-2-x-packages-incompatible

## Tabix Server

You need a tabix HTTP server serving up controlled access data that's used in the variant calling pipeline.  We can't share this publicaly so email the authors for information about this component. 

## Building the Workflow

Now that you have a VM built and system-level dependencies installed it's time to build the workflow.  Clone the project in git and cd into SeqWare-CGP-SomaticCore.  Now build with Maven:

    mvn clean install

You should see the output in target, the Workflow\_Bundle\_CgpCnIndelSnvStr\_1.0-SNAPSHOT\_SeqWare\_1.1.0-alpha.5 directory.

Assuming you have all the SeqWare and associated components installed correctly on your VM (you should) you would then test execute with:

    seqware bundle launch --dir target/Workflow\_Bundle\_CgpCnIndelSnvStr\_1.0-SNAPSHOT\_SeqWare\_1.1.0-alpha.5

That will run the integrated tests.

You can manually run other tests using the --ini parameter to pass custom ini files with your test settings in them.  In a production system, these ini files would be created by a "decider" that schedules workflows to many VMs in parallel but that is beyond the scope of this doc.  See the PanCancer SOP docs for more info: https://github.com/ICGC-TCGA-PanCancer/pancancer-info/tree/develop/docs

NOTE: to download "real" data from a GNOS repository you need to provide a valid GNOS .pem key file, see the ini in the bundle for more information.

## Running the Workflow

Once you build this on a SeqWare VM the next thing you'll want to do is actually run some test data.  The workflow can operate in two modes.  First, in local test mode and, second, in online mode where large data is downloaded from GNOS.  The latter takes about 30 hours to run whereas the former takes about 1.5 on 8 cores.  Take a look at CgpCnIndelSnvStrWorkflow.ini and make decisions about the following variables:

    # number of cores to use, you want 4.5G/core or more
    coresAddressable=32
    # the server that has various tabix-indexed files on it, see above, update with your URL
    tabixSrvUri=http://10.89.9.50/
    # in non local test mode make sure this path is valid
    pemFile=/mnt/seqware-build/ebi.pem
    # not "true" means the data will be downloaded using AliquotIDs
    testMode=false
    # uncomment if the above is "true"
    #tumourBamT=/mnt/testData/PD7303/PD7303a_ds.bam
    #controlBamT=/mnt/testData/PD7303/PD7303b_ds.bam
    # these are residing on the EBI GNOS repo
    tumourAliquotIds=f393bb07-270c-2c93-e040-11ac0d484533
    tumourAnalysisIds=ef26d046-e88a-4f21-a232-16ccb43637f2
    tumourBams=7723a85b59ebce340fe43fc1df504b35.bam
    controlAnalysisId=1b9215ab-3634-4108-9db7-7e63139ef7e9
    controlBam=8f957ddae66343269cb9b854c02eee2f.bam

Once you've modified (or copied and modified) the .ini file you can launch the workflow with something like:

    seqware bundle launch --dir <path>/target/Workflow_Bundle_CgpCnIndelSnvStr_1.0.0_SeqWare_1.1.0-alpha.5 --ini <path_to_modified_ini>

This will launch the workflow and print out some key debugging information that will help you if things go wrong.

## Building CGP Workflow Dependencies - Optional

The workflow build process pulls the various binary dependencies hosted on our Artifactory server.  If, for some reason, you need to rebuild these follow this process. If you're using x86\_64 on Ubuntu 12.04 then you should be fine, this is just provided if you need to start from scratch.

You need to build and install the following in this order:

* [PCAP v1.2.3](https://github.com/ICGC-TCGA-PanCancer/PCAP-core/archive/v1.2.3.tar.gz)
* [cgpBinCounts v1.0.0](https://github.com/cancerit/cgpBinCounts/archive/v1.0.0.tar.gz)
* [cgpNgsQc v1.0.0](https://github.com/cancerit/cgpNgsQc/archive/v1.0.0.tar.gz)
* [cgpVcf v1.2.2](https://github.com/cancerit/cgpVcf/archive/v1.2.2.tar.gz)
* [alleleCount v1.2.1](https://github.com/cancerit/alleleCount/archive/v1.2.1.tar.gz)
* [ascatNgs v1.5.0](https://github.com/cancerit/ascatNgs/archive/v1.5.0.tar.gz)
* [cgpPindel v1.1.2](https://github.com/cancerit/cgpPindel/archive/v1.1.2.tar.gz)
* [cgpCaVEManPostProcessing v1.0.2](https://github.com/cancerit/cgpCaVEManPostProcessing/archive/v1.0.2.tar.gz)
* [cgpCaVEManWrapper v1.2.0](https://github.com/cancerit/cgpCaVEManWrapper/archive/v1.2.0.tar.gz)
* [BRASS v2.1.0](https://github.com/cancerit/BRASS/archive/v2.1.0.tar.gz)
* [VAGrENT v2.0.0](https://github.com/cancerit/VAGrENT/archive/v2.0.0.tar.gz)
* [grass v1.0.1](https://github.com/cancerit/grass/archive/v1.0.1.tar.gz)

There is a script included in this package that automates this process, just run:

    workflow/bin/setup.sh

This creates ``workflow/bin/opt`` which should then be packaged (``tar.gz``) and deployed to Artifactory.

If you find there is a problem following upgrade please reference this script.

## Host currently needs reconf for SGE - Optional

If you launch our VM on a machine with more or fewer processors or memory you can tell SGE to use it using the following commands:

    sudo qconf -aattr queue slots "[master=`nproc`]" main.q
    sudo qconf -mattr queue load_thresholds "np_load_avg=`nproc`" main.q
    sudo qconf -rattr exechost complex_values h_vmem=`free -b|grep Mem | cut -d" " -f5` master

You shouldn't really need to change these but if you're changing instance types/flavors then you might.

## Add following to ~seqware/.bash\_profile - Optional

This is fixed on the HVM AMI but if you want to call oozie commands directly then you might need to define the follow env var:

    export OOZIE_URL=http://master:11000/oozie


