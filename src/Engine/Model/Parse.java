package Engine.Model;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Parse implements Runnable{
    // enums
    private double THOUSAND = Math.pow(10, 3);
    private double MILLION = Math.pow(10, 6);
    private double BILLION = Math.pow(10, 9);
    private double TRILLION = Math.pow(10, 12);


    //final ArrayList<String> NUMBER_SIZES = new ArrayList<String>(Arrays.asList("Thousand", "Million", "Billion", "Trillion")){};
    private static ConcurrentHashMap< String , Term  > AllTerms  = new ConcurrentHashMap<>();  // < str_term , obj_term >  // will store all the terms in curpos

    //Pattern NumberThousand = Pattern.compile("\\d* \\w Thousand");
    Pattern NUMBER_SIZE = Pattern.compile("\\d*" + " " + "(Thousand|Million|Billion|Trillion|percent|percentage|Dollars)");
    Pattern DATE_DD_MONTH = Pattern.compile("(3[01]|[0-2][0-9])" + " " + "(January|February|March|April|May|June|July|August|September|October|November|December|JANUARY|FEBRUARY|MARCH|APRIL|MAY|JUNE|JULY|AUGUST|SEPTEMBER|OCTOBER|NOVEMBER|DECEMBER)");
    Pattern DATE_MONTH_DD = Pattern.compile("(January|February|March|April|May|June|July|August|September|October|November|December|JANUARY|FEBRUARY|MARCH|APRIL|MAY|JUNE|JULY|AUGUST|SEPTEMBER|OCTOBER|NOVEMBER|DECEMBER)" + " " + "(3[01]|[0-2][0-9])");
    Pattern PRICE_SIMPLE = Pattern.compile( "$" + "\\d*");
    Pattern PRICE_SIMPLE_SIZE = Pattern.compile( "$" + "\\d*");



    public HashSet<String> parse(String text , Document currDoc) {
        text = remove_stop_words(text);
        String[] tokens = text.split(" ");
        getTerms(tokens ,currDoc);
        System.out.println("Parsing document number: " + currDoc.getDocNo());
        return null;
    }

    /**
     * check witch pattars the tokens match
     * @param tokensArray
     * @param currDoc
     */
    private void getTerms(String[] tokensArray, Document currDoc) {



        for (int i = 0; i < tokensArray.length; ) {


            // check if its date first ..
            //date - < Month + decimal >
            if (i < tokensArray.length - 1) {
                Matcher dateFormatMatcher2 = DATE_MONTH_DD.matcher( tokensArray[i]+ " " + tokensArray[i+1]);
                if (dateFormatMatcher2.find()) {
                    String term = PairTokensIsDate2Format(tokensArray[i], tokensArray[i + 1]);
//                    System.out.println("Term added: " + term);
                    addToDocTerms(term ,currDoc)  ; ;
                    i += 2;
                    continue;
                }
            }

            // check if its $ or % ..

            if ((tokensArray[i].startsWith("$") || tokensArray[i].startsWith("%")) && i < tokensArray.length) {
                if (check1WordPattern(tokensArray[i] , currDoc)) {
                    i += 1;
                    continue;
                }
            }

            if (isNumeric(cleanToken(tokensArray[i])))
            {  // change the term only if the first token is a number !!!!
                if (i < tokensArray.length - 3) {
                    if (check4WordsPattern(tokensArray[i], tokensArray[i + 1], tokensArray[i + 2], tokensArray[i + 3] , currDoc)) {
                        i += 4;
                        continue;
                    }
                }
                if (i < tokensArray.length - 2) {
                    if (check3WordsPattern(tokensArray[i], tokensArray[i + 1], tokensArray[i + 2] , currDoc)) {
                        i += 3;
                        continue;
                    }
                }
                if (i < tokensArray.length - 1) {
                    if (check2WordsPattern(tokensArray[i], tokensArray[i + 1] , currDoc)) {
                        i += 2;
                        continue;
                    }
                }
                if (i < tokensArray.length) {
                    if (check1WordPattern(tokensArray[i] , currDoc)) {
                        i += 1;
                        continue;
                    }
                }

            }
//            System.out.println("Term added: " + cleanToken(tokensArray[i])  );
            addToDocTerms(cleanToken(tokensArray[i]) , currDoc);
            i++ ;
        }
    }

    public static boolean isNumeric(String str)
    {
        try
        {
            double d = Double.parseDouble(str);
        }
        catch(NumberFormatException nfe)
        {
            return false;
        }
        return true;
    }

    private String cleanToken(String token) {
        token = token.replaceAll("[]\\[()?\",]", ""); // clean token
        if (!isNumeric(token))
            token = token.replaceAll("[.]", "");
        //if (!Character.isDigit(token.charAt(0))) token = token.replaceAll("[.]", ""); // clean token
        return token;
    }

    private boolean check1WordPattern(String token, Document currDoc) {
        String term;
        String originalToken = token;
        token = cleanToken(token);
        // < $number >
        if ( token.startsWith("$")  ){
            String temp = token.replace( "$" , "" ) ;
            //if (  Character.isDigit(temp.charAt(0))) {
            if (  isNumeric(temp)) {
                term = get_term_from_simple_price(temp , originalToken);
//                System.out.println("Term added: " + term);
                addToDocTerms(term, currDoc)  ; ;
                return true;
            }
        }
        //< number + % >
        if ( token.endsWith("%") ) {
            term = token;
//            System.out.println("Term added: " + term);
            addToDocTerms(term,currDoc)  ; ;
            return true;
        }

        // < number >
        if (isNumeric(token)){
        //if (Character.isDigit(token.charAt(0))) {
            term = get_term_from_simple_number(token);
//            System.out.println("Term added: " + term);
            addToDocTerms(term , currDoc)  ; ;
            return true;
        }

        // < simple token - just add as is >
        term = token ;
//        System.out.println("Term added: " + term);
        addToDocTerms(term, currDoc)  ; ;

        return false;
    }

    private String get_term_from_simple_price(String token ,String originalToken ) {
        originalToken = originalToken.replace( "$" , "") ;
        double value = Double.parseDouble(token);

        if (isBetween(value, 0, MILLION - 1))
            return originalToken  + " Dollars"  ;

        if (isBetween(value, MILLION, Double.MAX_VALUE))
            return checkVal(value / MILLION) + " M Dollars";

        return "ERROR!!!";

    }

    private boolean check2WordsPattern(String token1, String token2, Document currDoc) {
        String term = "";
        token1 = cleanToken(token1);
        token2 = cleanToken(token2);

        // check <decimal + NumberSize >
        Matcher numberSizeMatcher = NUMBER_SIZE.matcher(token1 + " " + token2);
        if (numberSizeMatcher.find()) {
            token1 =
                    term = PairTokensIsNumberFormat(token1, token2);
//            System.out.println("Term added: " + term);
            addToDocTerms(term , currDoc)  ; ;
            return true;
        }
        //datre < decimal + Month >
        Matcher dateFormatMatcher = DATE_DD_MONTH.matcher(token1 + " " + token2);
        if (dateFormatMatcher.find()) {
            term = PairTokensIsDateFormat(token1, token2);
//            System.out.println("Term added: " + term);
            addToDocTerms(term , currDoc)  ; ;
            return true;
        }
//        //date - < Month + decimal >
//        Matcher dateFormatMatcher2 = DATE_MONTH_DD.matcher(token1 + " " + token2);
//        if (dateFormatMatcher2.find()) {
//            term = PairTokensIsDate2Format(token1, token2);
//            System.out.println("Term added: " + term);
//            addToDocTerms(term)  ; ;
//            return true;
//        }
        return false;
    }

    /**
     * Check if term exits , and updates fields accordanly
     * @param term
     */
    private void addToDocTerms(String term , Document currDoc) {
        if ( term == "")
            return ;

        if ( AllTerms.containsKey(term)) {
            //update the existing term
        }
        else { // new term

            // mutex
            Term obj_term = new Term(0, 0) ;
            obj_term.addDoc(currDoc);
            AllTerms.put ( term  , obj_term ) ;

        }




    }

    private boolean check3WordsPattern(String token1, String token2, String token3, Document currDoc) {
//        String term = "";
//        token1 = cleanToken(token1);
//        token2 = cleanToken(token2);
//        token3 = cleanToken(token2);
//
//        // check <decimal + NumberSize >
//        Matcher numberSizeMatcher = NUMBER_SIZE.matcher(token1 + " " + token2);
//        if (numberSizeMatcher.find()) {
//            token1 =
//                    term = PairTokensIsNumberFormat(token1, token2);
//            System.out.println("Term added: " + term);
//            addToDocTerms(term , currDoc)  ; ;
//            return true;
//        }
//        //datre < decimal + Month >
//        Matcher dateFormatMatcher = DATE_DD_MONTH.matcher(token1 + " " + token2);
//        if (dateFormatMatcher.find()) {
//            term = PairTokensIsDateFormat(token1, token2);
//            System.out.println("Term added: " + term);
//            addToDocTerms(term , currDoc)  ; ;
//            return true;
//        }
////        //date - < Month + decimal >
////        Matcher dateFormatMatcher2 = DATE_MONTH_DD.matcher(token1 + " " + token2);
////        if (dateFormatMatcher2.find()) {
////            term = PairTokensIsDate2Format(token1, token2);
////            System.out.println("Term added: " + term);
////            addToDocTerms(term)  ; ;
////            return true;
////        }
      return false;




    }

    private boolean check4WordsPattern(String token1, String token2, String token3,String token4, Document currDoc) {
        //        String term = "";
//        token1 = cleanToken(token1);
//        token2 = cleanToken(token2);
//        token3 = cleanToken(token2);
//
//        // check <decimal + NumberSize >
//        Matcher numberSizeMatcher = NUMBER_SIZE.matcher(token1 + " " + token2);
//        if (numberSizeMatcher.find()) {
//            token1 =
//                    term = PairTokensIsNumberFormat(token1, token2);
//            System.out.println("Term added: " + term);
//            addToDocTerms(term , currDoc)  ; ;
//            return true;
//        }
//        //datre < decimal + Month >
//        Matcher dateFormatMatcher = DATE_DD_MONTH.matcher(token1 + " " + token2);
//        if (dateFormatMatcher.find()) {
//            term = PairTokensIsDateFormat(token1, token2);
//            System.out.println("Term added: " + term);
//            addToDocTerms(term , currDoc)  ; ;
//            return true;
//        }
////        //date - < Month + decimal >
////        Matcher dateFormatMatcher2 = DATE_MONTH_DD.matcher(token1 + " " + token2);
////        if (dateFormatMatcher2.find()) {
////            term = PairTokensIsDate2Format(token1, token2);
////            System.out.println("Term added: " + term);
////            addToDocTerms(term)  ; ;
////            return true;
////        }
//        return false;
        return false;
    }

    private String getSpecialTermFromTwoTokens(String token, String anotherToken) {
        return null;
    }

    private String PairTokensIsNumberFormat(String token, String anotherToken) {
        String term = "";
        String temp = anotherToken;
        switch (temp) {
            case "Thousand":
                term = token + "K";
                break;
            case "Million":
                term = token + "M";
                break;
            case "Billion":
                term = token + "B";
                break;
            case "percent":
                term = token + "%";
                break;
            case "percentage":
                term = token + "%";
                break;
            case "Dollars":
                term = get_term_from_simple_price( token , token ) ;
                break;
            case "Trillion":
                double value = Double.parseDouble(token) * TRILLION;
                term =get_term_from_simple_number(value + "");
                break;
        }
        return term;
    }

    /* Month DD */
    private String PairTokensIsDate2Format(String token, String anotherToken) {
        String term = "";
        String temp = token;
        switch (temp.toLowerCase()) {
            case "january":
                term = "01-" + anotherToken;
                break;
            case "february":
                term = "02-" + anotherToken;
                break;
            case "march":
                term = "03-" + anotherToken;
                break;
            case "april":
                term = "04-" + anotherToken;
                break;
            case "may":
                term = "05-" + anotherToken;
                break;
            case "june":
                term = "06-" + anotherToken;
                break;
            case "july":
                term = "07-" + anotherToken;
                break;
            case "august":
                term = "08-" + anotherToken;
                break;
            case "september":
                term = "09-" + anotherToken;
                break;
            case "october":
                term = "10-" + anotherToken;
                break;
            case "november":
                term = "11-" + anotherToken;
                break;
            case "december":
                term = "12-" + anotherToken;
                break;
        }
        return term;

    }

    /* DD Month */
    private String PairTokensIsDateFormat(String token, String anotherToken) {
        String term = "";
        String temp = anotherToken;
        switch (temp.toLowerCase()) {
            case "january":
                term = "01-" + token;
                break;
            case "february":
                term = "02-" + token;
                break;
            case "march":
                term = "03-" + token;
                break;
            case "april":
                term = "04-" + token;
                break;
            case "may":
                term = "05-" + token;
                break;
            case "june":
                term = "06-" + token;
                break;
            case "july":
                term = "07-" + token;
                break;
            case "august":
                term = "08-" + token;
                break;
            case "september":
                term = "09-" + token;
                break;
            case "october":
                term = "10-" + token;
                break;
            case "november":
                term = "11-" + token;
                break;
            case "december":
                term = "12-" + token;
                break;
        }
        return term;
    }

    /**
     * check and handle a token of decimal num
     *
     * @param token a number
     */
    private String get_term_from_simple_number(String token) {
        double value = Double.parseDouble(token);

        if (isBetween(value, 0, THOUSAND - 1))
            return checkVal(value) + "";
        if (isBetween(value, THOUSAND, MILLION - 1))
            return checkVal(value / THOUSAND) + "K";

        if (isBetween(value, MILLION, BILLION - 1))
            return checkVal(value / MILLION) + "M";

        if (isBetween(value, BILLION, Double.MAX_VALUE))
            return checkVal(value / BILLION) + "B";


        return "ERROR!!!";
    }

    private String checkVal(double v) {
        Double d = v;
        if (v == d.intValue())
            return d.intValue() + "";
        else return v + "";
    }

    public static boolean isBetween(double x, double lower, double upper) {
        return lower <= x && x <= upper;
    }

    public String remove_stop_words(String str) {
        String res = "";
        int k = 0, i, j;
        ArrayList<String> wordsList = new ArrayList<String>();
        ArrayList<String> result = new ArrayList<String>();
        String sCurrentLine;
        String[] stopwords = new String[2000];
        try {
            FileReader fr = new FileReader("src\\Engine\\resources\\stop_words.txt"); // read stop words from the file
            BufferedReader br = new BufferedReader(fr);
            while ((sCurrentLine = br.readLine()) != null) {
                stopwords[k] = sCurrentLine;
                k++;
            }
            StringBuilder builder = new StringBuilder(str);
            String[] words = builder.toString().split("\\s");
            for (String word : words) {
                wordsList.add(word);
                result.add(word);
            }
            for (int ii = 0; ii < wordsList.size(); ii++) {
                for (int jj = 0; jj < k; jj++) {
                    if (stopwords[jj].contains(wordsList.get(ii).toLowerCase())) {
                        result.remove(wordsList.get(ii));
                        break;
                    }
                }
            }
            for (String s : result) {
                res += s + " ";
            }
        } catch (Exception ex) {
            System.out.println(ex);
        }

        return res;
    }

    @Override
    public void run() {

    }
}




