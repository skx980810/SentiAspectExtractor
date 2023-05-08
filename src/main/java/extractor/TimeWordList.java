package extractor;

import java.util.HashSet;
import java.util.List;

import edu.stanford.nlp.ling.IndexedWord;

public class TimeWordList {
	private HashSet<String> timeWordSet;
	
	public void initTimeWordList(String path) {
		boolean isFileExists = FileIOs.isFileExists(path);
		if( !isFileExists ) {
			System.err.println("文件不存在! "+path);
			return;
		}
		timeWordSet = new HashSet<String>();
		List<String> wordList = FileIOs.readFileGetStringList(path);
		for(String word:wordList) {
			if(word.length()==0) {
				continue;
			}
			timeWordSet.add(word);
		}
	}
	
	public boolean isTimeWord(String word) {
		return timeWordSet.contains(word);
	}
	
	public boolean isTimeNode(IndexedWord node) {
		if( node==null ) {
			return false;
		}
		boolean isTimeLemma = isTimeWord(node.lemma().toLowerCase());
		if( isTimeLemma ) {
			return true;
		}
		boolean isTimeWord = isTimeWord(node.word().toLowerCase());
		if( isTimeWord ) {
			return true;
		}
		String NER = node.ner()==null ? "":node.ner();
		if( NER.equals("DATE") || NER.equals("DURATION") ) {
			return true;
		}
		return false;
	}

}
