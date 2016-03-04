FROM pancancer/seqware_whitestar_pancancer:1.1.2

ENV SANGER_VERSION 1.0.8
LABEL SANGER_VERSION $SANGER_VERSION

USER root

### START of CGP INSTALL ###
#
ENV OPT /opt/wtsi-cgp
ENV PATH $OPT/bin:$PATH
ENV PERL5LIB $OPT/lib/perl5:$PERL5LIB

RUN apt-get -yqq update && \
    apt-get -yqq install libreadline6-dev build-essential autoconf software-properties-common python-software-properties \
      wget time curl zlib1g-dev libncurses5-dev \
      libgd2-xpm-dev libexpat1-dev python unzip libboost-dev libboost-iostreams-dev \
      libpstreams-dev libglib2.0-dev gfortran libcairo2-dev cpanminus libwww-perl \
      openjdk-7-jdk && \
    apt-get clean

RUN mkdir -p /tmp/downloads $OPT/bin $OPT/etc $OPT/lib $OPT/share
WORKDIR /tmp/downloads

RUN cpanm --mirror http://cpan.metacpan.org -l $OPT File::ShareDir File::ShareDir::Install Bio::Root::Version Const::Fast Graph && \
     rm -rf ~/.cpanm

RUN export SOURCE_JKENT_BIN=https://github.com/ENCODE-DCC/kentUtils/raw/master/bin/linux.x86_64 && \
    curl -sSL -o $OPT/bin/wigToBigWig -C - --retry 10 ${SOURCE_JKENT_BIN}/wigToBigWig && chmod +x $OPT/bin/wigToBigWig && \
    curl -sSL -o $OPT/bin/bigWigMerge -C - --retry 10 ${SOURCE_JKENT_BIN}/bigWigMerge && chmod +x $OPT/bin/bigWigMerge

#BWA
RUN curl -sSL -o tmp.tar.gz --retry 10 https://github.com/lh3/bwa/archive/0.7.12.tar.gz && \
    tar --strip-components 1 -zxf tmp.tar.gz && \
    make  && \
    cp bwa $OPT/bin/. && \
    rm -rf *

#BIOBAMBAM
RUN curl -sSL -o tmp.tar.gz --retry 10 https://github.com/gt1/biobambam2/releases/download/2.0.25-release-20151105154334/biobambam2-2.0.25-release-20151105154334-x86_64-etch-linux-gnu.tar.gz && \
    tar --strip-components 1 -zxf tmp.tar.gz && \
    rm -f bin/curl && \
    cp -r bin/* $OPT/bin/. && \
    cp -r etc/* $OPT/etc/. && \
    cp -r lib/* $OPT/lib/. && \
    cp -r share/* $OPT/share/. && \
    rm -rf *

# htslib - used multiple times later
RUN curl -sSL -o tmp.tar.gz --retry 10 https://github.com/samtools/htslib/archive/1.2.1.tar.gz && \
    mkdir /tmp/downloads/htslib && \
    tar -C /tmp/downloads/htslib --strip-components 1 -zxf tmp.tar.gz && \
    make -C /tmp/downloads/htslib && \
    rm -f /tmp/downloads/tmp.tar.gz

ENV HTSLIB /tmp/downloads/htslib

# legacy samtools
RUN curl -sSL -o tmp.tar.gz --retry 10 https://github.com/samtools/samtools/archive/0.1.20.tar.gz && \
    mkdir /tmp/downloads/samtools && \
    tar -C /tmp/downloads/samtools --strip-components 1 -zxf tmp.tar.gz && \
    perl -i -pe 's/^CFLAGS=\s*/CFLAGS=-fPIC / unless /\b-fPIC\b/' samtools/Makefile && \
    make -C samtools && \
    cp samtools/samtools $OPT/bin/. && \
    export SAMTOOLS=/tmp/downloads/samtools && \
    cpanm --mirror http://cpan.metacpan.org -l $OPT Bio::DB::Sam && \
    rm -rf /tmp/downloads/samtools /tmp/downloads/tmp.tar.gz ~/.cpanm

# bam_stats + PCAP build
RUN curl -sSL -o tmp.tar.gz --retry 10 https://github.com/ICGC-TCGA-PanCancer/PCAP-core/archive/v1.13.1.tar.gz && \
    mkdir /tmp/downloads/PCAP && \
    tar -C /tmp/downloads/PCAP --strip-components 1 -zxf tmp.tar.gz && \
    make -C /tmp/downloads/PCAP/c && \
    cp /tmp/downloads/PCAP/bin/bam_stats $OPT/bin/. && \
    make -C /tmp/downloads/PCAP/c clean && \
    cd /tmp/downloads/PCAP && \
    cpanm --mirror http://cpan.metacpan.org -l $OPT . && \
    cd /tmp/downloads && \
    rm -rf /tmp/downloads/PCAP /tmp/downloads/tmp.tar.gz ~/.cpanm

RUN curl -sSL -o tmp.zip --retry 10 https://github.com/samtools/tabix/archive/master.zip && \
    unzip -q tmp.zip && \
    cd /tmp/downloads/tabix-master && \
    make && \
    cp tabix $OPT/bin/. && \
    cp bgzip $OPT/bin/. && \
    cd perl && \
    perl Makefile.PL INSTALL_BASE=$INST_PATH && \
    make && make test && make install && \
    cd /tmp/downloads && \
    rm -rf /tmp/downloads/tabix-master /tmp/downloads/tmp.zip

# start of cgpVcf block
# the commit UUID for the release of cgpVcf in use

ENV CGPVCF_UUID 5cc538ded838a4ba94feedff1b51ee3ebc4b65f4

# build vcftools using patch from cgpVcf release
RUN curl -sSL -o tmp.tar.gz --retry 10 http://sourceforge.net/projects/vcftools/files/vcftools_0.1.12a.tar.gz/download && \
    mkdir /tmp/downloads/vcftools && \
    tar -C /tmp/downloads/vcftools --strip-components 1 -zxf /tmp/downloads/tmp.tar.gz && \
    cd /tmp/downloads/vcftools && \
    curl -sSL -o vcfToolsInstLocs.diff --retry 10 https://raw.githubusercontent.com/cancerit/cgpVcf/$CGPVCF_UUID/patches/vcfToolsInstLocs.diff && \
    patch Makefile < vcfToolsInstLocs.diff && \
    curl -sSL -o vcfToolsProcessLog.diff --retry 10 https://raw.githubusercontent.com/cancerit/cgpVcf/$CGPVCF_UUID/patches/vcfToolsProcessLog.diff && \
    patch perl/Vcf.pm < vcfToolsProcessLog.diff && \
    make  PREFIX=$OPT && \
    cd /tmp/downloads && \
    rm -rf /tmp/downloads/vcftools /tmp/downloads/tmp.tar.gz

# bedtools, make sure it is a suitable version without the input switch
RUN curl -sSL -o tmp.tar.gz --retry 10 https://github.com/arq5x/bedtools2/releases/download/v2.21.0/bedtools-2.21.0.tar.gz && \
    mkdir /tmp/downloads/bedtools2 && \
    tar -C /tmp/downloads/bedtools2 --strip-components 1 -zxf tmp.tar.gz && \
    make  -C /tmp/downloads/bedtools2 && \
    cp /tmp/downloads/bedtools2/bin/* $OPT/bin/. && \
    rm -rf /tmp/downloads/bedtools2 /tmp/downloads/tmp.tar.gz

# cgpVcf
RUN curl -sSL -o tmp.tar.gz --retry 10 https://github.com/cancerit/cgpVcf/archive/v1.3.1.tar.gz && \
    mkdir /tmp/downloads/cgpVcf && \
    tar -C /tmp/downloads/cgpVcf --strip-components 1 -zxf tmp.tar.gz && \
    cd /tmp/downloads/cgpVcf && \
    cpanm --mirror http://cpan.metacpan.org -l $OPT . && \
    cd /tmp/downloads && \
    rm -rf /tmp/downloads/cgpVcf /tmp/downloads/tmp.tar.gz ~/.cpanm

# alleleCount - only want C version
RUN curl -sSL -o tmp.tar.gz --retry 10 https://github.com/cancerit/alleleCount/archive/v2.1.2.tar.gz && \
    mkdir /tmp/downloads/alleleCount && \
    tar -C /tmp/downloads/alleleCount --strip-components 1 -zxf tmp.tar.gz && \
    cd /tmp/downloads/alleleCount/c && \
    mkdir bin && \
    make && \
    cp /tmp/downloads/alleleCount/c/bin/alleleCounter $OPT/bin/. && \
    cd /tmp/downloads && \
    rm -rf /tmp/downloads/alleleCount /tmp/downloads/tmp.tar.gz

# verifyBamId
RUN curl -sSL -o $OPT/bin/verifyBamId --retry 10 https://github.com/statgen/verifyBamID/releases/download/v1.1.2/verifyBamID.1.1.2 && \
    chmod +x $OPT/bin/verifyBamId && \
    rm -f /tmp/downloads/verifyBamId

# cgpNgsQc
RUN curl -sSL -o tmp.tar.gz --retry 10 https://github.com/cancerit/cgpNgsQc/archive/v1.1.0.tar.gz && \
    mkdir /tmp/downloads/cgpNgsQc && \
    tar -C /tmp/downloads/cgpNgsQc --strip-components 1 -zxf tmp.tar.gz && \
    cd /tmp/downloads/cgpNgsQc && \
    cpanm --mirror http://cpan.metacpan.org -l $OPT . && \
    cd /tmp/downloads && \
    rm -rf /tmp/downloads/cgpNgsQc /tmp/downloads/tmp.tar.gz ~/.cpanm

# ascatNgs
RUN curl -sSL -o tmp.tar.gz --retry 10 https://github.com/cancerit/ascatNgs/archive/v1.6.0.tar.gz && \
    mkdir /tmp/downloads/ascatNgs && \
    tar -C /tmp/downloads/ascatNgs --strip-components 1 -zxf tmp.tar.gz && \
    cd /tmp/downloads/ascatNgs/perl && \
    cpanm --mirror http://cpan.metacpan.org -l $OPT . && \
    cd /tmp/downloads && \
    rm -rf /tmp/downloads/ascatNgs /tmp/downloads/tmp.tar.gz ~/.cpanm

# cgpPindel
RUN curl -sSL -o tmp.tar.gz --retry 10 https://github.com/cancerit/cgpPindel/archive/v1.5.5.tar.gz && \
    mkdir /tmp/downloads/cgpPindel && \
    tar -C /tmp/downloads/cgpPindel --strip-components 1 -zxf tmp.tar.gz && \
    cd /tmp/downloads/cgpPindel && \
    g++ -O3 -o $OPT/bin/pindel c++/pindel.cpp && \
    g++ -O3 -o $OPT/bin/filter_pindel_reads c++/filter_pindel_reads.cpp && \
    cd /tmp/downloads/cgpPindel/perl && \
    cpanm --mirror http://cpan.metacpan.org -l $OPT . && \
    cd /tmp/downloads && \
    rm -rf /tmp/downloads/cgpPindel /tmp/downloads/tmp.tar.gz ~/.cpanm

# cgpCaVEManPostProcessing
RUN curl -sSL -o tmp.tar.gz --retry 10 https://github.com/cancerit/cgpCaVEManPostProcessing/archive/1.5.3.tar.gz && \
    mkdir /tmp/downloads/cgpCaVEManPostProcessing && \
    tar -C /tmp/downloads/cgpCaVEManPostProcessing --strip-components 1 -zxf tmp.tar.gz && \
    cd /tmp/downloads/cgpCaVEManPostProcessing && \
    cpanm --mirror http://cpan.metacpan.org -l $OPT . && \
    cd /tmp/downloads && \
    rm -rf /tmp/downloads/cgpCaVEManPostProcessing /tmp/downloads/tmp.tar.gz ~/.cpanm

# CaVEMan
RUN curl -sSL -o tmp.tar.gz --retry 10 https://github.com/cancerit/CaVEMan/archive/1.9.1.tar.gz && \
    mkdir /tmp/downloads/CaVEMan && \
    tar -C /tmp/downloads/CaVEMan --strip-components 1 -zxf tmp.tar.gz && \
    cd /tmp/downloads/CaVEMan && \
    make && \
    cp /tmp/downloads/CaVEMan/bin/caveman $OPT/bin/. && \
    cp /tmp/downloads/CaVEMan/bin/mergeCavemanResults $OPT/bin/. && \
    cd /tmp/downloads && \
    rm -rf /tmp/downloads/CaVEMan /tmp/downloads/tmp.tar.gz ~/.cpanm ~/.cache/hts-ref


# cgpCaVEManWrapper
RUN curl -sSL -o tmp.tar.gz --retry 10 https://github.com/cancerit/cgpCaVEManWrapper/archive/1.9.2.tar.gz && \
    mkdir /tmp/downloads/cgpCaVEManWrapper && \
    tar -C /tmp/downloads/cgpCaVEManWrapper --strip-components 1 -zxf tmp.tar.gz && \
    cd /tmp/downloads/cgpCaVEManWrapper && \
    cpanm --mirror http://cpan.metacpan.org -l $OPT . &&\
    cd /tmp/downloads && \
    rm -rf /tmp/downloads/cgpCaVEManWrapper /tmp/downloads/tmp.tar.gz ~/.cpanm

# VAGrENT
RUN curl -sSL -o tmp.tar.gz --retry 10 https://github.com/cancerit/VAGrENT/archive/v2.1.2.tar.gz && \
    mkdir /tmp/downloads/VAGrENT && \
    tar -C /tmp/downloads/VAGrENT --strip-components 1 -zxf tmp.tar.gz && \
    cd /tmp/downloads/VAGrENT && \
    cpanm --mirror http://cpan.metacpan.org -l $OPT . &&\
    cd /tmp/downloads && \
    rm -rf /tmp/downloads/VAGrENT /tmp/downloads/tmp.tar.gz /tmp/downloads/*.tmp.bioperl ~/.cpanm

# grass
RUN curl -sSL -o tmp.tar.gz --retry 10 https://github.com/cancerit/grass/archive/v1.1.6.tar.gz && \
    mkdir /tmp/downloads/grass && \
    tar -C /tmp/downloads/grass --strip-components 1 -zxf tmp.tar.gz && \
    cd /tmp/downloads/grass && \
    cpanm --mirror http://cpan.metacpan.org -l $OPT . &&\
    cd /tmp/downloads && \
    rm -rf /tmp/downloads/grass /tmp/downloads/tmp.tar.gz ~/.cpanm


# BRASS
# blat first
RUN curl -sSL -o $OPT/bin/blat --retry 10 http://hgdownload.cse.ucsc.edu/admin/exe/linux.x86_64/blat/blat && \
    chmod ugo+x $OPT/bin/blat

# pre-compiled exonerate
RUN curl -sSL http://ftp.ebi.ac.uk/pub/software/vertebrategenomics/exonerate/exonerate-2.2.0-x86_64.tar.gz | \
    tar -C $OPT/bin --strip-components=2 -zx exonerate-2.2.0-x86_64/bin/exonerate && \
    chmod ugo+x $OPT/bin/exonerate

# perl mod Graph installed at top of file due to being required in Bio/Brass.pm
RUN curl -sSL -o tmp.tar.gz --retry 10 https://github.com/cancerit/BRASS/archive/v4.0.14.tar.gz && \
    mkdir /tmp/downloads/BRASS && \
    tar -C /tmp/downloads/BRASS --strip-components 1 -zxf tmp.tar.gz && \
    cd /tmp/downloads/BRASS && \
    rm -rf cansam* && \
    unzip -q distros/cansam.zip && \
    mv cansam-master cansam && \
    make -C cansam && \
    make -C c++ && \
    cp c++/augment-bam $OPT/bin/. && \
    cp c++/brass-group $OPT/bin/. && \
    cp c++/filterout-bam $OPT/bin/. && \
    tar zxf distros/velvet_1.2.10.tgz && \
    cd velvet_1.2.10 && \
    make MAXKMERLENGTH=95 velveth velvetg && \
    mv velveth $OPT/bin/velvet95h && \
    mv velvetg $OPT/bin/velvet95g && \
    make  clean && \
    make velveth velvetg && \
    mv velveth $OPT/bin/velvet31h && \
    mv velvetg $OPT/bin/velvet31g && \
    ln -fs $OPT/bin/velvet95h $OPT/bin/velveth && \
    ln -fs $OPT/bin/velvet95g $OPT/bin/velvetg && \
    cd /tmp/downloads/BRASS && \
    cd /tmp/downloads/BRASS/perl && \
    cpanm --mirror http://cpan.metacpan.org -l $OPT . && \
    cd /tmp/downloads && \
    rm -rf /tmp/downloads/BRASS /tmp/downloads/tmp.tar.gz ~/.cpanm

# build the R bits
RUN curl -sSL -o tmp.tar.gz --retry 10 http://ftp.heanet.ie/mirrors/cran.r-project.org/src/base/R-3/R-3.1.3.tar.gz && \
    mkdir /tmp/downloads/R-build && \
    tar -C /tmp/downloads/R-build --strip-components 1 -zxf tmp.tar.gz && \
    cd /tmp/downloads/R-build && \
    ./configure --with-cairo=yes --prefix=$OPT && \
    make && \
    make check && \
    make install && \
    cd /tmp/downloads && \
    rm -rf /tmp/downloads/R-build /tmp/downloads/tmp.tar.gz

RUN echo '(".Rprofile: Setting UK repository")\n\
r = getOption("repos") # hard code the UK repo for CRAN\n\
r["CRAN"] = "http://cran.uk.r-project.org"\n\
options(repos = r)\n\
rm(r)\n\
source("http://bioconductor.org/biocLite.R")\n\
biocLite("gam", ask=FALSE)\n\
biocLite("VGAM", ask=FALSE)\n\
biocLite("stringr", ask=FALSE)\n\
biocLite("BiocGenerics", ask=FALSE)\n\
biocLite("poweRlaw", ask=FALSE)\n\
biocLite("S4Vectors", ask=FALSE)\n\
biocLite("IRanges", ask=FALSE)\n\
biocLite("GenomeInfoDb", ask=FALSE)\n\
biocLite("zlibbioc", ask=FALSE)\n\
biocLite("XVector", ask=FALSE)\n\
biocLite("RColorBrewer", ask=FALSE)\n\
biocLite("GenomicRanges", ask=FALSE)\n\
biocLite("copynumber", ask=FALSE)' > tmp.R && \
    Rscript tmp.R && \
    rm tmp.R

# Add ssearch36 BRASS dep
RUN   curl -sSL -o tmp.tar.gz --retry 10 https://github.com/wrpearson/fasta36/releases/download/v36.3.8/fasta-36.3.8-linux64.tar.gz && \
      mkdir  /tmp/downloads/fasta && \
      tar -C /tmp/downloads/fasta --strip-components 2 -zxf tmp.tar.gz && \
      cp /tmp/downloads/fasta/bin/ssearch36 $OPT/bin/. && \
      rm -rf /tmp/downloads/fasta

#
### END of CGP INSTALL ###

COPY ./src					/home/seqware/CGP-Somatic-Docker/src
COPY ./workflow				/home/seqware/CGP-Somatic-Docker/workflow
COPY ./scripts				/home/seqware/CGP-Somatic-Docker/scripts
COPY ./pom.xml				/home/seqware/CGP-Somatic-Docker/pom.xml
COPY ./workflow.properties	/home/seqware/CGP-Somatic-Docker/workflow.properties

RUN chown -R seqware /home/seqware/CGP-Somatic-Docker

USER seqware

RUN     echo "options(bitmapType='cairo')" > /home/seqware/.Rprofile

WORKDIR /home/seqware/CGP-Somatic-Docker

# default entry will run test data
ENTRYPOINT /home/seqware/CGP-Somatic-Docker/scripts/run_sanger.sh
