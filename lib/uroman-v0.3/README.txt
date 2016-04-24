uroman version 0.3 
Release date: January 22, 2016
Author: Ulf Hermjakob, USC Information Sciences Institute

uroman is a universal romanizer. It converts text in any script to the Latin alphabet.

Usage: uroman.pl < STDIN
   or: uroman.pl -l <lang-code> < STDIN
       where the optional <lang-code> is a 3-letter languages code, e.g. tur, ukr, yid.
Examples: bin/uroman.pl < text/zho.txt
          bin/uroman.pl -l tur < text/tur.txt

Identifying the input as Turkish, Ukrainian or Yiddish will improve romanization
for those languages as some letters in those languages have different sound values
from other languages using the same script (French, Russian, Hebrew respectively).
No effect for other languages in this version.

New features in version 0.3
 * Covers Mandarin (Chinese)
 * Improved romanization for numerous languages
 * Preserves capitalization (e.g. from Latin, Cyrillic, Greek scripts)
 * Maps from native digits to Western numbers
 * Faster for South Asian languages

Other features
 * Web interface: http://www.isi.edu/~ulf/uroman.html
 * Vowelization is provided when locally computable, e.g. for many South Asian
   languages and Tibetan.

Limitations
 * This version of uroman assumes all CJK ideographs to be Mandarin (Chinese).
   This means that Japanese kanji are incorrectly romanized; however, Japanese
   hiragana and katakana are properly romanized.
 * A romanizer is not a full transliterator. For example, this version of
   uroman does not vowelize text that lacks explicit vowelization such as
   normal text in Arabic and Hebrew (without diacritics/points).

