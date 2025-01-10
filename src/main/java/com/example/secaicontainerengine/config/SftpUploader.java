package com.example.secaicontainerengine.config;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileWriter;

@Component
@Slf4j
public class SftpUploader {

    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final String flag;

    public SftpUploader(
            @Value("${sftp.host}") String host,
            @Value("${sftp.port}") int port,
            @Value("${sftp.username}") String username,
            @Value("${sftp.password}") String password,
            @Value("${sftp.flag}") String flag) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.flag = flag;
    }

    public void uploadDirectory(String localDir, String remoteDir) throws Exception {
        JSch jsch = new JSch();
        Session session = jsch.getSession(username, host, port);
        session.setPassword(password);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();

        ChannelSftp sftpChannel = (ChannelSftp) session.openChannel("sftp");
        sftpChannel.connect();

        try {
            //上传模型数据
            uploadDirectoryRecursive(sftpChannel, new File(localDir), remoteDir);

            //创建标志文件并写入到解压目录中
            File parentDir = new File(localDir);
            File[] subDirs = parentDir.listFiles(File::isDirectory);
            File flagFile = createFlagFile(localDir + '/' + subDirs[0].getName(), flag);
            //上传完成的标志文件
            uploadFile(localDir + '/' + subDirs[0].getName() + '/' + flag, remoteDir + '/' + subDirs[0].getName());

            //上传完毕后，删除本地目录
            deleteLocalDirectory(new File(localDir));
        }catch (Exception e){
            log.error("上传失败，错误信息：{}", e.getMessage());
        } finally {
            sftpChannel.disconnect();
            session.disconnect();
        }
    }

    private void uploadDirectoryRecursive(ChannelSftp sftp, File localFile, String remoteDir) throws Exception {
        if (!localFile.exists()) {
            throw new IllegalArgumentException("本地目录或文件不存在: " + localFile.getAbsolutePath());
        }

        if (localFile.isDirectory()) {
            try {
                sftp.mkdir(remoteDir);
            } catch (Exception ignored) {
                // 如果目录已存在，忽略异常
            }

            for (File file : localFile.listFiles()) {
                String remotePath = remoteDir + "/" + file.getName();
                uploadDirectoryRecursive(sftp, file, remotePath);
            }
        } else {
            sftp.put(localFile.getAbsolutePath(), remoteDir);
        }
    }

    private void deleteLocalDirectory(File directory) {
        if (directory.isDirectory()) {
            // 递归删除子文件和子目录
            for (File file : directory.listFiles()) {
                deleteLocalDirectory(file);
            }
        }
        // 删除文件或空目录
        if (directory.delete()) {
            log.info("已删除: {}", directory.getAbsolutePath());
        } else {
            log.warn("删除失败: {}", directory.getAbsolutePath());
        }
    }

    // 上传单个文件
    public void uploadFile(String localFilePath, String remoteFilePath) throws Exception {
        JSch jsch = new JSch();
        Session session = jsch.getSession(username, host, port);
        session.setPassword(password);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();

        ChannelSftp sftpChannel = (ChannelSftp) session.openChannel("sftp");
        sftpChannel.connect();

        try {
            sftpChannel.put(localFilePath, remoteFilePath);
        } finally {
            sftpChannel.disconnect();
            session.disconnect();
        }
    }

    // 创建标志文件
    private File createFlagFile(String localDir, String flagFileName) throws Exception {
        File flagFile = new File(localDir + "/" + flagFileName);
        try (FileWriter writer = new FileWriter(flagFile)) {
            writer.write("Upload completed at: " + System.currentTimeMillis());
        }
        return flagFile;
    }

}
