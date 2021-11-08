package service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import setting.LogController;
import util.CommonUtil;
import util.ReadWriteUtil;
import util.SFTPUtil;
import util.Utility;

public class main {
	static {
		LogController.createLogFile();
	}
	public static void main(String[] args){
		
		LogController.writeMessage(LogController.DEBUG, "Start SFTP Download program");
		LogController.writeMessage(LogController.DEBUG, "Start time: " + new Timestamp(new Date().getTime()));
		
		SFTPUtil sftp = null;
		File dir = null;
		String fileName = "";
		String fileNameWithoutExt = "";
		List<String> parts = null;
		String HKbranchCode = "";
		String refNum = "";
		boolean isPDF = false;
		List<String> processedFiles = null;
		List<String> processedRefNum = null;
		List<String> exceptionRefNum = null;
		
		String ppk_path = Utility.getProperty("private_key_path");
        sftp = new SFTPUtil(Utility.getProperty("user_name"), Utility.getProperty("host"), 22 ,ppk_path);  
        
		try{
			SFTPUtil.SFTPDownload(sftp);
			
	     	dir=new File(Utility.getProperty("pending_path"));
	     	
	     	for (File file: dir.listFiles()) {

	     		fileName = file.getName();
	     		fileNameWithoutExt = FilenameUtils.removeExtension(fileName);
	     		parts = Arrays.asList(fileNameWithoutExt.split("_"));
	     		
	     		//LogController.writeMessage(LogController.DEBUG, "parts0: " + parts.get(0) + "parts1: " + parts.get(1) + "length: " + parts.size());
	     		
	     		isPDF = (parts.size() == 4) ? true : false;
	     		
	     		exceptionRefNum = new ReadWriteUtil().readExceptionRefNum();
	     		processedRefNum = new ReadWriteUtil().readProcessedRefNum();

	     		if (isPDF && !exceptionRefNum.contains(refNum)) {
	     			
	     			HKbranchCode = parts.get(2);
	     			refNum = parts.get(0);
	     			String target = Utility.getProperty("backup_path") + HKbranchCode + "\\WMCAPP\\" + refNum + "\\" + fileName;
		     		File refFolder = new File(Utility.getProperty("backup_path") + HKbranchCode + "\\WMCAPP\\" + refNum + "\\");	
	     			
	     			if (!refFolder.isDirectory()) {
	     				refFolder.mkdir();
		     		}
	     			if (processedRefNum.contains(refNum)) {
	     				String contentToAppend = fileName + "\r\n";
	     				new ReadWriteUtil(contentToAppend).writeException();
	     				LogController.writeMessage(LogController.ERROR, "Exception occurs: " + fileName);
	     			}
	     			else {
		     			FileUtils.copyFile(file, new File(target));  //copy pdf .zip
		     			LogController.writeMessage("Copy PDF .zip to: " + target);
		     			String contentToAppend = fileName + "\r\n";
		     			new ReadWriteUtil(contentToAppend).writeProcessed();
	     			}
	     		}
	     	}
	     	exceptionRefNum.clear();
	     	
	     	processedFiles = new ReadWriteUtil().readProcessed();
	     	exceptionRefNum = new ReadWriteUtil().readExceptionRefNum();
	     	for (String line: processedFiles) {
	     		deleteFile(Utility.getProperty("pending_path") + line);  //delete original pdf .zip
	     	}
	     	
	     	for (String line: processedFiles) {
	     		fileNameWithoutExt = FilenameUtils.removeExtension(line);
	     		parts = Arrays.asList(fileNameWithoutExt.split("_"));
	     		refNum = parts.get(0);
	     		HKbranchCode = parts.get(2);
	     		
	     		for (File file: dir.listFiles()) {
	     			fileName = file.getName();
		     		isPDF = (Arrays.asList(FilenameUtils.removeExtension(fileName).split("_")).size() == 4) ? true : false;
	     			
	     			if (!isPDF && fileName.matches("(.*)" + refNum + "(.*)") && !exceptionRefNum.contains(refNum)) {  //match refNum to processed pdf .zip and refNum is not in exception
	     				String target = Utility.getProperty("backup_path") + HKbranchCode + "\\WMCAPP\\" + refNum + "\\" + fileName;
	     				FileUtils.moveFile(file, new File(target));  //move images .zip
	     				LogController.writeMessage(LogController.DEBUG, "Move images .zip: " + fileName);
	     			}
	     		}
	     	}
	     	
	     	//Log out the files that have been deleted after executing the housekeep function
	     	Set<String> allFiles = listHousekeptFiles(Utility.getProperty("backup_path"));
	     	for (String s: allFiles) {
	     		LogController.writeMessage(LogController.DEBUG, "Housekept Files: " + s);
	     	} 	
	     	
		} catch(IOException e){
			LogController.writeExceptionMessage(LogController.ERROR, e);
		} catch(Exception e){
			LogController.writeExceptionMessage(LogController.ERROR, e);
		}
		
		LogController.writeMessage(LogController.DEBUG, "Finish SFTP Download program");
		LogController.writeMessage(LogController.DEBUG, "End time: " + new Timestamp(new Date().getTime()));
		
	}

	public static void deleteFile(String del) {
		File delFile=new File(del);
 		if (!delFile.delete()) {
 			if (delFile.exists()) {
 				for (int i = 0; i < 6; i++) {
 					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						LogController.writeExceptionMessage(LogController.ERROR, e);
					}
 					System.gc();
 					if (delFile.delete()) {
 						LogController.writeMessage(LogController.DEBUG, del + " Deletion Success");
 						break;
 					}
 				}
 			}
 		} else {
 			LogController.writeMessage(LogController.DEBUG, del + " Deletion Success");
 		}
	}
	
	public static Set<String> listHousekeptFiles(String dir) throws IOException {
		Set<String> fileList = new HashSet<>();
		Files.walkFileTree(Paths.get(dir), new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				if (!Files.isDirectory(file)) {
					String fileExtension = FilenameUtils.getExtension(file.getFileName().toString());
					long diff = (new Date().getTime() - attrs.lastModifiedTime().toMillis()) / (24 * 60 * 60 * 1000);
					if (diff >= Long.parseLong(Utility.getProperty("housekeep_interval")) && "zip".equals(fileExtension)) {
						fileList.add(file.getFileName().toString());
						LogController.writeMessage(LogController.DEBUG, "Start Housekeeping File: " + file.getFileName().toString());
						Files.delete(file);
					}
				}
				return FileVisitResult.CONTINUE;
			}
		});
		return fileList;
	}
	
}
