use strict;
use warnings;

my $header = 'RIFF';
my $input  = 'sr02.iso';
my $search_buffer = 1024 * 1024;
my $extracted_size = 200 * 1024 * 1024;

open my $f, '<', $input or die $!;
binmode $f;
my $i = 0;
while (!eof($f)) {
    seek($f, $i, 0);
    print tell($f), "\n";
    my $buffer;
    read $f, $buffer, $search_buffer;
    my $j = index $buffer, $header;
    if (-1 != $j) {
      $i += $j;
      seek $f, $i, 0;
      
      print "$i.avi\n";
      seek $f, $i, 0;
      read $f, $buffer, $extracted_size;
      open my $g, '>', "$i.avi";
      binmode $g;
      print $g $buffer;
      close $g;
      $buffer = undef;      
    }
    $i += $search_buffer - length($header);
}
close $f;
