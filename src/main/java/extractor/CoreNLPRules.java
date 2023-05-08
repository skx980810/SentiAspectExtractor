package extractor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.trees.GrammaticalRelation;
import edu.stanford.nlp.trees.Tree;

public class CoreNLPRules {
	
	public String simplifyTag(String tag) {
		if(tag.length()>2) {
			tag=tag.substring(0,2);
		}
		return tag;
	}
	
	public boolean isAdv(String tag) {
		return simplifyTag(tag).equals("RB");
	}
	
	public boolean isAdv(IndexedWord node) {
		return isAdv(node.tag());
	}
    
    public boolean isAdj(String tag) {
    	return simplifyTag(tag).equals("JJ");
	}
    
    public boolean isAdj(IndexedWord node) {
		return isAdj(node.tag());
	}
    
    public boolean isVerb(String tag) {
		return simplifyTag(tag).equals("VB");
	}
    
    public boolean isVerb(IndexedWord node) {
		return isVerb(node.tag());
	}
    
    public boolean isNoun(String tag) {
		return simplifyTag(tag).equals("NN");
	}
    
    public boolean isNoun(IndexedWord node) {
		return isNoun(node.tag());
	}
    
    public boolean isHave(String word,String[] arr) {
		for(int i=0;i<arr.length;i++) {
			if(word.equals(arr[i])) {
			   return true;
		    }
		}
	    return false;
    }
    
    private String[] verbCollocateWthAdvcl = {"make","find"};
	public boolean isVerbCollocateWthAdvcl(IndexedWord node) {
		return isHave(node.lemma(),verbCollocateWthAdvcl);
	}
	
	private String[] corefDT = {"this","that","those","these"};
	public boolean isCorefDT(IndexedWord node) {
		return isHave(node.lemma(),corefDT);
	}
	
	public boolean isModReln(String reln) {
		String relnSimplified = reln;
		int indexOfColon = reln.indexOf(":");
		if( indexOfColon!=-1 ) {
			relnSimplified = reln.substring(0,indexOfColon);
		}
		return relnSimplified.indexOf("mod")!=-1;
	}
	
	//查找最近的主语
	//left：subj
	//right：gov of subj
	public Pair<IndexedWord,IndexedWord> getNearestSubjGovPair(IndexedWord node,SemanticGraph graph) {
  		ArrayList<IndexedWord> nodeListToRoot = new ArrayList<IndexedWord>();
  		nodeListToRoot.add(node);
  		nodeListToRoot.addAll(graph.getPathToRoot(node));
  		for(int i=0;i<nodeListToRoot.size();i++) {
  			IndexedWord nodeToRoot = nodeListToRoot.get(i);
  			IndexedWord nodeGov = i+1<nodeListToRoot.size() ? nodeListToRoot.get(i+1) : null ;
  			String reln = nodeGov!=null ? graph.getEdge(nodeGov,nodeToRoot).getRelation().toString() : "" ;
  			if( isSubjReln(reln) ) { 
				return ImmutablePair.of(null,null);
  			}
  			Set<IndexedWord> childSet = graph.getChildren(nodeToRoot);
  			ArrayList<IndexedWord> subjList = new ArrayList<IndexedWord>();
  			for(IndexedWord child:childSet) {
  				reln = graph.getEdge(nodeToRoot,child).getRelation().toString();
  				if( isSubjReln(reln) ) {
  					subjList.add(child);
  				}
  			}
  			if( subjList.size()!=0 ) {
  				IndexedWord subj = selectFromSameLevelSubj(node,subjList);
  				Pair<IndexedWord,IndexedWord> subjGovPair = ImmutablePair.of(subj,nodeToRoot);
  				return subjGovPair;
  			}
  		}
		return ImmutablePair.of(null,null);
  	}
	
	public IndexedWord getNearestSubj(IndexedWord node,SemanticGraph graph) {
		Pair<IndexedWord,IndexedWord> subjGovPair = getNearestSubjGovPair(node,graph);
		return subjGovPair.getLeft();
  	}
	
	//找到直接的主语
  	public IndexedWord getImmediateSubj(IndexedWord node,SemanticGraph graph) {
  		ArrayList<IndexedWord> subjList = new ArrayList<IndexedWord>();
  		Set<IndexedWord> childSet = graph.getChildren(node);
  		for(IndexedWord child:childSet) {
			String reln = graph.getEdge(node,child).getRelation().toString();
			if( isSubjReln(reln) ) {
				subjList.add(child);
			}
		}
  		if( subjList.size()!=0 ) {
  			IndexedWord subj = selectFromSameLevelSubj(node,subjList);
			return subj;	
		}else {
			return null;
		}
  	}
  	
    //从同一深度的 ArrayList 中找到合适的 subj
  	public IndexedWord selectFromSameLevelSubj(IndexedWord node, ArrayList<IndexedWord> subjList) {
  		IndexedWord subj = null;
  		if( subjList.size()==1 ) {
			subj = subjList.get(0);
		}
		else {
			IndexedWord nearestNonPRPSubj = null;
			IndexedWord nearestPRPSubj = null;
			for(int j=0;j<subjList.size();j++) {
				IndexedWord candidateSubj = subjList.get(j);
				if( candidateSubj.tag().equals("PRP") ) {
					if( nearestPRPSubj==null ) {
						nearestPRPSubj = candidateSubj;
					}else if( getDistanceBetweenNodes(nearestPRPSubj,node)>getDistanceBetweenNodes(candidateSubj,node) ){
						nearestPRPSubj = candidateSubj;
					}
				}else {
					if( nearestNonPRPSubj==null ) {
						nearestNonPRPSubj = candidateSubj;
					}else if( getDistanceBetweenNodes(nearestNonPRPSubj,node)>getDistanceBetweenNodes(candidateSubj,node) ){
						nearestNonPRPSubj = candidateSubj;
					}
				}
			}
			if( nearestNonPRPSubj==null ) {
				subj = nearestPRPSubj;
			}
			else {
				subj = nearestNonPRPSubj;
			}
		}	
  		return subj;	
  	}
  	
  	private int getDistanceBetweenNodes(IndexedWord node1,IndexedWord node2) {
		return Math.abs(node1.index()-node2.index());
	}
  	
  	public boolean isSubjReln(String reln) {
  		return reln.indexOf("subj")!=-1;
  	}
  	
    //找到最近的宾语
  	public Set<IndexedWord> getNearestObj(IndexedWord node,SemanticGraph graph) {
  		Set<IndexedWord> nearestObjSet = new HashSet<IndexedWord>();
  		Queue queue = new LinkedList<IndexedWord>(); 
        queue.add(node);
        boolean hasFound = false;
        while(!queue.isEmpty()) {
        	node = (IndexedWord)queue.poll();
        	Set<IndexedWord> childSet = graph.getChildren(node);
        	for(IndexedWord child:childSet) {
  				String reln = graph.getEdge(node,child).getRelation().toString();
  				if( isLegalObjReln(reln) ) {
  					nearestObjSet.add(child);
  					hasFound = true;
  				}
  				queue.add(child);
  			}
        	if( hasFound ) {
        		break;
        	}
        }
  		return nearestObjSet;
  	}
  	
    //找到直系关系中的通过“obl:as”方式连接的宾语
  	public Set<IndexedWord> getImmediateAsObj(IndexedWord node,SemanticGraph graph) {
  		Set<IndexedWord> childSet = graph.getChildren(node);
    	Set<IndexedWord> asObjNodeSet = new HashSet<IndexedWord>();
		for(IndexedWord child:childSet) {
			String reln = graph.getEdge(node,child).getRelation().toString();
			if( reln.equals("obl:as") ) {
				asObjNodeSet.add(child);
			}
		}
		return asObjNodeSet;
  	}
  	
    //找到直系关系中的所有宾语
  	public Set<IndexedWord> getImmediateObj(IndexedWord node,SemanticGraph graph) {
  		Set<IndexedWord> childSet = graph.getChildren(node);
    	Set<IndexedWord> objNodeSet = new HashSet<IndexedWord>();
		for(IndexedWord child:childSet) {
			String reln = graph.getEdge(node,child).getRelation().toString();
			if( isLegalObjReln(reln) ) {
				objNodeSet.add(child);
			}
		}
		return objNodeSet;
  	}
  	
    //找到直系关系中的所有直接宾语
  	public Set<IndexedWord> getImmediateDirectObj(IndexedWord node,SemanticGraph graph) {
  		Set<IndexedWord> childSet = graph.getChildren(node);
    	Set<IndexedWord> objNodeSet = new HashSet<IndexedWord>();
		for(IndexedWord child:childSet) {
			String reln = graph.getEdge(node,child).getRelation().toString();
			if( isDirectObjReln(reln) ) {
				objNodeSet.add(child);
			}
		}
		return objNodeSet;
  	}
  	
  	//是否是直接宾语关系
  	public boolean isDirectObjReln(String reln) {
  		return reln.indexOf("obj")!=-1;
  	}
  	
    //是否是合规的间接宾语关系
  	public boolean isLegalInDirectObjReln(String reln) {
  		if( reln.indexOf("obl")!=-1 ) {
			// "as"是有争议的
			//if( reln.indexOf("as")!=-1 ) {
			//	return false;
			//}
			if( reln.indexOf("tmod")!=-1 ) {
				return false;
			}
			if( reln.indexOf("than")!=-1 ) {
				return false;
			}
			if( reln.indexOf("despite")!=-1 ) {
				return false;
			}
			if( reln.indexOf("after")!=-1 ) {
				return false;
			}
			if( reln.indexOf("over")!=-1 ) {
				return false;
			}
			if( reln.indexOf("npmod")!=-1 ) {
				return false;
			}
			if( reln.indexOf("within")!=-1 ) {
				return false;
			}
			if( reln.indexOf("except")!=-1 ) {
				return false;
			}
			if( reln.indexOf("through")!=-1 ) {
				return false;
			}
			if( reln.indexOf("from")!=-1 ) {
				return false;
			}
			else {
				return true;
			}
		}
  		return false;
  	}
  	
  	//是否为合理的宾语关系(直接间接皆可)
  	public boolean isLegalObjReln(String reln) {
  		boolean isDirectObjReln = isDirectObjReln(reln);
  		boolean isLegalInDirectObjReln = isLegalInDirectObjReln(reln);
  		return isDirectObjReln || isLegalInDirectObjReln;
  	}
  	
    //为动词找到直接的宾语
  	public Set<IndexedWord> getDirectObjForVerb(IndexedWord node,SemanticGraph graph) {
  		Set<IndexedWord> childSet = graph.getChildren(node);
    	Set<IndexedWord> objNodeSet = new HashSet<IndexedWord>();
		for(IndexedWord child:childSet) {
			String reln = graph.getEdge(node,child).getRelation().toString();
			if( isLegalObjRelnForVerb(reln) ) {
				objNodeSet.add(child);
			}
		}
		return objNodeSet;
  	}
  	
    //对于动词来说，是为合理的宾语关系
  	public boolean isLegalObjRelnForVerb(String reln) {
  		if( reln.indexOf("obj")!=-1 ) {
			return true;
		}else if( reln.indexOf("obl")!=-1 ) {
			if( reln.indexOf("by")!=-1 ) {
				return true;
			}
		}
  		return false;
  	}
    
    //找到 并列词汇
	public ArrayList<IndexedWord> getImmediateAndNode(IndexedWord node, SemanticGraph graph) {
		ArrayList<IndexedWord> andNodeSet = new ArrayList<IndexedWord>();
  		Set<IndexedWord> childSet = graph.getChildren(node);
    	for(IndexedWord child:childSet) {
			String reln = graph.getEdge(node,child).getRelation().toString();
			if( reln.equals("conj:and") && !andNodeSet.contains(child) ) {
				andNodeSet.add(child);
			}
		}
    	Set<IndexedWord> govSet = graph.getParents(node);
    	for(IndexedWord gov:govSet) {
			String reln = graph.getEdge(gov,node).getRelation().toString();
			if( reln.equals("conj:and") && !andNodeSet.contains(gov) ) {
				andNodeSet.add(gov);
			}
		}
		return andNodeSet;
	}
	public ArrayList<IndexedWord> getAllAndNode(IndexedWord node, SemanticGraph graph){
		ArrayList<IndexedWord> andNodeList = new ArrayList<IndexedWord>();
  		LinkedList<IndexedWord> queue = new LinkedList<IndexedWord>(); 
        queue.add(node);
        while(!queue.isEmpty()) {
        	node = (IndexedWord)queue.poll();
        	ArrayList<IndexedWord> immeAndNodeList = getImmediateAndNode(node,graph);
        	for(IndexedWord immeAndNode:immeAndNodeList) {
  				if( !andNodeList.contains(immeAndNode) ) {
  					andNodeList.add(immeAndNode);
  					queue.add(immeAndNode);
  				}
  			}
        }
        return andNodeList;
	}
	
	//找到 compound
	public ArrayList<IndexedWord> getImmediateCompoundNode(IndexedWord node, SemanticGraph graph) {
		ArrayList<IndexedWord> compoundNodeList = new ArrayList<IndexedWord>();
  		Set<IndexedWord> childSet = graph.getChildren(node);
    	for(IndexedWord child:childSet) {
			String reln = graph.getEdge(node,child).getRelation().toString();
			if( reln.equals("compound") && !compoundNodeList.contains(child) ) {
				compoundNodeList.add(child);
			}
		}
    	Set<IndexedWord> govSet = graph.getParents(node);
    	for(IndexedWord gov:govSet) {
			String reln = graph.getEdge(gov,node).getRelation().toString();
			if( reln.equals("compound") && !compoundNodeList.contains(gov) ) {
				compoundNodeList.add(gov);
			}
		}
		return compoundNodeList;
	}
	public ArrayList<IndexedWord> getAllCompoundNode(IndexedWord node, SemanticGraph graph){
		ArrayList<IndexedWord> compoundNodeList = new ArrayList<IndexedWord>();
  		LinkedList<IndexedWord> queue = new LinkedList<IndexedWord>(); 
        queue.add(node);
        while(!queue.isEmpty()) {
        	node = (IndexedWord)queue.poll();
        	ArrayList<IndexedWord> immeCompoundNodeList = getImmediateCompoundNode(node,graph);
        	for(IndexedWord immeCompoundNode:immeCompoundNodeList) {
  				if( !compoundNodeList.contains(immeCompoundNode) ) {
  					compoundNodeList.add(immeCompoundNode);
  					queue.add(immeCompoundNode);
  				}
  			}
        }
        return compoundNodeList;
	}
	
	//找到 appos
	public ArrayList<IndexedWord> getImmediateApposNode(IndexedWord node, SemanticGraph graph) {
		ArrayList<IndexedWord> apposNodeSet = new ArrayList<IndexedWord>();
  		Set<IndexedWord> childSet = graph.getChildren(node);
    	for(IndexedWord child:childSet) {
			String reln = graph.getEdge(node,child).getRelation().toString();
			if( reln.equals("appos") && !apposNodeSet.contains(child) ) {
				apposNodeSet.add(child);
			}
		}
    	Set<IndexedWord> govSet = graph.getParents(node);
    	for(IndexedWord gov:govSet) {
			String reln = graph.getEdge(gov,node).getRelation().toString();
			if( reln.equals("appos") && !apposNodeSet.contains(gov) ) {
				apposNodeSet.add(gov);
			}
		}
		return apposNodeSet;
	}
	public ArrayList<IndexedWord> getAllApposNode(IndexedWord node, SemanticGraph graph){
		ArrayList<IndexedWord> apposNodeList = new ArrayList<IndexedWord>();
  		LinkedList<IndexedWord> queue = new LinkedList<IndexedWord>(); 
        queue.add(node);
        while(!queue.isEmpty()) {
        	node = (IndexedWord)queue.poll();
        	ArrayList<IndexedWord> immeApposNodeList = getImmediateApposNode(node,graph);
        	for(IndexedWord immeApposNode:immeApposNodeList) {
  				if( !apposNodeList.contains(immeApposNode) ) {
  					apposNodeList.add(immeApposNode);
  					queue.add(immeApposNode);
  				}
  			}
        }
        return apposNodeList;
	}
	
	public Tree getNearestNounTree(Tree root,Tree leaf) {
		//i=0 为自身单词；i=1为词性
		Tree grandFather = leaf.ancestor(2,root);
		String label = grandFather.value();
		if( label.startsWith("N") ) {
			return grandFather;
		}
		else {
			return null;
		}
	}
	
	public Tree getMaxNounTree(Tree root,Tree leaf) {
		Tree maxNounTree = null;
		int depth = root.depth();
		//i=0 为自身单词；i=1为词性
		for(int i=2;i<depth;i++) {
			Tree ancestor = leaf.ancestor(i,root);
			if( ancestor==null ) {
				break;
			}
			String label = ancestor.value();
			if( label.startsWith("N") ) {
				maxNounTree = ancestor;
			}else {
				break;
			}
		}
		return maxNounTree;
	}
	
	//返回 node1 和 node2 的最低共同祖先；
	public Tree getLowestCommonAncestor(Tree root,Tree node1,Tree node2) {
		int depth = root.depth();
		for(int i=0;i<depth;i++) {
			Tree ancestor = node1.ancestor(i,root);
			if( ancestor==null ) {
				break;
			}
			if( ancestor.contains(node2) ) {
				return ancestor;
			}
		}
		return null;
	}
	
	//返回共同祖先下的最大名词团块
	public Tree getMaxNounTreeUnderLCA(Tree root,Tree apleaf,Tree opleaf) {
		Tree lowestCommonAncestor = getLowestCommonAncestor(root,apleaf,opleaf);
		Tree maxNounTree = null;
		int depth = root.depth();
		//i=0 为自身单词；i=1为词性
		for(int i=2;i<depth;i++) {
			Tree ancestor = apleaf.ancestor(i,root);
			if( ancestor==null ) {
				break;
			}
			String label = ancestor.value();
			if( label.startsWith("N") ) {
				maxNounTree = ancestor;
			}else {
				break;
			}
			if( ancestor.equals(lowestCommonAncestor) ) {
				break;
			}
		}
		return maxNounTree;
	}
	
	// 判断节点是否在某种类型团块中
	public boolean isNodeInCentainPhrase(Tree node,Tree root,String phraseName) {
		boolean isNodeInCentainPhrase = false;
		int depth = root.depth();
		for(int i=1;i<depth;i++) {
			Tree ancestor = node.ancestor(i,root);
			if( ancestor==null ) {
				break;
			}
			String nodeString = ancestor.value();
			if( nodeString.equals(phraseName) ) {
				isNodeInCentainPhrase = true;
				break;
			}
		}
		return isNodeInCentainPhrase;
	}
	
	//是否位于ADVP中
	public boolean isNodeInADVP(Tree node,Tree root) {
		String phraseName = "ADVP";
		return isNodeInCentainPhrase(node,root,phraseName);
	}
	
	//是否位于ADVP中
	public boolean isNodeInADJP(Tree node,Tree root) {
		String phraseName = "ADJP";
		return isNodeInCentainPhrase(node,root,phraseName);
	}
	
	//是否位于从句中
	public boolean isNodeInSBAR(Tree node,Tree root) {
		String phraseName = "S";
		return isNodeInCentainPhrase(node,root,phraseName);
	}	
	
	//是否位于PRN中
	public boolean isNodeInPRN(Tree node,Tree root) {
		String phraseName = "PRN";
		return isNodeInCentainPhrase(node,root,phraseName);
	}
	
	//Tree node 和 IndexedWord node 指代同一个对象
	public boolean isReferToSame(Tree tree,IndexedWord node) {
		String treeLabel = tree.label().toString();
		int lastHYPHIndex = treeLabel.lastIndexOf("-");
		String word = treeLabel.substring(0,lastHYPHIndex);
		int index = Integer.parseInt( treeLabel.substring(lastHYPHIndex+1,treeLabel.length()) );
		return word.equals(node.word()) && index==node.index();
	}

}
