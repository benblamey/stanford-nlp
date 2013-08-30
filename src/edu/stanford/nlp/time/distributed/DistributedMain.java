package edu.stanford.nlp.time.distributed;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.tokensregex.MatchedExpression;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.AnnotationPipeline;
import edu.stanford.nlp.pipeline.POSTaggerAnnotator;
import edu.stanford.nlp.pipeline.WhitespaceTokenizerAnnotator;
import edu.stanford.nlp.pipeline.WordsToSentencesAnnotator;
import edu.stanford.nlp.time.GUTimeAnnotator;
import edu.stanford.nlp.time.HeidelTimeAnnotator;
import edu.stanford.nlp.time.TimeAnnotations;
import edu.stanford.nlp.time.TimeAnnotator;
import edu.stanford.nlp.time.Timex;
import edu.stanford.nlp.time.XMLUtils;
import edu.stanford.nlp.util.CollectionFactory;
import edu.stanford.nlp.util.CollectionValuedMap;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.HasInterval;
import edu.stanford.nlp.util.StringUtils;
import edu.stanford.nlp.util.ValuedInterval;

public class DistributedMain {

    public static void main(String[] args) throws Exception {
        String date = null;//"2013-04-23";// props.getProperty("date");



        // Tries searching the classpath by default (see: IOUtils)
        //       String uri = "edu/stanford/nlp/models/pos-tagger/english-left3words/english-left3words-distsim.tagger";

        //SUTime.class.getResource().toString();

        //    System.err.println(uri);

//        Properties props = StringUtils.argsToProperties(new String[0]);
//        props.put("pos.model", uri);
        AnnotationPipeline pipeline;
        pipeline = getPipeline();

        String in = "Summer '12 "
                + "\n\n foo '12"
                + "\n\n fffSummer '11.";

        processText(pipeline, in, date);
    }

    public static AnnotationPipeline getPipeline() throws Exception {

        Properties props = new Properties();

        AnnotationPipeline pipeline = new AnnotationPipeline();


        // include EOL when tokenizing
        props.put(WhitespaceTokenizerAnnotator.EOL_PROPERTY, "true");

        props.put("sutime.rules",
                "edu/stanford/nlp/models/sutime/distributed.defs.txt,"
                + "edu/stanford/nlp/models/sutime/distributed.defs.g.txt,"
                + "edu/stanford/nlp/models/sutime/defs.sutime.txt,"
                + "edu/stanford/nlp/models/sutime/english.sutime.txt,"
                + "edu/stanford/nlp/models/sutime/english.holidays.sutime.txt");

        props.put("sutime.verbose", true);

        pipeline.addAnnotator(new WhitespaceTokenizerAnnotator(props));
        //WhitespaceTokenizerFactory

        //pipeline.addAnnotator(new PTBTokenizerAnnotator(PTBTokenizerAnnotator.DEFAULT_OPTIONS + ",tokenizeNLs"));

        final boolean endOfLineIsEndOfSentence = true;

        String end_of_sentence_regex;

        // The default in WordToSentenceProcessor include the apostrophe ('), which causes problem parsing "Summer '11".
        if (endOfLineIsEndOfSentence) {
            end_of_sentence_regex = "(\\.|[!?]+)[\\r\\n]*";
        } else {
            end_of_sentence_regex = "\\.|[!?]+";
        }

        pipeline.addAnnotator(new WordsToSentencesAnnotator(true, end_of_sentence_regex)); // true to debug

        pipeline.addAnnotator(new POSTaggerAnnotator(
                props.getProperty("pos.model"),
                false));
//    pipeline.addAnnotator(new NumberAnnotator(false));
//    pipeline.addAnnotator(new QuantifiableEntityNormalizingAnnotator(false, false));
        String timeAnnotator = props.getProperty("timeAnnotator", "sutime");
        if ("gutime".equals(timeAnnotator)) {
            //useGUTime = true;
            pipeline.addAnnotator(new GUTimeAnnotator());
        } else if ("heideltime".equals(timeAnnotator)) {
            //requiredDocDateFormat = "yyyy-MM-dd";
            pipeline.addAnnotator(new HeidelTimeAnnotator("heideltime", props));
        } else if ("sutime".equals(timeAnnotator)) {
            pipeline.addAnnotator(new TimeAnnotator("sutime", props));
        } else {
            throw new IllegalArgumentException("Unknown timeAnnotator: " + timeAnnotator);
        }
        return pipeline;
    }

    public static List<Node> createTimexNodes(String str, Integer charBeginOffset, List<CoreMap> timexAnns) {
        List<ValuedInterval<CoreMap, Integer>> timexList = new ArrayList<ValuedInterval<CoreMap, Integer>>(timexAnns.size());
        for (CoreMap timexAnn : timexAnns) {
            timexList.add(new ValuedInterval<CoreMap, Integer>(timexAnn,
                    MatchedExpression.COREMAP_TO_CHAR_OFFSETS_INTERVAL_FUNC.apply(timexAnn)));
        }
        Collections.sort(timexList, HasInterval.CONTAINS_FIRST_ENDPOINTS_COMPARATOR);
        return createTimexNodesPresorted(str, charBeginOffset, timexList);
    }

    private static List<Node> createTimexNodesPresorted(String str, Integer charBeginOffset, List<ValuedInterval<CoreMap, Integer>> timexList) {
        if (charBeginOffset == null) {
            charBeginOffset = 0;
        }
        List<Node> nodes = new ArrayList<Node>();
        int previousEnd = 0;
        List<Element> timexElems = new ArrayList<Element>();
        List<ValuedInterval<CoreMap, Integer>> processed = new ArrayList<ValuedInterval<CoreMap, Integer>>();
        CollectionValuedMap<Integer, ValuedInterval<CoreMap, Integer>> unprocessed =
                new CollectionValuedMap<Integer, ValuedInterval<CoreMap, Integer>>(CollectionFactory.<ValuedInterval<CoreMap, Integer>>arrayListFactory());
        for (ValuedInterval<CoreMap, Integer> v : timexList) {
            CoreMap timexAnn = v.getValue();
            int begin = timexAnn.get(CoreAnnotations.CharacterOffsetBeginAnnotation.class) - charBeginOffset;
            int end = timexAnn.get(CoreAnnotations.CharacterOffsetEndAnnotation.class) - charBeginOffset;
            if (begin >= previousEnd) {
                // Add text
                nodes.add(XMLUtils.createTextNode(str.substring(previousEnd, begin)));
                // Add timex
                Timex timex = timexAnn.get(TimeAnnotations.TimexAnnotation.class);
                Element timexElem = timex.toXmlElement();
                nodes.add(timexElem);
                previousEnd = end;

                // For handling nested timexes
                processed.add(v);
                timexElems.add(timexElem);
            } else {
                unprocessed.add(processed.size() - 1, v);
            }
        }
        if (previousEnd < str.length()) {
            nodes.add(XMLUtils.createTextNode(str.substring(previousEnd)));
        }
        for (Integer i : unprocessed.keySet()) {
            ValuedInterval<CoreMap, Integer> v = processed.get(i);
            String elemStr = v.getValue().get(CoreAnnotations.TextAnnotation.class);
            int charStart = v.getValue().get(CoreAnnotations.CharacterOffsetBeginAnnotation.class);
            List<Node> innerElems = createTimexNodesPresorted(elemStr, charStart, (List<ValuedInterval<CoreMap, Integer>>) unprocessed.get(i));
            Element timexElem = timexElems.get(i);
            XMLUtils.removeChildren(timexElem);
            for (Node n : innerElems) {
                timexElem.appendChild(n);
            }
        }
        return nodes;
    }

    public static void processText(AnnotationPipeline pipeline, String text, String date) throws IOException {

        text = text.replace("{", "(").replace("}", ")");


        System.err.println("Processing line: " + text);

        Annotation annotation = textToAnnotation(pipeline, text, date);

        String text2 = annotation.get(CoreAnnotations.TextAnnotation.class);

        List<CoreLabel> tokens = annotation.get(CoreAnnotations.TokensAnnotation.class);
        for (CoreLabel token : tokens) {
            Integer start = token.get(CoreAnnotations.TokenBeginAnnotation.class);
            Integer end = token.get(CoreAnnotations.TokenEndAnnotation.class);
            if (start != null || end != null) {
                String tokenText = text.substring(start, end);
                System.err.println("Token: " + token);
            }
        }



        List<CoreMap> timexes = annotation.get(
                TimeAnnotations.TimexAnnotations.class);
        for (CoreMap timexannotation : timexes) {
            Integer characterOffsetStart = timexannotation.get(CoreAnnotations.CharacterOffsetBeginAnnotation.class);
            Integer characterOffsetEnd = timexannotation.get(CoreAnnotations.CharacterOffsetEndAnnotation.class);
        }

        // Sort the timex annotations according to character offsets.
        List<ValuedInterval<CoreMap, Integer>> timexList = new ArrayList<ValuedInterval<CoreMap, Integer>>(timexes.size());
        for (CoreMap timexAnn : timexes) {
            timexList.add(new ValuedInterval<CoreMap, Integer>(timexAnn,
                    MatchedExpression.COREMAP_TO_CHAR_OFFSETS_INTERVAL_FUNC.apply(timexAnn)));
        }
        Collections.sort(timexList, HasInterval.CONTAINS_FIRST_ENDPOINTS_COMPARATOR);


        {
            annotation.get(CoreAnnotations.SentencesAnnotation.class);

        }

        // Reverse so that we start with the annotations at the end of the text.
        // Collections.reverse(timexList);

        StringBuilder newText = new StringBuilder();

        int lastIndex = 0;

        for (ValuedInterval<CoreMap, Integer> vi : timexList) {
            int characterOffsetStart = vi.getValue().get(CoreAnnotations.CharacterOffsetBeginAnnotation.class);
            int characterOffsetEnd = vi.getValue().get(CoreAnnotations.CharacterOffsetEndAnnotation.class);

            if (characterOffsetStart > 0 && (characterOffsetStart > lastIndex)) {
                newText.append(text.substring(lastIndex, characterOffsetStart));
            }

            Timex get = vi.getValue().get(edu.stanford.nlp.time.TimeAnnotations.TimexAnnotation.class);

            String gnuPlot = get.getGNUPlot();

            System.out.println("plot: " + gnuPlot);
            "".toString();


            newText.append("{"); //  
            newText.append(text.substring(characterOffsetStart, characterOffsetEnd));
            newText.append("}"); // 

            lastIndex = characterOffsetEnd;
        }

        newText.append(text.substring(lastIndex));

        //ProcessTextResult ptr = new ProcessTextResult();
        // ptr.textAreaText = StringEscapeUtils.escapeHtml4(text);

        String highlightedHtml = newText.toString(); // Has { } to indicate annotations at correct indices.

        // Now parsed, so can escape HTML entities in the incoming string.
        //highlightedHtml = StringEscapeUtils.escapeHtml4(highlightedHtml);


        highlightedHtml = highlightedHtml.replace("{", "<span class=\"highlight\">");
        highlightedHtml = highlightedHtml.replace("}", "</span>");
        highlightedHtml = highlightedHtml;

        // Insert tags for carriage returns in HTML.
        highlightedHtml = highlightedHtml.replaceAll("\\r?\\n", "<br/>\n");

        System.out.println(highlightedHtml); //    	List<Node> timexNodes = createTimexNodes(

    }

    public static Annotation textToAnnotation(AnnotationPipeline pipeline, String text, String date) {
        Annotation annotation = new Annotation(text);
        annotation.set(CoreAnnotations.DocDateAnnotation.class, date);
        pipeline.annotate(annotation);
        return annotation;
    }
}