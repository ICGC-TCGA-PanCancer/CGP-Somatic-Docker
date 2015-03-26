#!/usr/bin/perl -w
use common::sense;
use JSON;
use Data::Dumper;

use constant URL   => 'https://gtrepo-osdc-tcga.annailabs.com/cghub/metadata/analysisFull';
use constant QUERY => 'curl -s -XGET "http://pancancer.info/elasticsearch/INDEX/' .
                      'donor/_search?size=1000&fields=variant_calling_results.sanger_var' .
                      'iant_calling.gnos_repo,variant_calling_results.sanger_variant_calling.gnos_id" -d '; 


my $query_file = shift;
my $index = shift || 'p_150205030101';
my $query = QUERY;
$query =~ s/INDEX/$index/;

{
    $/ = '';
    my $query_string;
    unless ($query_file) {
	$query_string = <DATA>;
    }
    else {
	open Q, $query_file or die $!;
	$query_string = <Q>;
	close Q;
    }
    $query .= qq('$query_string');
}

my $json = JSON->new->allow_nonref;
my $json_string = `$query`;
my $json_obj = $json->decode($json_string);
my $hits = $json_obj->{hits}->{hits};
for my $hit (@$hits) {
    my $id   = $hit->{fields}->{'variant_calling_results.sanger_variant_calling.gnos_id'}->[0];
    my $repo = $hit->{fields}->{'variant_calling_results.sanger_variant_calling.gnos_repo'}->[0];
    my $url  = join('',$repo,'cghub/metadata/analysisFull/',$id);
    say $url;
}


__DATA__
{
   "query":{
      "match_all" : { }
   },
   "filter":{
      "bool":{
         "must":[
            {
               "type":{
                  "value":"donor"
               }
            },
            {
               "terms":{
                  "flags.is_normal_specimen_aligned":[
                     "T"
                  ]
               }
            },
	    {
		 "terms":{
                  "flags.is_train2_pilot":[
                     "T"
                  ]
		 }
	     }, 
            {
               "terms":{
                  "flags.are_all_tumor_specimens_aligned":[
                     "T"
                  ]
               }
            },
            {
               "terms":{
                  "flags.is_sanger_variant_calling_performed":[
                     "T"
                  ]
               }
            }
         ],
         "must_not":[
            {
               "terms":{
                  "flags.is_manual_qc_failed":[
                     "T"
                  ]
               }
            },
            {
               "terms":{
                  "flags.is_donor_blacklisted":[
                     "T"
                  ]
               }
            }
         ]
      }
   }
}
