package hei.school.sarisary.endpoint.rest.controller.health;

import hei.school.sarisary.PojaGenerated;
import hei.school.sarisary.file.BucketComponent;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static java.io.File.createTempFile;

@RestController
@AllArgsConstructor
@PojaGenerated
public class BlackAndWhite {
    BucketComponent bucketComponent;
    private static final String IMAGE_KEY_PREFIX = "images/";

    @PutMapping("/black-and-white/{id}")
    public ResponseEntity<String> toBlackAndWhite(@PathVariable String id, @RequestParam("file") MultipartFile file) {
        try {
            String fileSuffix = ".png";
            String filePrefix = UUID.randomUUID().toString();
            File tempFile = createTempFile(filePrefix, fileSuffix);
            file.transferTo(tempFile);

            String originalImageKey = IMAGE_KEY_PREFIX + id + "_original" + fileSuffix;
            String transformedImageKey = IMAGE_KEY_PREFIX + id + "_bw" + fileSuffix;

            bucketComponent.upload(tempFile, originalImageKey);

            BufferedImage bwImage = convertToBlackAndWhite(tempFile);

            File bwTempFile = createTempFile(UUID.randomUUID().toString(), fileSuffix);
            ImageIO.write(bwImage, "png", bwTempFile);

            bucketComponent.upload(bwTempFile, transformedImageKey);

            tempFile.delete();
            bwTempFile.delete();

            return ResponseEntity.ok().build();
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    @GetMapping("/black-and-white/{id}")
    public ResponseEntity<Map<String, String>> getBlackAndWhiteImageUrls(@PathVariable String id) {
        String originalImageKey = IMAGE_KEY_PREFIX + id + ".png";
        String transformedImageKey = IMAGE_KEY_PREFIX + id + "_bw.png";

        URL originalPresignedUrl = can_presign(originalImageKey);
        URL transformedPresignedUrl = can_presign(transformedImageKey);

        Map<String, String> urls = new HashMap<>();
        urls.put("original_url", originalPresignedUrl.toString());
        urls.put("transformed_url", transformedPresignedUrl.toString());

        return ResponseEntity.ok(urls);
    }
    private URL can_presign(String fileBucketKey) {
        return bucketComponent.presign(fileBucketKey, Duration.ofMinutes(2));
    }

    private BufferedImage convertToBlackAndWhite(File file) throws IOException {
        BufferedImage originalImage = ImageIO.read(file);
        BufferedImage bwImage = new BufferedImage(originalImage.getWidth(), originalImage.getHeight(), BufferedImage.TYPE_BYTE_BINARY);

        Graphics2D graphics = bwImage.createGraphics();
        graphics.drawImage(originalImage, 0, 0, null);
        graphics.dispose();

        return bwImage;
    }
}