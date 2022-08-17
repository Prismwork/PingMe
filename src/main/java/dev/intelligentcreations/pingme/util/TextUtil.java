package dev.intelligentcreations.pingme.util;

import com.google.common.base.Strings;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextUtil {
    public static int getLastPattern(String input, Pattern pattern){
        if (Strings.isNullOrEmpty(input)) {
            return 0;
        }
        int i = 0;
        Matcher matcher = pattern.matcher(input);
        while (matcher.find()) {
            i = matcher.end();
        }
        return i;
    }
}
