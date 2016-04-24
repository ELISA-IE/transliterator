################################################################
#                                                              #
# UTF8                                                         #
#                                                              #
################################################################

package NLP::UTF8;

sub new {
   local($caller) = @_;

   my $object = {};
   my $class = ref( $caller ) || $caller;
   bless($object, $class);
   return $object;
}

sub unicode_string2string {
# input: string that might contain unicode sequences such as "U+0627"
# output: string in pure utf-8
   local($caller,$s) = @_;

   my $pre; 
   my $unicode; 
   my $post; 
   my $r1; 
   my $r2; 
   my $r3;

   ($pre,$unicode,$post) = ($s =~ /^(.*)U\+([0-9A-F][0-9A-F][0-9A-F][0-9A-F])(.*)$/);
   return $s unless defined($post);
   $r1 = $caller->unicode_string2string($pre);
   $r2 = $caller->unicode_hex_string2string($unicode);
   $r3 = $caller->unicode_string2string($post);
   $result = $r1 . $r2 . $r3;
   return $result;
}

sub unicode_hex_string2string {
# input: "0627" (interpreted as hex code)
# output: utf-8 string for Arabic letter alef
   local($caller,$unicode) = @_;
   return "" unless defined($unicode);
   my $d = hex($unicode);
   return $caller->unicode2string($d);
}

sub unicode2string {
# input: non-neg integer, e.g. 0x627
# output: utf-8 string for Arabic letter alef
   local($caller,$d) = @_;
   return "" unless defined($d) && $d >= 0;
   return sprintf("%c",$d) if $d <= 0x7F;

   my $lastbyte1 = ($d & 0x3F) | 0x80;
   $d >>= 6;
   return sprintf("%c%c",$d | 0xC0, $lastbyte1) if $d <= 0x1F;

   my $lastbyte2 = ($d & 0x3F) | 0x80;
   $d >>= 6;
   return sprintf("%c%c%c",$d | 0xE0, $lastbyte2, $lastbyte1) if $d <= 0xF;

   my $lastbyte3 = ($d & 0x3F) | 0x80;
   $d >>= 6;
   return sprintf("%c%c%c%c",$d | 0xF0, $lastbyte3, $lastbyte2, $lastbyte1) if $d <= 0x7;

   my $lastbyte4 = ($d & 0x3F) | 0x80;
   $d >>= 6;
   return sprintf("%c%c%c%c%c",$d | 0xF8, $lastbyte4, $lastbyte3, $lastbyte2, $lastbyte1) if $d <= 0x3;

   my $lastbyte5 = ($d & 0x3F) | 0x80;
   $d >>= 6;
   return sprintf("%c%c%c%c%c%c",$d | 0xFC, $lastbyte5, $lastbyte4, $lastbyte3, $lastbyte2, $lastbyte1) if $d <= 0x1;
   return ""; # bad input
}

sub html2utf8 {
   local($caller, $string) = @_;

   return $string unless $string =~ /\&\#\d{3,5};/;

   my $prev = "";
   my $s = $string;
   while ($s ne $prev) {
      $prev = $s;
      ($pre,$d,$post) = ($s =~ /^(.*)\&\#(\d+);(.*)$/);
      if (defined($d) && ((($d >= 160) && ($d <= 255))
                       || (($d >= 1500) && ($d <= 1699))
                       || (($d >= 19968) && ($d <= 40879)))) {
         $html_code = "\&\#" . $d . ";";
         $utf8_code = $caller->unicode2string($d);
         $s =~ s/$html_code/$utf8_code/;
      }
   }
   return $s;
}

sub xhtml2utf8 {
   local($caller, $string) = @_;

   return $string unless $string =~ /\&\#x[0-9a-fA-F]{2,5};/;

   my $prev = "";
   my $s = $string;
   while ($s ne $prev) {
      $prev = $s;
      if (($pre, $html_code, $x, $post) = ($s =~ /^(.*)(\&\#x([0-9a-fA-F]{2,5});)(.*)$/)) {
         $utf8_code = $caller->unicode_hex_string2string($x);
         $s =~ s/$html_code/$utf8_code/;
      }
   }
   return $s;
}

sub utf8_marker {
   return sprintf("%c%c%c\n", 0xEF, 0xBB, 0xBF);
}

sub enforcer {
# input: string that might not conform to utf-8
# output: string in pure utf-8, with a few "smart replacements" and possibly "?"
   local($caller,$s,$no_repair) = @_;

   my $ascii;
   my $utf8;
   my $rest;

   return $s if $s =~ /^[\x00-\x7F]*$/;

   $no_repair = 0 unless defined($no_repair);
   $orig = $s;
   $result = "";

   while ($s ne "") {
      ($ascii,$rest) = ($s =~ /^([\x00-\x7F]+)(.*)$/);
      if (defined($ascii)) {
	 $result .= $ascii;
	 $s = $rest;
	 next;
      }
      ($utf8,$rest) = ($s =~ /^([\xC0-\xDF][\x80-\xBF])(.*)$/);
      ($utf8,$rest) = ($s =~ /^([\xE0-\xEF][\x80-\xBF][\x80-\xBF])(.*)$/) 
	 unless defined($rest);
      ($utf8,$rest) = ($s =~ /^([\xF0-\xF7][\x80-\xBF][\x80-\xBF][\x80-\xBF])(.*)$/)
	 unless defined($rest);
      ($utf8,$rest) = ($s =~ /^([\xF8-\xFB][\x80-\xBF][\x80-\xBF][\x80-\xBF][\x80-\xBF])(.*)$/) 
	 unless defined($rest);
      if (defined($utf8)) {
	 $result .= $utf8;
	 $s = $rest;
	 next;
      }
      ($c,$rest) = ($s =~ /^(.)(.*)$/);
      if (defined($c)) {
	 if    ($no_repair)   { $result .= "?"; }
	 elsif ($c =~ /\x85/) { $result .= "..."; }
	 elsif ($c =~ /\x91/) { $result .= "'"; }
	 elsif ($c =~ /\x92/) { $result .= "'"; }
	 elsif ($c =~ /\x93/) { $result .= $caller->unicode2string(0x201C); }
	 elsif ($c =~ /\x94/) { $result .= $caller->unicode2string(0x201D); }
	 elsif ($c =~ /[\xC0-\xFF]/) {
	    $c2 = $c;
	    $c2 =~ tr/[\xC0-\xFF]/[\x80-\xBF]/;
	    $result .= "\xC3$c2";
	 } else {
	    $result .= "?";
	 }
	 $s = $rest;
	 next;
      }
      $s = "";
   }
   $result .= "\n" if ($orig =~ /\n$/) && ! ($result =~ /\n$/);
   return $result;
}

sub split_into_utf8_characters {
# input: utf8 string
# output: list of sub-strings, each representing a utf8 character
   local($caller,$string,$group_control) = @_;

   @characters = ();
   $end_of_token_p_string = "";
   $skipped_bytes = "";
   $group_control = "" unless defined($group_control);
   $group_ascii_numbers = ($group_control =~ /ASCII numbers/);
   $group_ascii_spaces = ($group_control =~ /ASCII spaces/);
   $group_ascii_punct = ($group_control =~ /ASCII punct/);
   $group_ascii_chars = ($group_control =~ /ASCII chars/);
   $return_only_chars = ($group_control =~ /return only chars/);
   if ($group_control =~ /ASCII all/) {
      $group_ascii_numbers = 1;
      $group_ascii_spaces = 1;
      $group_ascii_chars = 1;
      $group_ascii_punct = 1;
   }
   $string .= " ";
   while ($string =~ /\S/) {
      # one-character UTF-8 = ASCII
      if ($string =~ /^[\x00-\x7F]/) {
	 if ($group_ascii_numbers && ($string =~ /^[12]\d\d\d\.[01]?\d.[0-3]?\d([^0-9].*)?$/)) {
	    ($date) = ($string =~ /^(\d\d\d\d\.\d?\d.\d?\d)([^0-9].*)?$/);
	    push(@characters,$date);
	    $string = substr($string, length($date));
	 } elsif ($group_ascii_numbers && ($string =~ /^\d/)) {
	    ($number) = ($string =~ /^(\d+(,\d\d\d)*(\.\d+)?)/);
	    push(@characters,$number);
	    $string = substr($string, length($number));
	 } elsif ($group_ascii_spaces && ($string =~ /^(\s+)/)) {
	    ($space) = ($string =~ /^(\s+)/);
	    $string = substr($string, length($space));
	 } elsif ($group_ascii_punct && (($punct_seq) = ($string =~ /^(-+|\.+|[:,%()"])/))) {
	    push(@characters,$punct_seq);
	    $string = substr($string, length($punct_seq));
	 } elsif ($group_ascii_chars && (($word) = ($string =~ /^(\$[A-Z]*|[A-Z]{1,3}\$)/))) {
	    push(@characters,$word);
	    $string = substr($string, length($word));
	 } elsif ($group_ascii_chars && (($abbrev) = ($string =~ /^((?:Jan|Feb|Febr|Mar|Apr|Jun|Jul|Aug|Sep|Sept|Oct|Nov|Dec|Mr|Mrs|Dr|a.m|p.m)\.)/))) {
	    push(@characters,$abbrev);
	    $string = substr($string, length($abbrev));
	 } elsif ($group_ascii_chars && (($word) = ($string =~ /^(second|minute|hour|day|week|month|year|inch|foot|yard|meter|kilometer|mile)-(?:long|old)/i))) {
	    push(@characters,$word);
	    $string = substr($string, length($word));
	 } elsif ($group_ascii_chars && (($word) = ($string =~ /^(zero|one|two|three|four|five|six|seven|eight|nine|ten|eleven|twelve|thirteen|fourteen|fifteen|sixteen|seventeen|eighteen|nineteen|twenty|thirty|forty|fifty|sixty|seventy|eighty|ninety|hundred|thousand|million|billion|trillion)-/i))) {
	    push(@characters,$word);
	    $string = substr($string, length($word));
	 } elsif ($group_ascii_chars && (($word) = ($string =~ /^([a-zA-Z]+)(?:[ ,;%?|()"]|'s |' |\. |\d+[:hms][0-9 ])/))) {
	    push(@characters,$word);
	    $string = substr($string, length($word));
	 } elsif ($group_ascii_chars && ($string =~ /^([\x21-\x27\x2A-\x7E]+)/)) { # exclude ()
	    ($ascii) = ($string =~ /^([\x21-\x27\x2A-\x7E]+)/);  # ASCII black-characters
	    push(@characters,$ascii);
	    $string = substr($string, length($ascii));
	 } elsif ($group_ascii_chars && ($string =~ /^([\x21-\x7E]+)/)) {
	    ($ascii) = ($string =~ /^([\x21-\x7E]+)/);  # ASCII black-characters
	    push(@characters,$ascii);
	    $string = substr($string, length($ascii));
	 } elsif ($group_ascii_chars && ($string =~ /^([\x00-\x7F]+)/)) {
	    ($ascii) = ($string =~ /^([\x00-\x7F]+)/);
	    push(@characters,$ascii);
	    $string = substr($string, length($ascii));
	 } else {
	    push(@characters,substr($string, 0, 1));
	    $string = substr($string, 1);
	 }

      # two-character UTF-8
      } elsif ($string =~ /^[\xC0-\xDF][\x80-\xBF]/) {
	 push(@characters,substr($string, 0, 2));
	 $string = substr($string, 2);

      # three-character UTF-8
      } elsif ($string =~ /^[\xE0-\xEF][\x80-\xBF][\x80-\xBF]/) {
	 push(@characters,substr($string, 0, 3));
	 $string = substr($string, 3);

      # four-character UTF-8
      } elsif ($string =~ /^[\xF0-\xF7][\x80-\xBF][\x80-\xBF][\x80-\xBF]/) {
	 push(@characters,substr($string, 0, 4));
	 $string = substr($string, 4);

      # five-character UTF-8
      } elsif ($string =~ /^[\xF8-\xFB][\x80-\xBF][\x80-\xBF][\x80-\xBF][\x80-\xBF]/) {
	 push(@characters,substr($string, 0, 5));
	 $string = substr($string, 5);

      # six-character UTF-8
      } elsif ($string =~ /^[\xFC-\xFD][\x80-\xBF][\x80-\xBF][\x80-\xBF][\x80-\xBF][\x80-\xBF]/) {
	 push(@characters,substr($string, 0, 6));
	 $string = substr($string, 6);

      # not a UTF-8 character
      } else {
         $skipped_bytes .= substr($string, 0, 1);
	 $string = substr($string, 1);
      }
      
      $end_of_token_p_string .= ($string =~ /^\S/) ? "0" : "1" 
	 if $#characters >= length($end_of_token_p_string);
   }
   return ($return_only_chars) ? @characters : ($skipped_bytes, $end_of_token_p_string, @characters);
}

sub max_substring_info {
   local($caller,$s1,$s2,$info_type) = @_;

   ($skipped_bytes1, $end_of_token_p_string1, @char_list1) = $caller->split_into_utf8_characters($s1);
   ($skipped_bytes2, $end_of_token_p_string2, @char_list2) = $caller->split_into_utf8_characters($s2);
   return 0 if $skipped_bytes1 || $skipped_bytes2;

   $best_substring_start1 = 0;
   $best_substring_start2 = 0;
   $best_substring_length = 0;

   foreach $start_pos2 ((0 .. $#char_list2)) {
      last if $start_pos2 + $best_substring_length > $#char_list2;
      foreach $start_pos1 ((0 .. $#char_list1)) {
         last if $start_pos1 + $best_substring_length > $#char_list1;
         $matching_length = 0;
         while (($start_pos1 + $matching_length <= $#char_list1)
	     && ($start_pos2 + $matching_length <= $#char_list2)
	     && ($char_list1[$start_pos1+$matching_length] eq $char_list2[$start_pos2+$matching_length])) {
	    $matching_length++;
	 }
	 if ($matching_length > $best_substring_length) {
	    $best_substring_length = $matching_length;
	    $best_substring_start1 = $start_pos1;
	    $best_substring_start2 = $start_pos2;
	 }
      }
   }
   if ($info_type =~ /^max-ratio1$/) {
      $length1 = $#char_list1 + 1;
      return ($length1 > 0) ? ($best_substring_length / $length1) : 0;
   } elsif ($info_type =~ /^max-ratio2$/) {
      $length2 = $#char_list2 + 1;
      return ($length2 > 0) ? ($best_substring_length / $length2) : 0;
   } elsif ($info_type =~ /^substring$/) {
      return join("", @char_list1[$best_substring_start1 .. $best_substring_start1+$best_substring_length-1]);
   } else {
      $length1 = $#char_list1 + 1;
      $length2 = $#char_list2 + 1;
      $info = "s1=$s1;s2=$s2";
      $info .= ";best_substring_length=$best_substring_length";
      $info .= ";best_substring_start1=$best_substring_start1";
      $info .= ";best_substring_start2=$best_substring_start2";
      $info .= ";length1=$length1";
      $info .= ";length2=$length2";
      return $info;
   }
}

sub char_length {
   local($caller,$string,$byte_offset) = @_;

   my $char = ($byte_offset) ? substr($string, $byte_offset) : $string;
   return 1 if $char =~ /^[\x00-\x7F]/;
   return 2 if $char =~ /^[\xC0-\xDF]/;
   return 3 if $char =~ /^[\xE0-\xEF]/;
   return 4 if $char =~ /^[\xF0-\xF7]/;
   return 5 if $char =~ /^[\xF8-\xFB]/;
   return 6 if $char =~ /^[\xFC-\xFD]/;
   return 0;
}

sub length_in_utf8_chars {
   local($caller,$s) = @_;

   $s =~ s/[\x80-\xBF]//g;
   $s =~ s/[\x00-\x7F\xC0-\xFF]/c/g;
   return length($s);
}

sub byte_length_of_n_chars {
   local($caller,$char_length,$string,$byte_offset,$undef_return_value) = @_;

   $byte_offset = 0 unless defined($byte_offset);
   $undef_return_value = -1 unless defined($undef_return_value);
   my $result = 0;
   my $len;
   foreach $i ((1 .. $char_length)) {
      $len = $caller->char_length($string,($byte_offset+$result));
      return $undef_return_value unless $len;
      $result += $len;
   }
   return $result;
}

sub replace_non_ASCII_bytes {
   local($caller,$string,$replacement) = @_;

   $replacement = "HEX" unless defined($replacement);
   if ($replacement =~ /^(Unicode|U\+4|\\u|HEX)$/) {
      $new_string = "";
      while (($pre,$utf8_char, $post) = ($string =~ /^([\x09\x0A\x20-\x7E]*)([\x00-\x08\x0B-\x1F\x7F]|[\xC0-\xDF][\x80-\xBF]|[\xE0-\xEF][\x80-\xBF][\x80-\xBF]|[\xF0-\xF7][\x80-\xBF][\x80-\xBF][\x80-\xBF]|[\xF8-\xFF][\x80-\xBF]+|[\x80-\xBF])(.*)$/s)) {
	 if ($replacement =~ /Unicode/) {
	    $new_string .= $pre . "<U" . (uc $caller->utf8_to_unicode($utf8_char)) . ">";
	 } elsif ($replacement =~ /\\u/) {
	    $new_string .= $pre . "\\u" . (uc sprintf("%04x", $caller->utf8_to_unicode($utf8_char)));
	 } elsif ($replacement =~ /U\+4/) {
	    $new_string .= $pre . "<U+" . (uc $caller->utf8_to_4hex_unicode($utf8_char)) . ">";
	 } else {
	    $new_string .= $pre . "<HEX-" . $caller->utf8_to_hex($utf8_char) . ">";
	 }
	 $string = $post;
      }
      $new_string .= $string;
   } else {
      $new_string = $string;
      $new_string =~ s/[\x80-\xFF]/$replacement/g;
   }
   return $new_string;
}

sub valid_utf8_string_p {
   local($caller,$string) = @_;

   return $string =~ /^(?:[\x09\x0A\x20-\x7E]|[\xC0-\xDF][\x80-\xBF]|[\xE0-\xEF][\x80-\xBF][\x80-\xBF]|[\xF0-\xF7][\x80-\xBF][\x80-\xBF][\x80-\xBF])*$/;
}

sub utf8_to_hex {
   local($caller,$s) = @_;

   $hex = "";
   foreach $i ((0 .. length($s)-1)) {
      $hex .= uc sprintf("%2.2x",ord(substr($s, $i, 1)));
   }
   return $hex;
}

sub utf8_to_4hex_unicode {
   local($caller,$s) = @_;

   return sprintf("%4.4x", $caller->utf8_to_unicode($s));
}

sub utf8_to_unicode {
   local($caller,$s) = @_;

   $unicode = 0;
   foreach $i ((0 .. length($s)-1)) {
      $c = substr($s, $i, 1);
      if ($c =~ /^[\x80-\xBF]$/) {
	 $unicode = $unicode * 64 + (ord($c) & 0x3F);
      } elsif ($c =~ /^[\xC0-\xDF]$/) {
	 $unicode = $unicode * 32 + (ord($c) & 0x1F);
      } elsif ($c =~ /^[\xE0-\xEF]$/) {
	 $unicode = $unicode * 16 + (ord($c) & 0x0F);
      } elsif ($c =~ /^[\xF0-\xF7]$/) {
	 $unicode = $unicode * 8 + (ord($c) & 0x07);
      } elsif ($c =~ /^[\xF8-\xFB]$/) {
	 $unicode = $unicode * 4 + (ord($c) & 0x03);
      } elsif ($c =~ /^[\xFC-\xFD]$/) {
	 $unicode = $unicode * 2 + (ord($c) & 0x01);
      }
   }
   return $unicode;
}

sub charhex {
   local($caller,$string) = @_;

   my $result = "";
   while ($string ne "") {
      $char = substr($string, 0, 1);
      $string = substr($string, 1);
      if ($char =~ /^[ -~]$/) {
         $result .= $char;
      } else {
	 $hex = sprintf("%2.2x",ord($char));
	 $hex =~ tr/a-f/A-F/;
         $result .= "<HEX-$hex>";
      }
   }
   return $result;
}

sub windows1252_to_utf8 {
   local($caller,$s, $norm_to_ascii_p) = @_;

   return $s if $s =~ /^[\x00-\x7F]*$/; # all ASCII

   $norm_to_ascii_p = 1 unless defined($norm_to_ascii_p);
   my $result = "";
   my $c = "";
   while ($s ne "") {
      $n_bytes = 1;
      if ($s =~ /^[\x00-\x7F]/) {
	 $result .= substr($s, 0, 1);  # ASCII
      } elsif ($s =~ /^[\xC0-\xDF][\x80-\xBF]/) {
	 $result .= substr($s, 0, 2);  # valid 2-byte UTF8
         $n_bytes = 2;
      } elsif ($s =~ /^[\xE0-\xEF][\x80-\xBF][\x80-\xBF]/) {
	 $result .= substr($s, 0, 3);  # valid 3-byte UTF8
         $n_bytes = 3;
      } elsif ($s =~ /^[\xF0-\xF7][\x80-\xBF][\x80-\xBF][\x80-\xBF]/) {
	 $result .= substr($s, 0, 4);  # valid 4-byte UTF8
         $n_bytes = 4;
      } elsif ($s =~ /^[\xF8-\xFB][\x80-\xBF][\x80-\xBF][\x80-\xBF][\x80-\xBF]/) {
	 $result .= substr($s, 0, 5);  # valid 5-byte UTF8
         $n_bytes = 5;
      } elsif ($s =~ /^[\xA0-\xBF]/) {
	 $c = substr($s, 0, 1);
	 $result .= "\xC2$c";
      } elsif ($s =~ /^[\xC0-\xFF]/) {
	 $c = substr($s, 0, 1);
	 $c =~ tr/[\xC0-\xFF]/[\x80-\xBF]/;
	 $result .= "\xC3$c";
      } elsif ($s =~ /^\x80/) {
	 $result .= "\xE2\x82\xAC";  # Euro sign
      } elsif ($s =~ /^\x82/) {
	 $result .= "\xE2\x80\x9A";  # single low quotation mark
      } elsif ($s =~ /^\x83/) {
	 $result .= "\xC6\x92";      # Latin small letter f with hook
      } elsif ($s =~ /^\x84/) {
	 $result .= "\xE2\x80\x9E";  # double low quotation mark
      } elsif ($s =~ /^\x85/) {
	 $result .= ($norm_to_ascii_p) ? "..." : "\xE2\x80\xA6";  # horizontal ellipsis (three dots)
      } elsif ($s =~ /^\x86/) {
	 $result .= "\xE2\x80\xA0";  # dagger
      } elsif ($s =~ /^\x87/) {
	 $result .= "\xE2\x80\xA1";  # double dagger
      } elsif ($s =~ /^\x88/) {
	 $result .= "\xCB\x86";      # circumflex
      } elsif ($s =~ /^\x89/) {
	 $result .= "\xE2\x80\xB0";  # per mille sign
      } elsif ($s =~ /^\x8A/) {
	 $result .= "\xC5\xA0";      # Latin capital letter S with caron
      } elsif ($s =~ /^\x8B/) {
	 $result .= "\xE2\x80\xB9";  # single left-pointing angle quotation mark
      } elsif ($s =~ /^\x8C/) {
	 $result .= "\xC5\x92";      # OE ligature
      } elsif ($s =~ /^\x8E/) {
	 $result .= "\xC5\xBD";      # Latin capital letter Z with caron
      } elsif ($s =~ /^\x91/) {
	 $result .= ($norm_to_ascii_p) ? "`" : "\xE2\x80\x98";  # left single quotation mark
      } elsif ($s =~ /^\x92/) {
	 $result .= ($norm_to_ascii_p) ? "'" : "\xE2\x80\x99";  # right single quotation mark
      } elsif ($s =~ /^\x93/) {
	 $result .= "\xE2\x80\x9C";  # left double quotation mark
      } elsif ($s =~ /^\x94/) {
	 $result .= "\xE2\x80\x9D";  # right double quotation mark
      } elsif ($s =~ /^\x95/) {
	 $result .= "\xE2\x80\xA2";  # bullet
      } elsif ($s =~ /^\x96/) {
	 $result .= ($norm_to_ascii_p) ? "-" : "\xE2\x80\x93";  # n dash
      } elsif ($s =~ /^\x97/) {
	 $result .= ($norm_to_ascii_p) ? "-" : "\xE2\x80\x94";  # m dash
      } elsif ($s =~ /^\x98/) {
	 $result .= ($norm_to_ascii_p) ? "~" : "\xCB\x9C";      # small tilde
      } elsif ($s =~ /^\x99/) {
	 $result .= "\xE2\x84\xA2";  # trade mark sign
      } elsif ($s =~ /^\x9A/) {
	 $result .= "\xC5\xA1";      # Latin small letter s with caron
      } elsif ($s =~ /^\x9B/) {
	 $result .= "\xE2\x80\xBA";  # single right-pointing angle quotation mark
      } elsif ($s =~ /^\x9C/) {
	 $result .= "\xC5\x93";      # oe ligature
      } elsif ($s =~ /^\x9E/) {
	 $result .= "\xC5\xBE";      # Latin small letter z with caron
      } elsif ($s =~ /^\x9F/) {
	 $result .= "\xC5\xB8";      # Latin capital letter Y with diaeresis
      } else {
	 $result .= "?";
      }
      $s = substr($s, $n_bytes);
   }
   return $result;
}

sub number_of_utf8_character {
   local($caller, $s) = @_;

   $s2 = $s;
   $s2 =~ s/[\x80-\xBF]//g;
   return length($s2);
}

sub cap_letter_reg_exp {
   # includes A-Z and other Latin-based capital letters with accents, umlauts and other decorations etc.
   return "[A-Z]|\xC3[\x80-\x96\x98-\x9E]|\xC4[\x80\x82\x84\x86\x88\x8A\x8C\x8E\x90\x94\x964\x98\x9A\x9C\x9E\xA0\xA2\xA4\xA6\xA8\xAA\xAC\xAE\xB0\xB2\xB4\xB6\xB9\xBB\xBD\xBF]|\xC5[\x81\x83\x85\x87\x8A\x8C\x8E\x90\x92\x96\x98\x9A\x9C\x9E\xA0\xA2\xA4\xA6\xA8\xAA\xAC\xB0\xB2\xB4\xB6\xB8\xB9\xBB\xBD]";
}

sub regex_extended_case_expansion {
   local($caller, $s) = @_;

   if ($s =~ /\xC3/) {
   $s =~ s/\xC3\xA0/\xC3\[\x80\xA0\]/g;
   $s =~ s/\xC3\xA1/\xC3\[\x81\xA1\]/g;
   $s =~ s/\xC3\xA2/\xC3\[\x82\xA2\]/g;
   $s =~ s/\xC3\xA3/\xC3\[\x83\xA3\]/g;
   $s =~ s/\xC3\xA4/\xC3\[\x84\xA4\]/g;
   $s =~ s/\xC3\xA5/\xC3\[\x85\xA5\]/g;
   $s =~ s/\xC3\xA6/\xC3\[\x86\xA6\]/g;
   $s =~ s/\xC3\xA7/\xC3\[\x87\xA7\]/g;
   $s =~ s/\xC3\xA8/\xC3\[\x88\xA8\]/g;
   $s =~ s/\xC3\xA9/\xC3\[\x89\xA9\]/g;
   $s =~ s/\xC3\xAA/\xC3\[\x8A\xAA\]/g;
   $s =~ s/\xC3\xAB/\xC3\[\x8B\xAB\]/g;
   $s =~ s/\xC3\xAC/\xC3\[\x8C\xAC\]/g;
   $s =~ s/\xC3\xAD/\xC3\[\x8D\xAD\]/g;
   $s =~ s/\xC3\xAE/\xC3\[\x8E\xAE\]/g;
   $s =~ s/\xC3\xAF/\xC3\[\x8F\xAF\]/g;
   $s =~ s/\xC3\xB0/\xC3\[\x90\xB0\]/g;
   $s =~ s/\xC3\xB1/\xC3\[\x91\xB1\]/g;
   $s =~ s/\xC3\xB2/\xC3\[\x92\xB2\]/g;
   $s =~ s/\xC3\xB3/\xC3\[\x93\xB3\]/g;
   $s =~ s/\xC3\xB4/\xC3\[\x94\xB4\]/g;
   $s =~ s/\xC3\xB5/\xC3\[\x95\xB5\]/g;
   $s =~ s/\xC3\xB6/\xC3\[\x96\xB6\]/g;
   $s =~ s/\xC3\xB8/\xC3\[\x98\xB8\]/g;
   $s =~ s/\xC3\xB9/\xC3\[\x99\xB9\]/g;
   $s =~ s/\xC3\xBA/\xC3\[\x9A\xBA\]/g;
   $s =~ s/\xC3\xBB/\xC3\[\x9B\xBB\]/g;
   $s =~ s/\xC3\xBC/\xC3\[\x9C\xBC\]/g;
   $s =~ s/\xC3\xBD/\xC3\[\x9D\xBD\]/g;
   $s =~ s/\xC3\xBE/\xC3\[\x9E\xBE\]/g;
   }
   if ($s =~ /\xC5/) {
   $s =~ s/\xC5\x91/\xC5\[\x90\x91\]/g;
   $s =~ s/\xC5\xA1/\xC5\[\xA0\xA1\]/g;
   $s =~ s/\xC5\xB1/\xC5\[\xB0\xB1\]/g;
   }

   return $s;
}

sub extended_lower_case {
   local($caller, $s) = @_;

   $s =~ tr/A-Z/a-z/;
   return $s unless $s =~ /[\xC3-\xC5][\x80-\xBF]/;

   $s =~ s/\xC3\x80/\xC3\xA0/g;
   $s =~ s/\xC3\x81/\xC3\xA1/g;
   $s =~ s/\xC3\x82/\xC3\xA2/g;
   $s =~ s/\xC3\x83/\xC3\xA3/g;
   $s =~ s/\xC3\x84/\xC3\xA4/g;
   $s =~ s/\xC3\x85/\xC3\xA5/g;
   $s =~ s/\xC3\x86/\xC3\xA6/g;
   $s =~ s/\xC3\x87/\xC3\xA7/g;
   $s =~ s/\xC3\x88/\xC3\xA8/g;
   $s =~ s/\xC3\x89/\xC3\xA9/g;
   $s =~ s/\xC3\x8A/\xC3\xAA/g;
   $s =~ s/\xC3\x8B/\xC3\xAB/g;
   $s =~ s/\xC3\x8C/\xC3\xAC/g;
   $s =~ s/\xC3\x8D/\xC3\xAD/g;
   $s =~ s/\xC3\x8E/\xC3\xAE/g;
   $s =~ s/\xC3\x8F/\xC3\xAF/g;
   $s =~ s/\xC3\x90/\xC3\xB0/g;
   $s =~ s/\xC3\x91/\xC3\xB1/g;
   $s =~ s/\xC3\x92/\xC3\xB2/g;
   $s =~ s/\xC3\x93/\xC3\xB3/g;
   $s =~ s/\xC3\x94/\xC3\xB4/g;
   $s =~ s/\xC3\x95/\xC3\xB5/g;
   $s =~ s/\xC3\x96/\xC3\xB6/g;
   $s =~ s/\xC3\x98/\xC3\xB8/g;
   $s =~ s/\xC3\x99/\xC3\xB9/g;
   $s =~ s/\xC3\x9A/\xC3\xBA/g;
   $s =~ s/\xC3\x9B/\xC3\xBB/g;
   $s =~ s/\xC3\x9C/\xC3\xBC/g;
   $s =~ s/\xC3\x9D/\xC3\xBD/g;
   $s =~ s/\xC3\x9E/\xC3\xBE/g;
   return $s unless $s =~ /[\xC3-\xC5][\x80-\xBF]/;

   $s =~ s/\xC4\x80/\xC4\x81/g;
   $s =~ s/\xC4\x82/\xC4\x83/g;
   $s =~ s/\xC4\x84/\xC4\x85/g;
   $s =~ s/\xC4\x86/\xC4\x87/g;
   $s =~ s/\xC4\x88/\xC4\x89/g;
   $s =~ s/\xC4\x8A/\xC4\x8B/g;
   $s =~ s/\xC4\x8C/\xC4\x8D/g;
   $s =~ s/\xC4\x8E/\xC4\x8F/g;
   $s =~ s/\xC4\x90/\xC4\x91/g;
   $s =~ s/\xC4\x92/\xC4\x93/g;
   $s =~ s/\xC4\x94/\xC4\x95/g;
   $s =~ s/\xC4\x96/\xC4\x97/g;
   $s =~ s/\xC4\x98/\xC4\x99/g;
   $s =~ s/\xC4\x9A/\xC4\x9B/g;
   $s =~ s/\xC4\x9C/\xC4\x9D/g;
   $s =~ s/\xC4\x9E/\xC4\x9F/g;
   $s =~ s/\xC4\x9E/\xC4\x9F/g;
   $s =~ s/\xC4\xA0/\xC4\xA1/g;
   $s =~ s/\xC4\xA2/\xC4\xA3/g;
   $s =~ s/\xC4\xA4/\xC4\xA5/g;
   $s =~ s/\xC4\xA6/\xC4\xA7/g;
   $s =~ s/\xC4\xA8/\xC4\xA9/g;
   $s =~ s/\xC4\xAA/\xC4\xAB/g;
   $s =~ s/\xC4\xAC/\xC4\xAD/g;
   $s =~ s/\xC4\xAE/\xC4\xAF/g;
   $s =~ s/\xC4\xB0/i/g;
   $s =~ s/\xC4\xB2/\xC4\xB3/g;
   $s =~ s/\xC4\xB4/\xC4\xB5/g;
   $s =~ s/\xC4\xB6/\xC4\xB7/g;
   $s =~ s/\xC4\xB9/\xC4\xBA/g;
   $s =~ s/\xC4\xBB/\xC4\xBC/g;
   $s =~ s/\xC4\xBD/\xC4\xBE/g;
   $s =~ s/\xC4\xBF/\xC5\x80/g;

   $s =~ s/\xC5\x81/\xC5\x82/g;
   $s =~ s/\xC5\x83/\xC5\x84/g;
   $s =~ s/\xC5\x85/\xC5\x86/g;
   $s =~ s/\xC5\x87/\xC5\x88/g;
   $s =~ s/\xC5\x8A/\xC5\x8B/g;
   $s =~ s/\xC5\x8C/\xC5\x8D/g;
   $s =~ s/\xC5\x8E/\xC5\x8F/g;
   $s =~ s/\xC5\x90/\xC5\x91/g;
   $s =~ s/\xC5\x92/\xC5\x93/g;
   $s =~ s/\xC5\x94/\xC5\x95/g;
   $s =~ s/\xC5\x96/\xC5\x97/g;
   $s =~ s/\xC5\x98/\xC5\x99/g;
   $s =~ s/\xC5\x9A/\xC5\x9B/g;
   $s =~ s/\xC5\x9C/\xC5\x9D/g;
   $s =~ s/\xC5\x9E/\xC5\x9F/g;
   $s =~ s/\xC5\xA0/\xC5\xA1/g;
   $s =~ s/\xC5\xA2/\xC5\xA3/g;
   $s =~ s/\xC5\xA4/\xC5\xA5/g;
   $s =~ s/\xC5\xA6/\xC5\xA7/g;
   $s =~ s/\xC5\xA8/\xC5\xA9/g;
   $s =~ s/\xC5\xAA/\xC5\xAB/g;
   $s =~ s/\xC5\xAC/\xC5\xAD/g;
   $s =~ s/\xC5\xAE/\xC5\xAF/g;
   $s =~ s/\xC5\xB0/\xC5\xB1/g;
   $s =~ s/\xC5\xB2/\xC5\xB3/g;
   $s =~ s/\xC5\xB4/\xC5\xB5/g;
   $s =~ s/\xC5\xB6/\xC5\xB7/g;
   $s =~ s/\xC5\xB9/\xC5\xBA/g;
   $s =~ s/\xC5\xBB/\xC5\xBC/g;
   $s =~ s/\xC5\xBD/\xC5\xBE/g;
   return $s;
}

sub repair_doubly_converted_utf8_strings {
   local($caller, $s) = @_;

   if ($s =~ /\xC3[\x83-\x85]\xC2[\x80-\xBF]/) {
      $s =~ s/\xC3\x83\xC2([\x80-\xBF])/\xC3$1/g;
      $s =~ s/\xC3\x84\xC2([\x80-\xBF])/\xC4$1/g;
      $s =~ s/\xC3\x85\xC2([\x80-\xBF])/\xC5$1/g;
   }
   return $s;
}

sub repair_misconverted_windows_to_utf8_strings {
   local($caller, $s) = @_;

   # correcting conversions of UTF8 using Latin1-to-UTF converter
   if ($s =~ /\xC3\xA2\xC2\x80\xC2[\x90-\xEF]/) {
      my $result = "";
      while (($pre,$last_c,$post) = ($s =~ /^(.*?)\xC3\xA2\xC2\x80\xC2([\x90-\xEF])(.*)$/s)) {
         $result .= "$pre\xE2\x80$last_c";
         $s = $post;
      }
      $result .= $s;
      $s = $result;
   }
   # correcting conversions of Windows1252-to-UTF8 using Latin1-to-UTF converter
   if ($s =~ /\xC2[\x80-\x9F]/) {
      my $result = "";
      while (($pre,$c_windows,$post) = ($s =~ /^(.*?)\xC2([\x80-\x9F])(.*)$/s)) {
	 $c_utf8 = $caller->windows1252_to_utf8($c_windows, 0);
         $result .= ($c_utf8 eq "?") ? "$pre$c_windows" : "$pre$c_utf8";
         $s = $post;
      }
      $result .= $s;
      $s = $result;
   }
   return $s;
}

1;

