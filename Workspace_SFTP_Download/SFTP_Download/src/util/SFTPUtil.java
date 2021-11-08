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
import java.util.List;
import java.util.Properties;  
import java.util.Vector;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;  
//import org.slf4j.Logger;  
//import org.slf4j.LoggerFactory;  
  
import com.jcraft.jsch.Channel;  
import com.jcraft.jsch.ChannelSftp;  
import com.jcraft.jsch.JSch;  
import com.jcraft.jsch.JSchException;  
import com.jcraft.jsch.Session;  
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.ChannelSftp.LsEntry;

import setting.LogController; 


public class SFTPUtil {
  //  private transient Logger log = LoggerFactory.getLogger(this.getClass());  
    
    private ChannelSftp sftp;  
    private Session session;  
    private Channel channel;
    private String username; 
    private String password;  
    private String privateKey;  
    private String host;  
    private int port;  
   
    public SFTPUtil(String username, String password, String host, int port) {  
        this.username = username;  
        this.password = password;  
        this.host = host;  
        this.port = port;  
    } 
    
    public SFTPUtil(String username, String host, int port, String privateKey) {  
        this.username = username;  
        this.host = host;  
        this.port = port;  
        this.privateKey = privateKey;  
    }  
    
    public SFTPUtil(){}  

    public void login(){  
        try {  
            JSch jsch = new JSch();  
            if (privateKey != null) {  
                jsch.addIdentity(privateKey);  
            }  
            session = jsch.getSession(username, host, port);  
           
            if (password != null) {  
                session.setPassword(password);    
            }  
            Properties config = new Properties();  
            config.put("StrictHostKeyChecking", "no");  
                
            session.setConfig(config);  
            session.connect();  
              
            channel = session.openChannel("sftp");  
            channel.connect();  
    
            sftp = (ChannelSftp) channel;  
        } catch (JSchException e) {  
            e.printStackTrace();
        }  
    }    
    
    public void logout(){  
    	
        if (sftp != null) {  
            if (sftp.isConnected()) {  
                sftp.exit();
                LogController.writeMessage(LogController.DEBUG, "SFTP channel exited.");
            }  
        }  
        if (session != null) {  
            if (session.isConnected()) {  
                session.disconnect();  
                LogController.writeMessage(LogController.DEBUG, "SFTP session disconnected.");
            }  
        }  
        if (channel != null) {
        	if (channel.isConnected()) {  
                channel.disconnect();  
                LogController.writeMessage(LogController.DEBUG, "SFTP channel disconnected.");
            }  
        }
    }  

    public void upload(String basePath,String directory, String sftpFileName, InputStream input) throws SftpException{  
        try {   
            sftp.cd(basePath);
            sftp.cd(directory);  
        } catch (SftpException e) { 
            String [] dirs=directory.split("/");
            String tempPath=basePath;
            for(String dir:dirs){
            	if(null== dir || "".equals(dir)) continue;
            	tempPath+="/"+dir;
            	try{ 
            		sftp.cd(tempPath);
            	}catch(SftpException ex){
            		sftp.mkdir(tempPath);
            		sftp.cd(tempPath);
            	}
            }
        }  
        sftp.put(input, sftpFileName);
    } 
 
    /** 
     * 
     * @param directory remote
     * @param downloadFile 
     * @param saveFile local path
     */    
    public void download(String directory, String downloadFile, String saveFile) throws SftpException, FileNotFoundException{  
        if (directory != null && !"".equals(directory)) {  
            sftp.cd(directory);  
        }  
        File file = new File(saveFile);  
        sftp.get(downloadFile, new FileOutputStream(file));  
    }  
    
    /**  
     *  
     * @param directory remote 
     * @param downloadFile 
     * @return byte[] 
     */  
    public byte[] download(String directory, String downloadFile) throws SftpException, IOException{  
        if (directory != null && !"".equals(directory)) {  
            sftp.cd(directory);  
        }  
        InputStream is = sftp.get(downloadFile);  
          
        byte[] fileData = IOUtils.toByteArray(is);  
          
        return fileData;  
    }  
    
    /** 
     * Delete
     * @param directory 
     * @param deleteFile 
     */  
    public void delete(String directory, String deleteFile) throws SftpException{  
        sftp.cd(directory);  
        sftp.rm(deleteFile);  
    }  
    
    /** 
     * list
     * @param directory 
     * @param sftp 
     */  
    public Vector<?> listFiles(String directory) throws SftpException {  
    	sftp.cd(directory);
        return sftp.ls("*");  
    }  
    
    public int getCount(String directory) throws SftpException {
		sftp.cd(directory);
		Vector<?> files = sftp.ls("*");
	    return files.size();
    }
      
	public static void SFTPDownload(SFTPUtil sftp) throws Exception {
		try{
			List<String> downloadedList = new ReadWriteUtil().readDownloaded();

	     	sftp.login();  
	     	
	     	Vector<?> filesList = sftp.listFiles(Utility.getProperty("target_path"));
	     	
	     	for (int i = 0; i < sftp.getCount(Utility.getProperty("target_path")); i++) {
	     		
	     		LsEntry entry = (LsEntry) filesList.get(i);
	     		String fileName = entry.getFilename();
	     		String fileExtension = FilenameUtils.getExtension(fileName);
	     		
	     		LogController.writeMessage(LogController.DEBUG, "Iterate the file: " + fileName);
	     		//LogController.writeMessage("Creation time: " + new Timestamp(new Date(entry.getAttrs().getMTime()*1000L).getTime()));
	     		if (!downloadedList.contains(fileName) && "zip".equals(fileExtension)) {
	     			sftp.download(Utility.getProperty("target_path"), fileName, Utility.getProperty("pending_path") + fileName);
		     		String contentToAppend = fileName + "\r\n";
		     		new ReadWriteUtil(contentToAppend).writeDownloaded();
	     		}	
	     	}
		}catch(IOException e){
			LogController.writeExceptionMessage(LogController.ERROR, e);
		}catch(SftpException e){
			LogController.writeExceptionMessage(LogController.ERROR, e);
		}
		finally{
			sftp.logout();
		}
	}
}
