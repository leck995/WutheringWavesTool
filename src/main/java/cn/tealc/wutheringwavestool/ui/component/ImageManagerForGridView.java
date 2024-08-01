package cn.tealc.wutheringwavestool.ui.component;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-07-29 22:52
 */
public class ImageManagerForGridView {
    private final double width;
    private final double height;
    public ImageManagerForGridView(double width, double height) {
        this.width = width;
        this.height = height;
    }

    private final Map<String, Image> imageMap=new LinkedHashMap<>();
    public void setImage(String url, ImageView imageView){
        Image pair = imageMap.get(url);

        if (pair == null){//不存在缓存，虚拟线程加载图片
            System.out.println(url);
            Image image1 = new Image(url,width,height,true,true,true);
            imageMap.put(url,image1);
            imageView.setImage(image1);
        }else { //存在缓存
            imageView.setImage(pair);
        }
        //限制最大容量为200
        if (imageMap.size() > 200){
            imageMap.remove(imageMap.keySet().iterator().next());
        }
    }


    public void clear(){
        imageMap.clear();
    }
}