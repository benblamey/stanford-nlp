/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package test.java.edu.stanford.nlp.time.distributed;

import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.AnnotationPipeline;
import edu.stanford.nlp.time.TimeAnnotations;
import edu.stanford.nlp.time.distributed.DistributedMain;
import edu.stanford.nlp.util.CoreMap;
import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Ben
 */
public class DistributedSUTimeTest {
    
    private AnnotationPipeline _pipeline;
    
    public DistributedSUTimeTest() {
    }


    @Before
    public void setUp() throws Exception {
        _pipeline = DistributedMain.getPipeline();
    }
    
    @org.junit.Test
    public void testSummer() throws Exception {
        Annotation annotation = new Annotation("Summer");
        _pipeline.annotate(annotation);
        assertTrue(annotation.has(TimeAnnotations.TimexAnnotations.class));
        "".toCharArray();
    }


//    @org.junit.Test
//    public void testSummer2012() throws Exception {
//        Annotation annotation = new Annotation("Summer 2012");
//        _pipeline.annotate(annotation);
//        assertTrue(annotation.has(TimeAnnotations.TimexAnnotations.class));
//    }



}
