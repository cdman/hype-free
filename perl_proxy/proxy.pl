#!/usr/bin/perl
use strict;
use warnings;
use HTTP::Proxy;
use HTTP::Proxy::HeaderFilter::simple;
use Data::Dumper;

my $proxy = HTTP::Proxy->new;
$proxy->x_forwarded_for(0);
$proxy->port(3129);
$proxy->push_filter(
	mime    => undef,
	request => HTTP::Proxy::HeaderFilter::simple->new(
		sub { $_[1]->header('X-Forwarded-For' => '10.1.2.3') },
    ),
);
$proxy->start;