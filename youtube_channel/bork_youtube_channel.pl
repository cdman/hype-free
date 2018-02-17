use strict;
use warnings;
use LWP::Simple;

my $youtubeCN = get(shift @ARGV);
die("Script tag not found!\n")
	unless $youtubeCN =~ /&lt;script src=&quot;(.*?)&quot;&gt;/;
my $youtubeScr = $1;
$youtubeScr =~ s/&amp;/&/g;
$youtubeScr =~ s/&amp;/&/g;
my $gmodScr = get($youtubeScr);

$gmodScr =~ s/http:\/\/\?container/http:\/\/www.gmodules.com\/ig\/ifr\?/;
print "<script type=\"text/javascript\">$gmodScr</script>\n"; 