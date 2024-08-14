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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * @description: 处理传递过来的Image,进行高斯模糊和对比度调整
 * @author: Leck
 * @create: 2024-01-29 16:16
 */
public class MainBackgroundTask extends Task<Background> {
    private static final Logger LOG= LoggerFactory.getLogger(MainBackgroundTask.class);
    private GaussianFilter gaussianFilter;
    private ContrastFilter contrastFilter;

    private Image image;

    public MainBackgroundTask(Image image) {
        this.image = image;
        gaussianFilter = new GaussianFilter(16);
        contrastFilter = new ContrastFilter();
        //contrastFilter.setBrightness(0.9f);
        contrastFilter.setContrast(1.1f);
    }

    @Override
    protected Background call(){
        BufferedImage bufferedImage = null;
        try {
            bufferedImage = Thumbnails
                    .of(SwingFXUtils.fromFXImage(image,null))
                    .size(128,72)
                    .asBufferedImage();
        } catch (IOException e) {
            LOG.error("Error", e);
        }

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