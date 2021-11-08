package util;

import java.io.ByteArrayInputStream;  
import java.io.File;  
import java.io.FileInputStream;  
import java.io.FileNotFoundException;  
import java.io.FileOutputStream;  
import java.io.IOException;  
import java.io.InputStream;  
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;  
import java.util.Vector;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;  

import setting.LogController; 


public class ReadWriteUtil {
    
    private String contentToAppend;
   
    public ReadWriteUtil(String contentToAppend) {  
        this.contentToAppend = contentToAppend;  
    } 
    
    public ReadWriteUtil(){}  

    public void writeProcessed() throws IOException {
    	Files.write(Paths.get(Utility.getProperty("processed_path")), contentToAppend.getBytes(), StandardOpenOption.APPEND);
    }
    
    public void writeDownloaded() throws IOException {
    	Files.write(Paths.get(Utility.getProperty("downloaded_path")), contentToAppend.getBytes(), StandardOpenOption.APPEND);
    }
    
    public void writeException() throws IOException {
    	Files.write(Paths.get(Utility.getProperty("exception_path")), contentToAppend.getBytes(), StandardOpenOption.APPEND);
    }
    
    public List<String> readProcessed() throws IOException {
    	return Files.readAllLines(Paths.get(Utility.getProperty("processed_path")));
    }
    
    public List<String> readProcessedRefNum() throws IOException {
    	List<String> ls = Files.readAllLines(Paths.get(Utility.getProperty("processed_path")));
    	List<String> result = new ArrayList<>();
    	for (String element: ls) {
    		String fileNameWithoutExt = FilenameUtils.removeExtension(element);
    		List<String> parts = Arrays.asList(fileNameWithoutExt.split("_"));
    		result.add(parts.get(0));
    	}
    	return result;
    }
    
    public List<String> readDownloaded() throws IOException {
    	return Files.readAllLines(Paths.get(Utility.getProperty("downloaded_path")));
    }
    
    public List<String> readException() throws IOException {
    	return Files.readAllLines(Paths.get(Utility.getProperty("exception_path")));
    }
    
    public List<String> readExceptionRefNum() throws IOException {
    	List<String> ls = Files.readAllLines(Paths.get(Utility.getProperty("exception_path")));
    	List<String> result = new ArrayList<>();
    	for (String element: ls) {
    		String fileNameWithoutExt = FilenameUtils.removeExtension(element);
    		List<String> parts = Arrays.asList(fileNameWithoutExt.split("_"));
    		result.add(parts.get(0));
    	}
    	return result;
    }
}
