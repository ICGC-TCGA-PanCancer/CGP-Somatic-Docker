#!/usr/bin/perl

use strict;
use warnings FATAL => qw(all);
use autodie qw(:all);

my $command = join q{ }, @ARGV;

system([13], $command);
