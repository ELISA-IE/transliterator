################################################################
#                                                              #
# Romanizer                                                    #
#                                                              #
################################################################

package NLP::Romanizer;

use NLP::Chinese;
use NLP::UTF8;
use NLP::utilities;
$utf8 = NLP::UTF8;
$util = NLP::utilities;
$chinesePM = NLP::Chinese;

sub new {
   local($caller) = @_;

   my $object = {};
   my $class = ref( $caller ) || $caller;
   bless($object, $class);
   return $object;
}

sub load_unicode_data {
   local($this, *ht, $filename) = @_;
   # /nfs/nlg/users/textmap/brahms-ml/arabic/bin/modules/NLP/UnicodeData.txt

   $n = 0;
   if (open(IN, $filename)) {
      while (<IN>) {
	 if (($unicode_value, $char_name, $general_category, $canon_comb_classes, $bidir_category, $char_decomp_mapping, $decimal_digit_value, $digit_value, $numeric_value, $mirrored, $unicode_1_0_name, $comment_field, $uc_mapping, $lc_mapping, $title_case_mapping) = split(";", $_)) {
            $utf8_code = $utf8->unicode_hex_string2string($unicode_value);
	    $ht{UTF_TO_CHAR_NAME}->{$utf8_code} = $char_name;
	    $ht{UTF_TO_CAT}->{$utf8_code} = $general_category;
	    $ht{UTF_TO_NUMERIC}->{$utf8_code} = $numeric_value unless $numeric_value eq "";
	    $n++;
	    # print "$unicode_value $char_name ($utf8_code)\n" if $char_name =~ /tibetan/i;
	 }
      }
      close(IN);
   } else {
      print STDERR "Can't open $filename\n";
   }
   # print STDERR "Processed $n entries from $filename\n";
}

sub load_unicode_overwrite_romanization {
   local($this, *ht, $filename) = @_;
   # /nfs/nlg/users/textmap/brahms-ml/arabic/bin/modules/NLP/UnicodeDataOverwrite.txt

   $n = 0;
   if (open(IN, $filename)) {
      while (<IN>) {
	 next if /^#/;
         $unicode_value = $util->slot_value_in_double_colon_del_list($_, "u");
         $romanization = $util->slot_value_in_double_colon_del_list($_, "r");
         $numeric = $util->slot_value_in_double_colon_del_list($_, "num");
         $picture = $util->slot_value_in_double_colon_del_list($_, "pic");
	 $entry_processed_p = 0;
         $utf8_code = $utf8->unicode_hex_string2string($unicode_value);
	 if ($unicode_value && $romanization) {
	    $ht{UTF_TO_CHAR_ROMANIZATION}->{$utf8_code} = $romanization;
	    $entry_processed_p = 1;
	 }
	 if ($unicode_value && $numeric) {
	    $ht{UTF_TO_NUMERIC}->{$utf8_code} = $numeric;
	    $entry_processed_p = 1;
	 }
	 if ($unicode_value && $picture) {
	    $ht{UTF_TO_PICTURE_DESCR}->{$utf8_code} = $picture;
	    $entry_processed_p = 1;
	 }
	 $n++ if $entry_processed_p;
      }
      close(IN);
   } else {
      print STDERR "Can't open $filename\n";
   }
   # print STDERR "Processed $n entries from $filename\n";
}

sub unicode_hangul_romanization {
   local($this, $s, $pass_through_p) = @_;

   $pass_through_p = 0 unless defined($pass_through_p);
   @leads = split(/\s+/, "g gg n d dd r m b bb s ss - j jj c k t p h");
   # @vowels = split(/\s+/, "a ae ya yai e ei ye yei o oa oai oi yo u ue uei ui yu w wi i");
   @vowels = split(/\s+/, "a ae ya yae eo e yeo ye o wa wai oe yo u weo we wi yu eu yi i");
   @tails = split(/\s+/, "- g gg gs n nj nh d l lg lm lb ls lt lp lh m b bs s ss ng j c k t p h");
   $result = "";
   # print "unicode_hangul_romanization $s\n";
   @chars = $utf8->split_into_utf8_characters($s, "return only chars");
   foreach $char (@chars) {
      # print "utf8 char: $char\n";
      $unicode = $utf8->utf8_to_unicode($char);
      # print "unicode: $unicode\n";
      if (($unicode >= 0xAC00) && ($unicode <= 0xD7A3)) {
	 $code = $unicode - 0xAC00;
	 $lead_index = int($code / (28*21));
	 $vowel_index = int($code/28) % 21;
	 $tail_index = $code % 28;
	 # print "l:$lead_index v:$vowel_index t:$tail_index\n";
	 $rom = $leads[$lead_index] . $vowels[$vowel_index] . $tails[$tail_index];
	 $rom =~ s/-//g;
	 $result .= $rom;
      } elsif ($pass_through_p) {
	 $result .= $char;
      }
   }
   return $result;
}

sub load_romanization_table {
   local($this, *ht, $filename) = @_;
   # /nfs/nlg/users/textmap/brahms-ml/arabic/bin/modules/NLP/romanization-table.txt
   $n = 0;
   if (open(IN, $filename)) {
      while (<IN>) {
	 next if /^#/;
         $utf8_source_string = $util->slot_value_in_double_colon_del_list($_, "s");
         $utf8_target_string = $util->slot_value_in_double_colon_del_list($_, "t");
	 $utf8_source_string =~ s/\s*$//;
	 $utf8_target_string =~ s/\s*$//;
	 $utf8_target_string =~ s/^"(.*)"$/$1/;
	 $utf8_target_string =~ s/^'(.*)'$/$1/;
         $numeric = $util->slot_value_in_double_colon_del_list($_, "num");
	 $numeric =~ s/\s*$//;
         $lang_code = $util->slot_value_in_double_colon_del_list($_, "lcode");
         $prob = $util->slot_value_in_double_colon_del_list($_, "p") || 1;
	 unless (($utf8_target_string eq "") && ($numeric =~ /\d/)) {
	    if ($lang_code) {
               $ht{UTF_CHAR_MAPPING_LANG_SPEC}->{$lang_code}->{$utf8_source_string}->{$utf8_target_string} = $prob;
	    } else {
               $ht{UTF_CHAR_MAPPING}->{$utf8_source_string}->{$utf8_target_string} = $prob;
	    }
	 }
	 if ($numeric =~ /\d/) {
	    $ht{UTF_TO_NUMERIC}->{$utf8_source_string} = $numeric;
	 }
         $n++;
      }
      close(IN);
   } else {
      print STDERR "Can't open $filename\n";
   }
   # print STDERR "Processed $n entries from $filename\n";
}

sub protect_parentheses {
   local($this, $s) = @_;

   $s =~ s/\(/\x05/g;
   $s =~ s/\)/\x06/g;
   return $s;
}

sub romanize {
   local($this, $s, $lang_code, $output_style, *ht, *pinyin_ht) = @_;

   my $result = "";
   my $log ="";
   $s =~ s/[\x00-\x07]//g;
   # \x01 start guarded area
   # \x02 end   guarded area
   # \x03 start temporary tag
   # \x04 end   temporary tag
   # \x05 original open  parenthesis
   # \x06 original close parenthesis
   # \x07 null consonant
   my @chars = $utf8->split_into_utf8_characters($s, "return only chars");
   my $prev_char = "";
   my $prev_numeric_value = "";
   my $prev_char_name = "";
   $letter_plus_pattern = "LETTER|VOWEL SIGN|AU LENGTH MARK|CONSONANT SIGN|SIGN VIRAMA|SIGN COENG|SIGN AL-LAKUNA|SIGN ASAT|SIGN ANUSVARA|SIGN ANUSVARAYA|SIGN BINDI|TIPPI|SIGN NIKAHIT|SIGN CANDRABINDU|SIGN VISARGA|SIGN REAHMUK|SIGN NUKTA|SIGN DOT BELOW";
   while (@chars) {
      my $char = shift @chars;
      my $char_name = $ht{UTF_TO_CHAR_NAME}->{$char} || "";
      foreach $script_name (("BENGALI", "DEVANAGARI", "MODI", "GURMUKHI", "GUJARATI", "MYANMAR", "KHMER", "ORIYA", "KANNADA", "MALAYALAM", "TAMIL", "TELUGU", "SINHALA", "TIBETAN")) {
         $result .= "(__START_" . $script_name . "__)" 
	           if ($char_name =~ /\b$script_name\b.*\b(?:$letter_plus_pattern)\b/)
            && ! ($prev_char_name =~ /\b$script_name\b.*\b(?:$letter_plus_pattern)\b/);
         $result .= "(__END_" . $script_name . "__)"
	      if ($prev_char_name =~ /\b$script_name\b.*\b(?:$letter_plus_pattern)\b/)
                 && ! ($char_name =~ /\b$script_name\b.*\b(?:$letter_plus_pattern)\b/);
      }
      $found_char_mapping_p = 0;
      unless ($found_char_mapping_p) {
         foreach $string_length (reverse(1 .. 6)) {
	    next if ($string_length-2) > $#chars;
	    $multi_char_substring = join("", $char, @chars[0..($string_length-2)]);
	    my @mappings = keys %{$ht{UTF_CHAR_MAPPING_LANG_SPEC}->{$lang_code}->{$multi_char_substring}};
	    @mappings = keys %{$ht{UTF_CHAR_MAPPING}->{$multi_char_substring}} unless @mappings;
	    if (@mappings) {
	       my $mapping = $mappings[0];
	       $mapping = $this->protect_parentheses($mapping) unless $mapping =~ /^\(.*\)$/;
	       if (($char_name =~ /\b(?:SUBJOINED LETTER|VOWEL SIGN|AU LENGTH MARK|CONSONANT SIGN|SIGN VIRAMA|SIGN COENG|SIGN ASAT|SIGN ANUSVARA|SIGN ANUSVARAYA|SIGN BINDI|TIPPI|SIGN NIKAHIT|SIGN CANDRABINDU|SIGN VISARGA|SIGN REAHMUK|SIGN DOT BELOW|ARABIC (?:DAMMA|DAMMATAN|FATHA|FATHATAN|HAMZA|KASRA|KASRATAN|MADDAH|SHADDA|SUKUN))\b/) && ($result =~ /\)$/)) {
		  if ($result =~ /__START_[A-Z_]+__\)$/) {
		     $result .= "($mapping)";
		  } else {
	             $result =~ s/\)$/$mapping)/ unless $result =~ /__\$/;
		  }
	       } else {
	          $result .= $mapping;
	       }
	       $found_char_mapping_p = 1;
	       foreach $_ ((0..($string_length-2))) {
	          $char = shift @chars;
	          $char_name = $ht{UTF_TO_CHAR_NAME}->{$char};
	          $numeric_value = "";
	       }
	       last;
	    }
         }
      }
      unless ($found_char_mapping_p) {
         if ($char_name =~ /\bSIGN (?:VIRAMA|AL-LAKUNA|ASAT|COENG)\b/) {
	    $result .= "(__VIRAMA__)";
	    $found_char_mapping_p = 1;
	 } elsif ($char_name =~ /\bSIGN NUKTA\b/) {
	    # nothing
	    $found_char_mapping_p = 1;
	 }
      }
      unless ($found_char_mapping_p) {
         $numeric_value = $ht{UTF_TO_NUMERIC}->{$char};
	 $numeric_value = "" unless defined($numeric_value);
	 $numeric_value = "" if $char_name =~ /SUPERSCRIPT/;
	 # print "Numeric($char): $numeric_value\n" if $numeric_value;
         if ($char =~ /^[\x00-\x7F]/) {
            $result .= $this->protect_parentheses($char);
         } elsif ($numeric_value =~ /\d/) {
	    $result .= "\xC2\xB7" if (($prev_numeric_value =~ /\d/) && ($numeric_value =~ /\d\d/)) 
			          || (($prev_numeric_value =~ /\d\d/) && ($numeric_value =~ /\d/));
	    $result .= "(" . $numeric_value . ")";
         } elsif (($char =~ /^[\xEA-\xED]/)
	       && ($romanized_char = $this->unicode_hangul_romanization($char, 0))) {
	    $result .= $romanized_char;
	 } elsif ($chinesePM->string_contains_utf8_cjk_unified_ideograph_p($char)
	       && ($tonal_translit = $chinesePM->tonal_pinyin($char, *pinyin_ht, ""))) {
	       $result .= $util->de_accent_string($tonal_translit);
	 } elsif (($char_name =~ /\b(ACCENT|TONE|THAI CHARACTER MAI|COMBINING DIAERESIS|COMBINING DIAERESIS BELOW|COMBINING MACRON|COMBINING VERTICAL LINE ABOVE|COMBINING DOT ABOVE RIGHT|COMBINING TILDE)\b/) && ($ht{UTF_TO_CAT}->{$char} =~ /^Mn/)) {
	    # skip/nothing 
         } elsif ($char_name) {
	    if ($romanized_char = $ht{UTF_TO_CHAR_ROMANIZATION}->{$char}
			       || $this->romanize_charname($char_name, $lang_code, $output_style, *ht, $char)) {
	       if ($romanized_char eq "\"\"") {
	       } elsif ($romanized_char =~ /\s/) {
	          $result .= $char;
	       } elsif ($char_name =~ /(?:HIRAGANA|KATAKANA) LETTER SMALL Y/) {
	          $result .= "(__JAP_CONTRACTION__)($romanized_char)";
	       } elsif (($char_name =~ /\b(?:SUBJOINED LETTER|VOWEL SIGN|AU LENGTH MARK|CONSONANT SIGN|SIGN VIRAMA|SIGN COENG|SIGN ASAT|SIGN ANUSVARA|SIGN ANUSVARAYA|SIGN BINDI|TIPPI|SIGN NIKAHIT|SIGN CANDRABINDU|SIGN VISARGA|SIGN REAHMUK|SIGN DOT BELOW|ARABIC (?:DAMMA|DAMMATAN|FATHA|FATHATAN|HAMZA|KASRA|KASRATAN|MADDAH|SHADDA|SUKUN))\b/) && ($result =~ /\)$/)) {
		  $result =~ s/\)$/$romanized_char)/ unless $result =~ /__\)$/;
	       } elsif ($char_name =~ /THAI CHARACTER/) {
		  $result .= $romanized_char;
	       } else {
	          $result .= "($romanized_char)";
	       }
	    } else {
	       $result .= "($char_name)";
	    }
         } else {
	    $result .= $char;
         }
      }
      $prev_char = $char;
      $prev_numeric_value = $numeric_value;
      $prev_char_name = $char_name || "";
   }
   foreach $script_name (("BENGALI", "DEVANAGARI", "MODI", "GURMUKHI", "GUJARATI", "MYANMAR", "KHMER", "ORIYA", "KANNADA", "MALAYALAM", "TAMIL", "TELUGU", "SINHALA", "TIBETAN")) {
      $result .= "(__END_" . $script_name . "__)" if $prev_char_name =~ /\b$script_name\b.*\b(?:$letter_plus_pattern)\b/;
   }
   $result .= "(__END_TIBETAN__)" if ($prev_char_name =~ /\bTIBETAN\b.*\b(?:LETTER|VOWEL SIGN)\b/);
   $result =~ s/\(__SOKUON__\)\(([bcdfghjklmnpqrstwz])/\($1$1/gi;
   $result =~ s/([AEIOU])\)\(__CHOONPU__\)/$1$1\)/gi;
   $result =~ s/I\)\(__JAP_CONTRACTION__\)\(Y/y/gi;
   # remove vowel "A" between consonant and medial consonant
   foreach $script_name (("MYANMAR")) {
      $prev_result = "";
      while ($result ne $prev_result) {
         $prev_result = $result;
	 $start_tag = "__START_" . $script_name . "__";
         $result =~ s/^(.*\($start_tag\)(?:\([^()]*[AEIOU][^()]*\))*\([BCDFGHJKLMNPQRSTVWXYZ]+)A([HRWY][AEIOU]+\).*)$/$1$2/i;
      }
   }
   # add vowel "A" where no vowel is present (could be killed by virama later on)
   foreach $script_name (("BENGALI", "DEVANAGARI", "MODI", "GURMUKHI", "GUJARATI", "MYANMAR", "ORIYA", "KANNADA", "MALAYALAM", "TAMIL", "TELUGU", "SINHALA")) {
      $start_tag = "__START_" . $script_name . "__";
      next unless $result =~ /$start_tag/;
      $old_result = $result;
      $result = "";
      while (($token, $rest) = ($old_result =~ /^(.+?)([ ].*|)$/)) {
         $prev_token = "";
         while ($token ne $prev_token) {
            $prev_token = $token;
            $token =~ s/^(.*\($start_tag\)(?:\([^()]*[AEIOU][^()]*\))*\([BCDFGHJKLMNPQRSTVWXYZ]+)((?:\+(?:H|M|N|NG))?\).*)$/$1a$2/i;
         }
	 $result .= $token;
         $old_result = $rest;
      }
   }
   $script_name = "KHMER";
   $start_tag = "__START_" . $script_name . "__";
   if ($result =~ /$start_tag/) {
      $old_result = $result;
      $result = "";
      while (($token, $rest) = ($old_result =~ /^(.+?)([ ].*|)$/)) {
         $prev_token = "";
         while ($token ne $prev_token) {
            $prev_token = $token;
            $token =~ s/^(.*\($start_tag\)(?:\([^()]*[AEIOUY][^()]*\))*\([A-Z]*)[AO]-([AEIOUY].*)$/$1$2/i;
            $token =~ s/^(.*\($start_tag\)(?:\([^()]*[AEIOUY][^()]*\))*\([A-Z]*[AO])-([^AEIOUY].*)$/$1$2/i;
         }
         $result .= $token;
	 $old_result = $rest;
      }
   }
   $result =~ s/(\([A-Z]{1,7})\+([A-Z]{1,2}\))/$1$2/gi;
   $result =~ s/([BCDFGHJKLMNPQRSTVWXYZ])A\)\(__VIRAMA__\)/$1\)/gi;
   $result =~ s/([BCDFGHJKLMNPQRSTVWXYZ])O\)\(__VIRAMA__\)/$1\)/gi if $result =~ /__START_KHMER__/;
   $result =~ s/(\(__START_TIBETAN__\))\(-A\)/$1(')/gi;
   $result =~ s/(\(__START_TIBETAN__\))\(A\)(\([AEIOU][^()]*\))/$1$2/gi;
   $result =~ s/(\(__START_TIBETAN__\)(?:\([^()]+\))*\([^()]*[^()AEIOU]\)\()-A([AEIOU]\))/$1a'$2/gi;
   $result =~ s/(\(__START_TIBETAN__\)(?:\([^()]+\))*\([^()]*[AEIOU]\))\(-A\)(\([^()]*[AEIOU]\))/$1(')$2/gi;
   $result =~ s/(\(__START_TIBETAN__\)(?:\([^()]+\))*)\(-A([AEIOU])\)/$1($2)/gi;
   $result =~ s/(\(__START_TIBETAN__\)(?:\([^()]+\))*\([^()]*Y)(\))/$1a$2/gi;
   $result =~ s/(\(__START_TIBETAN__\))(\([^AEIOU()]+\))(\(__END_TIBETAN__\))/$1$2(a)$3/gi;
   $result =~ s/(\(__START_TIBETAN__\))((?:\([^AEIOU()]+\))+?)(\([^AEIOU()]+\)(?:\(S\))?)(\(__END_TIBETAN__\))/$1$2(a)$3$4/gi;
   $result =~ s/(\(__START_TIBETAN__\)(?:\([^()]+\))*)\(([BCDFGHJKLMNPQRSTVWXYZ])A*-A\)/$1($2)/gi;
   $result =~ s/\(-A\)/(a)/gi;
   # $result =~ s/\(([BCDFGHJKLMNPQRSTVWXYZ])-A\)/($1)/gi;
   $result =~ s/\(__[^()]+__\)//gi;
   $result =~ s/\(([a-z]{1,7}|[0-9]{1,10}|[aeiouhkms]*'[aeiou]*)\)/$1/gi;
   $result =~ s/\(([a-z]{1,7})\+([a-z]{1,7})\)/$1$2/gi;
   $result =~ s/\x03.*?\x04//g;
   $result =~ s/\x05/\(/g;
   $result =~ s/\x06/\)/g;
   $result =~ s/[\x00-\x07]//g;
   $result =~ s/\xE2\x80\x8C//g; # remove ZERO WIDTH NON-JOINER (often used tocontrol rendering in Arabic or Indic scripts)
   $result =~ s/\xE2\x80\x8D//g; # remove ZERO WIDTH JOINER (often used tocontrol rendering in Arabic or Indic scripts)
   $result =~ s/([a-z])\xE0\xA9\xB1([bcdfghjklmnpqrstvwxz])/$1$2$2/gi; # Gurmukhi addak (doubles following consonant)
   $result = $this->assemble_numbers_in_string($result);

   return $result;
}

sub char_is_combining_char {
   local($this, $c, *ht) = @_;

   return 0 unless $c;
   my $category = $ht{UTF_TO_CAT}->{$c};
   return 0 unless $category;
   return $category =~ /^M/;
}

sub mark_up_string_for_mouse_over {
   local($this, $s, *ht, $control, *pinyin_ht) = @_;

   $control = "" unless defined($control);
   $no_ascii_p = ($control =~ /NO-ASCII/);
   my $result = "";
   @chars = $utf8->split_into_utf8_characters($s, "return only chars");
   while (@chars) {
      $char = shift @chars;
      $numeric = $ht{UTF_TO_NUMERIC}->{$char};
      $numeric = "" unless defined($numeric);
      $pic_descr = $ht{UTF_TO_PICTURE_DESCR}->{$char};
      $pic_descr = "" unless defined($pic_descr);
      $next_char = ($#chars >= 0) ? $chars[0] : "";
      $next_char_is_combining_p = $this->char_is_combining_char($next_char, *ht);
      if ($no_ascii_p
       && ($char =~ /^[\x00-\x7F]*$/)
       && ! $next_char_is_combining_p) {
	 $result .= $util->guard_html($char);
      } elsif (($char =~ /^[\xE3-\xE9][\x80-\xBF]{2,2}$/) && $chinesePM->string_contains_utf8_cjk_unified_ideograph_p($char)) {
	 $unicode = $utf8->utf8_to_unicode($char);
	 $title = "CJK Unified Ideograph U+" . (uc sprintf("%04x", $unicode));
	 $title .= "&#xA;Chinese: $tonal_translit" if $tonal_translit = $chinesePM->tonal_pinyin($char, *pinyin_ht, "");
	 $title .= "&#xA;Number: $numeric" if $numeric =~ /\d/;
	 $result .= "<span title=\"$title\">" . $util->guard_html($char) . "<\/span>";
      } elsif ($char_name = $ht{UTF_TO_CHAR_NAME}->{$char}) {
	 $title = $char_name;
	 $title .= "&#xA;Number: $numeric" if $numeric =~ /\d/;
	 $title .= "&#xA;Picture: $pic_descr" if $pic_descr =~ /\S/;
	 $char_plus = $char;
	 while ($next_char_is_combining_p) {
	    # combining marks (Mc:non-spacing, Mc:spacing combining, Me: enclosing)
	    $next_char_name = $ht{UTF_TO_CHAR_NAME}->{$next_char};
	    $title .= "&#xA;+ $next_char_name";
	    $char = shift @chars;
	    $char_plus .= $char;
	    $next_char = ($#chars >= 0) ? $chars[0] : "";
	    $next_char_is_combining_p = $this->char_is_combining_char($next_char, *ht);
	 }
	 $result .= "<span title=\"$title\">" . $util->guard_html($char_plus) . "<\/span>";
	 $result .= "<wbr>" if $char_name =~ /^(FULLWIDTH COLON|FULLWIDTH COMMA|FULLWIDTH RIGHT PARENTHESIS|IDEOGRAPHIC COMMA|IDEOGRAPHIC FULL STOP|RIGHT CORNER BRACKET)$/;
      } elsif (($unicode = $utf8->utf8_to_unicode($char))
	    && ($unicode >= 0xAC00) && ($unicode <= 0xD7A3)) {
	 $title = "Hangul syllable U+" . (uc sprintf("%04x", $unicode));
	 $result .= "<span title=\"$title\">" . $util->guard_html($char) . "<\/span>";
      } else {
	 $result .= $util->guard_html($char);
      }
   }
   return $result;
}

sub romanize_charname {
   local($this, $char_name, $lang_code, $output_style, *ht, $char) = @_;

   $orig_char_name = $char_name;
   $char_name =~ s/^.* LETTER\s+//;
   $char_name =~ s/^.* SYLLABLE\s+//;
   $char_name =~ s/^.* SYLLABICS\s+//;
   $char_name =~ s/^.* LIGATURE\s+//;
   $char_name =~ s/^.* VOWEL SIGN\s+//;
   $char_name =~ s/^.* CONSONANT SIGN\s+//;
   $char_name =~ s/^.* CONSONANT\s+//;
   $char_name =~ s/^.* VOWEL\s+//;
   $char_name =~ s/ WITH .*$//;
   $char_name =~ s/ WITHOUT .*$//;
   foreach $_ ((1 .. 3)) {
      $char_name =~ s/^.*\b(?:ABKHASIAN|ACADEMY|AFRICAN|AIVILIK|AITON|AKHMIMIC|ALEUT|ALI GALI|ALPAPRAANA|ALTERNATE|ALTERNATIVE|AMBA|ARABIC|ARCHAIC|ASPIRATED|ATHAPASCAN|BASELINE|BLACKLETTER|BARRED|BASHKIR|BERBER|BHATTIPROLU|BIBLE-CREE|BIG|BINOCULAR|BLACKFOOT|BLENDED|BOTTOM|BROAD|BROKEN|CANDRA|CAPITAL|CARRIER|CHILLU|CLOSE|CLOSED|COPTIC|CROSSED|CRYPTOGRAMMIC|CURLY|CYRILLIC|DANTAJA|DENTAL|DIALECT-P|DIAERESIZED|DOTLESS|DOUBLE|DOUBLE-STRUCK|EASTERN PWO KAREN|EGYPTOLOGICAL|FARSI|FINAL|FLATTENED|GLOTTAL|GREAT|GREEK|HALF|HIGH|INITIAL|INSULAR|INVERTED|IOTIFIED|JONA|KANTAJA|KASHMIRI|KHAKASSIAN|KHAMTI|KHANDA|KIRGHIZ|KOMI|L-SHAPED|LATINATE|LITTLE|LONG|LOOPED|LOW|MAHAAPRAANA|MANCHU|MANDAILING|MATHEMATICAL|MEDIAL|MIDDLE-WELSH|MON|MONOCULAR|MOOSE-CREE|MULTIOCULAR|MUURDHAJA|N-CREE|NASKAPI|NDOLE|NEUTRAL|NIKOLSBURG|NORTHERN|NUBIAN|NUNAVIK|NUNAVUT|OJIBWAY|OLD|OPEN|ORKHON|OVERLONG|PERSIAN|PHARYNGEAL|PRISHTHAMATRA|R-CREE|REDUPLICATION|REVERSED|ROMANIAN|ROUND|ROUNDED|RUDIMENTA|RUMAI PALAUNG|SANYAKA|SARA|SAYISI|SCRIPT|SEBATBEIT|SEMISOFT|SGAW KAREN|SHAN|SHARP|SHWE PALAUNG|SHORT|SIBE|SIDEWAYS|SIMALUNGUN|SMALL|SOGDIAN|SOFT|SOUTH-SLAVEY|SOUTHERN|SPIDERY|STIRRUP|STRAIGHT|STRETCHED|SUBSCRIPT|SWASH|TAI LAING|TAILED|TAILLESS|TAALUJA|TH-CREE|TALL|TURNED|TODO|TOP|TROKUTASTI|TUAREG|UKRAINIAN|VISIGOTHIC|VOCALIC|VOICED|VOICELESS|VOLAPUK|WAVY|WESTERN PWO KAREN|WEST-CREE|WESTERN|WIDE|WOODS-CREE|Y-CREE|YENISEI|YIDDISH)\s+//;
   }
   $char_name =~ s/\s+(ABOVE|AGUNG|BAR|BARREE|BELOW|CEDILLA|CEREK|DIGRAPH|DOACHASHMEE|FINAL FORM|GHUNNA|GOAL|INITIAL FORM|ISOLATED FORM|KAWI|LELET|LELET RASWADI|LONSUM|MAHAPRANA|MURDA|MURDA MAHAPRANA|REVERSED|ROTUNDA|SASAK|SUNG|TAM|TEDUNG|TYPE ONE|TYPE TWO|WOLOSO)\s*$//;
   if ($char_name =~ /THAI CHARACTER/) {
      $char_name =~ s/^THAI CHARACTER\s+//;
      if ($char eq "\xE0\xB8\xAD") {
	 $char_name = "\x07"; # null-consonant
      } elsif ($char =~ /^\xE0\xB8[\x81-\xAE]/) {
	 # Thai consonants
	 $char_name =~ s/^([^AEIOU]*).*/$1/i;
      } elsif ($char_name =~ /^SARA [AEIOU]/) {
	 # Thai vowels
	 $char_name =~ s/^SARA\s+//;
      } else {
	 $char_name = $char;
      }
   }
   if ($orig_char_name =~ /(HIRAGANA LETTER|KATAKANA LETTER|SYLLABLE|LIGATURE)/) {
      $char_name = lc $char_name;
   } elsif ($char_name =~ /\b(ANUSVARA|ANUSVARAYA|NIKAHIT|SIGN BINDI|TIPPI)\b/) {
      $char_name = "+m";
   } elsif ($char_name =~ /\bSCHWA\b/) {
      $char_name = "e";
   } elsif ($char_name =~ /\s/) {
   } elsif ($orig_char_name =~ /KHMER LETTER/) {
      $char_name .= "-";
   } elsif ($orig_char_name =~ /CHEROKEE LETTER/) {
      # use whole letter as is
   } elsif ($orig_char_name =~ /KHMER INDEPENDENT VOWEL/) {
      $char_name =~ s/q//;
   } elsif ($orig_char_name =~ /LETTER/) {
      $char_name =~ s/^[AEIOU]+([^AEIOU]+)$/$1/i;
      $char_name =~ s/^([^-AEIOUY]+)[AEIOU].*/$1/i;
      $char_name =~ s/^(Y)[AEIOU].*/$1/i if $orig_char_name =~ /\b(?:BENGALI|DEVANAGARI|GURMUKHI|GUJARATI|KANNADA|MALAYALAM|MODI|MYANMAR|ORIYA|TAMIL|TELUGU|TIBETAN)\b.*\bLETTER YA\b/;
      $char_name =~ s/^(Y[AEIOU]+)[^AEIOU].*$/$1/i;
      $char_name =~ s/^([AEIOU]+)[^AEIOU]+[AEIOU].*/$1/i;
   }

   return ($orig_char_name =~ /\bCAPITAL\b/) ? $char_name : (lc $char_name);
}

sub assemble_numbers_in_string {
   local($this, $s) = @_;

   my $result = "";
   my $middot = "\xC2\xB7";
   my $pre; 
   my $number_s; 
   my $post;
   while (($pre, $number_s, $post) = ($s =~ /^(.*?)(\d+(?:\.\d+)?(?:$middot\d+)+)(.*)$/)) {
      $result .= $pre;
      $result .= $this->assemble_number($number_s);
      $s = $post;
   }
   $result .= $s;
   return $result;
}

sub assemble_number {
   local($this, $s) = @_;
   # e.g. 10 9 100 7 10 8 = 1978

   my $middot = "\xC2\xB7";
   my @tokens = split(/$middot/, $s); # middle dot U+00B7
   foreach $power ((10, 100, 1000, 10000, 100000, 1000000, 100000000, 1000000000, 1000000000000)) {
      for (my $i=0; $i <= $#tokens; $i++) {
	 if ($tokens[$i] == $power) {
            if (($i > 0) && ($tokens[($i-1)] < $power)) {
	       splice(@tokens, $i-1, 2, ($tokens[($i-1)] * $tokens[$i]));
	       $i--;
               if (($i < $#tokens) && ($tokens[($i+1)] < $power)) {
	          splice(@tokens, $i, 2, ($tokens[$i] + $tokens[($i+1)]));
	          $i--;
	       }
	    }
	 } 
	 # 400 300 (e.g. Egyptian)
	 my $gen_pattern = $power;
         $gen_pattern =~ s/^1/\[1-9\]/;
         if (($tokens[$i] =~ /^$gen_pattern$/) && ($i < $#tokens) && ($tokens[($i+1)] < $power)) {
	    splice(@tokens, $i, 2, ($tokens[$i] + $tokens[($i+1)]));
	    $i--;
	 }
      }
      last if $#tokens == 0;
   }
   my $result = join($middot, @tokens);
   return $result;
}

1;

