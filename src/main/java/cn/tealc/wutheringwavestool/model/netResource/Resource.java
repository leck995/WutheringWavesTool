package cn.tealc.wutheringwavestool.model.netResource;

/**
 * @program: WutheringWavesToolResources
 * @description:
 * @author: Leck
 * @create: 2024-10-06 16:28
 */
public class Resource {
    private String name;
    private String filePath;
    private String aimPath;
    private String md5;


    public Resource(String name, String filePath, String aimPath, String md5) {
        this.name = name;
        this.filePath = filePath;
        this.aimPath = aimPath;
        this.md5 = md5;
    }

    public Resource() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getAimPath() {
        return aimPath;
    }

    public void setAimPath(String aimPath) {
        this.aimPath = aimPath;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }
}