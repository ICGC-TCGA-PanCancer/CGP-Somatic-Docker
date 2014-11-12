#!/usr/bin/perl
#
# @File getTbi.pl
# @Author kr2
# @Created 12-Nov-2014 12:52:49
#

use strict;
use autodie qw(:all);
use Capture::Tiny qw(capture);

my $fai = shift @ARGV;
my $tbxSrv = shift @ARGV;

my ($stdout, $stderr, $exit) = capture { system(qq{wc -l $fai}) };
die "Error occurred while capturing content of $folder/start" if($stderr);
chomp $stdout;
my ($max) = $stdout=~ m/^([[:digit:]]+)/;

my $wget_base = $tbxSrv.'unmatchedNormal.%s.vcf.gz.tbi';

for(my $i=1; $i<=$max; $i++) {
  my $command = sprintf $wget_base, $i;
  system("wget $command");
}
