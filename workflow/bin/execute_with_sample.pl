#!/usr/bin/perl

use strict;
use warnings FATAL => qw(all);
use autodie qw(:all);

use Bio::DB::Sam;

if(scalar @ARGV < 2) {
  die "USAGE: ./execute_with_sample.pl getSample.bam ..yourcommand..%SM%...\n";
}

my $sample = sample_name(shift @ARGV);

my $command = join q{ }, @ARGV;
die "There was nothing to replace in the provided command, expecting to find %SM%\n" unless($command =~ s/%SM%/$sample/g);
warn "Modified command: $command\n";
exec($command);

sub sample_name {
  my $bam = shift;
  my @lines = split /\n/, Bio::DB::Sam->new(-bam => $bam)->header->text;
  my $sample;
  for(@lines) {
    if($_ =~ m/^\@RG.*\tSM:([^\t]+)/) {
      $sample = $1;
      last;
    }
  }
  die "Failed to determine sample from BAM header\n" unless(defined $sample);
  return $sample;
}
