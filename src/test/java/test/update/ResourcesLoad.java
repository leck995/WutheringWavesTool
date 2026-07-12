package test.update;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuro.game.model.game.FileInfo;
import com.kuro.game.model.game.GameResourceList;
import com.kuro.game.model.launcher.item.CdnData;
import com.kuro.game.model.launcher.LauncherResource;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/**
 * @description:
 * @author: Leck
 * @create: 2025-02-10 16:00
 */
public class ResourcesLoad {
    @Test
    public void test() throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        LauncherResource launcherResource = mapper.readValue(new File("response.json"), LauncherResource.class);
        System.out.println(launcherResource.getPredownload().getVersion());


        GameResourceList resource = mapper.readValue(new File("resource.json"), GameResourceList.class);
        GameResourceList preGameResourceList = mapper.readValue(new File("resource2.json"), GameResourceList.class);

        long sum = resource.getResource().stream().mapToLong(FileInfo::getSize).sum();
        System.out.println(resource.getResource().size() + " size:" + sum);
        long sum2 = preGameResourceList.getResource().stream().mapToLong(FileInfo::getSize).sum();
        System.out.println(preGameResourceList.getResource().size() + " size:" + sum2);
        check(resource.getResource(), preGameResourceList.getResource());


        List<CdnData> cdnList = launcherResource.getUpdateData().getCdnList();
        cdnList.sort(Comparator.comparingInt(CdnData::getPing));
  /*      for (DownloadFile downloadFile : resource.getResource()) {
            String url = String.format("%s%s%s", cdnList.getFirst().getUrl(), launcherResource.getUpdateData().getResourcesBasePath(), downloadFile.getDest());
            System.out.println("下载地址："+url);
        }*/

    }


    public void check(List<FileInfo> localFiles, List<FileInfo> newFiles){
        // 假设 localFiles 和 networkFiles 已经被填充

        Map<String, FileInfo> localMap = new HashMap<>();
        for (FileInfo file : localFiles) {
            localMap.put(file.getDest(), file);
        }

        // 检查需要更新和添加的文件
        List<FileInfo> filesToUpdate = new ArrayList<>();
        List<FileInfo> filesToAdd = new ArrayList<>();
        List<FileInfo> filesToNoPut = new ArrayList<>();
        for (FileInfo newFile : newFiles) {
            FileInfo localFile = localMap.get(newFile.getDest());
            if (localFile != null) {
                // 文件存在于本地，检查是否需要更新
                if (!localFile.getMd5().equals(newFile.getMd5())) {
                    filesToUpdate.add(newFile);
                }else {
                    filesToNoPut.add(newFile);
                }
            } else {
                // 文件不存在于本地，添加到添加列表
                filesToAdd.add(newFile);
            }
        }

        // 检查需要删除的文件
        Map<String, FileInfo> newMap = new HashMap<>();
        for (FileInfo file : newFiles) {
            newMap.put(file.getDest(), file);
        }
        List<FileInfo> filesToDelete = new ArrayList<>();
        for (FileInfo localFile : localFiles) {
            FileInfo newFile = newMap.get(localFile.getDest());
            if (newFile == null) {
                filesToDelete.add(localFile);
            }
        }
        // 输出结果
        long updateSum = filesToUpdate.stream().mapToLong(FileInfo::getSize).sum();
        System.out.println("需要更新的文件: " + filesToUpdate.size() + "size:"+updateSum);
        long addSum = filesToAdd.stream().mapToLong(FileInfo::getSize).sum();
        System.out.println("需要添加的文件: " + filesToAdd.size() + "size:"+ addSum);
        long deleteSum = filesToDelete.stream().mapToLong(FileInfo::getSize).sum();
        System.out.println("需要删除的文件: " + filesToDelete.size() + "size:"+ deleteSum);

        long noPutSum = filesToNoPut.stream().mapToLong(FileInfo::getSize).sum();
        System.out.println("不修改文件: " + filesToNoPut.size() + "size:"+ noPutSum);
    }



    @Test
    public void test2() throws IOException {
        Path folderPath = Paths.get("C:\\Leck\\Game\\Wuthering Waves"); // 替换为您的文件夹路径
        LocalDate targetDate = LocalDate.of(2025, 2, 10);
        List<Path> modifiedFiles = new ArrayList<>();

        try {
            Files.walkFileTree(folderPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    // 获取文件的最后修改时间
                    LocalDate fileDate = attrs.lastModifiedTime().toInstant()
                            .atZone(ZoneId.systemDefault()).toLocalDate();

                    // 筛选出指定日期的文件
                    if (fileDate.isEqual(targetDate)) {
                        modifiedFiles.add(file);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 输出修改日期为 2025-02-10 的文件
        modifiedFiles.forEach(System.out::println);
    }
}