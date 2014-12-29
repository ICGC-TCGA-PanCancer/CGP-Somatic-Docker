package GNOS::Upload;

use warnings;
use strict;

use feature qw(say);
use autodie;
use Carp qw( croak );

use Config;
$Config{useithreads} or croak('Recompile Perl with threads to run this program.');
use threads 'exit' => 'threads_only';
use Storable 'dclone';

use constant {
   MILLISECONDS_IN_AN_HOUR => 3600000
};

#############################################################################################
# DESCRIPTION                                                                               #
#############################################################################################
#  This module is wraps the gtupload script and retries the downloads if it freezes up.     #
#############################################################################################
# USAGE: run_upload($command, $log_file, $retries, $cooldown_min, $timeout_min);                  #
#        Where $command is the full gtupload command                                        #
#############################################################################################

sub run_upload {
    my ($class, $command, $log_file, $retries, $cooldown_min, $timeout_min) = @_;

    $retries //=30;
    $timeout_min //= 60;
    $cooldown_min //= 1;

    my $timeout_mili = ($timeout_min / 60) * MILLISECONDS_IN_AN_HOUR;
    my $cooldown_sec = $cooldown_min * 60;

    say "TIMEOUT: min $timeout_min milli $timeout_mili";

    my $thr = threads->create(\&launch_and_monitor, $command, $timeout_mili);

    my $count = 0;
    my $completed = 0;

    do {

        #check if upload completed

        open my $fh, '<', $log_file;
        my @lines = <$fh>;
        close $fh;

        if ( {grep {/(100.000% complete)/s} @lines} ) {
            $completed = 1;
        }
        elsif (not $thr->is_running()) {
            if (++$count < $retries ) {
                say 'KILLING THE THREAD!!';
                # kill and wait to exit
                $thr->kill('KILL')->join();
                $thr = threads->create(\&launch_and_monitor, $command, $timeout_mili);

            }
            else {
                say "Surpassed the number of retries: $retries";
                exit 1;
            }
        }

        sleep $cooldown_sec;

    } while (not $completed);

    say "Total number of attempts: $count";
    say 'DONE';
    $thr->join() if ($thr->is_running());

    return 0;
}

sub launch_and_monitor {
    my ($command, $timeout) = @_;

    my $my_object = threads->self;
    my $my_tid = $my_object->tid;

    local $SIG{KILL} = sub { say "GOT KILL FOR THREAD: $my_tid";
                             threads->exit;
                           };
    # system doesn't work, can't kill it but the open below does allow the sub-process to be killed
    #system($cmd);
    my $pid = open my $in, '-|', "$command 2>&1";

    # TODO: there's actually a progress file e.g.
    # /mnt/seqware-oozie/oozie-15b6645f-9922-4a1b-96aa-817bd4939084/seqware-results/upload/29ef0288-0d29-481d-b5d8-0672bfe1462d/29ef0288-0d29-481d-b5d8-0672bfe1462d.gto.progress
    # could be an alternative if the below prooves unreliable
    my $time_last_uploading = time;
    my $last_reported_uploaded = 0;

    while(<$in>) {

        # just print the output for debugging reasons
        print "$_";

        my ($uploaded, $percent, $rate) = $_ =~ m/^Status:\s+(\d+.\d+|\d+| )\s+[M|G]B\suploaded\s*\((\d+.\d+|\d+| )%\s*complete\)\s*current\s*rate:\s*(\d+.\d+|\d+| )\s*[M|k]B\/s/g;

        my $md5sum = 0;
        if ($_ =~ m/^Download resumed, validating checksums for existing data/g) { $md5sum = 1; } else { $md5sum = 0; }

        if ((defined($percent) && defined($last_reported_uploaded) && $percent > $last_reported_uploaded) || $md5sum) {
            $time_last_uploading = time;
            if (defined($md5sum)) { say "  IS MD5Sum State: $md5sum"; }
            if (defined($time_last_downloading) && defined($percent)) { say "  LAST REPORTED TIME $time_last_downloading SIZE: $percent"; }
        }
        elsif (($time_last_uploading != 0) and (time - $time_last_uploading) > $timeout) {
            say 'ERROR: Killing Thread - Timed Out '.time;
            exit;
        }
        # using percent here and not amount because
        $last_reported_uploaded = $percent;
    }
}

1;
