package com.example.cardexchange.Service;

import com.example.cardexchange.Pojo.CardVo;
import com.example.cardexchange.Pojo.RecognitionResultVo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Service
public class RecognitionService {

    @Value("${ai.service.url:http://localhost:8001}")
    private String aiServiceUrl;

    @Autowired
    private CardService cardService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate;

    public RecognitionService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(25000);
        this.restTemplate = new RestTemplate(factory);
    }

    public Map<String, Object> checkHealth() {
        Map<String, Object> status = new LinkedHashMap<>();
        try {
            ResponseEntity<String> resp = restTemplate.getForEntity(aiServiceUrl + "/health", String.class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> body = objectMapper.readValue(resp.getBody(), Map.class);
                status.put("online", true);
                status.putAll(body);
                return status;
            }
        } catch (Exception ignored) {
            // Fall through to offline payload
        }
        status.put("online", false);
        status.put("message", "图像识别微服务暂未连接 (" + aiServiceUrl + ")");
        return status;
    }

    public RecognitionResultVo recognize(MultipartFile file, int topK) throws IOException {
        byte[] bytes = file.getBytes();
        String originalFilename = file.getOriginalFilename();
        final String safeFilename = (originalFilename == null || originalFilename.trim().isEmpty())
                ? "upload.jpg"
                : originalFilename;

        ByteArrayResource resource = new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return safeFilename;
            }
        };

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", resource);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        String url = aiServiceUrl + "/recognize?top_k=" + Math.max(1, Math.min(topK, 10)) + "&use_ocr=true";

        ResponseEntity<String> response;
        try {
            response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
        } catch (ResourceAccessException ex) {
            throw new IllegalStateException("图像识别服务暂未启动，请先运行 Python ai-service（8001 端口）", ex);
        } catch (RestClientResponseException ex) {
            String detail = extractErrorDetail(ex.getResponseBodyAsString());
            throw new IllegalArgumentException(detail != null ? detail : "识别服务返回错误：" + ex.getRawStatusCode(), ex);
        }

        JsonNode root = objectMapper.readTree(response.getBody());
        RecognitionResultVo vo = new RecognitionResultVo();
        vo.setRecognized(root.path("recognized").asBoolean(false));
        vo.setMessage(root.path("message").asText(""));
        vo.setFaceDetected(root.path("faceDetected").asBoolean(false));
        vo.setFaceConfidence(root.path("faceConfidence").asDouble(0.0));
        vo.setElapsedMs(root.path("elapsedMs").asLong(0L));

        if (root.has("faceBox") && root.get("faceBox").isArray()) {
            List<Double> box = new ArrayList<>();
            for (JsonNode n : root.get("faceBox")) {
                box.add(n.asDouble());
            }
            vo.setFaceBox(box);
        }

        if (root.has("ocrTexts") && root.get("ocrTexts").isArray()) {
            List<String> ocrTexts = new ArrayList<>();
            for (JsonNode n : root.get("ocrTexts")) {
                ocrTexts.add(n.asText());
            }
            vo.setOcrTexts(ocrTexts);
        }

        // Load catalog once and index by img_url and team+playerName
        List<CardVo> catalog = cardService.listCards(null, null, null, "price_desc");
        Map<String, CardVo> byImgUrl = new HashMap<>();
        Map<String, CardVo> byTeamPlayer = new HashMap<>();
        for (CardVo c : catalog) {
            if (c.getImg_url() != null) {
                byImgUrl.put(c.getImg_url().toUpperCase(Locale.ROOT), c);
            }
            if (c.getTeam() != null && c.getPlayerName() != null) {
                byTeamPlayer.put((c.getTeam() + ":" + c.getPlayerName()).toUpperCase(Locale.ROOT), c);
            }
        }

        List<RecognitionResultVo.MatchItem> matches = new ArrayList<>();
        if (root.has("matches") && root.get("matches").isArray()) {
            for (JsonNode m : root.get("matches")) {
                RecognitionResultVo.MatchItem item = new RecognitionResultVo.MatchItem();
                String imgUrl = m.path("imgUrl").asText("");
                String team = m.path("team").asText("");
                String playerName = m.path("playerName").asText("");

                item.setImgUrl(imgUrl);
                item.setTeam(team);
                item.setPlayerName(playerName);
                item.setScore(m.path("score").asDouble(0.0));
                item.setFaceScore(m.path("faceScore").asDouble(0.0));
                item.setOcrMatched(m.path("ocrMatched").asBoolean(false));
                item.setConfidencePercent(m.path("confidencePercent").asInt(0));

                List<String> reasons = new ArrayList<>();
                if (m.has("matchReasons") && m.get("matchReasons").isArray()) {
                    for (JsonNode r : m.get("matchReasons")) {
                        reasons.add(r.asText());
                    }
                }
                item.setMatchReasons(reasons);

                CardVo matchedCard = byImgUrl.get(imgUrl.toUpperCase(Locale.ROOT));
                if (matchedCard == null) {
                    matchedCard = byTeamPlayer.get((team + ":" + playerName).toUpperCase(Locale.ROOT));
                }
                item.setCard(matchedCard);
                matches.add(item);
            }
        }
        vo.setMatches(matches);
        return vo;
    }

    private String extractErrorDetail(String body) {
        if (body == null || body.trim().isEmpty()) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(body);
            if (node.has("detail")) {
                return node.get("detail").asText();
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
