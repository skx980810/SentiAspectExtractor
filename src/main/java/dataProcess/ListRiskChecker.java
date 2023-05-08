package dataProcess;

import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.util.CoreMap;
import extractor.FileIOs;

public class ListRiskChecker {
	public StanfordCoreNLP parser;
	public String labeledFileName;
	public ArrayList<String> labeledAspectList;
	public static void main(String[] args) {
		String path = System.getProperty("user.dir");
		String resourcePath = path+"/src/main/resources/";
		String labeledFileName = resourcePath+"rest_labeled.txt";
		ListRiskChecker riskChecker = new ListRiskChecker();
		riskChecker.initLabeledFile(labeledFileName);
		
		String dictPath = System.getProperty("user.dir")+"/src/main/resources/dictionary/";
		String functionWordListPath = dictPath + "vagueWordList.txt";
		riskChecker.checkRisk( functionWordListPath );
		String implicitAspectWordListPath = dictPath + "implicitAspectWordList.txt";
		riskChecker.checkRisk( implicitAspectWordListPath );
		String peopleWordListPath = dictPath + "peopleWordList.txt";
		riskChecker.checkRisk( peopleWordListPath );
	}
	public String getLabeledFileName() {
		return labeledFileName;
	}
	public void setLabeledFileName(String labeledFileName) {
		this.labeledFileName = labeledFileName;
	}
	public void parserInit() {
		Properties props = new Properties();
		props.setProperty("annotators","tokenize, ssplit, pos, lemma");
		props.setProperty("tokenize.whitespace","true");
		parser = new StanfordCoreNLP(props);
	}
	private ArrayList<String> getLemmaList(String text){
		ArrayList<String> lemmaList = new ArrayList<String>();
		Annotation document = new Annotation(text);
    	parser.annotate(document);
	    List<CoreMap> sentences = (List<CoreMap>)document.get(CoreAnnotations.SentencesAnnotation.class);
	    for(CoreMap sentence: sentences) {
	    	for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
	    		lemmaList.add(token.lemma());
	    	}
        }
	    return lemmaList;
	}
	public void initLabeledFile(String labeledFileName) {
		boolean isFileExists = FileIOs.isFileExists(labeledFileName);
		if( !isFileExists ) {
			System.err.println("文件不存在! "+labeledFileName);
			return;
		}
		System.err.println("正在初始化已标注数据集: "+labeledFileName);
		parserInit();
		labeledAspectList = new ArrayList<String>();
		List<String> lineList = FileIOs.readFileGetStringList(labeledFileName);
		//跳过第一行，第一行为标题
		for(int i=1;i<lineList.size();i++) {
			String inputText = lineList.get(i);
			String[] elems = inputText.split("\t");
			String sourceName = elems[0]; //第一列为数据源;
			String textIndex = elems[1]; //第二列为文本序号;
			String text = elems[2]; //第三列为文本;
			String opListString = elems[3]; //第四列为opinion列表;
			String apListString_standard = elems[4]; //第五列为人工分析的结果列表;	
			ArrayList<String> opList = getOpinionList(opListString);
			HashMap<String,ArrayList<int[]>> opApListMap_standard = getStandardMap(apListString_standard);
			ArrayList<String> lemmaList = getLemmaList(text);
			for(int j=0;j<opList.size();j++) {
				String opString = opList.get(j);
				ArrayList<int[]> apList_standard = opApListMap_standard.get(opString);
				for(int k=0;k<apList_standard.size();k++) {
					int[] apIndexArr = apList_standard.get(k);
					String aspectText = getArrSlice(lemmaList,apIndexArr[0],apIndexArr[1]);
					labeledAspectList.add(aspectText);
				}
			}
		}	
	}
	
	private ArrayList<String> getOpinionList(String opListString){
		ArrayList<String> opList = new ArrayList<String>();
		String[] elems = opListString.split("; ");
		for(int i=0;i<elems.length;i++) {
			String opString = elems[i];
			opList.add( opString.replace("\\s+","") );
		}
		return opList;
	}
	
	private HashMap<String,ArrayList<int[]>> getStandardMap(String apListString_standard) {
		HashMap<String,ArrayList<int[]>> opApListMap = new HashMap<String,ArrayList<int[]>>();
		String[] elems = apListString_standard.split("; ");
		for(int i=0;i<elems.length;i++) {
			String elem = elems[i];
			int firstColonIndex = elem.indexOf(":");
			String opinionString = elem.substring(0,firstColonIndex);
			String acosListString = elem.substring(firstColonIndex+2,elem.length()-1);
			String[] acosStringArr = acosListString.split(", ");
			ArrayList<int[]> apList = new ArrayList<int[]>();
			for(int j=0;j<acosStringArr.length;j++) {
				String acosString = acosStringArr[j];
				String[] acosArr = acosString.split(" ");
				String aspect = acosArr[0];
				int[] apIndexArr = getIndexArr(aspect);
				apList.add(apIndexArr);
			}
			opApListMap.put(opinionString, apList);
		}
		return opApListMap;
	}
	
	private int[] getIndexArr(String indexString) {
		indexString = indexString.trim();
		String[] indexStringArr = indexString.split(",");
		int startIndex = Integer.parseInt(indexStringArr[0]);
		int endIndex = Integer.parseInt(indexStringArr[1]);
		int[] indexArr = new int[2];
		indexArr[0] = startIndex;
		indexArr[1] = endIndex;
		return indexArr;
	}
	
	private String getArrSlice(String[] arr,int start,int end) {
		String slice = "";
		for(int i=start;i<end;i++) {
			String unit = arr[i];
			slice += unit+" ";
		}
		return slice.trim();
	}
	
	private String getArrSlice(ArrayList<String> list,int start,int end) {
		String slice = "";
		for(int i=start;i<end;i++) {
			String unit = list.get(i);
			slice += unit+" ";
		}
		return slice.trim();
	}
	
	public void checkRisk(String tergetFileName) {
		boolean isFileExists = FileIOs.isFileExists(tergetFileName);
		if( !isFileExists ) {
			System.err.println("文件不存在! "+tergetFileName);
			return;
		}
		System.out.println("正在检查文件: "+tergetFileName);
		List<String> lineList = FileIOs.readFileGetStringList(tergetFileName);
		for(String line:lineList) {
			line = line.trim();
			if( line.length()==0 ) {
				continue;
			}
			int occur = countOccurInLabeled(line);
			if( occur>0 ) {
				System.out.println(line+": "+occur);
			}
		}
	}
	
	private int countOccurInLabeled(String word) {
		int sum = 0;
		for(String labeledAspect:labeledAspectList) {
			String[] elems = labeledAspect.split("\\s+");
			for(String elem:elems) {
				if( elem.equals(word) ) {
					sum++;
					break;
				}
			}
		}
		return sum;
	}

}
