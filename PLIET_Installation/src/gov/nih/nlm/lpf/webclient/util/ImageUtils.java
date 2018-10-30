/*
 * /*
 * Informational Notice:
 * This software was developed under contract funded by the National Library of Medicine, which is part of the National Institutes of Health, 
 * an agency of the Department of Health and Human Services, United States Government.
 *
 * The license of this software is an open-source BSD license.  It allows use in both commercial and non-commercial products.
 *
 * The license does not supersede any applicable United States law.
 *
 * The license does not indemnify you from any claims brought by third parties whose proprietary rights may be infringed by your usage of this software.
 *
 * Government usage rights for this software are established by Federal law, which includes, but may not be limited to, Federal Acquisition Regulation 
 * (FAR) 48 C.F.R. Part52.227-14, Rights in Dataï¿½General.
 * The license for this software is intended to be expansive, rather than restrictive, in encouraging the use of this software in both commercial and 
 * non-commercial products.
 *
 * LICENSE:
 *
 * Government Usage Rights Notice:  The U.S. Government retains unlimited, royalty-free usage rights to this software, but not ownership,
 * as provided by Federal law.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 * -	Redistributions of source code must retain the above Government Usage Rights Notice, this list of conditions and the following disclaimer.
 *
 * -	Redistributions in binary form must reproduce the above Government Usage Rights Notice, this list of conditions and the following disclaimer 
 * in the documentation and/or other materials provided with the distribution.
 *
 * -	The names,trademarks, and service marks of the National Library of Medicine, the National Cancer Institute, the National Institutes 
 * of Health,  and the names of any of the software developers shall not be used to endorse or promote products derived from this software without 
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE U.S. GOVERNMENT AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE U.S. GOVERNMENT
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package gov.nih.nlm.lpf.webclient.util;

/**
 *
 * @author 
 */
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.File;

import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;

import java.util.HashMap;

import org.apache.log4j.Logger;

public class ImageUtils 
{
    private static Logger log = Logger.getLogger(ImageUtils.class);

    /**
     * Decode string to image
     * @param imageString The string to decode
     * @return decoded image
     */
    public static BufferedImage decodeToImage(String imageString)
    {

        BufferedImage image = null;
        byte[] imageByte;
        try 
        {
            Base64 decoder = new Base64();
            imageByte = decoder.decode(imageString.getBytes());

            ByteArrayInputStream bis = new ByteArrayInputStream(imageByte);
            image = ImageIO.read(bis);
            bis.close();
        }
        catch (Exception e) 
        {
            log.error("Could not decode Base64 string to BufferedImage" , e);
        }
        return image;
    }

    /**
     * Encode image to Base64  string
     * @param image The image to encode
     * @param type jpeg, bmp, ...
     * @return encoded string
     */
    public static HashMap getImageData(String  imageFile, String type)
    {
        HashMap imageData = new HashMap();
        try
        {
            BufferedImage image = ImageIO.read(new File(imageFile));
            int height = image.getHeight();
            int width = image.getWidth();
            imageData.put("height", new Integer(height));
            imageData.put("width", new Integer(width));
            System.out.println("Image height: " + height + ", width: " + width);
            
            // convert to Base64 string
            String encodedData = encodeImageToString(image,  type);
            imageData.put("base64data", encodedData);
            return imageData;
        }
        catch (IOException e)
        {
            log.error("Could not convert image" + imageFile + "  to Base64 string", e);
        }
        return null;
    }
    
     public static String encodeFileToString(String  imageFile, String type)
    {
        String imageString = null;
        try
        {
            BufferedImage image = ImageIO.read(new File(imageFile));
            System.out.println("Image height: " + image.getHeight() + ", width: " + image.getWidth());
            imageString = encodeImageToString( image,  type);
        } 
        catch (IOException e)
        {
            log.error("Could not convert image" + imageFile + "  to Base64 string", e);
        }
        return imageString;
    }

    public static String encodeImageToString(BufferedImage image, String type)
    {
        String imageString = null;
        try
        {
           ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ImageIO.write(image, type, bos); 
            byte[] imageBytes = bos.toByteArray();  
            int len = imageBytes.length;
            
            byte[] base64encoded = Base64.encodeBase64( imageBytes, false);  // Apache RFC2045 compliant encoder
            Base64  encoder = new Base64();
            imageString = new String(base64encoded, "UTF-8");
           // System.out.println("Base64 encoded data:" + imageString);
            bos.close(); 
            System.out.println("Initial image size " + len +", final size " + imageString.length());
        } 
        catch (IOException e)
        {
            log.error("Could not convert image to Base64 string", e);
        }
        return imageString;
    }

   public static void main (String args[]) throws IOException {
        // Test image to string and string to image start 
       BufferedImage img = ImageIO.read(new File("C:/tmp/plImages/testImage.gif"));
      
        BufferedImage newImg;
        String imgstr;
        imgstr = encodeImageToString(img, "gif");
        System.out.println(imgstr);
     //   newImg = decodeToImage(imgstr);
     //   ImageIO.write(newImg, "png", new File("files/img/CopyOfTestImage.png"));
        // Test image to string and string to image finish 
    } 
}
