package extractor;
import edu.stanford.nlp.coref.CorefCoreAnnotations.CorefChainAnnotation;
import edu.stanford.nlp.coref.data.CorefChain;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.ling.CoreAnnotations.CoarseNamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.FineGrainedNamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.IndexAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.CoreSentence;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraph.OutputFormat;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.trees.*;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.util.CoreMap;

import java.util.*;
public class TestCoreNLP {
	static StanfordCoreNLP pipeline;
	public static void main(String[] args){
		    Properties props = new Properties();
		    props.setProperty("annotators","tokenize,ssplit,pos,lemma,parse,ner");
//		    props.setProperty("coref.algorithm", "neural");
//		    props.setProperty("tokenize.whitespace","true");
//		    props.setProperty("parse.maxlen", "100");
		    pipeline = new StanfordCoreNLP(props);
		    String text = "the sims 4 ran very smoothly on regular graphics .";
		    Annotation document = new Annotation(text);
		    pipeline.annotate(document);
		    int beginindex;
		    int endindex;
		    String word = null;
		    String pos = null;
		    String lemma = null;
		    String ner = null;
		    Map<Integer, CorefChain> corefChainMap = document.get(CorefChainAnnotation.class);
		    if( corefChainMap!=null && corefChainMap.size()!=0 ) {
		    	for(Map.Entry<Integer, CorefChain> entry:corefChainMap.entrySet() ) {
		   	 		System.out.println(entry.toString());
		    	}
		    }
		    List<CoreMap> sentences = (List<CoreMap>)document.get(CoreAnnotations.SentencesAnnotation.class);
		    for (CoreMap sentence : sentences) {
		    	//先获取当前句子的依存句法分析结果			
		        SemanticGraph dependencies = sentence.get(CollapsedCCProcessedDependenciesAnnotation.class);
		        Tree tree = sentence.get(TreeAnnotation.class);
		        System.out.println(tree.toString());
		    	for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
		    		//System.out.println(token.keySet());
		    		beginindex=token.get(CoreAnnotations.CharacterOffsetBeginAnnotation.class);
		    		endindex=token.get(CoreAnnotations.CharacterOffsetEndAnnotation.class);
		    		endindex = token.endPosition();
		    		word = token.word();
		    		pos = token.get(PartOfSpeechAnnotation.class);
		    		ner = token.get(CoarseNamedEntityTagAnnotation.class);
		    		lemma = token.lemma();
		    		ner = token.ner();
		    		System.out.println("["+beginindex+","+endindex+"]."+lemma+" | "+pos+" | "+ner);
		    	}
                //获取并打印依存句法的结果
		    	System.out.println(sentence.toString());
				System.out.println("Dependency Graph:\n " +dependencies.toString(SemanticGraph.OutputFormat.READABLE));
				IndexedWord root = dependencies.getFirstRoot();
				System.out.println(root);
				List<IndexedWord> rootChildren = dependencies.getChildList(root);
				for(int i=0;i<rootChildren.size();i++) {
					IndexedWord child = rootChildren.get(i);
					SemanticGraphEdge edge = dependencies.getEdge(root,child);					
					System.out.println(edge.getRelation());
				}
				System.out.println( dependencies.getChildList(root) );
			} 
		    
	}
	
}