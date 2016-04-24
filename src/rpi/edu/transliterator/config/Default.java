package rpi.edu.transliterator.config;

import java.io.File;

public class Default {
    public static final boolean DEBUG = false;

    public static final Character[] TOKEN_SEPARATOR
            = {'-', '－', ' ', ',', '•', '·', '_', '·', ':', '(', ')', '.', '．', '・'};
    public static final String TOKEN_SEPARATOR_STRING = "-,•·_·:().．・ －";


    public static final char SEGMENT_DELIMITER_CHAR    = '\u2063';
    public static final char PAIR_DELIMITER_CHAR       = '\u2064';
    public static final char START_SYMBOL_CHAR         = '\u2045';
    public static final char END_SYMBOL_CHAR           = '\u2046';
    public static final char VOID_TRANSLITERATION_CHAR = '\u2060';
    public static final char START_UNIT_CHAR           = '\u2e28';
    public static final char END_UNIT_CHAR             = '\u2e29';
    public static final char SPACE_SYMBOL_CHAR         = '◊';

    public static final String SEGMENT_DELIMITER   = SEGMENT_DELIMITER_CHAR + "";
    public static final String PAIR_DELIMITER      = PAIR_DELIMITER_CHAR + "";
    public static final String START_SYMBOL        = START_SYMBOL_CHAR + "";
    public static final String END_SYMBOL          = END_SYMBOL_CHAR + "";
    public static final String VOID_TRANSLITERTION = VOID_TRANSLITERATION_CHAR + "";
    public static final String START_UNIT          = START_UNIT_CHAR + "";
    public static final String END_UNIT            = END_UNIT_CHAR + "";
    public static final String SPACE_SYMBOL        = SPACE_SYMBOL_CHAR + "";

    public static final String MODEL_FILE_PATTERN = "${MODEL_DIR}"
            + File.separator + "${SOURCE_LANG}" + File.separator + "${SOURCE_LANG}"
            + "_" + "${TARGET_LANG}" + "." + "${MODEL_PARAM}" + ".model.txt";
    public static final String MAP_FILE_PATTERN = "${MAP_DIR}"
            + File.separator + "${MAP_SOURCE_LANG}" + "_"// File.separator
            + "${MAP_TARGET_LANG}" + "." + "${MAP_PARAM}" + ".map.txt";

    public static final boolean IGNORE_UNLINKED = false;

    public static final String MODEL_FILE_DIR  = "";
    public static final String SOURCE_LANGUAGE = "";
    public static final String TARGET_LANGUAGE = "";
    public static final String MODEL_PARAMETER = "";
    public static final boolean IGNORE_CASE = false;

    public static final String MAP_FILE_DIR = "";
    public static final String MAP_SOURCE_LANGUAGE = "";
    public static final String MAP_TARGET_LANGUAGE = "";
    public static final String MAP_PARAMETER = "";
    public static final double MAP_PROBABILITY_THRESHOLD = 0.2;

    public static final int PAIR_GRAM_NUMBER   = 3;
    public static final int SOURCE_GRAM_NUMBER = 3;

    public static final int TARGET_GRAM_NUMBER = 3;

    public static final double MIN_PROBABILITY = 10e-7;

    public static final int SEGMENT_MAX_CHAR_NUMBER = 3;
    public static final int SEARCH_WINDOW_LENGTH    = 3;
    public static final int SEARCH_INITIAL_WINDOW   = 4;
    public static final boolean UNIT_POSITION = false;

    public static final int TRIM_NUMBER = 50;
    public static final int BEAM_SIZE  = 20;
    public static final int FINAL_NUMBER = 10;

    public static final double JELINEK_MERCER_LAMBDA = 0.9;
    public static final double JM_LAMBDA_MIN = 0.5;
    public static final double JM_LAMBDA_MAX = 1.0;
    public static final int JM_LAMBDA_THRES  = 10;
    public static final double[] PAIR_GRAM_WEIGHT    = {.1, .3, 1.0};
    public static final double[] SOURCE_GRAM_WEIGHT  = {.1, .3, 1.0};
    public static final double[] TARGET_GRAM_WEIGHT  = {.1, .3, 1.0};

    public static final int LINK_NUMBER_THRESHOLD   = 3;
    public static final double LINK_SCORE_THRESHOLD = .6;
    public static final double SIMILARITY_THRESHOLD = .7;
    public static final boolean TYPE_FILTER = false;

    public static final double BIAS = .3;
    public static final int ENTITY_NUM = 10;

    /* APIs */
    public static final String LINKING_URL = "http://blender02.cs.rpi.edu:3301/linking?query=";
    public static final String TYPING_URL  = "http://blender02.cs.rpi.edu:3303/getrdftypes?query=";

    public static final String LOG_FILE = "";
    public static final String TYPE_LIST_FILE = "config/type_list.txt";
    public static final String TYPE_PREFIX = "http://dbpedia.org/ontology/";

    public static final String JAR_NAME = "trans.jar";
}
