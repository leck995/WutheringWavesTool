package cn.tealc.wutheringwavestool.thread;

import com.jhlabs.image.ContrastFilter;
import com.jhlabs.image.GaussianFilter;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import net.coobird.thumbnailator.Thumbnails;


import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * @program: AsmrPlayer-web
 * @description: 处理传递过来的Image,进行高斯模糊和对比度调整
 * @author: Leck
 * @create: 2024-01-29 16:16
 */
public class MainBackgroundTask extends Task<Background> {
    private final GaussianFilter gaussianFilter;
    private final ContrastFilter contrastFilter;



    public MainBackgroundTask() {
        gaussianFilter = new GaussianFilter(25);
        contrastFilter = new ContrastFilter();
        contrastFilter.setBrightness(0.45f);
    }

    @Override
    protected Background call() throws Exception {

        File roleIVFile=new File("assets/image/icon.png");
        Image image=new Image(roleIVFile.toURI().toString(),100,100,false,false);

        BufferedImage bufferedImage = Thumbnails
                .of(SwingFXUtils.fromFXImage(image,null))
                .sourceRegion(40,40,80,80)
                .width(100)
                .asBufferedImage();


        BufferedImage filter = gaussianFilter.filter(bufferedImage, null);
        filter=contrastFilter.filter(filter,null);
        javafx.scene.layout.BackgroundImage backgroundImage=new javafx.scene.layout.BackgroundImage(
                SwingFXUtils.toFXImage(filter,null),
                BackgroundRepeat.NO_REPEAT,
                BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER,
                new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO,true,true,true,true));
        return new Background(backgroundImage);
    }
}