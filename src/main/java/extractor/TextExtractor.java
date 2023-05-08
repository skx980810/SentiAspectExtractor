package extractor;

import java.util.ArrayList;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang3.tuple.Pair;

import edu.stanford.nlp.io.EncodingPrintWriter.out;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.trees.GrammaticalRelation;
import edu.stanford.nlp.trees.Tree;

public class TextExtractor {
	public AnalysisOptions OPT;
	public TextParser textParser;
	public PeopleWordList peopleWordList;
	public TimeWordList timeWordList;
	public VagueWordList vagueWordList;
	public VerbAspectList verbAspectList;
	private ImplicitAspectWordList implicitAspectWordList;
	private CoreNLPRules NLPRule;
	public void init() {
		textParser = new TextParser();
		textParser.setOption(OPT);
		textParser.parserInit();
		peopleWordList = new PeopleWordList();
		peopleWordList.initPeopleWordList(OPT.peopleWordListPath);
		timeWordList = new TimeWordList();
		timeWordList.initTimeWordList(OPT.timeWordListPath);
		vagueWordList = new VagueWordList();
		vagueWordList.initVagueWordList(OPT.vagueWordListPath);
		verbAspectList = new VerbAspectList();
		verbAspectList.initVerbAspectList(OPT.verbAspectWordListPath);
		implicitAspectWordList = new ImplicitAspectWordList();
		implicitAspectWordList.initImplicitAspectWordList(OPT.implicitAspectWordListPath);
		NLPRule = new CoreNLPRules();
	}
	
	public void setOption(AnalysisOptions opt) {
		this.OPT = opt;
	}
	
	public void parseText(String text) {
		textParser.initText(text);
	}
	
	public ArrayList<Aspect> extractForOpinion(int[] tokenIndexOPArr) {
    	if( !textParser.isTextInit() ) {
    		System.err.println(" extractForOpinion() 之前需要初始化文本!");
    		return null;
    	}
    	ArrayList< Aspect > aspectList = new ArrayList< Aspect >();
    	Opinion op = getOpinionFromIndexArr(tokenIndexOPArr);
    	if( op.isLegal() ) {
    		extractForGeneral(op,aspectList);
    		//根据词性分类讨论
    		String coreOpinionNodetag = op.getCoreOpinionNodeTag();
    		if( NLPRule.isAdj(coreOpinionNodetag) ) {
    			extractForAdjOpinion(op,aspectList);
    		}
        	else if( NLPRule.isVerb(coreOpinionNodetag) ) {
        		extractForVerbOpinion(op,aspectList);
        	}
        	else if( NLPRule.isNoun(coreOpinionNodetag) ) {
        		extractForNounOpinion(op,aspectList);
        	}
        	else if( NLPRule.isAdv(coreOpinionNodetag) ) {
        		extractForAdvOpinion(op,aspectList);
        	}
        	else {
        		extractForOtherOpinion(op,aspectList);
        	}
    	}
    	for(Aspect ap:aspectList) {
    		int[] tokenIndexArr = getTokenIndexForAspect(ap);
    		ap.setTokenIndexArr(tokenIndexArr);
    	}
		return aspectList;
    }
	
	private int[] getTokenIndexForAspect(Aspect ap) {
		int[] tokenIndexArr = new int[2];
		int[] aspectBeginPosScopeArr = ap.getAspectBeginPosScopeArr();
		tokenIndexArr[0] = textParser.getNodeIndexByBeginPos(aspectBeginPosScopeArr[0]);
		tokenIndexArr[1] = textParser.getNodeIndexByBeginPos(aspectBeginPosScopeArr[1]);
		return tokenIndexArr;
	}
	
	private Opinion getOpinionFromIndexArr(int[] tokenIndexOPArr) {
    	ArrayList<IndexedWord> opinionNodeList = new ArrayList<IndexedWord>();
    	int startTokenIndex = tokenIndexOPArr[0];
    	int endTokenIndex = tokenIndexOPArr[1];
    	for(int i=startTokenIndex;i<=endTokenIndex;i++) {
			IndexedWord opinionTermNode = textParser.getNodeByIndex(i);
			opinionNodeList.add(opinionTermNode);
		}
    	IndexedWord coreOpinionNode = selectCoreOpinionNode(opinionNodeList);
    	Opinion op = new Opinion(coreOpinionNode,opinionNodeList);
    	return op;
    }
	
	private Pattern pattern = Pattern.compile(".*?[a-zA-Z]+.*?");
	private boolean isContainEng(String text) {
		return pattern.matcher(text).matches();
	}
    
    private IndexedWord selectCoreOpinionNode(ArrayList<IndexedWord> opinion) {
    	if( opinion==null || opinion.size()==0 ) {
    		return null;
    	}
    	int size = opinion.size();
    	//opinion 只有一个单词
    	if( size==1 ) {
    		IndexedWord coreOpinionNode = opinion.get(0);
    		return coreOpinionNode;
    	}
    	//opinion 含有多个单词
    	else {
    		//快捷选择：根据尾词的词性进行快捷选择
    		IndexedWord endOpinionNode = opinion.get(size-1);
    		if( NLPRule.isAdj(endOpinionNode.tag()) || NLPRule.isVerb(endOpinionNode.tag()) || NLPRule.isNoun(endOpinionNode.tag()) ) {
    			return endOpinionNode;
    		}
    		//根据依赖关系进行选择
    		int maxDegreeNum = -1;
    		IndexedWord maxDegreeNode = null;
    		for(int i=size-1;i>=0;i--) {
    			IndexedWord opinionTermNode = opinion.get(i);
    			String word = opinionTermNode.word().toLowerCase();
    			int degree = 0;
    			if( isContainEng(word) ) {
    				SemanticGraph graph = textParser.getGraphByNode(opinionTermNode);
    				int outDegree = graph.getChildren(opinionTermNode).size();
    				int inDegree = graph.getParents(opinionTermNode).size();
    				degree = inDegree+outDegree;
    			}
    			if( degree>maxDegreeNum ) {
    				maxDegreeNum = degree;
    				maxDegreeNode = opinionTermNode;
    			}
    		}
    		if( maxDegreeNode==null ) {
    			System.err.println(" coreOpinionNode 选择失败!");
    		}
    		return maxDegreeNode;
    	}
    }
    
    public void extractForGeneral(Opinion op,ArrayList<Aspect> aspectList) {
    	IndexedWord coreNode = op.getCoreOpinionNode();
    	SemanticGraph graph = textParser.getGraphByNode(coreNode);
    	//System.out.println("Dependency Graph:\n " +graph.toString(SemanticGraph.OutputFormat.READABLE));
    	ArrayList<IndexedWord> nodeList = textParser.getNodeList();
    	for(int i=0;i<nodeList.size();i++) {
    		IndexedWord node = nodeList.get(i);
    		//找到Be-Verb,处理 "主-系-表结构" 
    		if( node.lemma().toLowerCase().equals("be") ) {
    			if( !graph.containsVertex(node) ) {
    				continue;
    			} 
    			IndexedWord BEGov = graph.getParent(node);
    			//Be-Verb 为根节点, "主-系-表结构"中 系语 为 从句 ：
    			if( BEGov==null ) {
    				IndexedWord subj = NLPRule.getImmediateSubj(node, graph);
    				IndexedWord ccompOfVerb = null;
    	    		Set<IndexedWord> childSet = graph.getChildren(node);
    	    		for(IndexedWord child:childSet) {
    	    			String reln = graph.getEdge(node,child).getRelation().toString();
    	    			if( reln.equals("ccomp") ) {
    	    				ccompOfVerb = child;
    	    			}
    	    		}
    	    		//opinion 位于"主-系-表"的主语中：取从句的主语为aspect:
    	    		//e.g. "my only 【complaint】 is that the mouse keypad is a little big ."
    	    		boolean isInSubjDescendants = isInDescendants(graph,subj,coreNode);
    	    		if( isInSubjDescendants && ccompOfVerb!=null ) {
    	    			IndexedWord clauseSubj = NLPRule.getImmediateSubj(ccompOfVerb,graph);
    	    			if( clauseSubj!=null && isLegalSubj(clauseSubj)) {
    	    				String reasonForSelection = "[R-2] opinion 位于主语中，且表语为从句，则取表语从句主语为aspect";
        					Aspect ap = new Aspect(clauseSubj,reasonForSelection);
          	  	    		extendAspectAndAddToList(ap,op,aspectList);
    	    			}
    	    		}
    	    	}else {
    				String reln = graph.getEdge(BEGov, node).getRelation().toString();
    				//"主-系-表结构"中 系语 为 短语 ：
    				if( reln.equals("cop") ) {
    					IndexedWord subj = NLPRule.getImmediateSubj(BEGov, graph);
        	    		//opinion 位于主-系-表，的主语中：取系语短语为aspect
        	    		//e.g. "the only thing i think that could be 【better】 is the volume of the speakers ."
        	    		//e.g. "- biggest 【disappointment】 is the track pad ."
        	    		boolean isInDescendants = isInDescendants(graph,subj,coreNode);
        	    		if( isInDescendants && NLPRule.isNoun(BEGov.tag()) ) {
        	    			String reasonForSelection = "[R-1] opinion 位于主语中，且表语为短语，则取短语为aspect";
        	    			Aspect ap = new Aspect(BEGov,reasonForSelection);
          	  	    		extendAspectAndAddToList(ap,op,aspectList);
        	    		}
    				}
    			}
    		}
    	}
    	
    	//关于以从句修饰的探索：
    	//case 1:修饰从句以括号与主体连接，e.g."- backlit and solid keyboard ( 【not flimsy or cheap】 )"
    	IndexedWord nodeLeadBracket = null;
    	IndexedWord startOPNode = op.getOpinionStartNode();
    	int startOPNodeIndex = textParser.getNodeIndexByNode(startOPNode);
    	for(int i=startOPNodeIndex-1;i>0;i--) {
    		IndexedWord node = nodeList.get(i);
    		if( node.word().equals(")") ) {
    			break;
    		}
    		else if(  node.word().equals("(") ) {
    			nodeLeadBracket = nodeList.get(i-1);
    		}
    	}
    	if( nodeLeadBracket!=null && isLegalByTag(nodeLeadBracket) ) {
    		int rightmostBoundary = nodeLeadBracket.index();
    		String reasonForSelection = "[R-7] opinion 位于括号中，取括号前一个单词为aspect";
    		Aspect ap = new Aspect(nodeLeadBracket,reasonForSelection);
    		ap.setRightMostBoundary(rightmostBoundary);
	    	extendAspectAndAddToList(ap,op,aspectList);
    	}
    	//case 2: 通过"acl"关系查找从句：
    	for (SemanticGraphEdge edge : graph.edgeIterable()) {
    		String reln = edge.getRelation().toString();
    		if( reln.startsWith("acl") ) {
    			IndexedWord gov = edge.getGovernor();
    			IndexedWord dep = edge.getDependent();
    			boolean isInACL = isInACL(graph,dep,coreNode);
    			if( isInACL && isLegalByTag(gov) ) {
    				if( peopleWordList.isPeopleNode(gov) ) {
    					continue;
    				}else {
    					IndexedWord clauseModObj = gov;
    					// 取acl的修饰对象为aspect    e.g. "a home pc laptop that works very well ."
    					int rightmostBoundary = clauseModObj.index();
        				String reasonForSelection = "[R-8] opinion 位于 acl 中，取 acl 的修饰对象为aspect";
        				Aspect ap = new Aspect(clauseModObj,reasonForSelection);
        				ap.setRightMostBoundary(rightmostBoundary);
        				extendAspectAndAddToList(ap,op,aspectList);
        				
        				// 若acl的修饰对象为主系表中的表语，则再取主语为aspect	e.g. "the ports were another thing that i was really excited to see "
        				IndexedWord subj = NLPRule.getImmediateSubj(clauseModObj,graph);
        				IndexedWord cop = null;
        				Set<IndexedWord> childOfClauseModObj = graph.getChildren(clauseModObj);
        				for(IndexedWord child:childOfClauseModObj) {
        					reln = graph.getEdge(clauseModObj, child).getRelation().toString();
        					if( reln.equals("cop") ) {
        						cop = child;
        					}
        				}
        				if( cop!=null && subj!=null ) {
        					reasonForSelection = "[R-9] opinion 位于 acl 中，且 acl 的修饰对象为表语，则取主语为aspect";
            				Aspect subjAp = new Aspect(subj,reasonForSelection);
            				extendAspectAndAddToList(subjAp,op,aspectList);
        				}
        				
    				}
    			}
    		}
    	}
    	
    	//关于主语的探讨主语：
    	Pair<IndexedWord,IndexedWord> subjGovPair = NLPRule.getNearestSubjGovPair(coreNode,graph);
    	IndexedWord nearestSubj = subjGovPair.getLeft();
    	IndexedWord nearestSubjGov = subjGovPair.getRight();
    	//若存在主语
    	if( nearestSubj!=null ) {
    		//若主语为人
    		if( peopleWordList.isPeopleNode(nearestSubj) ) {
    			
    		}
    		//若主语为疑问词, e.g. "just a home pc laptop that works very well ."
    		else if( nearestSubj.tag().equals("WDT") ) {
    			if( nearestSubjGov!=null ) {
    				int tokenIndex = textParser.getNodeIndexByNode(nearestSubj);
    				IndexedWord prevOfWDT = textParser.getNodeByIndex(tokenIndex-1);
    				if( prevOfWDT!=null && NLPRule.isNoun(prevOfWDT.tag()) ) {
    					int rightmostBoundary = prevOfWDT.index();
    					String reasonForSelection = "[R-8] opinion 位于 acl 中，取 acl 的修饰对象为 aspect (WH-Word)";
        				Aspect ap = new Aspect(prevOfWDT,reasonForSelection);
        				ap.setRightMostBoundary(rightmostBoundary);
        				extendAspectAndAddToList(ap,op,aspectList);
    				}
    			}
    		}
    		//若主语为动词：
    		else if( NLPRule.isVerb(nearestSubj.tag()) ) {
	    		Set<IndexedWord> objNodeSet = NLPRule.getImmediateObj(nearestSubj,graph);
	  			if( objNodeSet.size()!=0 ) {
	  				String reasonForSelection = "[R-5] opinion 的主语为动词，且存在宾语，则取宾语";
	  				Set<IndexedWord> setFiltered = filterObjNodeSet(graph,nearestSubj,objNodeSet);
	  				for(IndexedWord objNode:setFiltered) {
	  					Aspect ap = new Aspect(objNode,reasonForSelection);
	  					extendAspectAndAddToList(ap,op,aspectList);
	  				}
	  			}
	  			else {
	  				String reasonForSelection = "[R-6] opinion 的主语为动词，且不存在宾语，则取动词";
  					Aspect ap = new Aspect(nearestSubj,reasonForSelection);
    				extendAspectAndAddToList(ap,op,aspectList);
	  			}	
	    	}
    		//若主语为形容词 e.g. "most of my android apps have worked 【well】 ."
    		else if( NLPRule.isAdj(nearestSubj.tag()) ) {
    			IndexedWord nmod = null;
    			Set<IndexedWord> childSet = graph.getChildren(nearestSubj);
    			for(IndexedWord child:childSet) {
    				String reln = graph.getEdge(nearestSubj,child).getRelation().getShortName();
    				if( reln.equals("nmod") ) {
    					nmod = child;
    				}
    			}
    			if( isLegalSubj(nmod) ) {
    				String reasonForSelection = "[R-4] opinion 的主语为形容词，则取形容词的修饰对象";
    				Aspect ap = new Aspect(nmod,reasonForSelection);
    				extendAspectAndAddToList(ap,op,aspectList);
    			}
    		}
    		// 名词主语
    		else if ( isLegalSubj(nearestSubj) ) {
    			String reasonForSelection = "[R-3] opinion 的合法词性主语";
    			Aspect ap = new Aspect(nearestSubj,reasonForSelection);
				extendAspectAndAddToList(ap,op,aspectList);
    		}
    	}
    }
    
    private boolean isInDescendants(SemanticGraph graph,IndexedWord gov,IndexedWord dep) {
    	if( gov==null ) {
    		return false;
    	}
    	// 若 gov 和 dep 为一个节点，会返回这个节点本身：
    	List<IndexedWord> nodeInPath = graph.getShortestDirectedPathNodes(gov,dep);
    	if( nodeInPath==null || nodeInPath.size()==0 ) {
    		return false;
    	}
    	return true;
    }
    
    private boolean isInACL(SemanticGraph graph,IndexedWord gov,IndexedWord dep) {
    	if( gov==null ) {
    		return false;
    	}
    	// 若 gov 和 dep 为一个节点，会返回这个节点本身：
    	List<IndexedWord> nodeInPath = graph.getShortestDirectedPathNodes(gov,dep);
    	if( nodeInPath==null || nodeInPath.size()==0 ) {
    		return false;
    	}
    	for(int i=0;i<nodeInPath.size()-1;i++) {
    		IndexedWord govv = nodeInPath.get(i);
    		IndexedWord depp = nodeInPath.get(i+1);
    		String reln = graph.getEdge(govv, depp).getRelation().toString();
    		if( reln.startsWith("acl") ) {
    			return false;
    		}
    	}
    	return true;
    }
    
    public void extractForAdjOpinion(Opinion op,ArrayList<Aspect> aspectList) {
    	IndexedWord adj = op.getCoreOpinionNode();
    	SemanticGraph graph = textParser.getGraphByNode(adj);
    	//System.out.println("Dependency Graph:\n " +graph.toString(SemanticGraph.OutputFormat.READABLE));
    	
    	//寻找 adj 的修饰对象：
    	//case 1: 向祖先节点寻找
    	boolean hasFindStandardModObject = false;
    	Set<IndexedWord> modObjectSet = new HashSet<IndexedWord>();
    	Set<IndexedWord> govSet = graph.getParents(adj);
    	if( govSet!=null && govSet.size()!=0 ) {
    		for(IndexedWord gov:govSet) {
    			String reln = graph.getEdge(gov,adj).getRelation().toString();
    			if( NLPRule.isModReln(reln) ) {
    				modObjectSet.add(gov);
    			}
        	}
    	}
    	if( modObjectSet.size()!=0 ) {
    		for(IndexedWord modObject:modObjectSet) {
    			if( NLPRule.isNoun(modObject.tag()) ) {
    				hasFindStandardModObject = true;
    				// 直接修饰对象，e.g. "a great computer"
    				boolean isInFaultLoca = modObject.index()<adj.index() && textParser.havePartitionInBetween(modObject, adj);
    				boolean isInReasonableLoca = !OPT.isSuppByLocaDistribution || !isInFaultLoca;
    				if( isInReasonableLoca ) {
    					String reasonForSelection = "[R-10] adj_opinion 的修饰对象(直接修饰对象)";
            			Aspect ap = new Aspect(modObject,reasonForSelection,0);
            			extendAspectAndAddToList(ap,op,aspectList);
    				}
    				// 直接修饰对象的所属名词，e.g. "a laptop at a reasonable price"
    				IndexedWord mainNoun = null;
    				Set<IndexedWord> modObjGovSet = graph.getParents(modObject);
    				for(IndexedWord modObjGov:modObjGovSet) {
    					String reln = graph.getEdge(modObjGov,modObject).getRelation().getShortName();
    					String spec = graph.getEdge(modObjGov,modObject).getRelation().getSpecific();
    					if( reln.equals("nmod") && ( spec.equals("at") || spec.equals("with") || spec.equals("in") )) {
    						mainNoun = modObjGov;
    					}
    				}
    				if( mainNoun!=null && NLPRule.isNoun(mainNoun) ) {
    					String reasonForSelection = "[R-11] adj_opinion 的修饰对象(直接修饰对象的所属名词)";
            			Aspect ap = new Aspect(mainNoun,reasonForSelection,0);
            			extendAspectAndAddToList(ap,op,aspectList);
    				}
    			}
    			else if( NLPRule.isAdj(modObject.tag()) ) {
    				Set<IndexedWord> govOfModObject = graph.getParents(modObject);
    				for(IndexedWord govOfMod:govOfModObject) {
    					String reln = graph.getEdge(govOfMod,modObject).getRelation().toString();
    					boolean reasonableLocaDistribution = govOfMod.index()>adj.index();
    					boolean isInReasonableLoca = !OPT.isSuppByLocaDistribution || reasonableLocaDistribution;
    					if( NLPRule.isModReln(reln) && NLPRule.isNoun(govOfMod.tag()) && isInReasonableLoca ) {
    						hasFindStandardModObject = true;
    	    				String reasonForSelection = "[R-12] adj_opinion 的修饰对象(间接修饰对象)";
    	        			Aspect ap = new Aspect(govOfMod,reasonForSelection,0);
    	        			if( govOfMod.index()>modObject.index() && govOfMod.index()>adj.index() ) {
    							int leftMostBoundary = modObject.index()+1;
    							ap.setLeftMostBoundary(leftMostBoundary);
    						}
    	        			extendAspectAndAddToList(ap,op,aspectList);
    	    			}
    				}
    			}
    		}
    	}
    	//case 2: 隐性修饰对象, 向后一个单位寻找
    	if( !hasFindStandardModObject ) {
			int beginPos = adj.beginPosition();
			int tokenIndex = textParser.getNodeIndexByBeginPos(beginPos);
			IndexedWord nextNode = textParser.getNodeByIndex(tokenIndex+1);
			String reasonForSelection = "[R-10] adj_opinion 的修饰对象(隐性修饰对象)";
			if( nextNode!=null && NLPRule.isNoun(nextNode.tag())) {
				Aspect ap = new Aspect(nextNode,reasonForSelection,0);
				extendAspectAndAddToList(ap,op,aspectList);
			}
        }
    	
    	//根据使役结构寻找宾语：e.g. "it will take years of bad programming to make this chromebook as 【slow】 as my last one got ."
    	Set<IndexedWord> advclGovSet = graph.getParentsWithReln(adj, "advcl");
    	for(IndexedWord gov:advclGovSet) {
    		if( NLPRule.isVerbCollocateWthAdvcl(gov) ) {
    			Set<IndexedWord> objNodeSet = NLPRule.getImmediateDirectObj(gov,graph);
    			Set<IndexedWord> setFiltered = filterObjNodeSet(graph,gov,objNodeSet);
    			String reasonForSelection = "[R-13] adj_opinion 的宾语(通过使役结构取得)";
    			for(IndexedWord objNode:setFiltered) {
    				Aspect ap = new Aspect(objNode,reasonForSelection);
    				extendAspectAndAddToList(ap,op,aspectList);
    			}
    		}
    	}
    	
    	//讨论补语(一)：
    	Set<IndexedWord> ccompNodeSet = new HashSet<IndexedWord>();
    	Set<IndexedWord> advclNodeSet = new HashSet<IndexedWord>();
    	Set<IndexedWord> xcompVerbSet = new HashSet<IndexedWord>();
		Set<IndexedWord> childSet = graph.getChildren(adj);
		for(IndexedWord child:childSet) {
			String reln = graph.getEdge(adj,child).getRelation().toString();
			if( reln.equals("xcomp") && NLPRule.isVerb(child.tag()) ) {
				xcompVerbSet.add(child);
				String reasonForSelection = "[R-15] adj_opinion 提取补语动词";
				Aspect ap = new Aspect(child,reasonForSelection);
	  			extendAspectAndAddToList(ap,op,aspectList);
			}
			else if( reln.equals("advcl") ) {
				advclNodeSet.add(child);
			}
			else if( reln.equals("ccomp") ) {
				ccompNodeSet.add(child);
			}
		}
		
		//曾经讨论过的主语类别 nearestSubj.tag().indexOf("PRP")!=-1 || nearestSubj.tag().indexOf("DT")!=-1 
    	IndexedWord nearestSubj = NLPRule.getNearestSubj(adj,graph);
    	if( nearestSubj==null || peopleWordList.isPeopleNode(nearestSubj) ) {
    		//讨论宾语：e.g."i'm super happy with this product ."
    		//此处尝试着讨论过并列词的宾语，但效果不好
    		Set<IndexedWord> objNodeSet = NLPRule.getImmediateObj(adj,graph);
    		Set<IndexedWord> setFiltered = filterObjNodeSet(graph,adj,objNodeSet);
    		for(IndexedWord objNode:setFiltered) {
				String reasonForSelection = "[R-14] adj_opinion 的宾语(直接)";
				Aspect ap = new Aspect(objNode,reasonForSelection);
				extendAspectAndAddToList(ap,op,aspectList);
			}
    		//讨论补语(二)：
    		if( xcompVerbSet.size()!=0 ) {
  				for(IndexedWord xcompVerb:xcompVerbSet) {
  					//讨论补语动词的宾语 e.g. "i was extremely hesitant to buy a used macbook pro"
  					Set<IndexedWord> xcompVerbObjNodeSet = NLPRule.getImmediateObj(xcompVerb,graph);
  					Set<IndexedWord> xcompVerbObjSetFiltered = filterObjNodeSet(graph,xcompVerb,xcompVerbObjNodeSet);
  					for(IndexedWord objNode:xcompVerbObjSetFiltered) {
  						String reasonForSelection = "[R-17] adj_opinion 提取补语动词的宾语";
  						Aspect ap = new Aspect(objNode,reasonForSelection);
  	    				extendAspectAndAddToList(ap,op,aspectList);
  					}	
  				    //讨论补语动词的从句 e.g. "i'm happy to report that the keyboard is great."
  		    		IndexedWord xcompVerb_ccomp = null;
  		    		Set<IndexedWord> xcompVerbChildSet = graph.getChildren(xcompVerb);
  		    		for(IndexedWord child:xcompVerbChildSet) {
  		    			String reln = graph.getEdge(xcompVerb,child).getRelation().toString();
  		    			if( reln.equals("ccomp") ) {
  		    				xcompVerb_ccomp = child;
  		    			}
  		    		}
  		    		if( xcompVerb_ccomp!=null ) {
  		    			IndexedWord ccompSubj = NLPRule.getImmediateSubj(xcompVerb_ccomp,graph);
  		    			if( isLegalSubj(ccompSubj) ) {
  		    				String reasonForSelection = "[R-18] adj_opinion 提取补语动词的从句主语";
  		    	    		Aspect ap = new Aspect(ccompSubj,reasonForSelection);
  		    				extendAspectAndAddToList(ap,op,aspectList);
  		    			}
  		    		}
  				}
  			}
    		//讨论补语(三)：
    		if( advclNodeSet.size()!=0 || ccompNodeSet.size()!=0) {
    			Set<IndexedWord> clauseNodeSet = new HashSet<IndexedWord>();
    			clauseNodeSet.addAll(advclNodeSet);
    			clauseNodeSet.addAll(ccompNodeSet);
    			for(IndexedWord clauseNode:clauseNodeSet) {
    				IndexedWord clauseSubj = NLPRule.getImmediateSubj(clauseNode,graph);
    				if( isLegalSubj(clauseSubj) ) {
	    				String reasonForSelection = "[R-16] adj_opinion 提取从句主语";
	    	    		Aspect ap = new Aspect(clauseSubj,reasonForSelection);
	    				extendAspectAndAddToList(ap,op,aspectList);
	    			}	
    			}
    		}
    	}
    	
    }
    
    public void extractForVerbOpinion(Opinion op,ArrayList<Aspect> aspectList) {
    	IndexedWord verb = op.getCoreOpinionNode();
    	SemanticGraph graph = textParser.getGraphByNode(verb);
    	String reasonForSelection;
		//System.out.println("Dependency Graph:\n " +graph.toString(SemanticGraph.OutputFormat.READABLE));
    	
    	//提取verb的修饰对象
		Set<IndexedWord> govSet = graph.getParents(verb);
    	if( govSet!=null && govSet.size()!=0 ) {
    		for(IndexedWord gov:govSet) {
    			String reln = graph.getEdge(gov,verb).getRelation().toString();
    			if( NLPRule.isModReln(reln) && NLPRule.isNoun(gov.tag()) ) {
    				reasonForSelection = "[R-19] verb_opinion 的修饰对象(直接修饰对象)";
    				Aspect ap = new Aspect(gov,reasonForSelection,0);
    				extendAspectAndAddToList(ap,op,aspectList);
    			}
        	}
    	}
    	
    	//讨论宾语：
    	IndexedWord nearestSubj = NLPRule.getNearestSubj(verb,graph);
    	if( nearestSubj==null || peopleWordList.isPeopleNode(nearestSubj) ) {
    		//提取宾语(1) 显性的宾语
    		Set<IndexedWord> objNodeSet = NLPRule.getImmediateObj(verb,graph);
        	Set<IndexedWord> setFiltered = filterObjNodeSet(graph,verb,objNodeSet);
			for(IndexedWord objNode:setFiltered) {
				reasonForSelection = "[R-20] verb_opinion 的宾语(显性)";
				Aspect ap = new Aspect(objNode,reasonForSelection);
				extendAspectAndAddToList(ap,op,aspectList);
			}	
    		//提取宾语(2) 隐性的宾语
    		int beginPos = verb.beginPosition();
    		int tokenIndex = textParser.getNodeIndexByBeginPos(beginPos);
    		IndexedWord nextNode = textParser.getNodeByIndex(tokenIndex+1);
    		reasonForSelection = "[R-20] verb_opinion 的宾语(隐性)";
    		if( nextNode!=null ) {
    			if( NLPRule.isNoun(nextNode.tag()) ) {
    				Aspect ap = new Aspect(nextNode,reasonForSelection);
    				extendAspectAndAddToList(ap,op,aspectList);
    			}
    			else if( nextNode.tag().equals("DT") ) {
    				IndexedWord referentialObject = null;
    				IndexedWord gov = graph.getParent(nextNode);
    				String reln = gov!=null ? graph.getEdge(gov,nextNode).getRelation().toString() : "";
    				if( reln.equals("det") ) {
    					referentialObject = gov;
    				}
    				if( referentialObject!=null && NLPRule.isNoun(referentialObject.tag()) ) {
    					Aspect ap = new Aspect(referentialObject,reasonForSelection);
    					extendAspectAndAddToList(ap,op,aspectList);
    				}
    				else {
    					IndexedWord secondNextNode = textParser.getNodeByIndex(tokenIndex+2);
    					if( NLPRule.isNoun(secondNextNode.tag()) ) {
    						Aspect ap = new Aspect(secondNextNode,reasonForSelection);
    						extendAspectAndAddToList(ap,op,aspectList);
    					}
    				}
    			}
    		}
    		
    		//讨论补语：
    		IndexedWord ccomp = null;
    		Set<IndexedWord> childSet = graph.getChildren(verb);
    		for(IndexedWord child:childSet) {
    			String reln = graph.getEdge(verb,child).getRelation().toString();
    			if( reln.equals("ccomp") ) {
    				ccomp = child;
    			}
    		}
    		if( ccomp!=null ) {
    			IndexedWord ccompSubj = NLPRule.getImmediateSubj(ccomp,graph);
    			if( isLegalSubj(ccompSubj) ) {
    				reasonForSelection = "[R-21] verb_opinion 补语从句的主语";
    	    		Aspect ap = new Aspect(ccompSubj,reasonForSelection);
    				extendAspectAndAddToList(ap,op,aspectList);
    			}
    		}
    	}
    }
    
    private void extractForNounOpinion(Opinion op,ArrayList<Aspect> aspectList) {
    	IndexedWord noun = op.getCoreOpinionNode();
    	SemanticGraph graph = textParser.getGraphByNode(noun);
//    	System.out.println("Dependency Graph:\n " +graph.toString(SemanticGraph.OutputFormat.READABLE));
    	
    	//查找opinion的修饰对象：
    	String reasonForSelection = "";
    	Set<IndexedWord> nounGovSet = graph.getParents(noun);
    	//case 1: 查找其祖先关系中的修饰对象,e.g." a premium laptop"
    	for(IndexedWord gov:nounGovSet) {
    		String reln = gov!=null ? graph.getEdge(gov,noun).getRelation().toString() : "";
        	if( reln.equals("compound") && NLPRule.isNoun(gov) ) {
        		reasonForSelection = "[R-22] noun_opinion 的修饰对象(祖先)";
    			Aspect ap = new Aspect(gov,reasonForSelection,0);
    			extendAspectAndAddToList(ap,op,aspectList);
    		}
    	}
		//case 2: 查找其子代关系中的修饰对象,
    	//case 2.1: nmod:of    e.g."disadvantage of amazon seller "
    	//case 2.2: compound    e.g."i am a big chromebook enthusiast"
    	Set<IndexedWord> childSet = graph.getChildren(noun);
		for(IndexedWord child:childSet) {
			String reln = graph.getEdge(noun,child).getRelation().getShortName();
			if( reln.equals("nmod") && isLegalByTag(child) ) {
				reasonForSelection = "[R-24] noun_opinion 的修饰对象(孩子-nmod)";
				Aspect ap = new Aspect(child,reasonForSelection,0);
				extendAspectAndAddToList(ap,op,aspectList);
			}
			else if( reln.equals("compound") && NLPRule.isNoun(child) ) {
				reasonForSelection = "[R-23] noun_opinion 的修饰对象(孩子-compound)";
				Aspect ap = new Aspect(child,reasonForSelection,0);
				extendAspectAndAddToList(ap,op,aspectList);
	    	}
		}
		
		//查找宾语：
		//case-1:通过使役结构查找宾语	e.g. "makes this machine such a fun"
		Set<IndexedWord> predGovSet = new HashSet<IndexedWord>();
		for(IndexedWord gov:nounGovSet) {
    		String reln = gov!=null ? graph.getEdge(gov,noun).getRelation().toString() : "";
        	if( NLPRule.isLegalObjReln(reln) && NLPRule.isVerbCollocateWthAdvcl(gov) ) {
        		Set<IndexedWord> objNodeSet = NLPRule.getImmediateDirectObj(gov,graph);
        		Set<IndexedWord> setFiltered = filterObjNodeSet(graph,gov,objNodeSet);
    			reasonForSelection = "[R-25] noun_opinion 的宾语(通过使役结构取得)";
    			for(IndexedWord objNode:setFiltered) {
    				if( objNode.index()!=noun.index() ) {
    					Aspect ap = new Aspect(objNode,reasonForSelection);
        				extendAspectAndAddToList(ap,op,aspectList);
    				}
    			}
        	}
        }
		
		//根据主语讨论宾语：
		IndexedWord nearestSubj = NLPRule.getNearestSubj(noun,graph);
    	if( nearestSubj==null || peopleWordList.isPeopleNode(nearestSubj) ) {
    		//case-2:直接宾语	e.g. "so kudos to acer for the keyboard !"
    		Set<IndexedWord> objNodeSet = NLPRule.getImmediateObj(noun,graph);
    		if( objNodeSet.size()!=0 ) {
    			Set<IndexedWord> setFiltered = filterObjNodeSet(graph,noun,objNodeSet);
    			reasonForSelection = "[R-26] noun_opinion 的宾语(直接)";
    			for(IndexedWord objNode:setFiltered) {
    				Aspect ap = new Aspect(objNode,reasonForSelection);
    				extendAspectAndAddToList(ap,op,aspectList);
    			}
    		}
    	}
	}
    
    private void extractForAdvOpinion(Opinion op,ArrayList<Aspect> aspectList) {
    	IndexedWord adv = op.getCoreOpinionNode();
    	SemanticGraph graph = textParser.getGraphByNode(adv);
//    	System.out.println("Dependency Graph:\n " +graph.toString(SemanticGraph.OutputFormat.READABLE));
    	//讨论修饰对象：
    	Set<IndexedWord> advmodObjSet = new HashSet<IndexedWord>();
    	Set<IndexedWord> depObjSet = new HashSet<IndexedWord>();
    	Set<IndexedWord> parentSet = graph.getParents(adv);
    	for(IndexedWord advGov:parentSet) {
    		String reln = graph.getEdge(advGov,adv).getRelation().toString();
			if( NLPRule.isModReln(reln) ) {
				advmodObjSet.add(advGov);
			}
			else if( reln.equals("dep") ) {
				depObjSet.add(advGov);
			}
    	}
    	for(IndexedWord depObj:depObjSet) {
    		boolean havePartitionInBetween = textParser.havePartitionInBetween(depObj,adv);
			boolean reasonableLocaDistribution = !havePartitionInBetween && (depObj.index()<adv.index() || depObj.index()-adv.index()==1);
			boolean isInReasonableLoca = !OPT.isSuppByLocaDistribution || reasonableLocaDistribution;
			if( isInReasonableLoca && NLPRule.isNoun(depObj.tag()) ) {
				String reasonForSelection = "[R-27] adv_opinion 的修饰对象(祖先-dep)";
				Aspect ap = new Aspect(depObj,reasonForSelection,0);
				extendAspectAndAddToList(ap,op,aspectList);
			}
    	}
    	for(IndexedWord advmodObj:advmodObjSet) {
    		boolean havePartitionInBetween = textParser.havePartitionInBetween(advmodObj,adv);
			boolean reasonableLocaDistribution = !havePartitionInBetween && (advmodObj.index()<adv.index() || advmodObj.index()-adv.index()==1);
			boolean isInReasonableLoca = !OPT.isSuppByLocaDistribution || reasonableLocaDistribution;
			if( isInReasonableLoca && NLPRule.isNoun(advmodObj) ) {
				String reasonForSelection = "[R-27] adv_opinion 的修饰对象(祖先-mod)";
				Aspect ap = new Aspect(advmodObj,reasonForSelection,0);
				extendAspectAndAddToList(ap,op,aspectList);
			}
			else if( NLPRule.isAdv(advmodObj) || NLPRule.isAdj(advmodObj) || NLPRule.isVerb(advmodObj) ){
				for(IndexedWord gov:graph.getParents(advmodObj)) {
					String reln = graph.getEdge(gov,advmodObj).getRelation().toString();
					if( NLPRule.isModReln(reln) && NLPRule.isNoun(gov) ) {
						String reasonForSelection = "[R-29] adv_opinion 的修饰对象(间接修饰对象)";
						Aspect ap = new Aspect(gov,reasonForSelection,0);
						if( gov.index()>advmodObj.index() && gov.index()>adv.index() ) {
							int leftMostBoundary = advmodObj.index()+1;
							ap.setLeftMostBoundary(leftMostBoundary);
						}
						extendAspectAndAddToList(ap,op,aspectList);
					}
				}
			}
    	}
    	
    	//讨论谓语：
    	for(IndexedWord advmodObj:advmodObjSet) {
    		if( NLPRule.isVerb(advmodObj) ) {
    			String reasonForSelection = "[R-28] adv_opinion 的修饰对象(动词)";
    			Aspect ap = new Aspect(advmodObj,reasonForSelection,2);
    			extendAspectAndAddToList(ap,op,aspectList);
    		}
    	}
		
    	//讨论宾语：
		//通过使役结构，寻找宾语：e.g. "hdd makes this laptop very slow ."
    	for(IndexedWord advmodObj:advmodObjSet) {
    		if( NLPRule.isVerbCollocateWthAdvcl(advmodObj) ) {
    			Set<IndexedWord> objNodeSet = NLPRule.getImmediateDirectObj(advmodObj,graph);
        		Set<IndexedWord> setFiltered = filterObjNodeSet(graph,advmodObj,objNodeSet);
    			String reasonForSelection = "[R-30] adv_opinion 的宾语(通过使役结构取得)";
    			for(IndexedWord objNode:setFiltered) {
    				Aspect ap = new Aspect(objNode,reasonForSelection);
    				extendAspectAndAddToList(ap,op,aspectList);
    			}
    		}
    	}
    	//根据主语讨论宾语：
    	IndexedWord nearestSubj = NLPRule.getNearestSubj(adv,graph);
    	if( nearestSubj!=null && peopleWordList.isPeopleNode(nearestSubj) ) {
    		for(IndexedWord advmodObj:advmodObjSet) {
				Set<IndexedWord> objNodeSet = NLPRule.getImmediateObj(advmodObj,graph);
				if( objNodeSet.size()!=0 ) {
					String reasonForSelection = "[R-31] adv_opinion 的宾语";
					Set<IndexedWord> setFiltered = filterObjNodeSet(graph,advmodObj,objNodeSet);
					for(IndexedWord objNode:setFiltered) {
						Aspect ap = new Aspect(objNode,reasonForSelection,1);
	    				extendAspectAndAddToList(ap,op,aspectList);
					}	
				}
			}
    	}
    }
    
    private void extractForOtherOpinion(Opinion op,ArrayList<Aspect> aspectList) {
    	IndexedWord node = op.getCoreOpinionNode();
    	SemanticGraph graph = textParser.getGraphByNode(node);
//    	System.out.println("Dependency Graph:\n " +graph.toString(SemanticGraph.OutputFormat.READABLE));
    	Set<IndexedWord> govSet = graph.getParents(node);
    	for(IndexedWord gov:govSet) {
    		if( isLegalByTag(gov) ) {
    			String reasonForSelection = "[R-32] other_opinion 的修饰对象";
    			Aspect ap = new Aspect(gov,reasonForSelection,0);
    			extendAspectAndAddToList(ap,op,aspectList);
    		}
    	}
    }
    
    private Set<IndexedWord> filterObjNodeSet(SemanticGraph graph,IndexedWord gov,Set<IndexedWord> objNodeSet) {
    	Set<IndexedWord> setFiltered = new HashSet<IndexedWord>();
    	for(IndexedWord objNode:objNodeSet) {
    		boolean reasonableLocaDistribution = objNode.index()>gov.index(); //合理的位置：若宾语在附属结构后
    		boolean isInReasonableLoca = !OPT.isSuppByLocaDistribution || reasonableLocaDistribution;
    		if( isInReasonableLoca ) {
    			if( peopleWordList.isPeopleNode(objNode) ) {
    				String spec = graph.getEdge(gov, objNode).getRelation().getSpecific();
    				if( spec!=null && (spec.equals("to") || spec.equals("for") || spec.equals("as")) ) {
    					continue;
    				}
    				setFiltered.add(objNode);
    			}
        		else if( isLegalObject(objNode) ) {
    				setFiltered.add(objNode);
    			}
    		}
		}
    	return setFiltered;
    }
    
    private boolean isLegalByTag(IndexedWord node) {
    	if( node==null ) {
    		return false;
    	}
    	else if( NLPRule.isNoun(node.tag()) ) {
    		return true;
    	}
    	else if( node.tag().equals("CD") ) {
    		return true;
    	}
    	else if( node.tag().indexOf("DT")!=-1 ){
    		return true;
    	}
    	else if( node.tag().indexOf("PRP")!=-1 ){
    		return true;
    	}
    	return false;
    }
    
    private boolean isLegalObject(IndexedWord node) {
    	boolean isLegalByTag = isLegalByTag(node);
    	if( !isLegalByTag ) {
    		return false;
    	}
    	boolean isTimeNode = timeWordList.isTimeNode(node);
    	return !isTimeNode;
    }
    
    private boolean isLegalSubj(IndexedWord node) {
    	boolean isLegalByTag = isLegalByTag(node);
    	if( !isLegalByTag ) {
    		return false;
    	}
    	boolean isPeople = peopleWordList.isPeopleNode(node);
    	return !isPeople;
    }
    
    private void extendAspectAndAddToList(Aspect ap,Opinion op,ArrayList<Aspect> aspectList) {
    	if( ap==null || ap.getAspectNodeListSize()==0 ) {
    		return;
    	}
    	boolean isCoreNodeInOpinion = isCoreNodeInOpinion(ap,op);
    	if( isCoreNodeInOpinion ) {
    		return;
    	}
    	boolean isPureFunctionAspect = isPureFunctionAspect(ap);
    	if( isPureFunctionAspect ) {
    		return;
    	}
    	IndexedWord node = ap.getCoreAspectNode();
    	String tag = node.tag();
    	if( tag.indexOf("PRP")!=-1 || NLPRule.isCorefDT(node) ) {
    		checkCoref(op,ap,aspectList);
    	}
    	else if( NLPRule.isVerb(tag) ) {
    		extendVerbCoreAspect(op,ap,aspectList);
    	}
    	else if( NLPRule.isNoun(tag) || tag.equals("CD") || tag.equals("DT") ) {
    		extendNounCoreAspect(op,ap,aspectList);
    	}
    	else {
    		addAspectToListWithoutDup(op,ap,aspectList);
    	}	
    }
    
    private boolean isCoreNodeInOpinion(Aspect ap,Opinion op) {
    	IndexedWord coreNode = ap.getCoreAspectNode();
    	boolean isTermInOpinion = op.isNodeInOpinion(coreNode);
    	return isTermInOpinion;
    }
    
    private boolean isPureFunctionAspect(Aspect ap) {
    	IndexedWord functionCore = ap.getCoreAspectNode();
    	boolean coreApIsFunctionWord = vagueWordList.isVagueNode(functionCore);
    	if( !coreApIsFunctionWord ) {
    		return false;
    	}
    	SemanticGraph graph = textParser.getGraphByNode(functionCore);
    	Set<IndexedWord> childSet = graph.getChildren(functionCore);
    	Set<IndexedWord> modNodeSet = new HashSet<IndexedWord>();
		for(IndexedWord child:childSet) {
			String reln = graph.getEdge(functionCore,child).getRelation().toString();
			if( reln.indexOf("nmod")!=-1 ) {
				modNodeSet.add(child);
			}
			else if( NLPRule.isLegalObjReln(reln) ) {
				modNodeSet.add(child);
			}
			else if( reln.equals("compound") ) {
				modNodeSet.add(child);
			}
			else if( reln.equals("appos") ) {
				modNodeSet.add(child);
			}
		}
		boolean isPureFunction = true;
		// 若虚词有与之相连接的实词
		for(IndexedWord modNode:modNodeSet) {
			if( !vagueWordList.isVagueNode(modNode) ) {
				isPureFunction = false;
			}
		}
		return isPureFunction;
    }
    
    private void checkCoref(Opinion op,Aspect ap,ArrayList<Aspect> aspectList){
    	IndexedWord node = ap.getCoreAspectNode();
    	ArrayList< ArrayList<CoreLabel> > corefList = textParser.getCoReferList(node);
    	//指代可以被消解
    	if( corefList!=null && corefList.size()!=0) {
    		for(int i=0;i<corefList.size();i++) {
    			ArrayList<CoreLabel> coref = corefList.get(i);
    			ArrayList<IndexedWord> corefNodeList = new ArrayList<IndexedWord>();
    			for(int j=0;j<coref.size();j++) {
    				CoreLabel corefToken = coref.get(j);
    				int beginPos = corefToken.beginPosition();
    				int corefNodeIndex = textParser.getNodeIndexByBeginPos(beginPos);
    				IndexedWord corefNode = textParser.getNodeByIndex(corefNodeIndex);
    				corefNodeList.add(corefNode);
    			}
    			if( corefNodeList.size()!=0 ) {
    				boolean hasIntersectionWithOpinion = op.hasIntersectionWithOpinion(corefNodeList);
        			if( !hasIntersectionWithOpinion ) {
        				Aspect corefAspect = ap.copyAspect();
        				corefAspect.setAspectNodeList(corefNodeList);
        				addAspectToListWithoutDup(op,corefAspect,aspectList);
        			}
    			}
    		}
    	}else {
    		addAspectToListWithoutDup(op,ap,aspectList);
    	}
    }
    
    private void extendVerbCoreAspect(Opinion op,Aspect ap,ArrayList<Aspect> aspectList){
    	IndexedWord verbCore = ap.getCoreAspectNode();
    	SemanticGraph graph = textParser.getGraphByNode(verbCore);
    	IndexedWord prtNode = null;
    	Set<IndexedWord> childSet = graph.getChildren(verbCore);
		for(IndexedWord child:childSet) {
			String reln = graph.getEdge(verbCore,child).getRelation().toString();
			if( reln.indexOf("prt")!=-1 ) {
				prtNode = child;
			}
		}
		if( prtNode!=null ) {
			ArrayList<IndexedWord> chunk = new ArrayList<IndexedWord>();
			int startBeginPos = verbCore.beginPosition();
			int endBeginPos = prtNode.beginPosition();
			int startTokenIndex = textParser.getNodeIndexByBeginPos(startBeginPos);
			int endTokenIndex = textParser.getNodeIndexByBeginPos(endBeginPos);
			for(int i=startTokenIndex;i<=endTokenIndex;i++) {
				IndexedWord node = textParser.getNodeByIndex(i);
				chunk.add(node);
			}
			ap.setAspectNodeList(chunk);
		}
		if( verbAspectList.isVerbAspect(ap.getAspectNodeList()) ){
			addAspectToListWithoutDup(op,ap,aspectList);
		}
	}
    
    private void extendNounCoreAspect(Opinion op,Aspect ap,ArrayList<Aspect> aspectList){
    	//根据指代关系扩展核心
    	extendNounByCoref(op,ap,aspectList);
    	//根据并列及同位语关系扩展核心
    	ArrayList<IndexedWord> extended = new ArrayList<IndexedWord>();
    	if( OPT.coreExtendType==0 ) {
    		extended = extendBySemanticCompound(ap);
    	}
    	else if( OPT.coreExtendType==1  ) {
    		extended = extendNounBySemanticGraph(ap);
    	}
    	else if( OPT.coreExtendType==2 ) {
    		ArrayList<IndexedWord> extendedByConstituency = extendNounByConstituency(op,ap);
        	ArrayList<IndexedWord> extendedBySemanticGraph = extendNounBySemanticGraph(ap);
        	extended = mergeOrderly(extendedByConstituency,extendedBySemanticGraph);
    	}
    	if( extended.size()>0 ) {
    		ap.setAspectNodeList(extended);
    	}
		addAspectToListWithoutDup(op,ap,aspectList);
    }
    
    private void extendNounByCoref(Opinion op,Aspect ap,ArrayList<Aspect> aspectList) {
    	IndexedWord nounCore = ap.getCoreAspectNode();
    	SemanticGraph graph = textParser.getGraphByNode(nounCore);
    	IndexedWord possNode = null;
    	Set<IndexedWord> childSet = graph.getChildren(nounCore);
		for(IndexedWord child:childSet) {
			String reln = graph.getEdge(nounCore,child).getRelation().toString();
			String childWord = child.word().toLowerCase();
			if( reln.indexOf("nmod")!=-1 && (childWord.equals("its")||childWord.equals("it")) ){
				possNode = child;
			}
		}
		if( possNode!=null ) {
			String reasonForSelection = ap.getReasonForSelection();
			int aspectType = ap.getAspectType();
			Aspect possAp = new Aspect(possNode,reasonForSelection,aspectType);
			checkCoref(op,possAp,aspectList);
		}
    }
    
    private ArrayList<IndexedWord> extendNounByConstituency(Opinion op,Aspect ap){
    	IndexedWord opCore = op.getCoreOpinionNode();
    	IndexedWord apCore = ap.getCoreAspectNode();
    	SemanticGraph graph = textParser.getGraphByNode(apCore);
    	ArrayList<IndexedWord> chunk = new ArrayList<IndexedWord>();
    	Tree root = textParser.getTreeByNode(apCore);
    	Tree apCoreLeaf = textParser.getTreeLeafByNode(apCore);
    	Tree expansionNode = NLPRule.getMaxNounTree(root, apCoreLeaf);
    	//范围优化(1)
//    	Tree opCoreLeaf = textParser.getTreeLeafByNode(opCore);
//    	Tree expansionNode = NLPRule.getMaxNounTreeUnderLCA(root, apCoreLeaf, opCoreLeaf);
		if( expansionNode!=null ) {
			List<Tree> leafList = expansionNode.getLeaves();
			Tree startNode = leafList.get(0);
    		Tree endNode = leafList.get(leafList.size()-1);
    		int startIndex = textParser.getTreeNodeIndex(startNode);
    		int endIndex =  textParser.getTreeNodeIndex(endNode);
    		boolean coreNodeIsIn = false;
    		for(int i=startIndex;i<=endIndex;i++) {
    			IndexedWord node = graph.getNodeByIndex(i);
    			Tree tree = textParser.getTreeLeafByNode(node);
    			boolean isNodeInADVP = NLPRule.isNodeInADVP(tree,expansionNode);
    			boolean isNodeInADJP = NLPRule.isNodeInADJP(tree,expansionNode);
    			//范围优化(2)
//    			boolean isNodeInSBAR = NLPRule.isNodeInSBAR(tree,expansionNode);
//    			boolean isNodeInPRN = NLPRule.isNodeInPRN(tree,expansionNode);
//    			if( coreNodeIsIn && (isNodeInSBAR || isNodeInPRN) ) {
//    				break;
//    			}
//    			if( node.beginPosition()==apCore.beginPosition() ) {
//    				coreNodeIsIn = true;
//    			}
    			if( isLegalByTag(node) ) {
    				chunk.add(node);
    			}
    			else if( !isNodeInADVP && !isNodeInADJP ) {
    				chunk.add(node);
    			}
    		}
    	}
		return chunk;
    }
    
    private ArrayList<IndexedWord> extendNounBySemanticGraph(Aspect ap){
    	IndexedWord nounCore = ap.getCoreAspectNode();
    	SemanticGraph graph = textParser.getGraphByNode(nounCore);
    	ArrayList<IndexedWord> chunk = new ArrayList<IndexedWord>();
    	Set<IndexedWord> modNodeSet = new HashSet<IndexedWord>();
    	Set<IndexedWord> childSet = graph.getChildren(nounCore);
    	ArrayList<IndexedWord> compoundNodeList = NLPRule.getAllCompoundNode(nounCore,graph);
    	modNodeSet.addAll(compoundNodeList);
		for(IndexedWord child:childSet) {
			String reln = graph.getEdge(nounCore,child).getRelation().toString();
			if( reln.indexOf("nmod")!=-1 ) {
				modNodeSet.add(child);
			}else if( NLPRule.isLegalObjReln(reln) ) {
				IndexedWord caseNode = null;
				Set<IndexedWord> grandChildSet = graph.getChildren(child);
				for(IndexedWord grandChild:grandChildSet) {
					reln = graph.getEdge(child,grandChild).getRelation().toString();
					if( reln.equals("case") ) {
						caseNode = grandChild;
					}
				}
				boolean reasonableLocaDistribution = caseNode==null || caseNode.index()==nounCore.index()+1;
				boolean isInReasonableLoca = !OPT.isSuppByLocaDistribution || reasonableLocaDistribution;
				if( isInReasonableLoca ) {
					modNodeSet.add(child);
				}
			}
		}
		if( modNodeSet.size()!=0 ) {
			int nounCoreTokenIndex = textParser.getNodeIndexByNode(nounCore);
			int startTokenIndex = nounCoreTokenIndex;
			int endTokenIndex = nounCoreTokenIndex;
			for(IndexedWord modNode:modNodeSet) {
				int modObjectTokenIndex = textParser.getNodeIndexByNode(modNode);
				startTokenIndex = Math.min(startTokenIndex, modObjectTokenIndex);
				endTokenIndex = Math.max(endTokenIndex, modObjectTokenIndex);
			}
			for(int i=startTokenIndex;i<=endTokenIndex;i++) {
				IndexedWord node = textParser.getNodeByIndex(i);
				chunk.add(node);
			}
		}
		return chunk;
    }
    
    private ArrayList<IndexedWord> extendBySemanticCompound(Aspect ap){
    	IndexedWord core = ap.getCoreAspectNode();
    	SemanticGraph graph = textParser.getGraphByNode(core);
    	ArrayList<IndexedWord> chunk = new ArrayList<IndexedWord>();
    	ArrayList<IndexedWord> compoundNodeList = NLPRule.getAllCompoundNode(core,graph);
		if( compoundNodeList.size()!=0 ) {
			int nounCoreTokenIndex = textParser.getNodeIndexByNode(core);
			int startTokenIndex = nounCoreTokenIndex;
			int endTokenIndex = nounCoreTokenIndex;
			for(IndexedWord compoundNode:compoundNodeList) {
				int modObjectTokenIndex = textParser.getNodeIndexByNode(compoundNode);
				startTokenIndex = Math.min(startTokenIndex, modObjectTokenIndex);
				endTokenIndex = Math.max(endTokenIndex, modObjectTokenIndex);
			}
			for(int i=startTokenIndex;i<=endTokenIndex;i++) {
				IndexedWord node = textParser.getNodeByIndex(i);
				chunk.add(node);
			}
		}
    	return chunk;
    }
    
    private ArrayList<IndexedWord> mergeOrderly(ArrayList<IndexedWord> arr1,ArrayList<IndexedWord> arr2){
    	ArrayList<IndexedWord> mergered = new ArrayList<IndexedWord>();
    	int m = 0;//arr1的索引
    	int n = 0;//arr2的索引
    	for (int i = 0; i<arr1.size()+arr2.size(); i++) {
            if( m<arr1.size() && n<arr2.size() ){
            	if( arr1.get(m).index()<arr2.get(n).index() ) {
            		mergered.add(arr1.get(m));
            		m++;
            	}
            	else if( arr1.get(m).index()==arr2.get(n).index() ) {
            		mergered.add(arr1.get(m));
            		m++;
            		n++;
            	}
            	else {
            		mergered.add(arr2.get(n));
            		n++;
            	}
            }else if( m<arr1.size() ){
            	mergered.add(arr1.get(m));
        		m++;
            }else if( n<arr2.size() ){
            	mergered.add(arr2.get(n));
        		n++;
            }
        }
    	return mergered;
    }
    
    private void addAspectToListWithoutDup(Opinion op,Aspect ap,ArrayList<Aspect> aspectList) {
    	//先修剪 aspect 的范围：
    	trimAspectScope(op,ap);
    	if( ap==null || ap.getAspectNodeListSize()==0 ) {
    		return;
    	}
    	//设置隐式 aspect：
    	setImplicitAspect(ap);
    	//再无重复加入：
    	boolean isContain = false;
    	for(int i=0;i<aspectList.size();i++) {
    		Aspect apInList = aspectList.get(i);
    		if( ap.isSubset(apInList) ) {
    			isContain = true;
    			break;
    		}
    	}
    	if( !isContain ) {
    		for(int i=0;i<aspectList.size();i++) {
        		Aspect apInList = aspectList.get(i);
        		if( apInList.isSubset(ap) ) {
        			aspectList.remove(apInList);
        			i--;
        		}
        	}
    		aspectList.add(ap);
    	}
    }
    
    private void trimAspectScope(Opinion op,Aspect ap){
    	if( ap==null || ap.getAspectNodeListSize()==0 ) {
    		return;
    	}
    	IndexedWord apCoreNode = ap.getCoreAspectNode();
    	SemanticGraph graph = textParser.getGraphByNode(apCoreNode);
    	ArrayList<IndexedWord> apNodeList = ap.getAspectNodeList();
    	ArrayList<IndexedWord> trimedApNodeList = new ArrayList<IndexedWord>();
    	IndexedWord opinionStartNode = op.getOpinionStartNode();
    	IndexedWord opinionEndNode = op.getOpinionEndNode();
    	//修剪一：根据和opinion的相对位置进行修剪：
    	int startIndex = apNodeList.get(0).index();
		int endIndex =  apNodeList.get(apNodeList.size()-1).index();
		if( opinionStartNode!=null && opinionEndNode!=null) {
			int startMax = Math.max(startIndex, opinionStartNode.index());
			int endMin = Math.min(endIndex,opinionEndNode.index());
			// opinion 和 aspect 当前范围有交集
			if( endMin>=startMax ) {
				if( opinionEndNode.index()<=apCoreNode.index() ) {
					startIndex = opinionEndNode.index()+1;
				}
				if( opinionStartNode.index()>=apCoreNode.index() ) {
					endIndex = opinionStartNode.index()-1;
				}
			}
		}
		for(int i=startIndex;i<=endIndex;i++) {
			IndexedWord node = graph.getNodeByIndex(i);
			trimedApNodeList.add(node);
		}
		//修剪二：根据前期设置进行修剪
		int leftMostBoundary = ap.getLeftMostBoundary();
		int rightMostBoundary = ap.getRightMostBoundary();
		if( leftMostBoundary!=-1 ) {
			for(int i=0;i<trimedApNodeList.size();i++) {
	    		IndexedWord node = trimedApNodeList.get(i);
	    		if( node.index()<leftMostBoundary ) {
	    			trimedApNodeList.remove(node);
	    			i--;
	    		}
	    	}
		}
		if( rightMostBoundary!=-1 ) {
			for(int i=0;i<trimedApNodeList.size();i++) {
	    		IndexedWord node = trimedApNodeList.get(i);
	    		if( node.index()>rightMostBoundary ) {
	    			trimedApNodeList.remove(node);
	    			i--;
	    		}
	    	}
		}
		//修剪三：根据节点的属性进行修剪
		for(int i=0;i<trimedApNodeList.size();i++) {
    		IndexedWord node = trimedApNodeList.get(i);
    		String tag = node.tag();
    		String ner = node.ner();
    		if( tag.indexOf("PRP")!=-1 || tag.indexOf("DT")!=-1 || tag.equals("IN") || tag.equals("CC") || tag.startsWith("W") 
    			|| tag.equals("TO") || tag.equals(".") || tag.equals("HYPH") || tag.equals(",") ) {
    			trimedApNodeList.remove(node);
    			i--;
    		}
    		else if( ner.equals("ORDINAL") ) {
    			trimedApNodeList.remove(node);
    			i--;
    		}
    		else if( !NLPRule.isVerb(apCoreNode) && NLPRule.isAdv(node) ) {
    			trimedApNodeList.remove(node);
    			i--;
    		}
    		else if( timeWordList.isTimeNode(node) ) {
    			trimedApNodeList.remove(node);
    			i--;
    		}
    		else if( vagueWordList.isVagueNode(node) ) {
    			trimedApNodeList.remove(node);
    			i--;
    		}
    	}
		ap.setAspectNodeList(trimedApNodeList);
    }
    
    private void setImplicitAspect(Aspect ap) {
    	ArrayList<IndexedWord> aspectNodeList = ap.getAspectNodeList();
    	if( aspectNodeList.size()==1 ) {
    		IndexedWord node = aspectNodeList.get(0);
    		if( implicitAspectWordList.isWordInImplicitAspectWordSet(node.lemma().toLowerCase()) ) {
    			ap.setImplicitAspect(true);
    		}
    	}
    }
  	
}
