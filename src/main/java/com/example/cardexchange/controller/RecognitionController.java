package com.example.cardexchange.controller;

import com.example.cardexchange.Pojo.RecognitionResultVo;
import com.example.cardexchange.ResponseMessage;
import com.example.cardexchange.Service.RecognitionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/recognize")
public class RecognitionController {

    private static final long MAX_FILE_BYTES = 10L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTS = new HashSet<>(
            Arrays.asList(".png", ".jpg", ".jpeg", ".webp", ".bmp")
    );

    @Autowired
    private RecognitionService recognitionService;

    @GetMapping("/health")
    public ResponseMessage<Map<String, Object>> health() {
        return ResponseMessage.success(recognitionService.checkHealth());
    }

    @PostMapping
    public ResponseMessage<RecognitionResultVo> recognize(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "topK", defaultValue = "3") int topK) {
        if (file == null || file.isEmpty()) {
            return ResponseMessage.badRequestString("请先选择或拖入一张图片");
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            return ResponseMessage.badRequestString("图片大小不能超过 10MB");
        }

        String contentType = file.getContentType();
        String filename = file.getOriginalFilename();
        boolean validMime = contentType != null && contentType.toLowerCase(Locale.ROOT).startsWith("image/");
        boolean validExt = false;
        if (filename != null && filename.contains(".")) {
            String ext = filename.substring(filename.lastIndexOf('.')).toLowerCase(Locale.ROOT);
            validExt = ALLOWED_EXTS.contains(ext);
        }
        if (!validMime && !validExt) {
            return ResponseMessage.badRequestString("仅支持 PNG、JPG、JPEG、WebP 图片格式");
        }

        try {
            RecognitionResultVo result = recognitionService.recognize(file, topK);
            return ResponseMessage.success(result);
        } catch (IllegalStateException ex) {
            return new ResponseMessage<>(HttpStatus.SERVICE_UNAVAILABLE.value(), ex.getMessage(), null);
        } catch (IllegalArgumentException ex) {
            return ResponseMessage.badRequestString(ex.getMessage());
        } catch (Exception ex) {
            return new ResponseMessage<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "识别失败：" + ex.getMessage(), null);
        }
    }
}
