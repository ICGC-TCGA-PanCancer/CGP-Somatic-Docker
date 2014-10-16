SeqWare-CGP-SomaticCore
=======================

Seqware workflow for Cancer Genome Project core somatic calling pipeline


## Additional packages for base instance

sudo apt-get install g++
sudo apt-get install pkg-config
sudo apt-get install libncurses5-dev
sudo apt-get install libgd2-xpm-dev
sudo apt-get install libboost-all-dev
sudo apt-get install libpstreams-dev
sudo apt-get install libglib2.0-dev
sudo apt-get install r-base
sudo apt-get install r-cran-rcolorbrewer

## Host currently needs reconf for SGE

sudo qconf -aattr queue slots "[master=`nproc`]" main.q
sudo qconf -mattr queue load_thresholds "np_load_avg=`nproc`" main.q
sudo qconf -rattr exechost complex_values h_vmem=`free -b|grep Mem | cut -d" " -f5` master