use strict;
use warnings;

# list of e digits taken from: http://antwrp.gsfc.nasa.gov/htmltest/gifcity/e.2mil
# list of p digits taken from: http://www.geom.uiuc.edu/~huberty/math5337/groupe/digits.html

print findprime('e.2mil', '2.', 10), "\n";
print findprime('digits.html', '3.', 10), "\n";
# http://www.madras.fife.sch.uk/maths/amazingnofacts/fact018.html
# http://www.primepuzzles.net/puzzles/puzz_002.htm
print findprime('e.2mil', '2.', 9) + findprime('digits.html', '3.', 9) + 73939133, "\n";

sub findprime {
	my ($inputfile, $startrx, $digitcount) = @_;
	
	open my $fe, '<', $inputfile;
	my $digits = join('', <$fe>);
	close $fe;
	
	$digits =~ s/.*?\Q$startrx\E//s;
	$digits =~ s/\D//g;
	
	$digits =~ /\d{$digitcount}/g;
	my $sub_digits = $&;
	
	while ($digits =~ /\d/g) {
		return $sub_digits if (isprime($sub_digits));			
		$sub_digits = substr($sub_digits, 1) . $&;	
	}
	
	return -1;
}

sub isprime {
  my $num = shift;
  return 0 if ($num < 2);
  return 1 if (2 == $num);
  for (my $i = 2; $i < sqrt($num); $i += 1) 
  	{ return 0 if ($num % $i == 0); }
  return 1;
}