package extractor;

import java.util.ArrayList;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;

public class TextCorefChain {
	private ArrayList< ArrayList<CoreLabel> > chain;
	
	public TextCorefChain() {
		chain = new ArrayList< ArrayList<CoreLabel> >();
	}
	
	public void addCoref(ArrayList<CoreLabel> coref) {
		chain.add(coref);
	}
	
	public int getChainSize() {
		return chain.size();
	}
	
	public boolean isNodeInThisChain(IndexedWord node) {
		for(ArrayList<CoreLabel> coref:chain) {
			if( isNodeInOneCoref(node,coref) ) {
				return true;
			}
		}
		return false;
	}
	
    private boolean isNodeInOneCoref(IndexedWord node,ArrayList<CoreLabel> coref) {
		for(CoreLabel token:coref  ) {
			if( token.beginPosition()==node.beginPosition() ){
				return true;
			}
		}
		return false;
	}
    
    public ArrayList< ArrayList<CoreLabel> > getCorefList(IndexedWord node){
    	ArrayList< ArrayList<CoreLabel> > corefList = null;
    	//节点不在指代链条上，则返回 null
    	if( !isNodeInThisChain(node) ) {
    		return corefList;
    	}
    	//节点在指代链条上，则返回该链条上除本身外的指代
    	else {
    		corefList = new ArrayList< ArrayList<CoreLabel> >();
    		for(ArrayList<CoreLabel> coref:chain) {
    			if( !isNodeInOneCoref(node,coref) ) {
    				corefList.add(coref);
    			}
    		}
    		return corefList;
    	}
    }
	

}
