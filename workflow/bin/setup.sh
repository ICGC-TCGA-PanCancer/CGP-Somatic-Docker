#!/bin/bash

done_message () {
  set +e
    if [ $? -eq 0 ]; then
        echo " done."
        if [ "x$1" != "x" ]; then
            echo $1
        fi
    else
        echo " failed.  See setup.log file for error messages." $2
        echo "    Please check INSTALL file for items that should be installed by a package manager"
        exit 1
    fi
  set -e
}

get_distro () {
  set +e
  if hash curl 2>/dev/null; then
    curl -sS -o current.tar.gz -L $1
  else
    wget -nv -O current.tar.gz $1
  fi
  set -e
  mkdir -p current
  tar --strip-components 1 -C current -zxf current.tar.gz
}

repos=(
  "https://github.com/ICGC-TCGA-PanCancer/PCAP-core/archive/v1.2.3.tar.gz"
  "https://github.com/cancerit/cgpVcf/archive/v1.2.2.tar.gz"
  "https://github.com/cancerit/alleleCount/archive/v1.2.1.tar.gz"
  "https://github.com/cancerit/cgpBinCounts/archive/v1.0.0.tar.gz"
  "https://github.com/cancerit/cgpNgsQc/archive/v1.0.2.tar.gz"
  "https://github.com/cancerit/ascatNgs/archive/v1.5.1.tar.gz"
  "https://github.com/cancerit/cgpPindel/archive/v1.2.0.tar.gz"
  "https://github.com/cancerit/cgpCaVEManPostProcessing/archive/v1.0.2.tar.gz"
  "https://github.com/cancerit/cgpCaVEManWrapper/archive/v1.2.0.tar.gz"
  "https://github.com/cancerit/BRASS/archive/v2.1.0.tar.gz"
  "https://github.com/cancerit/VAGrENT/archive/v2.0.0.tar.gz"
  "https://github.com/cancerit/grass/archive/v1.0.1.tar.gz"
 )

# clear log file
INIT_DIR=$(dirname $(readlink -f $0))
echo > $INIT_DIR/setup.log
set -eu

for i in "${repos[@]}" ; do
  echo -n "Installing $i..."
  (
    get_distro $i
    cd current
    ./setup.sh $INIT_DIR/opt
    cd $INIT_DIR
    rm -rf current current.tar.gz
    echo; echo
  ) >>$INIT_DIR/setup.log 2>&1
  done_message "" "Failed during installation of $i."
done

find $INIT_DIR/opt -type l -exec cp {} {}.tmp$$ \; -exec mv {}.tmp$$ {} \;
