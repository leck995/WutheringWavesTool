package cn.tealc.wutheringwavestool.util;

import cn.tealc.wutheringwavestool.Config;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-08-02 00:23
 */
public class LocalResourcesManager {
    private static final Logger LOG= LoggerFactory.getLogger(LocalResourcesManager.class);
    public static final String BUFFER_DIR_TEMPLATE="assets/buffer/%s";
    public static final String HOME_ICON_DIR_TEMPLATE="assets/image/icon/%s.%s";
    public static final String HOME_ROLE_DIR_TEMPLATE="assets/image/role/%s.%s";
    public static final String HOME_ICON_DIR_TEMPLATE_2="assets/image/icon/%s";
    public static final String HOME_ROLE_DIR_TEMPLATE_2="assets/image/role/%s";
    static {
        File dir=new File("assets/buffer/");
        if (!dir.exists()) {
            boolean buffer = dir.mkdirs();
            if (!buffer){
                LOG.error("无法创建缓存目录");
            }else {
                LOG.info("成功创建缓存目录");
            }
        }
        File dir2=new File("assets/image/icon");
        if (!dir2.exists()) {
            boolean buffer = dir2.mkdirs();
            if (!buffer){
                LOG.error("无法创建ICON目录");
            }else {
                LOG.info("成功创建ICON目录");
            }
        }

        File dir3=new File("assets/image/role");
        if (!dir3.exists()) {
            boolean buffer = dir3.mkdirs();
            if (!buffer){
                LOG.error("无法创建Role目录");
            }else {
                LOG.info("成功创建Role目录");
            }
        }
    }
    public static Image imageBuffer(String url){
        File file=new File(String.format(BUFFER_DIR_TEMPLATE,getName(url)));
        if (file.exists()){ //有缓存，获取
            LOG.debug("{}有缓存，获取",file.getName());
            return new Image(file.toURI().toString(),true);
        }else { //无缓存，获取并保存
            LOG.debug("{}无缓存，获取并保存",file);
            Image image=new Image(url,true);
            saveImage(image,file);
            return image;
        }
    }

    public static Image imageBuffer(String url,double width,double height,boolean preserveRatio,boolean smooth){
        File file=new File(String.format(BUFFER_DIR_TEMPLATE,getName(url)));
        if (file.exists()){ //有缓存，获取
            LOG.debug("{}有缓存，获取",file.getName());
            return new Image(file.toURI().toString(),width,height,preserveRatio,smooth);
        }else { //无缓存，获取并保存
            LOG.debug("{}无缓存，获取并保存",file);
            Image image=new Image(url,width,height,preserveRatio,smooth,true);
            saveImage(image,file);
            return image;
        }
    }



    public static String addHomeIcon(String roleName,String url){
        File file=new File(String.format(HOME_ICON_DIR_TEMPLATE,roleName,getSuffix(url)));
        if (!file.exists()){ //无缓存，获取并保存
            LOG.debug("{}主页图标不存在，保存",file);
            Image image=new Image(url,true);
            saveImage(image,file);

        }
        return file.getName();
    }

    public static String addHomeRole(String roleName,String url){
        File file=new File(String.format(HOME_ROLE_DIR_TEMPLATE,roleName,getSuffix(url)));
        if (!file.exists()){ //无缓存，获取并保存
            LOG.debug("{}主页人物背景不存在，保存",file);
            Image image=new Image(url,true);
            saveImage(image,file);
        }
        return file.getName();
    }

    public static File homeIcon(){
        return new File(String.format(HOME_ICON_DIR_TEMPLATE_2, Config.setting.getHomeViewIcon()));
    }
    public static File homeRole(){
        return new File(String.format(HOME_ROLE_DIR_TEMPLATE_2, Config.setting.getHomeViewRole()));
    }



    private static String getName(String url){
        int index=url.lastIndexOf("/");
        return url.substring(index+1);
    }
    private static String getSuffix(String url){
        int index=url.lastIndexOf(".");
        return url.substring(index+1);
    }

    private static void saveImage(Image image, File file){
        Thread.startVirtualThread(()->{
            try {
                while (true){
                    if (image.getProgress() == 1){
                        break;
                    }else {
                        Thread.sleep(100);
                    }
                }
                BufferedImage read = SwingFXUtils.fromFXImage(image,null);
                ImageIO.write(read, getSuffix(file.getName()), file);
            } catch (IOException e) {
                LOG.error("保存图片缓存出错",e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }
}