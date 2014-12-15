#!/usr/bin/perl

use strict;
use warnings FATAL => qw(all);
use autodie qw(:all);

use File::Path qw(make_path);
use File::Copy;
use Capture::Tiny qw(capture);

use Bio::DB::Sam;

my $inbase = shift @ARGV;
my $control_bam = shift @ARGV;
my @tumour_bams = @ARGV;


my $cntl_sample = sample_name($control_bam);
my @tumour_samples;

for(@tumour_bams) {
  push @tumour_samples, sample_name($_);
}

my $cntl_path = "$inbase/".(scalar @tumour_samples)."/contamination";

my $tumour_count = scalar @tumour_samples;
for my $i(0..($tumour_count-1)) {
  my $tumour_path = "$inbase/$i/contamination";

  # make each tumour dataset contain the normal data too
  my $command = "cp $cntl_path/$cntl_sample* $tumour_path/.";
  my ($stdout, $stderr, $exit) = capture { system($command); };
  die $stderr if($exit != 0);
  my $tumour_samp = $tumour_samples[$i];
  
  $command = "cp -r $tumour_path $inbase/$i/verifyBamId_$tumour_samp";
  ($stdout, $stderr, $exit) = capture { system($command); };
  die $stderr if($exit != 0);

  my $archive = "$inbase/$tumour_samp.verifyBamId.tar.gz";
  $command = join ' ', 'tar -C', "$inbase/$i", '-czf', $archive, "verifyBamId_$tumour_samp";
  ($stdout, $stderr, $exit) = capture { system($command); };
  die $stderr if($exit != 0);
  md5file($archive);
}

sub sample_name {
  my $control_bam = shift;
  my @lines = split /\n/, Bio::DB::Sam->new(-bam => $control_bam)->header->text;
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

sub md5file {
  my $file = shift;
  my ($stdout, $stderr, $exit) = capture { system(qq{md5sum $file | awk '{print \$1}' > $file.md5}); };
  die $stderr if($exit != 0);
}
