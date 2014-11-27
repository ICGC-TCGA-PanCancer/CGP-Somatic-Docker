#!/bin/bash

set -ue

i=0
command=''
#output=''
for var in "$@"
do
  if [[ $i -eq 0 ]]
    then
    export PATH="$var/bin:$PATH"
    export PERL5LIB="$var/lib/perl5"
  else
    command="$command $var"
  fi
  i=$i+1
done

$command
