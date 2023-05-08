package extractor;

public class AnalysisOptions {
	public String dictPath;
	public String peopleWordListFileName;
	public String peopleWordListPath;
	public String timeWordListFileName;
	public String timeWordListPath;
	public String vagueWordListFileName;
	public String vagueWordListPath;
	public String verbAspectWordListFileName;
	public String verbAspectWordListPath;
	public String implicitAspectWordListFileName;
	public String implicitAspectWordListPath;
	
	public boolean isTextPreprocessing = true; //是否进行文本预处理
	public boolean isExplain = false;  //是否解释提取aspect的理由
	public boolean isUseCoreference = true;  //是否使用指代消解
	public boolean isSuppByLocaDistribution = true;  //是否以词和词之间的相对位置辅助分析
	/*如何扩展核心aspect节点
	 * 0:仅利用 compound 进行扩展；
	 * 1:利用 compound + nmod进行扩展；
	 * 2: 范围最广的扩展，利用选区分析进行扩展；
	 * */
	public int coreExtendType = 0;  //是否以词和词之间的相对位置辅助分析
	
	/*以何种格式输出aspect：
	 * 0:原始输出格式，针对opinion，以列表形式输出；e.g. [4,5]:[0,1] , [5,6] ;
	 * 1:json输出格式，以opinion aspect 列表形式输出；e.g. {"opinion": [1,2], "aspect": [3,4]}
	 * */
	public int outputFormat = 1;
	
	/*如何处理 illegal opinion / implicit opinion：
	 * 0: 输出“cannot_deal”
	 * 1: 只输出[-1,-1]作为aspect
	 * 2: 不输出该opinion
	 * */
	public int implicitOpinionDealType = 1;
	
	public AnalysisOptions() {
		dictPath = System.getProperty("user.dir")+"/src/main/resources/dictionary/";
		peopleWordListFileName = "peopleWordList.txt";
		timeWordListFileName = "timeWordList.txt";
		vagueWordListFileName = "vagueWordList.txt";
		verbAspectWordListFileName = "verbAspectList.txt";
		implicitAspectWordListFileName = "implicitAspectWordList.txt";
	}
	
	public void updatePath() {
		peopleWordListPath = dictPath + peopleWordListFileName;
		timeWordListPath = dictPath + timeWordListFileName;
		vagueWordListPath = dictPath + vagueWordListFileName;
		verbAspectWordListPath = dictPath + verbAspectWordListFileName;
		implicitAspectWordListPath = dictPath + implicitAspectWordListFileName;
	}
	

}
