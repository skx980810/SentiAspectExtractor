package extractor;

import java.util.HashSet;
import java.util.List;

import edu.stanford.nlp.ling.IndexedWord;

public class VagueWordList {
	private HashSet<String> vagueWordSet;
	
	public void initVagueWordList(String path) {
		boolean isFileExists = FileIOs.isFileExists(path);
		if( !isFileExists ) {
			System.err.println("文件不存在! "+path);
			return;
		}
		vagueWordSet = new HashSet<String>();
		List<String> wordList = FileIOs.readFileGetStringList(path);
		for(String word:wordList) {
			if(word.length()==0) {
				continue;
			}
			if( !word.startsWith("#") ) {
				vagueWordSet.add(word);
			}
		}
	}
	
	public boolean isWordInVagueSet(String word) {
		return vagueWordSet.contains(word);
	}
	
	public boolean isVagueWordByRegular(String word) {
		if( word.endsWith("thing") || word.endsWith("things") ) {
			return true;
		}
		else if( word.startsWith("one") ) {
			return true;
		}
		return false;
	}
	
	public boolean isVagueNode(IndexedWord node) {
		if( node==null ) {
			return false;
		}
		String lemma = node.lemma().toLowerCase();
		boolean isVagueWord = isWordInVagueSet(lemma);
		if( isVagueWord ) {
			return true;
		}
		String word = node.word().toLowerCase();
		boolean isVagueWordByRegular = isVagueWordByRegular(word);
		return isVagueWordByRegular;
	}

}
