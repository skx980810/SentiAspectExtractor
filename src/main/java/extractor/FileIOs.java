package extractor;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class FileIOs {
	
	public static void cleanFile(String fileName) {
		File file = new File(fileName);
    	try {
            //如果文件不存在，则创建新的文件
            if(!file.exists()){
                file.createNewFile();
            }
			FileWriter fileWritter = new FileWriter(fileName);
			fileWritter.write("");
			fileWritter.flush();
			fileWritter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
	}
	
	public static void createDirs(String fileName){
		File file = new File(fileName);
		if(!file.exists()){
			file.mkdirs();
        }
	}
	
	public static void delFolder(String folderPath) {
		try {
			delAllFile(folderPath); //删除完里面所有内容
			String filePath = folderPath;
			filePath = filePath.toString();
			java.io.File myFilePath = new java.io.File(filePath);
			myFilePath.delete(); //删除空文件夹
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	public static boolean delAllFile(String path) {
		boolean flag = false;
		File file = new File(path);
		if (!file.exists()) {
			return flag;
		}

		if (!file.isDirectory()) {
			return flag;
		}

		String[] tempList = file.list();
		File temp = null;
		for (int i = 0; i < tempList.length; i++) {
			if (path.endsWith(File.separator)) {
				temp = new File(path + tempList[i]);
			} else {
				temp = new File(path + File.separator + tempList[i]);
			}
			if (temp.isFile()) {
				temp.delete();
			}
			if (temp.isDirectory()) {
				delAllFile(path + "/" + tempList[i]);//先删除文件夹里面的文件
				delFolder(path + "/" + tempList[i]);//再删除空文件夹
				flag = true;
			}
		}
		return flag;
    }
	
	public static void copyFile(String inputFName, String outputFName){
		File source = new File(inputFName);
		File dest = new File(outputFName);
		InputStream is = null;
	    OutputStream os = null;
		try {
			is = new FileInputStream(source);
			os = new FileOutputStream(dest);
			byte[] buffer = new byte[1024];
			int length;
			while ((length = is.read(buffer)) > 0) {
				os.write(buffer, 0, length);
			}
			is.close();
			os.close();
		 } catch (FileNotFoundException e) {
			 e.printStackTrace();
		 } catch (IOException e) {
			 e.printStackTrace();
	     }
	}
	
	public static void writeStringListToFile(String fname,List<String> arr,boolean isAppend,boolean isRemainNullLine) {
		File file = new File(fname);
    	try {
            //如果文件不存在，则创建新的文件
            if(!file.exists()){
                file.createNewFile();
            }
            //打开一个写文件器，构造函数中的第二个参数true表示以追加形式写文件
			FileWriter fileWritter = new FileWriter(fname,isAppend);
			for(String text:arr) {
				if( text==null ) {
					if( isRemainNullLine ) {
						fileWritter.write("\n");
					}
					continue;
				}
				fileWritter.write(text);
				fileWritter.write("\n");
			}
			fileWritter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    	
    }
	
	public static void writeStringToFile(String fname,String text,boolean isAppend) {
		if(text==null) {
			return;
		}
		File file = new File(fname);
    	try {
            //如果文件不存在，则创建新的文件
            if(!file.exists()){
                file.createNewFile();
            }
            //打开一个写文件器，构造函数中的第二个参数true表示以追加形式写文件
			FileWriter fileWritter = new FileWriter(fname,isAppend);
			fileWritter.write(text);
			fileWritter.write("\n");
			fileWritter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    	
    }
	
	public static boolean isFileExists(String fileName) {
		if(fileName==null || fileName.trim().length()==0) {
			return false;
		}
		File file = new File(fileName);
		try {
            //如果文件不存在，则创建新的文件
            if(!file.exists()){
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
		return true;
	}
	
	public static List<String> readFileGetStringList(String fname) {
		   List<String> arr = new ArrayList<String>();
		   File file=new File(fname);
		   FileInputStream fis;
		   try {
			   fis = new FileInputStream(file);
			   BufferedReader br = new BufferedReader(new InputStreamReader(fis));
			   String line = null;
			   while ((line = br.readLine()) != null) {
				   arr.add(line);
			   }
			   br.close();
		   } catch (Exception e) {
			   // TODO Auto-generated catch block
			   e.printStackTrace();
		   }
		   return arr;
	}
	
	public static File[] getAllFileUnderPackage(String packageName) {
		File file = new File( packageName );
		File flist[] = file.listFiles();
		if (flist == null || flist.length == 0) {
			return null;
		}
		return flist;
	}
	
	public static String[] getAllFileNamesUnderPackage(String packageName) {
		File flist[] = getAllFileUnderPackage(packageName);
		String names[] = new String[flist.length];
		for(int i=0;i<flist.length;i++) {
			names[i] = flist[i].getName();
		}
		return names;
	}
	
	public static void deleteLastNonNullLine(String fileName) {
		List<String> lineList = readFileGetStringList(fileName);
		int end = 0;
		for(int i=lineList.size()-1;i>=0;i-- ) {
			String line = lineList.get(i);
			if(line!=null && line.trim().length()>0) {
				end = i;
				break;
			}
		}
		ArrayList<String> newLineList = new ArrayList<String>();
		for(int i=0;i<end;i++) {
			newLineList.add( lineList.get(i) );
		}
		 writeStringListToFile(fileName,newLineList,false,false);
	}
	
	public static String getNextAvailableFilename(String sFileNameStart, String sFileNameEnd) {
		for(int i = 0; i <= 1000; ++i) {
			String sFileName = sFileNameStart + i + sFileNameEnd;
	        File f = new File(sFileName);
	        if (!f.isFile()) {
	        	return sFileName;
	        }
	    }
		return "";
	}

	public static String s_ChopFileNameExtension(String sFilename) {
		if (sFilename != null && sFilename != "") {
			int iLastDotPos = sFilename.lastIndexOf(".");
	        if (iLastDotPos > 0) {
	           sFilename = sFilename.substring(0, iLastDotPos);
	        }
	    }
		return sFilename;
	}

}
