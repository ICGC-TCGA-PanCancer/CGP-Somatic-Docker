#!/usr/bin/perl

use strict;

my ($link_dir) = @ARGV;

system("mkdir -p $link_dir");

check_tools();

download("$link_dir/reference", "http://s3.amazonaws.com/pan-cancer-data/pan-cancer-reference/genome.fa.gz");
download("$link_dir/reference", "http://s3.amazonaws.com/pan-cancer-data/pan-cancer-reference/genome.fa.gz.fai");
download("$link_dir/reference", "https://s3.amazonaws.com/pan-cancer-data/pan-cancer-reference/cgp_reference.tar.gz");
# need to unpack cgp_reference.tar.gz (unpack is a keyword)
expand("$link_dir/reference/", "$link_dir/reference/cgp_reference.tar.gz");

sub expand {
  my ($changeTo, $archive) = @_;
  my $res = system("tar -C $changeTo -kzxvf $archive");
  die ("Failed to unpack $archive!\n") if($res != 0);
}

sub download {
  my ($dir, $url) = @_;
  system("mkdir -p $dir");
  $url =~ /\/([^\/]+)$/;
  my $file = $1;

  print "\nDOWNLOADING HEADER:\n\n";
  my $r = `curl -I $url | grep Content-Length`;
  $r =~ /Content-Length: (\d+)/;
  my $size = $1;
  print "\n+REMOTE FILE SIZE: $size\n";
  my $fsize = -s "$dir/$file";
  print "+LOCAL FILE SIZE: $size\n";

  if (!-e "$dir/$file" || -l "$dir/$file" || -s "$dir/$file" == 0 || -s "$dir/$file" != $size) {
    my $cmd = "wget -c -O $dir/$file $url"; 
    print "\nDOWNLOADING: $cmd\nFILE: $file\n\n";
    my $r = system($cmd);
    if ($r) {
      print "+DOWNLOAD FAILED!\n";
      $cmd = "lwp-download $url $dir/$file";
      print "\nDOWNLOADING AGAIN: $cmd\nFILE: $file\n\n";
      my $r = system($cmd);
      if ($r) {
        die ("+SECOND DOWNLOAD FAILED! GIVING UP!\n");
      }
    }
  }
}


sub check_tools {
  if (system("which curl") || (system("which lwp-download") && system("which wget"))) {
    die "+TOOLS NOT FOUND: Can't find curl and/or one of lwp-download and wget, please make sure these are installed and in your path!\n";
  }
}
