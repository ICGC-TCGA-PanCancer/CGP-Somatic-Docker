package GNOS::Download;

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
    MILLISECONDS_IN_AN_HOUR => 3600000,
};

#############################################################################################
# DESCRIPTION                                                                               #
#############################################################################################
#  This module is wraps the gtdownload script and retries the downloads if it freezes up.   #
#############################################################################################
# USAGE: run_upload($command, $file, $retries, $cooldown_min, $timeout_min);                #
#        Where the command is the full gtdownlaod command                                   #
#############################################################################################

sub run_download {
    my ($class, $command, $file, $retries, $cooldown_min, $timeout_min) = @_;

    $retries //= 30;
    $timeout_min //= 60;
    $cooldown_min //= 1;

    my $timeout_mili = ($timeout_min / 60) * MILLISECONDS_IN_AN_HOUR;
    my $cooldown_sec = $cooldown_min * 60;

    say "TIMEOUT: min $timeout_min milli $timeout_mili";

    my $thr = threads->create(\&launch_and_monitor, $command, $timeout_mili);

    my $count = 0;
    while( not (-e $file) ) {
        say "FILE: $file DOES NOT EXIST... WAITING FOR THREAD TO COMPLETE...";
        if ( not $thr->is_running()) {
            if (++$count < $retries ) {
                say 'ERROR: THREAD NOT RUNNING BUT OUTPUT MISSING, RESTARTING THE THREAD!!';
                # kill and wait to exit
                $thr->kill('KILL')->join();
                $thr = threads->create(\&launch_and_monitor, $command, $timeout_mili);
            }
            else {
               say "ERROR: Surpassed the number of retries: $retries with count $count, EXITING!!";
               exit 1;
            }
        }

        sleep $cooldown_sec;
    }

    say "OUTPUT FILE $file EXISTS AND THREAD EXITED NORMALLY, Total number of tries: $count";
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

    say "THREAD STARTING, CMD: $command TIMEOUT: $timeout";

    my $pid = open my $in, '-|', "$command 2>&1";

    my $time_last_downloading = 0;
    my $last_reported_size = 0;
    while(<$in>) {

        # just print the output for debugging reasons
        print "$_";

        # these will be defined if the program is actively downloading
        my ($size, $percent, $rate) = $_ =~ m/^Status:\s*(\d+.\d+|\d+|\s*)\s*[M|G]B\s*downloaded\s*\((\d+.\d+|\d+|\s)%\s*complete\)\s*current rate:\s+(\d+.\d+|\d+| )\s+MB\/s/g;

	# override, let's use percent for size because it's always increasing whereas the units of the size change and this will interfere with the > $last_reported_size
	$size = $percent;

        # test to see if the thread is md5sum'ing after an earlier failure
        # this actually doesn't produce new lines, it's all on one line but you
        # need to check since the md5sum can take hours and this would cause a timeout
        # and a kill when the next download line appears since it could be well past
        # the timeout limit
        my $md5sum = 0;
        if ($_ =~ m/^Download resumed, validating checksums for existing data/g) { $md5sum = 1; } else { $md5sum = 0; }

        if ((defined($size) &&  defined($last_reported_size) && $size > $last_reported_size) || $md5sum) {
            $time_last_downloading = time;
            say "UPDATING LAST DOWNLOAD TIME: $time_last_downloading";
            if (defined($last_reported_size) && defined($size)) { say "  LAST REPORTED SIZE $last_reported_size SIZE: $size"; }
            if (defined($md5sum)) { say "  IS MD5Sum State: $md5sum"; }
        }
        elsif (($time_last_downloading != 0) and ( (time - $time_last_downloading) > $timeout) ) {
            say 'ERROR: Killing Thread - Timed out '.time;
            exit;
        }
        $last_reported_size = $size;
    }
}

1;
