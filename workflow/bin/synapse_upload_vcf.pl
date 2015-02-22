#!/usr/bin/env perl

use warnings;
use strict;

use feature qw(say);
use autodie;
use Carp::Always;
use Carp qw( croak );

use Getopt::Long;
use XML::DOM;
use XML::XPath;
use XML::XPath::XMLParser;
use JSON;

use Cwd 'abs_path';

use Data::Dumper;

#############################################################################################
# DESCRIPTION                                                                               #
#############################################################################################
# 1) Use elastic search index to find a list of analysis metadata URLs for the variant
#    worflow results we want.  Alternatively, use the suypplied GNOS metadata URL and skip
#    elastic search
# 2) Download and parse the metadata
# 3) Assemble a JSON data structure suitable for synapse upload 
# 4) Grab the VCF worflow files from GNOS, if required 
# 5) Deploy the upload to synapse
#############################################################################################


# Edit as required!
use constant pem_file     => 'gnostest.pem';     #
use constant output_dir   => 'test_output_dir';  # configurable as command line args
use constant xml_dir      => 'xml';              #
use constant parent_id    => 'syn3155834';       #
use constant pem_conf     => 'conf/pem.conf';    #

use constant sftp_url     => 'sftp://tcgaftps.nci.nih.gov/tcgapancan/pancan/variant_calling_pilot_64/OICR_Sanger_Core';

#############
# VARIABLES #
#############

my $parser        = new XML::DOM::Parser;
my $output_dir    = output_dir;
my $xml_dir       = xml_dir;
my $pem_file      = pem_file;
my $parent_id     = parent_id;
my $pem_conf      = pem_conf;
my $sftp_url      = sftp_url;
my $download      = 0;

my ($metadata_url,$use_cached_xml,$help,$pemconf,$local_path,$local_xml);
$help = 1 unless @ARGV > 0;
GetOptions(
    "metadata-url=s"   => \$metadata_url,
    "use-cached-xml"   => \$use_cached_xml,
    "local-xml=s"      => \$local_xml,
    "sftp-url=s"       => \$sftp_url,
    "output-dir=s"     => \$output_dir,
    "xml-dir=s"        => \$xml_dir,
    "pem-file=s"       => \$pem_file,
    "parent-id=s"      => \$parent_id,
    "pem-conf=s"       => \$pem_conf,
    "local-path=s"     => \$local_path,
    "download"         => \$download,
    "help"             => \$help
    );

die << 'END' if $help;
Usage: synapse_upload_vcf.pl[--metadata-url url] 
                            [--use-cached_xml] 
                            [--local-xml /path/to/local/metadata.xml -- Note: still downloads BWA metadata from GNOS]
                            [--output-dir dir]
                            [--xml-dir]
                            [--pem-file file.pem]
                            [--parent-id syn2897245]
                            [--perm-conf conf/pem.conf]
                            [--local-path /path/to/local/files] 
                            [--sftp-url sftp://tcgaftps.nci.nih.gov/tcgapancan/pancan/variant_calling_pilot_64/OICR_Sanger_Core]
                            [--download optional flag to Download files from GNOS]
                            [--help]
END
;
 

$output_dir = "vcf/$output_dir";
run("mkdir -p $output_dir");
run("mkdir -p $xml_dir");

my $pwd = `pwd`;
chomp $pwd;

# If we don't have a url, get the list by elastic search
my @metadata_urls;
unless ($metadata_url || $local_xml) {
    say "Getting metadata URLs by elastic search...";
    @metadata_urls = `./get_donors_by_elastic_search.pl`;
    chomp @metadata_urls;
}
else {
    @metadata_urls = grep {defined $_} ($local_xml,$metadata_url);
}

# First, read in the metadata and save the workflow
# version
my %variant_workflow_version;
my %to_be_processed;
for my $url (@metadata_urls) {
    say "metadata URL=$url";
    my $metad = download_metadata($url,$local_xml);

    # save workflow version 
    workflow_version($metad);
   
    my ($analysis_id) = $url =~ m!/([^/]+)$!;
    $to_be_processed{$analysis_id} = $metad;
}

# get the pem config
my %pem;
if ($pem_conf && -e $pem_conf) {
    open CONF, $pem_conf or die $!;
    while (<CONF>) {
	chomp;
	$_ or next;
	my ($url,$pemfile) = split;
	$pem{$url} = $pemfile;
    }
    close CONF;
}

# Then, do the upload only for the most recent version
my $go;
while (my ($analysis_id,$metad) = each %to_be_processed) {
    next unless newest_workflow_version($metad);
    #next unless $analysis_id =~ /2f8bb636-5828-4106-97cc-041a6842cf27|6a60ea77-f728-48c4-b83b-8a84bb61248a|803ca8c2-a57e-4d25-b6ae-410f60365b39/;
    my $json  = generate_output_json($metad);

    open JFILE, ">$output_dir/$analysis_id.json";
    print JFILE $json;
    close JFILE;

    say "JSON saved as $output_dir/$analysis_id.json";

    my $upload_flag = $local_path ? '--upload-files' : '';

    my $helper = abs_path($0);
    $helper =~ s/\.pl//g;

    run("$helper $upload_flag --parentId $parent_id  < $output_dir/$analysis_id.json");

}

# Check to see if this donor has VCF results from a more recent
# version of the workflow.
sub newest_workflow_version {
    return workflow_version(@_);
}

sub workflow_version {
    my $metad = shift;
    my ($data) = values %$metad;
    my ($donor_id) = keys %{$data->{analysis_attr}->{submitter_donor_id}};
    my $center     = $data->{center_name};
    my ($workflow) =  keys %{$data->{analysis_attr}->{variant_workflow_name}};
    my ($version)  =  keys %{$data->{analysis_attr}->{variant_workflow_version}};
    
    # unique donor ID
    $donor_id = join('-',$center,$donor_id);
    
    # check if our workflow version is more recent
    return is_more_recent($donor_id,$workflow,$version);
}

sub is_more_recent {
    my $donor   = shift;
    my $name    = shift;
    my $version = shift;
    my ($primary,$secondary,$tertiary) = split('\.',$version);

    my $wf_version = $variant_workflow_version{$donor}{$name};
    if (not defined $wf_version) {
	$variant_workflow_version{$donor}{$name} = [$primary,$secondary,$tertiary];
	return 1;
    }
    elsif ( # newer
	$primary  > $wf_version->[0]
	||
	($secondary > $wf_version->[1] && $primary == $wf_version->[0])
	||
	($tertiary  > $wf_version->[2] && $primary == $wf_version->[0] && $secondary == $wf_version->[1])
	) {
	$variant_workflow_version{$donor}{$name} = [$primary,$secondary,$tertiary];
	return 1;
    }
    elsif ( # ==
        $primary   == $wf_version->[0]
        &&
        $secondary == $wf_version->[1]
        &&
        $tertiary  == $wf_version->[2]
	) {
	return 1;
    }
    return 0;
}

# This method gets the information about the BWA alignment outputs/VCF inputs
sub get_sample_data {
    my $metad = shift;
    my $json  = shift;
    my $anno  = $json->{annotations};
    my ($input_json_string) = keys %{$metad->{variant_pipeline_input_info}};
    my $input_data = decode_json $input_json_string;

    my ($inputs)     = values %$input_data; 

    my ($tumor_sid, $normal_sid, @urls, $tumor_aid, $normal_aid);

    # This bit deals with the tumor/normal analysis ids
    # and makes a hook to grab BAM download URLs
    for my $specimen (@$inputs) {
	my $type = $specimen->{attributes}->{dcc_specimen_type};
	my $sample_id = $specimen->{specimen};
	my $analysis_id =  $specimen->{attributes}->{analysis_id};
	my $is_tumor = $type =~ /tumou?r|xenograft|cell line/i;
	if ($is_tumor) {
	    $tumor_sid = $tumor_sid ? "$tumor_sid,$sample_id"   : $sample_id;
	    $tumor_aid = $tumor_aid ? "$tumor_aid,$analysis_id" : $analysis_id;
	}
	else {
	    $normal_sid = $normal_sid ? "$normal_sid,$sample_id"   : $sample_id;
	    $normal_aid = $normal_aid ? "$normal_aid,$analysis_id" : $analysis_id;
	}
	
	push @urls, $specimen->{attributes}->{analysis_url};
    }
    say "TUMOR\t$tumor_sid\t$tumor_aid";

    # look up BAM download information (complicated)
    $json->{used_urls} = get_bamfile_download_urls(@urls);

    $anno->{sample_id_normal}   = $normal_sid;
    $anno->{analysis_id_normal} = $normal_aid;
    $anno->{sample_id_tumor}    = $tumor_sid;
    $anno->{analysis_id_tumor}  = $tumor_aid;
    $anno->{sequence_source}    = 'WGS';
}


sub get_bamfile_download_urls {
    my @meta_urls = @_;
    my @bam_urls;
    for my $url (@meta_urls) {
	my $bam_url = download_alignment_metadata($url);
	push @bam_urls, $bam_url;
    }
    return \@bam_urls;
}


# We will neeed to grab the files from GNOS assuming synpase upload is
# not concurrent with GNOS upload
sub download_vcf_files {
    my $metad = shift;
    my $url   = shift;
    my @files = @_;

    (my $base_url = $url) =~ s!^(https?://[^/]+)\S+!$1!;
    my $pem = $pem{$base_url} || $pem_file;

    say "This is where I will be downloading files from GNOS";
    chdir $output_dir or die $!;
    for my $file (@files) {
	chomp($file = `basename $file`);
	my $download_url = $metad->{$url}->{download_url};
	my ($analysis_id) = $download_url =~ m!/([^/]+)$!;
	my $command = "gtdownload -c $pem_file ";
	$command .= "$download_url/$file";

        #say "This would be the download command:";
	say $command;

	# real download
	# system $command;

	# fake download!
	my $rand = rand()*100;
	system "echo $rand > $file";

	unless (-e $file) {
	    die "There was a problem getting this file: $file";
	}
    }

    chdir $pwd or die $!;
}

sub get_files {
    my $metad = shift;
    my $url   = shift;
    my ($analysis_id) = $url =~ m!/([^/]+)$!;
    my $file_data     = $metad->{$url}->{file};

    if ($download && !$local_path) {
	my @files_to_download = map{"$output_dir/$analysis_id/$_"} map {$_->{filename}} @$file_data;
	download_vcf_files($metad,$url,@files_to_download);
    }

    if ($local_path) {
	unless (-d $local_path) {
	    die "Error: Local path $local_path does not exist!";
	}
	$local_path =~ s!/$!!;
    }


    my $file_path = $local_path || sftp_url;
    my @files_to_upload   = map{$file_path . "/$_"} map {$_->{filename}} @$file_data;
    return \@files_to_upload;
}

sub generate_output_json {
    my ($metad) = @_;
    my $data = {};

    foreach my $url ( keys %{$metad} ) {
	$data->{files} = get_files($metad,$url);

	my $atts = $metad->{$url}->{analysis_attr};
	my $anno = $data->{annotations} = {};
	
	# top-level annotations
        $anno->{center_name}     = $metad->{$url}->{center_name};
        $anno->{reference_build} =  $metad->{$url}->{reference_build};

	# get original sample information
	get_sample_data($atts,$data);

	# from the attributes hash
	($anno->{donor_id})                   = keys %{$atts->{submitter_donor_id}};
	($anno->{study})                      = keys %{$atts->{STUDY}};
	($anno->{alignment_workflow_name})    = keys %{$atts->{alignment_workflow_name}};
	($anno->{alignment_workflow_version}) = keys %{$atts->{alignment_workflow_version }};
	($anno->{sequence_source})            = keys %{$atts->{sequence_source}};
	($anno->{workflow_url})               = keys %{$atts->{variant_workflow_bundle_url}};
	#($anno->{workflow_src_url})           = keys %{$atts->{variant_workflow_source_url}};
	($anno->{project_code})               = keys %{$atts->{dcc_project_code}};
	($anno->{workflow_version})           = keys %{$atts->{variant_workflow_version}};
	($anno->{workflow_name})              = keys %{$atts->{variant_workflow_name}};
        $anno->{original_analysis_id}         = join(',',sort keys %{$atts->{original_analysis_id}});

	# harder to get attributes
	$anno->{call_type} = (grep {/\.somatic\./} @{$data->{files}}) ? 'somatic' : 'germline';

	my $wiki = $data->{wiki_content} = {};
	$wiki->{title}                = $metad->{$url}->{title};
	$wiki->{description}          = $metad->{$url}->{description};

	my $exe_urls = $data->{executed_urls} = [];
	push @$exe_urls, keys %{$atts->{variant_workflow_bundle_url}};
	push @$exe_urls, keys %{$atts->{alignment_workflow_bundle_url}};
	push @$exe_urls, keys %{$atts->{variant_workflow_source_url}};
    }


    my $json = JSON->new->pretty->encode( $data);
    #say $json;
    return $json;
}

sub download_metadata {
    my $url = shift;
    my $local_xml = shift;

    my $metad = {};

    my ($id) = $url =~ m!/([^/]+)$!;
    $id =~ s/\.xml$//;

    my $xml_path = $local_xml || download_url( $url, "$xml_dir/data_$id.xml" );

    -e $xml_path or die "Error: Local xml file $xml_path does not exist!";

    $metad->{$url} = parse_metadata($xml_path);

    return $metad;
}

sub download_alignment_metadata {
    my $url = shift;

    my ($id) = $url =~ m!/([^/]+)$!;
    my $xml_path = download_url( $url, "$xml_dir/data_$id.xml" );
    
    return parse_alignment_metadata($xml_path);
}

sub parse_metadata {
    my ($xml_path) = @_;
    my $doc        = $parser->parsefile($xml_path);
    my $m          = {};

    $m->{'analysis_id'}  = getVal( $doc, 'analysis_id' );
    $m->{'center_name'}  = getVal( $doc, 'center_name' );
    $m->{'title'}        = getVal( $doc, 'TITLE');
    $m->{'description'}  = getVal( $doc, 'DESCRIPTION');
    $m->{'platform'}     = getVal( $doc, 'platform');
    $m->{'download_url'} = getVal( $doc, 'analysis_data_uri');
    $m->{'reference_build'} = getTagAttVal( $doc, 'STANDARD', 'short_name' );
    $m->{'analysis_center'} = getTagAttVal( $doc, 'ANALYSIS', 'analysis_center' );

    push @{ $m->{'file'} },
      getValsMulti( $doc, 'FILE', "checksum,filename,filetype" );

    $m->{'analysis_attr'} = getAttrs($doc);
    return ($m);
}

sub parse_alignment_metadata {
    my ($xml_path) = @_;
    my $doc        = $parser->parsefile($xml_path);

    my $download_url = getVal( $doc, 'analysis_data_uri');
    my @bams = getValsMulti( $doc, 'FILE', "checksum,filename,filetype" );

    # gets just the output bam file name, we hope
    my $bam_file = $bams[0]->{filename};
    
    return join('/',$download_url,$bam_file);
}

sub getBlock {
    my ( $xml_file, $xpath ) = @_;

    my $block = "";
    ## use XPath parser instead of using REGEX to extract desired XML fragment, to fix issue: https://jira.oicr.on.ca/browse/PANCANCER-42
    my $xp = XML::XPath->new( filename => $xml_file )
      or die "Can't open file $xml_file\n";

    my $nodeset = $xp->find($xpath);
    foreach my $node ( $nodeset->get_nodelist ) {
        $block .= XML::XPath::XMLParser::as_string($node) . "\n";
    }

    return $block;
}

sub download_url {
    my ( $url, $path ) = @_;

    # skip if we have a local copy and a request to use it
    if ($use_cached_xml && -e $path && ! -z $path) {
	say "Using cached copy of $path";
	return $path;
    }

    my $response = run("wget -q -O $path $url");
    if ($response) {
        $ENV{PERL_LWP_SSL_VERIFY_HOSTNAME} = 0;
        $response = run("lwp-download $url $path");
        if ($response) {
            say "ERROR DOWNLOADING: $url";
            exit 1;
        }
    }
    return $path;
}

sub getVal {
    my ( $node, $key ) = @_;

    if ( $node ) {
        if ( defined( $node->getElementsByTagName($key) ) ) {
            if ( defined( $node->getElementsByTagName($key)->item(0) ) ) {
                if (
                    defined(
                        $node->getElementsByTagName($key)->item(0)
                          ->getFirstChild
                    )
                  )
                {
                    if (
                        defined(
                            $node->getElementsByTagName($key)->item(0)
                              ->getFirstChild->getNodeValue
                        )
                      )
                    {
                        return ( $node->getElementsByTagName($key)->item(0)
                              ->getFirstChild->getNodeValue );
                    }
                }
            }
        }
    }
    return (undef);
}

sub getAttrs {
    my ($node) = @_;

    my $r     = {};
    my $nodes = $node->getElementsByTagName('ANALYSIS_ATTRIBUTE');
    for ( my $i = 0 ; $i < $nodes->getLength ; $i++ ) {
        my $anode = $nodes->item($i);
        my $tag   = getVal( $anode, 'TAG' );
        my $val   = getVal( $anode, 'VALUE' );
        $r->{$tag}{$val} = 1;
    }

    return $r;
}

sub getTagAttVal {
    my $doc = shift;
    my $tag = shift;
    my $att = shift;
    my $nodes = $doc->getElementsByTagName($tag);
    my $n = $nodes->getLength;

    for (my $i = 0; $i < $n; $i++)
    {
	my $node = $nodes->item($i);
	my $val = $node->getAttributeNode($att);
	return $val->getValue if $val;
    }
    return undef;
}

sub getValsWorking {
    my ( $node, $key, $tag ) = @_;

    my @result;
    my $nodes = $node->getElementsByTagName($key);
    for ( my $i = 0 ; $i < $nodes->getLength ; $i++ ) {
        my $anode = $nodes->item($i);
        my $tag   = $anode->getAttribute($tag);
        push @result, $tag;
    }

    return @result;
}

sub getValsMulti {
    my ( $node, $key, $tags_str ) = @_;
    my @result;
    my @tags = split /,/, $tags_str;
    my $nodes = $node->getElementsByTagName($key);
    for ( my $i = 0 ; $i < $nodes->getLength ; $i++ ) {
        my $data = {};
        foreach my $tag (@tags) {
            my $anode = $nodes->item($i);
            my $value = $anode->getAttribute($tag);
            if ( defined($value) && $value ne '' ) { $data->{$tag} = $value; }
        }
        push @result, $data;
    }
    return (@result);
}

sub run {
    my ( $cmd, $do_die ) = @_;

    say "CMD: $cmd";
    my $result = system($cmd);
    if ( $do_die && $result ) {
        croak "ERROR: CMD '$cmd' returned non-zero status";
    }

    return ($result);
}

1;
