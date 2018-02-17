use strict;
use warnings;
use XML::Simple;
use HTML::Entities;

binmode STDOUT, ":utf8";
my $xml = XML::Simple->new();
my $data = $xml->XMLin("google-reader-subscriptions.xml");

walker($data);

sub walker {
	my $r = shift;
	
	if ("SCALAR" eq ref($r)) {
		return;
	}
	elsif ("ARRAY" eq ref($r)) {
		walker($_) for (@$r);
	}
	elsif ("HASH" eq ref($r)) {
		if (exists($r->{xmlUrl}) && exists($r->{title})) {
			printf("<p><a href='%s'>%s</a></p>\n", 
				encode_entities($r->{xmlUrl}), encode_entities($r->{title}));
		}
		else {
			walker($_) for (values %$r);
		}
	}
}