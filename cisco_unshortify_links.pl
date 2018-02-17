use strict;
use warnings;
use threads;
use Thread::Queue;
use LWP::UserAgent;
use HTTP::Message;

$| = 1;

my $q = Thread::Queue->new();
threads->create(sub {
  my $can_accept = HTTP::Message::decodable();
  while (my $item = $q->dequeue()) {
    my $ua = LWP::UserAgent->new;
    my $response = $ua->get("http://cisco.com/web/go/$item/index.html", 
        'Accept-Encoding' => $can_accept,
    );
    my $page = $response->decoded_content;
    next unless $page;
    next unless $page =~ /var sTargetURL ="(.*?)"/;
    print "http://cisco.com/go/$item => $1\n"
  }
})->detach() for (1..10);

my @alphabet = ('a'..'z', '0'..'9');
my @word = (0);

my %seen;
while( 1 ) {
  my $w = join('', map { $alphabet[$_] } @word);
  
  while ($q->pending() > 100) { sleep 1; }
  $q->enqueue($w);
  
  for (my $i = 0; $i < scalar(@word); ++$i) {
    $word[$i] += 1;
    last if ($word[$i] < scalar(@alphabet));
    
    $word[$i] = 0;
    if ($i+1 >= scalar(@word)) { push @word, -1; }
  }
}