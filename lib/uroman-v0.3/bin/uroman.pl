#!/usr/bin/perl -w

# uroman  Nov. 12, 2015 - Jan. 22, 2016
# version 0.3
# Author: Ulf Hermjakob

# Usage: uroman.pl {-l [tur|ukr|yid]} < STDIN

use FindBin;
use Cwd "abs_path";
use File::Basename qw(dirname);
use File::Spec;

my $bin_dir = abs_path(dirname($0));
my $root_dir = File::Spec->catfile($bin_dir, File::Spec->updir());
my $data_dir = File::Spec->catfile($root_dir, "data");
my $lib_dir = File::Spec->catfile($root_dir, "lib");

use lib "$FindBin::Bin/../lib";
use NLP::Chinese;
use NLP::Romanizer;
use NLP::UTF8;
$chinesePM = NLP::Chinese;
$romanizer = NLP::Romanizer;
%ht = ();
%pinyin_ht = ();
$lang_code = "";
$query = "";

while (@ARGV) {
   $arg = shift @ARGV;
   if ($arg =~ /^-+(l|lc|lang-code)$/) {
      $lang_code = lc (shift @ARGV || "")
   } elsif ($arg =~ /^-+(q|query)$/) {
      $query = lc (shift @ARGV || "")
   } else {
      print STDERR "Ignoring unrecognized arg $arg\n";
   }
}

$unicode_data_filename = File::Spec->catfile($data_dir, "UnicodeData.txt");
$unicode_data_overwrite_filename = File::Spec->catfile($data_dir, "UnicodeDataOverwrite.txt");
$romanization_table_filename = File::Spec->catfile($data_dir, "romanization-table.txt");
$chinese_tonal_pinyin_filename = File::Spec->catfile($data_dir, "Chinese_to_Pinyin.txt");

$romanizer->load_unicode_data(*ht, $unicode_data_filename);
$romanizer->load_unicode_overwrite_romanization(*ht, $unicode_data_overwrite_filename);
$romanizer->load_romanization_table(*ht, $romanization_table_filename);
$chinese_to_pinyin_not_yet_loaded_p = 1;

#while (<>) {
#   my $line = $_;
#   if ($chinese_to_pinyin_not_yet_loaded_p && $chinesePM->string_contains_utf8_cjk_unified_ideograph_p($line)) {
#      $chinesePM->read_chinese_tonal_pinyin_files(*pinyin_ht, $chinese_tonal_pinyin_filename);
#      $chinese_to_pinyin_not_yet_loaded_p = 0;
#   }
#   print $romanizer->romanize($line, $lang_code, "", *ht, *pinyin_ht) . "\n";
#}

if ($chinese_to_pinyin_not_yet_loaded_p && $chinesePM->string_contains_utf8_cjk_unified_ideograph_p($query)) {
   $chinesePM->read_chinese_tonal_pinyin_files(*pinyin_ht, $chinese_tonal_pinyin_filename);
   $chinese_to_pinyin_not_yet_loaded_p = 0;
}
print $romanizer->romanize($query, $lang_code, "", *ht, *pinyin_ht) . "\n";
exit 0;

