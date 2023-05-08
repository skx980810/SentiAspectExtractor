package extractor;

import java.io.IOException;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONObject;

import edu.stanford.nlp.io.EncodingPrintWriter.out;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

public class SentiAspectExtractor {
	public AnalysisOptions opt;
	private String inputFileName = "";
	private String outputFileName = "";
	private String sResultsFileExtension = "_out.txt";
	public static void main(String[] args) throws IOException {
		SentiAspectExtractor extractor = new SentiAspectExtractor();
//		String inputFileName = "test_input";
//		String path = System.getProperty("user.dir")+"/src/main/resources/";
//		String inputFilePath = path+inputFileName;
//		String argStr = "-inputfile "+inputFilePath;
////		               +" -coreExtendType "+2;
////		               +" -outputFormat "+0
//		              // +" -explain ";
//		extractor.initialiseAndRun(argStr.split(" "));
		extractor.initialiseAndRun(args);
	}
	public void initialiseAndRun(String[] args) {
		opt = new AnalysisOptions();
		boolean[] argumentRecognised = new boolean[args.length];
		for(int i = 0; i < args.length; i++ ) {
			if ( args[i].equalsIgnoreCase("-help") ) {
				help();
				argumentRecognised[i] = true;
	        }
			else if ( args[i].equalsIgnoreCase("-inputfile") ) {
				this.inputFileName = args[i + 1];
				argumentRecognised[i] = true;
				argumentRecognised[i + 1] = true;
	        }
			else if( args[i].equalsIgnoreCase("-outputfile") ) {
				this.outputFileName = args[i + 1];
				argumentRecognised[i] = true;
				argumentRecognised[i + 1] = true;
			}
			else if( args[i].equalsIgnoreCase("-dict") ) {
				opt.dictPath = args[i + 1];
				argumentRecognised[i] = true;
				argumentRecognised[i + 1] = true;
			}
			else if( args[i].equalsIgnoreCase("-explain") ) {
				opt.isExplain = true;
				argumentRecognised[i] = true;
			}
			else if( args[i].equalsIgnoreCase("-coreextendtype") ) {
				opt.coreExtendType = Integer.parseInt(args[i + 1]);
				argumentRecognised[i] = true;
				argumentRecognised[i + 1] = true;
			}
			else if( args[i].equalsIgnoreCase("-outputformat") ) {
				opt.outputFormat = Integer.parseInt(args[i + 1]);
				argumentRecognised[i] = true;
				argumentRecognised[i + 1] = true;
			}
			else if( args[i].equalsIgnoreCase("-implicitopiniondealtype") ) {
				opt.implicitOpinionDealType = Integer.parseInt(args[i + 1]);
				argumentRecognised[i] = true;
				argumentRecognised[i + 1] = true;
			}
			else if( args[i].equalsIgnoreCase("-istextpreprocessing") ) {
				opt.isTextPreprocessing = Boolean.parseBoolean(args[i + 1]);
				argumentRecognised[i] = true;
				argumentRecognised[i + 1] = true;
			}
	    }
		for(int i = 0; i < args.length; i++) {
			if ( !argumentRecognised[i] ) {
				System.out.println("Unrecognised command - wrong spelling or case?: " + args[i]);
	            return;
	        }
	    }
		
		if( !FileIOs.isFileExists(this.inputFileName) ) {
			System.out.println("Input file is not set or does not exist!");
			return;
		}
		
		if( outputFileName==null || outputFileName.length()==0 ) {
			outputFileName = FileIOs.getNextAvailableFilename(FileIOs.s_ChopFileNameExtension(inputFileName), sResultsFileExtension);
		}
		
		//开始解析aspect
		init();
		extractSentiObject();
		
	}
	
	public void help() {
		System.out.println("Here we will explain some parameters:");
		System.out.println("    -inputFile\tSet the input file address.");
		System.out.println("    -outputFile\tSet the output file path. If not set, the analysis result will be output in the same directory as the input file.");
		System.out.println("    -dict\tSet the address of the dictionary to be used for analysis.");
		System.out.println("    -explain\tSet whether to output the reason for extracting the aspect when outputting the result.");
		System.out.println("    -coreExtendType\tSet the expansion method for noun aspects.");
		String placeHolder = String.format("%" + "    -coreextendtype".length() + "s", "") ;
		System.out.println( placeHolder + "\t" + "0 corresponds to AER-3.1, 1 corresponds to AER-3.2, 2 corresponds to AER-3.3.\n"
		                  + placeHolder + "\t" + "The default parameter is 0." );
		System.out.println("    -outputFormat\tSet the output format of the result");
		placeHolder = String.format("%" + "    -outputformat".length() + "s", "") ;
		System.out.println( placeHolder + "\t" + "0 represents the text-based output format, such as '[4,5]:[0,1] , [5,6] ;'");
		System.out.println( placeHolder + "\t" + "1 represents the JSON output format, such as '{\"opinion\": [1,2], \"aspect\": [3,4]}'");
		System.out.println( placeHolder + "\t" + "The default parameter is 1.");
		System.out.println( "    -implicitOpinionDealType\tSet the handling method for illegal or implicit opinions.");
		placeHolder = String.format("%" + "    -implicitOpinionDealType".length() + "s", "") ;
		System.out.println( placeHolder + "\t" + "0: Output 'cannot_deal'");
		System.out.println( placeHolder + "\t" + "1: Output [-1, -1] as an aspect");
		System.out.println( placeHolder + "\t" + "2: Do not output this opinion");
		System.out.println( placeHolder + "\t" + "The default parameter is 1.");
		System.out.println( "    -isTextPreprocessing\tSet whether to perform text preprocessing. The default parameter is true.");
	}
	
	public void init() {
		spliterInit();
		opt.updatePath();
		textExtractor = new TextExtractor();
		textExtractor.setOption(opt);
		textExtractor.init();
	}
	public TextExtractor textExtractor;
	private boolean isSpliterInit = false;
	private StanfordCoreNLP spliter;
	private void spliterInit() {
		if( !isSpliterInit ) {
			Properties props = new Properties();
		    props.setProperty("annotators","tokenize, ssplit");
		    spliter = new StanfordCoreNLP(props);
		    isSpliterInit = true;
		}
	}
	
	private boolean isLegalIndexArr(int[] indexArr) {
		if(indexArr.length!=2 || indexArr[0]<0 || indexArr[1]<0 || indexArr[0]==indexArr[1] ) {
			return false;
		}
		return true;
	}
	
	public void extractSentiObject() {
		System.out.println(LocalDateTime.now()+" Aspect 分析开始!");
		List<String> inputTextList = FileIOs.readFileGetStringList(inputFileName);
		ArrayList<String> outputTextList = new ArrayList<String>();
		for(int i=0;i<inputTextList.size();i++) {
			String inputText = inputTextList.get(i);
			String[] elems = inputText.split("\t");
			//合法性检查
			if( elems.length<2 ) {
				continue;
			}
			String text = elems[0];
			String opListString = elems[1];
			ArrayList<int[]> opList = transStringToOPList(opListString);
			
			//优化文本
			HashMap<Integer,Integer> oriIndexTokenIndexMap = new HashMap<Integer,Integer>();
			HashMap<Integer,Integer> tokenIndexOriIndexMap = new HashMap<Integer,Integer>();
			text = optimizeText(text,oriIndexTokenIndexMap,tokenIndexOriIndexMap);
			
			textExtractor.parseText(text);
			//存储每个 opinion 对应的 aspect 列表：
			ArrayList< ArrayList<Aspect> > aspectListOfOpinion = new ArrayList< ArrayList<Aspect> >();
			for(int j=0;j<opList.size();j++) {
				int[] oriIndexOPArr = opList.get(j);
				// implicit opinion 
				if( !isLegalIndexArr(oriIndexOPArr) ) {
					aspectListOfOpinion.add(null);
				}
				//explicit opinion:
				else {
					int[] tokenIndexOPArr = transOriIndexArrToTokenIndexArr(oriIndexOPArr,oriIndexTokenIndexMap);
					ArrayList<Aspect> aspectList = textExtractor.extractForOpinion(tokenIndexOPArr);
					aspectListOfOpinion.add(aspectList);
				}
			}
			
			//形成输出文本
			String rsForText = shapeOutputText(opList,aspectListOfOpinion,tokenIndexOriIndexMap);
			String outputText = "";
			if( opt.outputFormat==0 ) {
				outputText = inputText+"\t"+rsForText;
				boolean isAppend = true;
				FileIOs.writeStringToFile(outputFileName, outputText, isAppend);
			}
			else if( opt.outputFormat==1 ) {
				outputText = rsForText;
				outputTextList.add(outputText);
			}
			
		}
		
		if( opt.outputFormat==1 ) {
			String rsText = outputTextList.toString();
			FileIOs.writeStringToFile(outputFileName, rsText, false);
		}
		
		System.out.println(LocalDateTime.now()+" Aspect 分析结束!");
		System.out.println("结果输出在:" + outputFileName);
	}
	
	private String shapeOutputText(ArrayList<int[]> opList,ArrayList< ArrayList<Aspect> > aspectListOfOpinion,HashMap<Integer,Integer> tokenIndexOriIndexMap) {
		String rsForText = "";
		int[] nullArr = new int[2];
		nullArr[0] = -1;
		nullArr[1] = -1;
		String nullArrString = transIntArrToString(nullArr);
		if( opt.outputFormat==0 ) {
			for(int i=0;i<opList.size();i++) {
				int[] oriIndexOPArr = opList.get(i);
				String oriIndexOPStr = transIntArrToString(oriIndexOPArr);
				ArrayList<Aspect> aspectList = aspectListOfOpinion.get(i);
				String rsForOpinion = "" ;
				// implicit opinion 
				if( aspectList==null ) {
					if( opt.implicitOpinionDealType==0 ) {
						rsForOpinion += oriIndexOPStr+":cannot_deal";
					}else if( opt.implicitOpinionDealType==1 ) {
						rsForOpinion += oriIndexOPStr+":"+nullArrString;
					}else if( opt.implicitOpinionDealType==2 ) {
						continue;
					}
				}
				//explicit opinion:
				else {
					if( aspectList.size()==0 ) {
						rsForOpinion += oriIndexOPStr + ":" + nullArrString;
						if( opt.isExplain ) {
							rsForOpinion += "null";
						}
					}else {
						rsForOpinion += oriIndexOPStr + ":";
						for(int k=0;k<aspectList.size();k++) {
							Aspect ap = aspectList.get(k);
							int[] aspectTokenIndexArr = ap.getTokenIndexArr();
							int[] aspectOriIndexArr = transTokenIndexArrToOriIndexArr(aspectTokenIndexArr,tokenIndexOriIndexMap);
							rsForOpinion += transIntArrToString(aspectOriIndexArr);
							if( opt.isExplain ) {
								rsForOpinion += ap.getReasonForSelection();
							}
							if( k!=aspectList.size()-1 ) {
								rsForOpinion += " , ";
							}
						}
					}
				}
				rsForText += rsForOpinion+"; ";
			}
		}
		else if( opt.outputFormat==1 ) {
			ArrayList<JSONObject> jsonObjectList = new ArrayList<JSONObject>();
			for(int i=0;i<opList.size();i++) {
				int[] oriIndexOPArr = opList.get(i);
				ArrayList<Aspect> aspectList = aspectListOfOpinion.get(i);
				// implicit opinion 
				if( aspectList==null ) {
					if( opt.implicitOpinionDealType==0 ) {
						JSONObject object = new JSONObject();
						object.put("opinion_index",i);
						object.put("opinion", transIntArrToList(oriIndexOPArr) );
						object.put("aspect","cannot_deal");
						jsonObjectList.add(object);
					}else if( opt.implicitOpinionDealType==1 ) {
						JSONObject object = new JSONObject();
						object.put("opinion_index",i);
						object.put("opinion", transIntArrToList(oriIndexOPArr) );
						object.put("aspect", transIntArrToList(nullArr) );
						jsonObjectList.add(object);
					}else if( opt.implicitOpinionDealType==2 ) {
						continue;
					}
				}
				//explicit opinion:
				else {
					if( aspectList.size()==0 ) {
						JSONObject object = new JSONObject();
						object.put("opinion_index",i);
						object.put("opinion",transIntArrToList(oriIndexOPArr) );
						object.put("aspect", transIntArrToList(nullArr) );
						if( opt.isExplain ) {
							object.put("explain","null");
						}
						jsonObjectList.add(object);
					}else {
						for(int k=0;k<aspectList.size();k++) {
							Aspect ap = aspectList.get(k);
							int[] aspectTokenIndexArr = ap.getTokenIndexArr();
							int[] aspectOriIndexArr = transTokenIndexArrToOriIndexArr(aspectTokenIndexArr,tokenIndexOriIndexMap);
							JSONObject object = new JSONObject();
							object.put("opinion_index",i);
							object.put("opinion", transIntArrToList(oriIndexOPArr) );
							object.put("aspect",  transIntArrToList(aspectOriIndexArr) );
							if( opt.isExplain ) {
								object.put("explain",ap.getReasonForSelection());
							}
							jsonObjectList.add(object);
						}
					}
				}
			}
			rsForText = jsonObjectList.toString();
		}
		return rsForText;
	}
	
	private String[] abbrArr = {"t","d","m","s","re","ve","ll"};
	private String[] listSymbolArr = {"+","-","*"};
	private boolean isNumeric(String str) {
        return StringUtils.isNumeric(str);
    }
	private String optimizeText(String text,HashMap<Integer,Integer> oriIndexTokenIndexMap,HashMap<Integer,Integer> tokenIndexOriIndexMap) {
		spliterInit();
		String optimizedText = "";
		String[] elems = text.split("\\s+");
		int[] beginPositionArr = new int[elems.length];
		int[] endPositionArr = new int[elems.length];
		for(int i=0;i<elems.length;i++) {
			String elem = elems[i];
			String preElem = i-1>=0 ? elems[i-1]:"";
			String nextElem = i+1<elems.length ? elems[i+1]:"";
			//不预处理文本
			if( !opt.isTextPreprocessing ) {
				if( i!=0 ) {
					optimizedText += " ";
				}
				int startPosition = optimizedText.length();
				beginPositionArr[i] = startPosition;
				optimizedText += elem;
				endPositionArr[i] = optimizedText.length();
				continue;
			}
			//开始进行文本预处理：
			//检查到缩写符号：
			if( elem.equals("'") && isHave(nextElem,abbrArr) ) {
				int startPosition = optimizedText.length();
				beginPositionArr[i] = startPosition;
				optimizedText += elem;
				endPositionArr[i] = optimizedText.length();
				startPosition = optimizedText.length();
				beginPositionArr[i+1] = startPosition;	
				optimizedText += nextElem;
				endPositionArr[i] = optimizedText.length();
				i++;
			}
			//检查到以特殊符号为开头,则删除
			else if( i==0 && isHave(elem,listSymbolArr) ) {
				int startPosition = optimizedText.length();
				beginPositionArr[i] = startPosition;
				optimizedText += " ";
				endPositionArr[i] = optimizedText.length();
			}
			//检查“*” e.g. "i would * not * recommend this laptop .",该符号大概率会影响分析结果,删除
			else if( elem.equals("*") && !isNumeric(preElem) ) {
				int startPosition = optimizedText.length();
				beginPositionArr[i] = startPosition;
				optimizedText += " ";
				endPositionArr[i] = optimizedText.length();
			}
			//检查“`”,该符号大概率会影响分析结果,删除
			else if( elem.toLowerCase().equals("`") ) {
				int startPosition = optimizedText.length();
				beginPositionArr[i] = startPosition;
				optimizedText += " ";
				endPositionArr[i] = optimizedText.length();
			}
			//检查“- - ”
			else if( elem.equals("-") && preElem.equals("-") ) {
				int startPosition = optimizedText.length();
				beginPositionArr[i] = startPosition;
				optimizedText += elem;
				endPositionArr[i] = optimizedText.length();
			}
			else {
				if( i!=0 ) {
					optimizedText += " ";
				}
				//检查错误写法“im”
				if( elem.toLowerCase().equals("im") ) {
					int startPosition = optimizedText.length();
					beginPositionArr[i] = startPosition;
					elem = elem.replace("m", "'m");
					optimizedText += elem;
					endPositionArr[i] = optimizedText.length();
				}
				//检查“&”,该符号大概率会影响分析结果
//				else if( elem.toLowerCase().equals("&") ) {
//					int startPosition = optimizedText.length();
//					beginPositionArr[i] = startPosition;
//					optimizedText += "and";
//					endPositionArr[i] = optimizedText.length();
//				}
				else {
					int startPosition = optimizedText.length();
					beginPositionArr[i] = startPosition;
					optimizedText += elem;
					endPositionArr[i] = optimizedText.length();
				}
			}
		}
		//System.out.println(optimizedText);
	    ArrayList<Integer> beginPositionList = new ArrayList<Integer>();
		ArrayList<Integer> endPositionList = new ArrayList<Integer>();
		ArrayList<CoreLabel> tokenList = new ArrayList<CoreLabel>();
		Annotation document = new Annotation(optimizedText);
		spliter.annotate(document);
		List<CoreMap> sentences = (List<CoreMap>)document.get(CoreAnnotations.SentencesAnnotation.class);
	    for (CoreMap sentence : sentences) {
	    	for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
	    		int beginPosition = token.beginPosition();
	    		int endPosition = token.endPosition();
	    		beginPositionList.add( beginPosition );
	    		endPositionList.add( endPosition );
	    		tokenList.add(token);
	    	}
	    }
	    
	    ArrayList<Integer> deleteOriIndexList = new ArrayList<Integer>();
	    for(int i=0;i<beginPositionArr.length;i++) {
	    	int oriIndex = i;
	    	int beginPosition = beginPositionArr[i];
	    	int corTokenIndex = -1;
	    	for(int j=0;j<beginPositionList.size();j++) {
	    		int ti_beginPosition = beginPositionList.get(j);
	    		int ti_endPosition = endPositionList.get(j);
	    		if( ti_beginPosition<=beginPosition && beginPosition<=ti_endPosition ) {
	    			corTokenIndex = j;
	    			break;
	    		}
	    	}
	    	if( corTokenIndex!=-1 ) {
	    		if( !oriIndexTokenIndexMap.containsKey(oriIndex) ) {
		    		oriIndexTokenIndexMap.put(oriIndex, corTokenIndex);
		    	}
		    }
	    	// corTokenIndex=-1 则为删掉的字符,在token中找不到
	    	else {
	    		deleteOriIndexList.add(oriIndex);
	    	}
	    	//String oriWord = elems[oriIndex];
	    	//String newSplitWord = tokenList.get(corTokenIndex).word();
	    	//System.out.println(oriWord+":"+newSplitWord);
	    }
	    for(int deleteOriIndex:deleteOriIndexList) {
	    	boolean hasFindCor = false;
	    	for(int j=deleteOriIndex+1;j<beginPositionArr.length;j++) {
	    		if( oriIndexTokenIndexMap.containsKey(j) ) {
	    			int corTokenIndex = oriIndexTokenIndexMap.get(j);
	    			oriIndexTokenIndexMap.put(deleteOriIndex, corTokenIndex);
	    			hasFindCor = true;
	    			break;
	    		}
	    	}
	    	if( !hasFindCor ) {
	    		System.err.println("optimizeText() 出现匹配缺失!");
	    	}
	    }
	    
	    for(int i=0;i<tokenList.size();i++) {
	    	int tokenIndex = i;
	    	int beginPosition = beginPositionList.get(tokenIndex);
	    	int corOriIndex = -1;
	    	for(int j=0;j<beginPositionArr.length;j++) {
	    		int ti_beginPosition = beginPositionArr[j];
	    		int ti_endPosition = endPositionArr[j];
	    		if( ti_beginPosition<=beginPosition && beginPosition<=ti_endPosition ) {
	    			corOriIndex = j;
	    			break;
	    		}
	    	}
	    	if( corOriIndex!=-1 ) {
		    	if( !tokenIndexOriIndexMap.containsKey(tokenIndex) ) {
		    		tokenIndexOriIndexMap.put(tokenIndex, corOriIndex);
		    	}
		    }else {
		    	System.err.println("optimizeText(): tokenIndex 出现匹配缺失!");
	    	}
	    }
	    
//	    for(int i=0;i<tokenList.size();i++) {
//	    	CoreLabel token = tokenList.get(i);
//	    	String word = token.word();
//	    	int oriIndex = tokenIndexOriIndexMap.get(i);
//	    	String oriWord = elems[oriIndex];
//    		System.out.println(word+":"+oriWord);
//	    }
	    
		return optimizedText;
	}
	
	private int[] transOriIndexArrToTokenIndexArr(int[] oriIndexOPArr,HashMap<Integer,Integer> oriIndexTokenIndexMap) {
		int startOriIndex = oriIndexOPArr[0];
		int endOriIndex = oriIndexOPArr[1]-1;
		int startTokenIndex = oriIndexTokenIndexMap.get(startOriIndex);
		int endTokenIndex = oriIndexTokenIndexMap.get(endOriIndex);
		int[] tokenIndexOPArr = new int[2];
		tokenIndexOPArr[0] = startTokenIndex;
		tokenIndexOPArr[1] = endTokenIndex;
		return tokenIndexOPArr;
	}
	
	private int[] transTokenIndexArrToOriIndexArr(int[] tokenIndexArr,HashMap<Integer,Integer> tokenIndexOriIndexMap) {
		int[] oriIndexOPArr = new int[2];
		oriIndexOPArr[0] = -1;
		oriIndexOPArr[1] = -1;
		int startTokenIndex = tokenIndexArr[0];
		int endTokenIndex = tokenIndexArr[1];
		if( startTokenIndex!=-1 && endTokenIndex!=-1 ) {
			int startOriIndex = tokenIndexOriIndexMap.get(startTokenIndex);
			int endOriIndex = tokenIndexOriIndexMap.get(endTokenIndex)+1;
			oriIndexOPArr[0] = startOriIndex;
			oriIndexOPArr[1] = endOriIndex;
		}
		return oriIndexOPArr;
	}
	
    private ArrayList<int[]> transStringToOPList(String OPListString){
		ArrayList<int[]> opList = new ArrayList<int[]>();
		String[] elems = OPListString.split(";");
		for(String elem:elems) {
			int[] indexArr = getIndexArr(elem);
			if( indexArr==null ) {
				continue;
			}
			opList.add(indexArr);
		}
		return opList;
	}
	
	private int[] getIndexArr(String indexString) {
		indexString = indexString.trim();
		if( indexString.length()==0 ) {
			return null;
		}
		String[] indexStringArr = indexString.split(",");
		int startIndex = Integer.parseInt(indexStringArr[0]);
		int endIndex = Integer.parseInt(indexStringArr[1]);
		int[] indexArr = new int[2];
		indexArr[0] = startIndex;
		indexArr[1] = endIndex;
		return indexArr;
	}
	
	private boolean isHave(String word,String[] arr) {
		for(int i=0;i<arr.length;i++) {
			if(word.equals(arr[i])) {
			   return true;
		    }
		}
	    return false;
    }
	
	private ArrayList<Integer> transIntArrToList(int[] arr) {
		ArrayList<Integer> list = new ArrayList<Integer>();
		for(int i=0;i<arr.length;i++) {
			list.add(arr[i]);
		}
		return list;
	}
	
	private String transIntArrToString(int[] arr) {
		String rs = "[";
		for(int i=0;i<arr.length;i++) {
			rs += arr[i];
			if( i!=arr.length-1 ) {
				rs += ",";
			}
		}
		rs += "]"; 
		return rs;
	}
	
	private String transIntArrToString_WithoutBracket(int[] arr) {
		String rs = "";
		for(int i=0;i<arr.length;i++) {
			rs += arr[i];
			if( i!=arr.length-1 ) {
				rs += ",";
			}
		}
		return rs;
	}

}
