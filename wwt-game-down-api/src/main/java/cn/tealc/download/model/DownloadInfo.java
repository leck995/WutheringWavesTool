package cn.tealc.download.model;

import java.util.List;

/**
 * 单个文件的下载描述，由调用方将服务器清单项转换为该对象后交给下载引擎。
 *
 * @param url       文件的完整或相对下载 URL（baseUrl 拼接前的相对路径或完整 URL）
 * @param destPath  相对目标路径（相对保存根目录）
 * @param fileSize  文件字节数
 * @param md5       整文件 MD5，为空则跳过整文件校验
 * @param basePath  来源子目录（fromFolder），可为空
 * @param chunkInfoList 服务器预定义的分块（start/end/md5），非空则走分块下载，否则整文件下载
 */
public record DownloadInfo(
        String url,
        String destPath,
        long fileSize,
        String md5,
        String basePath,
        List<ChunkInfo> chunkInfoList) {

    public DownloadInfo {
        chunkInfoList = chunkInfoList == null ? List.of() : List.copyOf(chunkInfoList);
    }

    public static DownloadInfo of(String url, String destPath, long fileSize, String md5) {
        return new DownloadInfo(url, destPath, fileSize, md5, null, List.of());
    }
}