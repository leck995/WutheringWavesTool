package cn.tealc.wutheringwavestool.thread;

import cn.tealc.teafx.utils.colorThief.ColorMap;
import cn.tealc.teafx.utils.colorThief.ColorThief;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.scene.image.Image;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.net.URL;
import java.util.List;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-08-01 19:17
 */
public class ImgColorBgTask extends Task<Background> {

    private String url;

    public ImgColorBgTask(String url) {
        this.url = url;
    }

    @Override
    protected Background call() throws Exception {
        BufferedImage image = ImageIO.read(URI.create(url).toURL());
        ColorMap colorMap = ColorThief.getColorMap(image, 3,10,true);
        List<Color> colors = colorMap.getColors();
        LinearGradient linearGradient = new LinearGradient(
                0.0, 0.0, 1.0, 0.0, true, CycleMethod.NO_CYCLE,
                new Stop(0.0, colors.get(0)),
                new Stop(1.0, colors.get(1)));
        return new Background(new BackgroundFill(linearGradient, new CornerRadii(5.0), null));
    }
}