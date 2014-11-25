use strict;
use Template;
use Getopt::Long;

# vars
my $tsv = "data/PCAWG_Data_Freeze_Train_2.0_Pilot-58.tsv";
my $output_dir = "results";
my $defaults = "config/settings.conf";
my $template = "template/workflow_config.ini.tt";

my $srv_hash = {};
my $def = {};
my $tt = Template->new;

GetOptions(
  "tsv=s" => \$tsv,
  "output-dir=s" => \$output_dir,
  "defaults=s" => \$defaults,
  "template=s" => \$template,
) or die ("Getopt error");

# program

# read in the defaults
open DEF, "<$defaults" or die;
while(<DEF>) {
  chomp;
  if(/(\S+)\s*=\s*(\S+)/) {
    my $key = $1;
    my $val = $2;
    $def->{$key} = $val;
  }
}
# now deal with hash values
my @down_srv = split /,/, $def->{'gnosServer'};
my @upload_srv = split /,/, $def->{'uploadServer'};
my @down_pem = split /,/,  $def->{'pemFile'};
my @upload_pem = split /,/,  $def->{'uploadPemFile'};
for (my $i=0; $i<scalar(@down_srv); $i++) {
  $srv_hash->{$down_srv[$i]}{'upload'} = $upload_srv[$i];
  $srv_hash->{$down_srv[$i]}{'down_pem'} = $down_pem[$i];
  $srv_hash->{$down_srv[$i]}{'upload_pem'} = $upload_pem[$i];
}
close DEF;

# scripts to run
my @scripts;

# loop over the contents of the data file
open IN, "<$tsv" or die;
while(<IN>) {
  chomp;
  push @scripts, process($_, $def);
}
close IN;

# write out a script file for triggering these
open OUT, ">$output_dir/run_scripts.sh" or die;
foreach my $script (@scripts) {
  print OUT "$script\n";
}
close OUT;


# subs
sub process {

  my ($line, $def) = @_;

  my @a = split /\t/, $line;
  my $tumourAliquotIds = $a[13];
  my $tumourAnalysisIds = $a[15];
  my $tumourBams = $a[16];
  my $controlAnalysisId = $a[8];
  my $controlBam = $a[9];

  # make a working dir
  system("mkdir -p $output_dir/$tumourAliquotIds");

  my $values = fill_values($def, \@a);

  # make an ini file
  $tt->process($template, $values, "$output_dir/$tumourAliquotIds/config.ini") || die $tt->error;

  # make a submission script
  my $command = "cd $output_dir/$tumourAliquotIds; seqware_1.1.0-alpha.5 bundle launch --dir /glusterfs/netapp/homes1/BOCONNOR/provisioned-bundles/Workflow_Bundle_SangerPancancerCgpCnIndelSnvStr_1.0.0_SeqWare_1.1.0-alpha.5 --ini config.ini";

  # add to master list of scripts
  return($command);
}

sub fill_values {
  my ($def, $a) = @_;

  # read defaults
  my $r = {};
  foreach my $key (keys %{$def}) {
    $r->{$key} = $def->{$key};
  }

  # now process
  my $tumourAliquotIds = $a->[13];
  my $tumourAnalysisIds = $a->[15];
  my $tumourBams = $a->[16];
  my $controlAnalysisId = $a->[8];
  my $controlBam = $a->[9];
  my $norm_gnos_download_srv = $a->[7];
  my $tumour_gnos_download_srv = $a->[14];
  die "$norm_gnos_download_srv ne $tumour_gnos_download_srv\n" if ($norm_gnos_download_srv ne $tumour_gnos_download_srv);

  # correctly deal with download/upload server
  # the pem keys may change depending on where
  my $upload_srv = $srv_hash->{$norm_gnos_download_srv}{'upload'};
  my $upload_pem = $srv_hash->{$norm_gnos_download_srv}{'upload_pem'};
  my $down_pem = $srv_hash->{$norm_gnos_download_srv}{'down_pem'};
  $r->{"pemFile"} = $down_pem;
  $r->{"uploadPemFile"} = $upload_pem;
  $r->{"gnosServer"} = $norm_gnos_download_srv;
  $r->{"uploadServer"} = $upload_srv;

  # now deal with file UUIDs
  $r->{"tumourAliquotIds"} = $tumourAliquotIds;
  $r->{"tumourAnalysisIds"} = $tumourAnalysisIds;
  $r->{"tumourBams"} = $tumourBams;
  $r->{"controlAnalysisId"} = $controlAnalysisId;
  $r->{"controlBam"} = $controlBam;

  return($r);
}
