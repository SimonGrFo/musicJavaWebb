package com.example.musicservice.service;

import com.example.musicservice.model.Track;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Service
public class LastFmService {

    private static final Logger logger = LoggerFactory.getLogger(LastFmService.class);
    private static final String TRACK_SEARCH_METHOD = "track.search";

    @Value("${lastfm.api.key}")
    private String apiKey;

    @Value("${lastfm.api.url}")
    private String lastFmUrl;

    @Value("${lastfm.api.format}")
    private String apiFormat;

    private final WebClient webClient;

    public LastFmService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl(lastFmUrl).build();
    }

    private String buildUrl(String method, Map<String, String> params) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(lastFmUrl)
                .queryParam("method", method)
                .queryParam("api_key", apiKey)
                .queryParam("format", apiFormat);

        params.forEach(builder::queryParam);

        return builder.toUriString();
    }

    public Mono<Track> searchTrack(String trackName) {
        Map<String, String> params = Map.of("track", trackName);

        String url = buildUrl(TRACK_SEARCH_METHOD, params);
        logger.debug("Constructed URL: {}", url);

        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> {
                    JSONObject trackObject = new JSONObject(response)
                            .getJSONObject("results")
                            .getJSONObject("trackmatches")
                            .getJSONArray("track")
                            .getJSONObject(0);

                    Track track = new Track();
                    track.setName(trackObject.getString("name"));
                    track.setArtist(trackObject.getString("artist"));
                    track.setUrl(trackObject.getString("url"));
                    return track;
                })
                .doOnSuccess(track -> logger.info("Found track: '{}' by '{}'", track.getName(), track.getArtist()))
                .doOnError(e -> logger.error("Error while searching for track: {}", e.getMessage()));
    }
}