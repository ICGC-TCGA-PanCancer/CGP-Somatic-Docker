#!/usr/bin/perl

# @File qc_and_metrics.pl
# @Author kr2
# @Created 11-Nov-2014 11:54:24
#
use strict;
use autodie qw(:all);
use Capture::Tiny qw(capture);
use JSON;

if ( @ARGV < 2 ) {
  die "USAGE: rootOfOutdir control.bam tumour1.bam [tumour2.bam...]";
}

my $base_dir = shift @ARGV;
my $control_bam = shift @ARGV;
my @ordered_bams = @ARGV;

my $final_qc = qc_data( $base_dir, $control_bam, @ordered_bams );
my $encoded = encode_json $final_qc;
open my $JOUT, '>', "$base_dir/qc_metrics.json";
print $JOUT $encoded,"\n";
close $JOUT;

my $final_met = rum_metrics( $base_dir, @ordered_bams );
$encoded = encode_json $final_met;
open my $JOUT, '>', "$base_dir/process_metrics.json";
print $JOUT $encoded,"\n";
close $JOUT;

sub qc_data {
  my ( $base_dir, $cntl_bam, @bams ) = @_;
# this will need to be mildly intelligent, expecially for ASCAT to capture failures
  my %full_qc;
  my $count = 0;
  for my $bam (@bams) {
    my $aliqout_id = get_aliquot_id_from_bam($bam);
    my $to_process = "$base_dir/$count";
    $full_qc{$aliqout_id}{'sv'} = _qc_brass("$to_process/brass");
    $full_qc{$aliqout_id}{'snv_mnv'} = _qc_caveman("$to_process/caveman");
    $full_qc{$aliqout_id}{'indel'} = _qc_pindel("$to_process/pindel");
    $full_qc{$aliqout_id}{'cnv'} = _qc_ascat("$to_process/ascat");
    $full_qc{$aliqout_id}{'contamination'} = _qc_contam("$to_process/contamination");
    $count++;
  }
  my $aliqout_id = get_aliquot_id_from_bam($cntl_bam);
  $full_qc{$aliqout_id}{'contamination'} = _qc_contam("$base_dir/$count/contamination");
  _qc_genotypes(\%full_qc, $base_dir);
  return \%full_qc;
}

sub _qc_genotypes {
  my ($full_qc, $base_dir) = @_;
  
  my ($stdout, $stderr, $exit) = capture { system("cat $base_dir/genotype/summary.json"); };
  die "STDOUT: $stdout\n\nSTDERR: $stderr\n" if ( $exit != 0 );
  my $struc = decode_json $stdout;

  for my $tumour(@{$struc->{'tumours'}}) {
    my $tumour_name = $tumour->{'sample'};
    $full_qc->{$tumour_name}->{'genotype'}->{'frac_informative_genotype'} = $tumour->{'genotype'}->{'frac_informative_genotype'};
    $full_qc->{$tumour_name}->{'genotype'}->{'frac_matched_genotype'} = $tumour->{'genotype'}->{'frac_matched_genotype'};
    $full_qc->{$tumour_name}->{'genotype'}->{'total_loci'} = $struc->{'total_loci_genotype'};
    $full_qc->{$tumour_name}->{'genotype'}->{'compared_against'} = $struc->{'compared_against'};

    $full_qc->{$tumour_name}->{'gender'}->{'frac_match_gender'} = $tumour->{'gender'}->{'frac_match_gender'};
    $full_qc->{$tumour_name}->{'gender'}->{'gender_result'} = $tumour->{'gender'}->{'gender'};
    $full_qc->{$tumour_name}->{'gender'}->{'total_loci'} = $struc->{'total_loci_gender'};
    $full_qc->{$tumour_name}->{'gender'}->{'compared_against'} = $struc->{'compared_against'};
  }
  1;
}

# code duplication in packageResults.pl
sub get_aliquot_id_from_bam {
  my $bam = shift;
# samtools view -H PD13491a/PD13491a.bam | grep '^@RG' | perl -ne 'm/\tSM:([^\t]+)/; $x{$1}=1; END{print join("\n",keys %x),"\n";};'
  die "BAM file does not exist: $bam" unless ( -e $bam );

  my $command = sprintf q{samtools view -H %s | grep '^@RG'}, $bam;
  my ($stdout, $stderr, $exit) = capture { system($command); };
  die "STDOUT: $stdout\n\nSTDERR: $stderr\n" if ( $exit != 0 );

  my %names;
  for ( split "\n", $stdout ) {
    chomp $_;
    if ( $_ =~ m/\tSM:([^\t]+)/ ) {
      $names{$1} = 1;
    }
    else {
      die "Found RG line with no SM field: $_\n\tfrom: $bam\n";
    }
  }

  my @keys = keys %names;
  die "Multiple different SM entries: ."
    . join( q{,}, @keys )
    . "\n\tfrom: $bam\n"
    if ( scalar @keys > 1 );
  die "No SM entry found in: $bam\n" if ( scalar @keys == 0 );
  return $keys[0];
}

sub rum_metrics {
  my ( $base_dir, @bams ) = @_;
  my %run_met;
  my $count = 0;
  my $timings = "$base_dir/timings";
  for my $bam (@bams) {
    my $aliqout_id = get_aliquot_id_from_bam($bam);
    $run_met{'pairs'}{$aliqout_id}{'sv'} = _run_met( $count, $timings, 'BRASS' );
    $run_met{'pairs'}{$aliqout_id}{'snv_mnv'} = _run_met( $count, $timings, 'CaVEMan' );
    $run_met{'pairs'}{$aliqout_id}{'indel'} = _run_met( $count, $timings, 'cgpPindel' );
    $run_met{'pairs'}{$aliqout_id}{'cnv'} = _run_met( $count, $timings, 'ASCAT' );
    $run_met{'pairs'}{$aliqout_id}{'bbAllele'} = _run_met( $count, $timings, 'bbAllele' );
    $count++;
  }
  $run_met{'workflow'}{'Wall_s'} = _workflow_met($timings);
  return \%run_met;
}

sub _workflow_met {
  my $folder = shift;
  my ($stdout, $stderr, $exit) = capture { system(qq{cat $folder/workflow_start}); };
  die "Error occurred while capturing content of $folder/start" if ($stderr);
  chomp $stdout;
  my ($started) = $stdout =~ m/^([[:digit:]]+)/;

  ($stdout, $stderr, $exit) = capture { system(qq{cat $folder/workflow_end}); };
  die "Error occurred while capturing content of $folder/end" if ($stderr);
  chomp $stdout;
  my ($ended) = $stdout =~ m/^([[:digit:]]+)/;

  my $elapsed = $ended - $started;
  return $elapsed;
}

sub _run_met {
  my ( $inc, $folder, $alg ) = @_;
  my ( $total_cpu, $max_mem ) = ( 0, 0 );
  my (%met, %process_files);
  opendir( my $dh, $folder );
  while ( my $x = readdir($dh) ) {
    my $stub = $inc . '_' . $alg . '_';

    next unless ( index( $x, $stub ) == 0 );

    my ( $process, $index ) = $x =~ m/^${inc}_${alg}_([^_]+)_([[:digit:]]+)$/;
    $process_files{$process}{$index} = "$folder/$x";
  }
  closedir($dh);

  for my $process(keys %process_files) {
    for my $index(sort {$a<=>$b} keys %{$process_files{$process}}) {
      my $file = $process_files{$process}->{$index};
      open my $fh, '<', $file;
      my %process_metrics;
      while ( my $line = <$fh> ) {
        chomp $line;
        my ( $key, $value ) = split /\s+/, $line;
        $process_metrics{$key} = $value;
        if ( $key eq 'User_s' || $key eq 'System_s' ) {
          $total_cpu += $value;
        }
        elsif ( $key eq 'Max_kb' && $value > $max_mem ) {
          $max_mem = $value;
        }
      }
      close $fh;
      push @{$met{'detailed'}{$process}}, \%process_metrics;
      $met{'caller'} = $alg;
      $met{'total_cpu_s'} = $total_cpu;
      $met{'max_mem_mb'} = int 1 + ( $max_mem / 1024 ); # round up
    }
  }

  return \%met;
}

sub _qc_contam {
  my $to_process = shift;
  my ($stdout, $stderr, $exit) = capture { system(qq{cat $to_process/summary.json}); };
  die "STDOUT: $stdout\n\nSTDERR: $stderr\n" if ( $exit != 0 );
  my $struc = decode_json $stdout;
  my %qc = ( 'caller' => 'varifyBamId' );
  for my $key(keys %{$struc}) {
    $qc{$key} = $struc->{$key};
  }
  return \%qc;
}

sub _qc_brass {
  my $to_process = shift;
  my %qc = ( 'caller' => 'BRASS' );
  my ($stdout, $stderr, $exit) = capture { system([0,1], qq{grep -cv '^#' $to_process/intermediates/*.groups.filtered.bedpe}); };
  die "Error occurred while counting $to_process/intermediates/*.groups.filtered.bedpe" if ($stderr);
  chomp $stdout;
  $stdout =~ m/^([[:digit:]]+)/;
  $qc{'total_groups'} = $1;

  ($stdout, $stderr, $exit) = capture { system([0,1], qq{grep -cv '^#' $to_process/*.annot.bedpe}); };
  die "Error occurred while counting $to_process/*.annot.bedpe" if ($stderr);
  chomp $stdout;
  $stdout =~ m/^([[:digit:]]+)/;
  $qc{'filtered_groups'} = $1;
  return \%qc;
}

sub _qc_ascat {
  my $to_process = shift;
  my %qc = ( 'caller' => 'ASCAT' );
  my ($stdout, $stderr, $exit) = capture { system(qq{grep -F 'WARNING ASCAT failed to generate a solution' $to_process/*.samplestatistics.csv | wc -l }); };
  die "Error occurred while checking CN state $to_process/*.samplestatistics.csv" if ($stderr);
  chomp $stdout;
  $qc{'solution_possible'} = $stdout ? 0 : 1;
  if ( $qc{'solution_possible'} ) {
    ($stdout, $stderr, $exit) = capture { system(qq{cat $to_process/*.samplestatistics.csv}); };
    die "Error occurred while capturing ASCAT stats $to_process/*.samplestatistics.csv" if ($stderr);
    for ( split /\n/, $stdout ) {
      my ( $key, $value ) = split /\s+/, $_;
      $qc{$key} = $value;
    }
  }
  else {
    $qc{'NormalContamination'} = q{.};
    $qc{'Ploidy'} = q{.};
    $qc{'rho'} = q{.};
    $qc{'psi'} = q{.};
    $qc{'goodnessOfFit'} = q{.};
  }
  return \%qc;
}

sub _qc_pindel {
  my $to_process = shift;
  my %qc = ( 'caller' => 'cgpPindel' );
  my ($stdout, $stderr, $exit) = capture { system(qq{zgrep -v '^#' $to_process/*.flagged.vcf.gz | wc -l}); };
  die "Error occurred while counting VCF records in $to_process/*.flagged.vcf.gz" if ($stderr);
  chomp $stdout;
  $qc{'all_indel'} = $stdout;

  ($stdout, $stderr, $exit) = capture { system(qq{zgrep -v '^#' $to_process/*.flagged.vcf.gz | grep -Fw PASS | wc -l}); };
  die "Error occurred while counting passed VCF records in $to_process/*.flagged.vcf.gz" if ($stderr);
  chomp $stdout;
  $qc{'passed_indel'} = $stdout;

  ($stdout, $stderr, $exit) = capture { system(qq{wc -l $to_process/*.germline.bed}); };
  die "Error occurred while counting records in $to_process/*.germline.bed" if ($stderr);
  chomp $stdout;
  $stdout =~ m/^([[:digit:]]+)/;
  $qc{'likely_germline'} = $1;

  return \%qc;
}

sub _qc_caveman {
  my $to_process = shift;
  # I know that grep can cout things, but if it gets 0 it gives non-zero exit
  # for this reason pipe to wc and check stderr for messages
  my %qc = ( 'caller' => 'CaVEMan' );
  my ($stdout, $stderr, $exit) = capture { system(qq{zgrep -v '^#' $to_process/*.flagged.muts.vcf.gz | wc -l}); };
  die "Error occurred while counting VCF records in $to_process/*.flagged.muts.vcf.gz" if ($stderr);
  chomp $stdout;
  $qc{'all_somatic'} = $stdout;

  ($stdout, $stderr, $exit) = capture { system(qq{zgrep -v '^#' $to_process/*.flagged.muts.vcf.gz | grep -Fw PASS | wc -l}); };
  die "Error occurred while counting passed VCF records in $to_process/*.flagged.muts.vcf.gz" if ($stderr);
  chomp $stdout;
  $qc{'passed_somatic'} = $stdout;

  ($stdout, $stderr, $exit) = capture { system(qq{zgrep -v '^#' $to_process/*.snps.ids.vcf.gz | wc -l}); };
  die "Error occurred while counting VCF records in $to_process/*.snps.ids.vcf.gz" if ($stderr);
  chomp $stdout;
  $qc{'all_germline'} = $stdout;

  # need to check ASCAT was real input
  ($stdout, $stderr, $exit) = capture { system(qq{grep -F 'WARNING ASCAT failed to generate a solution' $to_process/../ascat/*.samplestatistics.csv | wc -l }); };
  die "Error occurred while checking CN state $to_process/../ascat/*.samplestatistics.csv" if ($stderr);
  chomp $stdout;
  $qc{'real_copynumber'} = $stdout ? 0 : 1;

  return \%qc;
}
