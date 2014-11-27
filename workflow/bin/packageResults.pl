#!/usr/bin/perl

use strict;
use autodie qw(:all);
use Capture::Tiny qw(capture);

# Will process the specified folder, expects to find a *.vcf.gz and *.vcf.gz.tbi file
# then tar.gz whole folder (content is not renamed in anyway)
# also generates the json QC and timing metrics files

if(@ARGV < 5) {
  die "USAGE: ./packageResults.pl output_folder tumour.bam to_process type primary_vcf_extension\n"
}

my $in_output_folder = shift @ARGV;
my $in_tumour_bam = shift @ARGV;
my $in_to_process = shift @ARGV;
my $in_type = shift @ARGV;
my $in_base_vcf = shift @ARGV;
my $in_workflow_name = shift @ARGV;
my $in_somatic_or_germline = shift @ARGV;
my $in_currdate = shift @ARGV;

my $aliquot_id_from_bam = get_aliquot_id_from_bam($in_tumour_bam);
copy_rename_vcfs($in_output_folder, $aliquot_id_from_bam, $in_type, $in_to_process, $in_base_vcf);
tar_output($in_output_folder, $aliquot_id_from_bam, $in_type, $in_to_process);

sub tar_output {
  my ($output_folder, $aliquot_id, $type, $to_process) = @_;
  # tar --exclude=*/logs -zcf ascat.tar.gz outdir/ascat
  $to_process =~ s|/$||; # make sure not trailing / as will mess up tar
  my $tar = sprintf 'tar --exclude=*/logs -zcf %s.tar.gz %s', "$output_folder/$aliquot_id.$in_workflow_name.$in_currdate.$in_somatic_or_germline.$type", $to_process;
  my ($stdout, $stderr, $exit) = capture { system($tar); };
  die $stderr if($exit != 0);
  md5file("$output_folder/$aliquot_id.$in_workflow_name.$in_currdate.$in_somatic_or_germline.$type.tar.gz");
}

# code duplication in qc_and_metrics.pl
sub get_aliquot_id_from_bam {
  my $bam = shift;
  # samtools view -H PD13491a/PD13491a.bam | grep '^@RG' | perl -ne 'm/\tSM:([^\t]+)/; $x{$1}=1; END{print join("\n",keys %x),"\n";};'
  die "BAM file does not exist: $bam" unless(-e $bam);
  my $command = sprintf q{samtools view -H %s | grep '^@RG'}, $bam;
  my ($stdout, $stderr, $exit) = capture { system($command); };
  die "STDOUT: $stdout\n\nSTDERR: $stderr\n" if($exit != 0);
  my %names;
  for(split "\n", $stdout) {
    chomp $_;
    if($_ =~ m/\tSM:([^\t]+)/) {
      $names{$1}=1;
    }
    else {
      die "Found RG line with no SM field: $_\n\tfrom: $bam\n";
    }
  }
  my @keys = keys %names;
  die "Multiple different SM entries: .".join(q{,},@keys)."\n\tfrom: $bam\n" if(scalar @keys > 1);
  die "No SM entry found in: $bam\n" if(scalar @keys == 0);
  return $keys[0];
}

sub copy_rename_vcfs {
  my ($output_folder, $aliquot_id, $type, $to_process, $base_vcf) = @_;
  for my $f_type(($base_vcf, "$base_vcf.tbi")) {
    my $source = "$to_process/*".$f_type;
    my $dest = "$output_folder/$aliquot_id.$in_workflow_name.$in_currdate.$in_somatic_or_germline.$type";
    if($f_type =~ m/\.vcf\.gz$/) {
      $dest .= '.vcf.gz';
    }
    elsif($f_type =~ m/\.vcf\.gz\.tbi$/) {
      $dest .= '.vcf.gz.tbi';
    }
    else {
      die "Unexpected file extension $f_type\n";
    }
    my ($stdout, $stderr, $exit) = capture { system("cp $source $dest"); };
    die $stderr if($exit != 0);
    md5file($dest);
  }
}

sub md5file {
  my $file = shift;
  my ($stdout, $stderr, $exit) = capture { system(qq{md5sum $file | awk '{print \$1}' > $file.md5}); };
  die $stderr if($exit != 0);
}

