#!/usr/bin/perl

use strict;
use warnings FATAL => qw(all);
use autodie qw(:all);

use File::Path qw(make_path);
use File::Copy;
use Capture::Tiny qw(capture);

use Bio::DB::Sam;

my $control_bam = shift;
my $indir = shift;

my $cntl_sample = sample_name($control_bam);
my %tumour_samples;
my %file_sets;
opendir(my $dh, $indir);
while(my $item = readdir $dh) {
  next if($item =~ m/^[.]/ || $item =~ m/\.gz$/);
  if($item =~ m/^([^.]+)/) {
    my $sample = $1;
    push @{$file_sets{$sample}}, "$indir/$item";
    $tumour_samples{$sample} = 1 unless($sample eq $cntl_sample);
  }
}
closedir $dh;

my @types = qw(allelic_counts bin_counts);
for my $tum_samp(keys %tumour_samples) {
  my $path = "$indir/$tum_samp";
  make_path $path unless(-d $path);

  for(@{$file_sets{$tum_samp}}, @{$file_sets{$cntl_sample}}) {
    copy $_, "$path/.";
  }
  my $archive = "$indir/$tum_samp.imputeCounts.tar.gz";
  my $command = join ' ', 'tar -C', $indir, '-czf', $archive, $tum_samp;
  my ($stdout, $stderr, $exit) = capture { system($command); };
  die $stderr if($exit != 0);
  md5file($archive);
}

sub merge_files {
  my ($files, $dest) = @_;
  if(scalar @{$files} == 1) {
    copy($files->[0], $dest);
  }
  else {
    my $command = join ' ', 'zcat', @{$files}, q{| sort -t ',' -k1,1 -k2,2n | gzip -c >}, $dest;
    my ($stdout, $stderr, $exit) = capture { system($command); };
    die $stderr if($exit != 0);
  }
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
