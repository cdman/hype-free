#!/usr/bin/perl
use strict;
use warnings;
use LWP::UserAgent;
use Digest::MD5;
use Digest::SHA1;
use HTTP::Request::Common;
use Getopt::Long;
use JSON qw/from_json/;

#    Copyright 2007-2009 Cd-MaN
#    This program is free software; you can redistribute it and/or modify
#    it under the terms of the GNU General Public License as published by
#    the Free Software Foundation; either version 3 of the License, or
#    (at your option) any later version.
#
#    This program is distributed in the hope that it will be useful,
#    but WITHOUT ANY WARRANTY; without even the implied warranty of
#    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#    GNU General Public License for more details.

#don't load the file in memory at once to conserve memory
$HTTP::Request::Common::DYNAMIC_FILE_UPLOAD = 1;

'$Rev: 25 $' =~ /\d+/;
our $version = "0.4.r$&";

our $verbose;
our $display_help;
our $distribute;
our ($output_bbcode, $output_csv, $ouput_tab, $output_html, $use_ssl, $scan_site, $scan_timeout);
our $log_file;
our $last_printed_line = '';

our $sites = {
	vt      => { name => 'VirusTotal', url => 'http://www.virustotal.com/', has_ssl => 1, has_nd => 1, func => \&process_file_vt, max_size => 10_000_000, },
	jotti   => { name => 'Jotti\'s malware scan', url => 'http://virusscan.jotti.org/', has_ssl  => 0, has_nd => 0, func => \&process_file_jotti, max_size => 10_000_000, },
	virus   => { name => 'Virus.Org', url => 'http://scanner.virus.org/', has_ssl => 0, has_nd => 1, func => \&process_file_virus, max_size => 5_000_000, },
	vchief  => { name => 'VirusChief', url => 'http://www.viruschief.com/', has_ssl => 0, has_nd => 0, func => \&process_file_vchief, max_size => 10_000_000, },
	fb      => { name => 'Filterbit', url => 'http://www.filterbit.com/', has_ssl => 0, has_nd => 0, func => \&process_file_fb, max_size => 20_000_000, },
	virscan => { name => 'VirSCAN', url => 'http://www.virscan.org/', has_ssl => 0, has_nd => 0, func => \&process_file_virscan, max_size => 10_000_000, },
	nvt     => { name => 'NoVirusThanks', url => 'http://scanner.novirusthanks.org/', has_ssl => 0, has_nd => 1, func => \&process_file_nvt, max_size => 10_000_000, },
};

$scan_site = 'vt';
if (!GetOptions(
  "verbose|v"    => \$verbose,
  "help|h"       => \$display_help,
  "no-distrib|n" => \$distribute,
  "bb-code|b"    => \$output_bbcode,
  "csv|c"        => \$output_csv,
  "tab|t"        => \$ouput_tab,
  "html|m"       => \$output_html,
  "ssl|s"        => \$use_ssl,
  "log|l=s"      => \$log_file,
  "site|i=s"     => \$scan_site,
  "timeout|e=i"  => \$scan_timeout,
  ) || $display_help || 0 == scalar(@ARGV)) {
  help();
  exit;
}
$distribute = !$distribute;

our $fout;
if (defined($log_file)) {
  open $fout, '>', $log_file;
} else {
  open $fout, ">&STDOUT";
}

my $scan_site_error;
if (!$sites->{$scan_site}) {
  $scan_site_error = "Unknwon site '$scan_site'. Supported sites: " . join(' ', keys %$sites);
}
elsif (!$distribute && !$sites->{$scan_site}->{has_nd}) {
  $scan_site_error = "Site doesn't support no-distribute";
}
elsif ($use_ssl && !$sites->{$scan_site}->{has_ssl}) {
	$scan_site_error = "Site doesn't support ssl";
}
if (defined $scan_site_error) {
	print STDERR $scan_site_error, "\n";
	exit 1;
}
$scan_site = $sites->{$scan_site};

my %processed_cache;
our $browser = LWP::UserAgent->new(agent => "unoffical ovscan/v$version contact: x_at_y_or_z\@yahoo.com",
  requests_redirectable => []);
#grab the proxy settings from the enviroment variables
#if it's the case
$browser->env_proxy();
$browser->cookie_jar({});

foreach my $glob_str (@ARGV) {
  foreach my $file_name (glob($glob_str)) {
    next unless (-f $file_name);
    if (-s $file_name > $scan_site->{max_size}) {
      print STDERR "The maximum file size allowed is ", $scan_site->{max_size} / 1_000_000,
      	". The \"$file_name\" exceeds this limit!\n";
      next;
    }
    if (-s $file_name == 0) {
      print STDERR "File \"$file_name\" has size zero. Will not be scanned\n";
      next;
    }

    #don't die if an error occured, since this is a lengthy process
    #and we should process at least the files which we can
    eval {
      local $SIG{ALRM} = sub { die "Scanning timeouted!\n" };
      print STDERR "Processing file $file_name\n" if ($verbose);

      #calculate the MD5 of the file and try to find out
      #if an identical file has already been process
      #and if so, display the results from there

      my ($file_md5, $file_sha1, $file_size) = (hash_file($file_name), -s $file_name);
      print STDERR "\tMD5: $file_md5\n\tSHA1: $file_sha1\n\tFile size: $file_size bytes\n" if ($verbose);

      my $found_in_cache = 0;
      foreach my $processed_file_name (keys %processed_cache) {
        if ($file_md5 eq $processed_cache{$processed_file_name}->{'MD5'}
        	&& $file_size == $processed_cache{$processed_file_name}->{'size'}
        	&& files_identical($processed_file_name, $file_name)) {
          print STDERR "\tFile identical to \"$processed_file_name\". Will display cached results\n" if ($verbose);
          $processed_cache{$file_name} = $processed_cache{$processed_file_name};
          $found_in_cache = 1;
          last;
        }
      }
      if (!$found_in_cache) {
      	die("Tried to process element \"$file_name\" which is not a file!\n") if (! -f $file_name);
      	alarm $scan_timeout if $scan_timeout;
        my $result = $scan_site->{func}->($file_name, $use_ssl);
        alarm 0;
        $result->{md5}  = $file_md5;
        $result->{sha1} = $file_sha1;
        $result->{size} = $file_size;
        $processed_cache{$file_name} = {
          'MD5' => $file_md5,
          'size' => $file_size,
          'result' => $result
        };
      }

      if ($verbose) {
        my $infected = 0;
        foreach my $av_engine (keys %{$processed_cache{$file_name}->{'result'}}) {
          next if (('sha1' eq $av_engine) or ('md5' eq $av_engine) or ('size' eq $av_engine));
          $infected++ if ($processed_cache{$file_name}->{'result'}->{$av_engine}->{'scan_result'} ne '-');
        }
        my $total_engines = scalar(keys %{$processed_cache{$file_name}->{'result'}}) - 3;
        print STDERR "Infection count $infected out of $total_engines\n";
      }
    };
    print STDERR "\nThe following error occured while processing \"$file_name\":\n$@\n" if ($@);
  }
}

my ($max_av_engine_name, $max_av_version, $max_last_update, $max_result, $max_file_name) =
  (length "Antivirus", length "Version", length "Last Update", length "Result", 0);
my %av_engine_names;
foreach my $file_name (keys %processed_cache) {
  $max_file_name = length($file_name) if (length($file_name) > $max_file_name);
  my %file_results = %{$processed_cache{$file_name}->{'result'}};
  foreach my $engine_name (keys %file_results) {
    next if (('sha1' eq $engine_name) or ('md5' eq $engine_name) or ('size' eq $engine_name));
    $av_engine_names{$engine_name} = 1;
    $max_av_engine_name = length($engine_name) if (length($engine_name) > $max_av_engine_name);
    $max_av_version     = length($file_results{$engine_name}->{'version'}) if (length($file_results{$engine_name}->{'version'}) > $max_av_version);
    $max_last_update    = length($file_results{$engine_name}->{'last_update'}) if (length($file_results{$engine_name}->{'last_update'}) > $max_last_update);
    $max_result         = length($file_results{$engine_name}->{'scan_result'}) if (length($file_results{$engine_name}->{'scan_result'}) > $max_result);
  }
}
my @engine_name_list = sort { lc($a) cmp lc($b) } keys %av_engine_names;

if ($output_csv || $ouput_tab) {
  my $separator = ($output_csv) ? ', ' : "\t";
  my @columns = (
    "File name",
    "Size",
    "MD5",
    "SHA1",
    "Dectection count",
    "Percentage",
    (map { ($_, "Version", "Last update", "Result") } @engine_name_list)
  );
  @columns = map { quote_elements($_) } @columns if ($output_csv);

  print $fout join($separator, @columns), "\n";
}

foreach my $processed_file (sort keys %processed_cache) {
  my %file_results = %{$processed_cache{$processed_file}->{'result'}};

  my ($detection_count, $total_count) = (0, 0);
  foreach my $av_engine (@engine_name_list) {
    next if (('sha1' eq $av_engine) or ('md5' eq $av_engine) or ('size' eq $av_engine));
    $detection_count++ if ($processed_cache{$processed_file}->{'result'}->{$av_engine}->{'scan_result'} ne '-');
    $total_count++;
  }
  my $percent_detection = sprintf("%.2f%%", (0 == $total_count) ? 0.0 : $detection_count * 100.0 / $total_count);

  if ($output_bbcode) {
    print $fout "[pre]\n";
    print $fout "---[ [url]", $scan_site->{url}, "[/url] ]---------------------------\n";
    print $fout "\nFile $processed_file\n";
    print $fout "Detection: $percent_detection ($detection_count/$total_count)\n\n";

    print $fout left_align($max_av_engine_name, "Antivirus") . " " . left_align($max_av_version, "Version") . " " .
      left_align($max_last_update, "Last Update") . " " . left_align($max_result, "Result") . "\n";
    foreach my $av_engine (@engine_name_list) {
      next if (('sha1' eq $av_engine) or ('md5' eq $av_engine) or ('size' eq $av_engine));
      my $result = $file_results{$av_engine}->{'scan_result'};
      $result = "[color=red]no virus found[/color]" if ('-' eq $result);
      print $fout left_align($max_av_engine_name, $av_engine) . " " . left_align($max_av_version, $file_results{$av_engine}->{'version'}) . " " .
        left_align($max_last_update, $file_results{$av_engine}->{'last_update'}) . " $result\n";
    }

    print $fout "\nAdditional information\n\n";
    print $fout "File size: " . $file_results{'size'} . " bytes\n";
    print $fout "MD5: " . $file_results{'md5'} . "\n";
    print $fout "SHA1: " . $file_results{'sha1'} . "\n";
    print $fout "[/pre]\n\n";
  } elsif ($output_csv || $ouput_tab) {
    my @columns = (
      $processed_file,
      $file_results{'size'},
      $file_results{'md5'},
      $file_results{'sha1'},
      $detection_count,
      $percent_detection
    );
    foreach my $av_engine (@engine_name_list) {
      next if (('sha1' eq $av_engine) or ('md5' eq $av_engine) or ('size' eq $av_engine));
      push @columns, (
        $av_engine,
        $file_results{$av_engine}->{'version'},
        $file_results{$av_engine}->{'last_update'},
        $file_results{$av_engine}->{'scan_result'}
      );
    }

    my $separator = ($output_csv) ? ', ' : "\t";
    @columns = map { quote_elements($_) } @columns if ($output_csv);
    print $fout join($separator, @columns), "\n";
  } elsif ($output_html) {
  	my ($url, $name) = ($scan_site->{url}, $scan_site->{name});
    print $fout <<END;
<table>
<caption><a href="$url">$name</a> scan results</caption>
<thead>
<tr><th>File name</th><td colspan="3">$processed_file</td></tr>
<tr><th>Detection</th><td colspan="3">$percent_detection ($detection_count/$total_count)</td></tr>
<tr><th>Antivirus</th><th>Version</th><th>Last update</th><th>Result</th></tr>
</thead>
<tbody>
END
    foreach my $av_engine (@engine_name_list) {
      next if (('sha1' eq $av_engine) or ('md5' eq $av_engine) or ('size' eq $av_engine));
      print $fout "<tr><td>" . $av_engine . "</td><td>" . $file_results{$av_engine}->{'version'} . "</td><td>" .
        $file_results{$av_engine}->{'last_update'} . "</td><td>" . $file_results{$av_engine}->{'scan_result'} . "</td></tr>\n";
    }
    print $fout <<END;
<tfoot>
<tr><th colspan="4">Additional information</th></tr>
<tr>
<th>File size:</th>
<td colspan="3">$file_results{'size'} bytes</td>
</tr>
<tr>
<th>MD5:</th>
<td colspan="3">$file_results{'md5'}</td>
</tr>
<tr>
<th>SHA1:</th>
<td colspan="3">$file_results{'sha1'}</td>
</tr>
</tfoot>
</table>
END
  } else {
    print $fout "\nFile $processed_file\n";
    print $fout "Detection: $percent_detection ($detection_count/$total_count)\n\n";

    print $fout left_align($max_av_engine_name, "Antivirus") . " " . left_align($max_av_version, "Version") . " " .
      left_align($max_last_update, "Last Update") . " " . left_align($max_result, "Result") . "\n";
    foreach my $av_engine (@engine_name_list) {
      next if (('sha1' eq $av_engine) or ('md5' eq $av_engine) or ('size' eq $av_engine));
      print $fout left_align($max_av_engine_name, $av_engine) . " " . left_align($max_av_version, $file_results{$av_engine}->{'version'}) . " " .
        left_align($max_last_update, $file_results{$av_engine}->{'last_update'}) . " " . $file_results{$av_engine}->{'scan_result'} . "\n";
    }

    print $fout "\nAdditional information\n\n";
    print $fout "File size: " . $file_results{'size'} . " bytes\n";
    print $fout "MD5: " . $file_results{'md5'} . "\n";
    print $fout "SHA1: " . $file_results{'sha1'} . "\n";
  }
}

exit;

sub left_align {
  my ($size, @elements) = @_;
  my $element = join("", @elements);
  $element .= " " while (length($element) < $size);
  return $element;
}

sub right_align {
  my ($size, @elements) = @_;
  my $element = join("", @elements);
  $element = " " . $element while (length($element) < $size);
  return $element;
}

sub help {
  print <<END;
Version: $version
Usage:

$0 [options] [file masks]

Options:
  -n --no-distrib  The sample is not distributed to AV vendors
  -h --help        Displays this help
  -v --verbose     Output detailed information about the progress
  -b --bb-code     Output the result as BBCode
  -c --csv         Output the result as CSV
  -t --tab         Output the result as tab delimited file
  -m --html        Output the result as HTML
  -s --ssl         Use SSL
  -e --timeout=[t] Timeout the scanning process after a number of seconds.
                   The timeout is per file
  -l --log=[file]  Save the output (the result of the scans) to the specified
                   file
  -i --site=[site] Use a site other than the default (VirusTotal) for scanning
                   Available sites:
END

  for my $site_name (sort keys %$sites) {
  	print "                   $site_name (",
      $sites->{$site_name}->{name}, ': ', $sites->{$site_name}->{url}, ")\n";
  }

  print <<END;
File masks:
  Specifies a file or a group of files to upload and scan

END
}

#used to overwrite the lines (for progress bars, etc)
sub print_line {
  return unless ($verbose);

  print STDERR "\r", " " x (length $last_printed_line), "\r";
  $last_printed_line = join("", @_);
  print STDERR $last_printed_line;
}

sub add_upload_progress {
  my ($file_upload_request, $file_name) = @_;
  return unless ($verbose);

  my ($total_size, $sent_size) = (1 + -s $file_name, 0);

  my $content_generator = $file_upload_request->content();
  die ("Content expected to be code reference!\n") unless ref($content_generator) eq "CODE";
  print STDERR "\n";
  $file_upload_request->content(
  	sub {
  	  my $chunk = &$content_generator();
  	  if (defined $chunk) {
  	  	$sent_size += length $chunk;
  	  	my $percentage = $sent_size * 100 / $total_size;
  	  	$percentage = 100 if ($percentage > 100);
  	  	print_line(sprintf("%.2f%% uploaded", $percentage));
  	  }
  	  return $chunk;
    }
  );
}

sub visual_wait {
  my $wait_timeout = shift;
  my $last_line = $last_printed_line;
  $last_line = substr($last_line, 0, 27) . '...'
  	if (length $last_line > 30);
  for (1..$wait_timeout) {
    print_line("Waiting for ", ($wait_timeout - $_), " more seconds [", $last_line, "]");
    sleep 1;
  }
  print_line($last_line);
}

sub process_file_vt {
  my ($file_name, $use_ssl) = @_;
  my $protocol = $use_ssl ? 'https' : 'http';

  my $file_upload_request = POST "$protocol://www.virustotal.com/vt/en/recepcionf",
    [
      'archivo' => [ $file_name ],
      'distribuir' => $distribute
    ],
    'Content_Type' => 'form-data';

  add_upload_progress($file_upload_request, $file_name);

  my $response = $browser->request($file_upload_request);
  print_line('');

  die("Request failed: " . $response->status_line . "\n") unless $response->header('Location');

  die ("Response header does not contain expected location header!\n")
    unless ($response->header('Location') =~ /[\/\?]([a-f0-9\-]+)$/i);
  my $file_id = $1;

  if ($response->header('Location') =~ /reanalisis/) {
  	print STDERR "File seen before, requesting reanalisys\n" if ($verbose);
  	my $reanalisys_request = GET "$protocol://www.virustotal.com/vt/en/reanaliza?$file_id",
  		'Referrer' => "$protocol://www.virustotal.com/vt/en/recepcionf";
  	$response = $browser->request($reanalisys_request);
  	die("Reanalisys request failed: " . $response->status_line . "\n") unless $response->header('Location');
  }

  print STDERR "Upload finished, waiting for scanning\n" if ($verbose);

  my $scan_request = GET "$protocol://www.virustotal.com/vt/en/resultado?$file_id-0-0",
    'Referrer' => "$protocol://www.virustotal.com/resultado.html?$file_id";

  my %results;
  my $wait_timeout = 30; #in seconds;
  while (1) {
    $response = $browser->request($scan_request);
    my $response_parsed = from_json($response->content());
    if ('ENCOLADO' eq $response_parsed->[0]) {
      print_line(sprintf("Enqued in position %d. Estimated start time between %d and %d %s",
        $response_parsed->[3]->[0], $response_parsed->[3]->[1], $response_parsed->[3]->[2], $response_parsed->[3]->[3]));
    } elsif ('ANALIZANDO' eq $response_parsed->[0]) {
      print_line(sprintf("Scanning. Scanned with %d engines", scalar(@{$response_parsed->[3]})));
    } elsif ('REFRESCAR' eq $response_parsed->[0]) {
      print_line("Server requested us to wait");
      my $wait_amount = $response_parsed->[1];
      print STDERR "\n" if ($verbose);
      visual_wait($wait_timeout);
    } elsif ('TERMINADO' eq $response_parsed->[0]) {
      print_line("Scanning done");
      foreach my $result (@{$response_parsed->[2]}) {
        $results{$result->[0]} = {
          version     => $result->[1],
          last_update => $result->[2],
          scan_result => $result->[3]
        };
      }
      last;
    } else {
      die("Unexpected status returned by server: " . $response_parsed->[0] . "\n");
    }

    visual_wait($wait_timeout);
  };

  print STDERR "\n" if ($verbose);

  return \%results;
}

sub process_file_nvt {
  my ($file_name) = @_;

  my $file_upload_request = POST "http://scanner.novirusthanks.org/",
    [
      'upfile' => [ $file_name ],
      'submitav' => $distribute,
      'submitfile' => 'Submit File',
    ],
    'Content_Type' => 'form-data';

  add_upload_progress($file_upload_request, $file_name);

  my $response = $browser->request($file_upload_request);
  print_line('');

  die("Request failed: " . $response->status_line . "\n") unless $response->header('Location');

  die ("Response header does not contain expected location header!\n")
    unless ($response->header('Location') =~ /session=([a-f0-9\-]+)$/i);
  my $sid = $1;
  my $scan_request = GET "http://scanner.novirusthanks.org/index.php?session=$sid";
  my $wait_timeout = 24; #in seconds;
  my %results;
  while (1) {
    $response = $browser->request($scan_request);
    die("Failed to parse response\n")
      unless ($response->content() =~ /<tr><th>STATUS:<\/th><td.*?>(.*?)<\/td><\/tr>/);
    if ($1 eq 'Scanning...') {
      print_line("Scanning...");
    }
    elsif ($1 eq 'Finished') {
      die("Failed to parse result\n")
        unless ($response->content() =~ /<!-- Virus information table -->.*?<table.*?<tbody>(.*?)<\/tbody>/is);
      my $result = $1;
      while ($result =~ /<tr><td>(.*?)<\/td><td>(.*?)<\/td><td>(.*?)<\/td><td>(.*?)<\/td><\/tr>/ig) {
        my ($av, $sig_ver, $engine_ver, $detection) = ($1, $2, $3, $4);
        $detection =~ s/<.*?>//g;
        $results{$av} = {
          version     => $engine_ver,
          last_update => $sig_ver,
          scan_result => $detection,
        };
      }
      last;
    }
    else {
      die("Unknown status: $1\n");
    }
    visual_wait($wait_timeout);
  }

  print STDERR "\n" if ($verbose);
  return \%results;
}

sub process_file_jotti {
  my ($file_name) = @_;

  my $file_upload_request = POST "http://virusscan.jotti.org/processupload.php",
    [ 'scanfile' => [ $file_name ] ],
    'Content_Type' => 'form-data';
  
  add_upload_progress($file_upload_request, $file_name);
  
  my $response = $browser->request($file_upload_request);
  print_line('');

  die("Request failed: " . $response->status_line . "\n") unless $response->is_success;
  die("Failed to find link in response\n") 
  	unless $response->content() =~ /top.location.href\s*=\s*"(.*\/([a-f0-9]+))";/i;
  my ($scan_url, $start_scan) = ($1, $2);
  die("SessionID not found in cookie!\n")
  	unless $response->header('Set-Cookie') =~ /sessionid=([a-f0-9]+)/; 
  my ($sessionid) = $1;
  
  my $wait_timeout = 30;
  
  my $start_scan_req = GET "http://virusscan.jotti.org/nestor/startscan.php?scanid=" 
  	. $start_scan . "&sessionid=" . $sessionid . "&output=json";
  while (1) {
  	$response = $browser->request($start_scan_req);
  	my $response_parsed = from_json($response->content());
  	if ($response_parsed->{code} eq 'SCAN_SPAWN_OK') {
  		print_line('Starting scan');
  		last;
  	}
  	
  	my $queue_pos = $response_parsed->{placeinqueue} ? $response_parsed->{placeinqueue} : '?';
  	print_line('Waiting. Place in the queue: ' . $queue_pos);
  	visual_wait($wait_timeout);
  }
  
  my %results;
  print_line('Scanning');
  for (1..6) {
  	my $get_progress_req = GET "http://virusscan.jotti.org/nestor/getscanprogress.php?sessionid="
  		. $sessionid . "&lang=en&scanid=" . $start_scan . "&timestamp=" . time() . "&output=json";
  	$response = $browser->request($get_progress_req);
  	my $response_parsed = from_json($response->content());
  	%results = ();
  	for (@{$response_parsed->{scanner}}) {
  		next unless $_->{scanok};
  		$_->{virusname} = '-'
  			if ('' eq $_->{virusname});
  		$results{$_->{scannerid}} = {
          version     => 'unknown',
          last_update => $_->{lastupdate},
          scan_result => $_->{virusname},
        };
  	}
  	visual_wait(10);
  }

  print STDERR "\n" if ($verbose);
  return \%results;
}

sub process_file_virus {
  my ($file_name) = @_;
  die("Virus.Org seems to have some difficulties at the moment (they block at 72% done). Disabled temporarily.\n");

  my $response;
  my $id_request = GET 'http://scanner.virus.org/advanced';
  $response = $browser->request($id_request);
  die("ID request failed: " . $response->status_line . "\n") unless $response->is_success;
  die("Response didn't contain upload_id") unless $response->content() =~ /\/upload\?upload_id=[^"]+/;

  my $file_upload_request = POST "http://scanner.virus.org$&",
    [
      'vscan[uploaded_data]' => [ $file_name ],
      'vscan[email]'  => '',
      'vscan[distav]' => $distribute ? 1 : 0,
      'commit' => 'Upload and Scan',
    ],
    'Content_Type' => 'form-data';

  add_upload_progress($file_upload_request, $file_name);

  $response = $browser->request($file_upload_request);
  print_line('');
  die("Upload request failed: " . $response->status_line . "\n") unless $response->header('Location');
  die("Upload request returned unknown redirect") unless $response->header('Location') =~ /f=([a-z0-9]+)&r=([a-z0-9]+)/i;
  my ($f, $r) = ($1, $2);
  my $rpage_request = GET $response->header('Location');
  $response = $browser->request($rpage_request);
  die("Response page request failed: " . $response->status_line . "\n") unless $response->is_success;

  print STDERR "Upload finished, waiting for scanning\n" if ($verbose);

  my $scan_request = POST "http://scanner.virus.org/scan/progress/$r/$f", [],
    'Referrer' => "http://scanner.virus.org/scan?f=$f&r=$r",
    'Accept' => 'text/javascript, text/html, application/xml, text/xml, */*',
    'X-requested-with' => 'XMLHttpRequest';
  my %results;
  my $wait_timeout = 10; #in seconds;
  while (1) {
  	$response = $browser->request($scan_request);
  	my $response_content = $response->content();
  	$response_content =~ /statusBar\(\s*".*?"\s*,\s*"(.*?)".*?setPercentage\(\s*".*?"\s*,\s*(\d+)/s;
  	print_line("Status: $1, Percent complete: $2%");
  	visual_wait($wait_timeout);
  }
}

sub process_file_vchief {
  my ($file_name) = @_;

  my $response;
  my $id_request = GET 'http://www.viruschief.com/';
  $response = $browser->request($id_request);

  my $file_upload_request = POST 'http://www.viruschief.com/index.html' ,
    [
      uploadfile    => [ $file_name ],
      MAX_FILE_SIZE => 10485760,
      submit_file   => 'Scan',
    ],
    'Content_Type' => 'form-data',
    'Referer'      => 'http://www.viruschief.com/';

  add_upload_progress($file_upload_request, $file_name);

  $response = $browser->request($file_upload_request);
  print_line('');
  die("Session ID not found in the reply") unless $response->content() =~ /session_id\s*=\s*["']([a-z0-9]+?)["']/i;
  my $session_id = $1;
  print STDERR "Upload finished, waiting for scanning\n" if ($verbose);

  my $scan_request = POST 'http://www.viruschief.com/ajax_result_update.html',
  	['session_id' => $session_id];
  my $wait_timeout = 10; #in seconds;
  my $response_rx = '<name>(.*?)</name><version_engine>(.*?)</version_engine><version_definition>(.*?)</version_definition><infection>(.*?)</infection>';
  while (1) {
  	$response = $browser->request($scan_request);
  	my $response_content = $response->content();
  	my %results;
  	while ($response_content =~ /$response_rx/g) {
	  $results{$1} = {
	    version     => $2,
	    last_update => $3,
	    scan_result => ('Nothing found' eq $4) ? '-' : $4,
	  };
  	}
  	print_line('Scanned with engines: ', scalar(keys %results));
  	if ($response_content =~ /<last>yes<\/last>/i) {
  		print STDERR "\n" if ($verbose);
  		return \%results;
  	}
  	else {
  		visual_wait($wait_timeout);
  	}
  }
}

sub process_file_fb {
  my ($file_name) = @_;
  my $response;

  my @chars=('a'..'z','A'..'Z','0'..'9','_');
  my $session_id = '';
  $session_id .= $chars[rand @chars] for (1..32);
  my $file_upload_request = POST 'http://www.filterbit.com/uploader.php' ,
    [
      FileUpload            => [ $file_name ],
      APC_UPLOAD_PROGRESS   => $session_id,
    ],
    'Content_Type' => 'form-data';

  add_upload_progress($file_upload_request, $file_name);

  $response = $browser->request($file_upload_request);
  print_line('');
  die("Upload request failed: " . $response->status_line . "\n") unless $response->is_success;

  my $scan_request = GET "http://www.filterbit.com/results.cgi?uid=$session_id&type=scanresults";
  my $wait_timeout = 10; #in seconds;
  while (1) {
  	$response = $browser->request($scan_request);
  	my $response_content = $response->content();
  	my %results;
  	while ($response_content =~ /<td nowrap="">(.*?)<\/td>.*?<td nowrap="">(.*?)<\/td>.*?<td>(.*?)<\/td>/sg) {
  	  my ($engine, $version, $result) = ($1, $2, $3);
  	  $result =~ s/[ \n\r]+//g;
	  $results{$engine} = {
	    version     => 'unknown',
	    last_update => $version,
	    scan_result => $result,
	  };
  	}
  	print_line('Scanned with engines: ', scalar(keys %results));
  	if ($response_content =~ /Final Result/i) {
  		print STDERR "\n" if ($verbose);
  		return \%results;
  	}
  	else {
  		visual_wait($wait_timeout);
  	}
  }
}

sub process_file_virscan {
  my ($file_name) = @_;
  my $response;
  die("Virscan.Org seems to have some difficulties at the moment (they are \n");

  my $id_request = GET 'http://virscan.org/';
  $response = $browser->request($id_request);
  die("Scan ID not found in the reply") unless $response->content() =~ /scanid \s*=\s*["']([a-z0-9]+)["']/i;
  my $scan_id = $1;

  my $file_upload_request = POST "http://virscan.org/cgi-bin/upload.cgi?sid=$scan_id&lang=en",
    [
      file            => [ $file_name ],
      upload_button   => 'Upload',
    ],
    'Content_Type' => 'form-data';
  add_upload_progress($file_upload_request, $file_name);

  $response = $browser->request($file_upload_request);
  print_line('');
  die("Upload request failed: " . $response->status_line . "\n") unless $response->content() =~ /sid=([a-z0-9]+)/i;
  my $check_cgi = $1;

  my $status_request = GET "http://virscan.org/cgi-bin/check.cgi?sid=$check_cgi&lang=en";
  $response = $browser->request($status_request);
  my $response_content = $response->content();
  if ($response_content =~ /parent.scanid.*?"([a-f0-9]+)".*?parent.md5.*?"([a-f0-9]+)"/si) {
  	print STDERR "File seen before, requesting reanalisys\n" if ($verbose);
  }
  elsif ($response_content =~ /sid=([a-z0-9]+).*?md5=([a-z0-9]+)/i) {
  	# normal scan
  }
  else {
  	die("Final check failed: " . $response->status_line . "\n");
  }
  my ($scan_cgi, $scan_md5) = ($1, $2);

  my $complete_data = '';
  $status_request = GET "http://virscan.org/cgi-bin/scan.cgi?sid=$scan_cgi&md5=$scan_md5&lang=en ";
  $response = $browser->request($status_request, sub {
  	my $data = shift;
  	$complete_data .= $data;
  	my %engines;
  	$engines{$1} = 1 while ($complete_data =~ /scan_scanner.*?"(.*)"/g);
  	print_line("Scanned with " . scalar(keys %engines) . " engine(s)");
  });

  my %results;
  while ($complete_data =~ /scan_scanner.*?"(.*)"/g) {
  	my $result = $1;
  	$result =~ /(.*?)\s*<.*?>(.*)</;
	$results{$1} = {
	  version     => 'unknown',
	  last_update => 'unknown',
	  scan_result => ('Nothing found' eq $2) ? '-' : $2,
	};
  }

  print STDERR "\n" if ($verbose);
  return \%results;
}

sub hash_file {
  my $file_name = shift;
  open my $f_hash, '<', $file_name or die("Failed to open \"$file_name\" to calculate its MD5: $!\n");
  binmode $f_hash;

  my ($ctx, $cty) = (Digest::MD5->new, Digest::SHA1->new);
  my $buffer;
  while (read $f_hash, $buffer, 4096) {
  	$ctx->add($buffer);
  	$cty->add($buffer);
  }

  close $f_hash;
  return ($ctx->hexdigest, $cty->hexdigest);
}

sub files_identical {
  my ($file_name1, $file_name2) = @_;
  return 0 if (! -f $file_name1 || ! -f $file_name2);
  return 0 if (-s $file_name1 != -s $file_name2);

  open my $f1, '<', $file_name1 or die("Failed to open \"$file_name1\": $!\n"); binmode $f1;
  open my $f2, '<', $file_name2 or die("Failed to open \"$file_name1\": $!\n"); binmode $f2;

  while (1) {
    my ($buff1, $buff2, $buff1_size, $buff2_size);
    $buff1_size = read $f1, $buff1, 4096;
    die("Failed to read from \"$file_name1\": $!\n") unless defined $buff1_size;
    $buff2_size = read $f2, $buff2, 4096;
    die("Failed to read from \"$file_name2\": $!\n") unless defined $buff2_size;

    die("Something went horribly wrong! Buffer sizes should be equal!") if ($buff1_size != $buff2_size);
    last if ($buff1_size == 0);

    if ($buff1 ne $buff2) {
      close $f1; close $f2;
      return 0;
    }
  }

  close $f1; close $f2;
  return 1;
}

#used to quote elements for csv's
sub quote_elements {
  if (/[\"\t]/) {
    s/\"/\"\"/g;
    return "\"$_\"";
  } else {
    return $_;
  }
}

