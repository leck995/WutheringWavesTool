package cn.tealc.wutheringwavestool;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.io.File;
import java.io.IOException;  
  
public class ImageProcessor {  
  
    public static BufferedImage convertToGrayscaleAndCrop(BufferedImage originalImage, int cropWidth) {  
        // 原始图片的宽度和高度  
        int originalWidth = originalImage.getWidth();  
        int originalHeight = originalImage.getHeight();  
  
        // 确保裁剪宽度不超过原始宽度  
        if (cropWidth > originalWidth) {  
            throw new IllegalArgumentException("Crop width cannot be greater than the original image width.");  
        }  
  
        // 创建一个新的BufferedImage来保存裁剪并转换为灰度的图片  
        // 注意：我们只需要原始图片的高度，因为我们是裁剪其宽度  
        BufferedImage croppedGrayImage = new BufferedImage(cropWidth, originalHeight, BufferedImage.TYPE_BYTE_GRAY);  
  
        // 使用Graphics2D来绘制裁剪后的部分到新的BufferedImage上  
        // 但因为我们实际上是要转换整个图片为灰度然后再裁剪，所以这里直接用ColorConvertOp转换，然后裁剪  
        // 但为了演示裁剪，我们还是先转换再裁剪  
        BufferedImage grayImage = convertToGrayscale(originalImage);  
  
        // 使用Graphics2D进行裁剪（这里其实是绘制裁剪后的部分）  
        Graphics2D g2d = croppedGrayImage.createGraphics();  
        // 注意：x坐标为原始图片宽度减去裁剪宽度，因为我们是从右侧开始裁剪  
        g2d.drawImage(grayImage, 0, 0, cropWidth, originalHeight,   
                      originalWidth - cropWidth, 0, originalWidth, originalHeight, null);  
        g2d.dispose();  
  
        return croppedGrayImage;  
    }  
  
    public static BufferedImage convertToGrayscale(BufferedImage image) {  
        // 使用ColorConvertOp将图片转换为灰度  
        ColorConvertOp op = new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null);
        return op.filter(image, null);  
    }
    private static BufferedImage invertColors(BufferedImage originalImage) {
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();

        BufferedImage inverseImage = new BufferedImage(width, height, originalImage.getType());

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int rgba = originalImage.getRGB(x, y);
                int alpha = (rgba >> 24) & 0xff;
                int red = 255 - ((rgba >> 16) & 0xff);
                int green = 255 - ((rgba >> 8) & 0xff);
                int blue = 255 - (rgba & 0xff);

                int newRgba = (alpha << 24) | (red << 16) | (green << 8) | blue;
                inverseImage.setRGB(x, y, newRgba);
            }
        }

        return inverseImage;
    }


    public static void main(String[] args) {  
        try {  
            File inputFile = new File("C:\\Users\\yiqiu\\Desktop\\3.png");
            BufferedImage originalImage = ImageIO.read(inputFile);
            BufferedImage image = invertColors(originalImage);

            // 保留右侧300像素  
            int cropWidth = 700;
            BufferedImage croppedGrayImage = convertToGrayscaleAndCrop(image, cropWidth);
  
            // 保存结果图片  
            File outputFile = new File("C:\\Users\\yiqiu\\Desktop\\32.png");
            ImageIO.write(croppedGrayImage, "jpg", outputFile);  
  
        } catch (IOException e) {  
            e.printStackTrace();  
        }  
    }  
}