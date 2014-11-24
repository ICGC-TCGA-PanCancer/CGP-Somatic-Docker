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
  die "USAGE: rootOfOutdir ordered.bam [ordered.bam2]";
}

my $base_dir = shift @ARGV;
my @ordered_bams = @ARGV;

my $final_qc = qc_data( $base_dir, @ordered_bams );
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
  my ( $base_dir, @bams ) = @_;
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
    $count++;
  }
  return \%full_qc;
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
    $run_met{'pairs'}{$aliqout_id}{'other'}{'binCounts'} = _run_met( $count, $timings, 'binCounts' );
    $run_met{'pairs'}{$aliqout_id}{'other'}{'bbAllele'} = _run_met( $count, $timings, 'bbAllele' );
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
  my %met;
  opendir( my $dh, $folder );
  while ( my $x = readdir($dh) ) {
    my $stub = $inc . '_' . $alg . '_';

    next unless ( index( $x, $stub ) == 0 );

    my ( $process, $index ) = $x =~ m/_([^_]+)_([[:digit:]]+)$/;
    my $file = "$folder/$x";
    open my $fh, '<', $file;
    while ( my $line = <$fh> ) {
      chomp $line;
      my ( $key, $value ) = split /\s+/, $line;
      $met{'detailed'}{$process}[ $index - 1 ]{$key} = $value;
      if ( $key eq 'User_s' || $key eq 'System_s' ) {
        $total_cpu += $value;
      }
      elsif ( $key eq 'Max_kb' && $value > $max_mem ) {
        $max_mem = $value;
      }
    }
    close $fh;
    $met{'caller'} = $alg;
    $met{'total_cpu_s'} = $total_cpu;
    $met{'max_mem_mb'} = int 1 + ( $max_mem / 1024 ); # round up
  }
  closedir($dh);
  return \%met;
}
sub _qc_brass {
  my $to_process = shift;
  my %qc = ( 'caller' => 'BRASS' );
  my ($stdout, $stderr, $exit) = capture { system(qq{wc -l $to_process/*.groups.filtered.bedpe}); };
  die "Error occurred while counting $to_process/*.groups.filtered.bedpe" if ($stderr);
  chomp $stdout;
  $stdout =~ m/^([[:digit:]]+)/;
  $qc{'groups'} = $1;

  ($stdout, $stderr, $exit) = capture { system(qq{wc -l $to_process/*.annot.bedpe}); };
  die "Error occurred while counting $to_process/*.annot.bedpe" if ($stderr);
  chomp $stdout;
  $stdout =~ m/^([[:digit:]]+)/;
  $qc{'assembled'} = $1;
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
