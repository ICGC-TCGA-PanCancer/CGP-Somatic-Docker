use strict;
use Template;
use Getopt::Long;

# vars
my $tsv = "data/PCAWG_Data_Freeze_Train_2.0_Pilot-58.tsv";
my $output_dir = "results";
my $defaults = "config/settings.conf";
my $template = "template/workflow_config.ini.tt";

my $d = {};

GetOptions(
  "tsv=s" => \$tsv,
  "output_dir=s" => \$output_dir,
  "defaults=s" => \$defaults,
  "template=s" => \$template,
) or die ("Getopt error");

# program

open DEF, "<$defaults" or die;
while(<DEF>) {
  
}
close DEF;

open IN, "<$tsv" or die;
while(<IN>) {
  chomp;
  my @a = split /\t/;
  
}
close IN;

my $tt = Template->new;
$tt->process("", $d) || die $tt->error;

