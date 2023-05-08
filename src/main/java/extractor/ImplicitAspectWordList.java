package extractor;

import java.util.HashSet;
import java.util.List;

public class ImplicitAspectWordList {
	private HashSet<String> implicitAspectWordSet;
	public void initImplicitAspectWordList(String path) {
		boolean isFileExists = FileIOs.isFileExists(path);
		if( !isFileExists ) {
			System.err.println("文件不存在! "+path);
			return;
		}
		implicitAspectWordSet = new HashSet<String>();
		List<String> wordList = FileIOs.readFileGetStringList(path);
		for(String word:wordList) {
			if(word.length()==0) {
				continue;
			}
			implicitAspectWordSet.add(word);
		}
	}
	
	public boolean isWordInImplicitAspectWordSet(String word) {
		return implicitAspectWordSet.contains(word);
	}

}
