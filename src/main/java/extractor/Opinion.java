package extractor;

import java.util.ArrayList;

import edu.stanford.nlp.ling.IndexedWord;

public class Opinion {
	private boolean islegal;
	private IndexedWord coreOpinionNode;
	private ArrayList<IndexedWord> opinionNodeList;
	public Opinion(IndexedWord coreOpinionNode,ArrayList<IndexedWord> opinionNodeList) {
		super();
		this.islegal = false;
		this.opinionNodeList = opinionNodeList;
		this.coreOpinionNode = coreOpinionNode;
		if( this.coreOpinionNode!=null ) {
			this.islegal = true;
		}
	}
	
	public boolean isLegal() {
		return islegal;
	}
	
	public IndexedWord getCoreOpinionNode() {
		return coreOpinionNode;
	}
	
	public void setCoreOpinionNode(IndexedWord coreOpinionNode) {
		this.coreOpinionNode = coreOpinionNode;
	}
	
	public String getCoreOpinionNodeTag() {
		if( coreOpinionNode==null || coreOpinionNode.tag()==null) {
			return "";
		}
		return coreOpinionNode.tag();
	}
	
	public String getCoreOpinionNodeString() {
		if( coreOpinionNode==null || coreOpinionNode.tag()==null) {
			return "";
		}
		return coreOpinionNode.toString();
	}

	public ArrayList<IndexedWord> getOpinionNodeList() {
		return opinionNodeList;
	}

	public void setOpinionNodeList(ArrayList<IndexedWord> opinionNodeList) {
		this.opinionNodeList = opinionNodeList;
	}
	
	public IndexedWord getOpinionStartNode() {
		if( opinionNodeList==null || opinionNodeList.size()==0 ) {
			return null;
		}else {
			IndexedWord startNode = opinionNodeList.get( 0 );
			return startNode;
		}
	}
	
	public IndexedWord getOpinionEndNode() {
		if( opinionNodeList==null || opinionNodeList.size()==0 ) {
			return null;
		}else {
			IndexedWord endNode = opinionNodeList.get( opinionNodeList.size()-1 );
			return endNode;
		}
	}
	
	public boolean isNodeInOpinion(IndexedWord node) {
    	for(int i=0;i<opinionNodeList.size();i++) {
    		if( opinionNodeList.get(i).beginPosition()==node.beginPosition() ) {
    			return true;
    		}
    	}
    	return false;
    }
	
	public boolean hasIntersectionWithOpinion(ArrayList<IndexedWord> nodeList) {
    	for(int i=0;i<nodeList.size();i++) {
    		if( isNodeInOpinion(nodeList.get(i)) ) {
    			return true;
    		}
    	}
    	return false;
    }
	
}
