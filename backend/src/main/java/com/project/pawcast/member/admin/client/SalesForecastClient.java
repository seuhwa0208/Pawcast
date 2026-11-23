package com.project.pawcast.member.admin.client;

import com.project.pawcast.common.exception.BusinessException;
import com.project.pawcast.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;
import java.util.Optional;

@Component
public class SalesForecastClient {

    private final RestClient restClient;
    private final String internalApiKey;

    public SalesForecastClient(
            RestClient.Builder builder,
            @Value("${ai.base-url:http://localhost:8000}") String baseUrl,
            @Value("${ai.internal-api-key:}") String internalApiKey) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.internalApiKey = internalApiKey;
    }

    public ForecastResponse retrain(LocalDate forecastDate) {
        try {
            ForecastResponse response = restClient.post()
                    .uri(uriBuilder -> uriBuilder.path("/api/sales/retrain")
                            .queryParamIfPresent("forecast_date", Optional.ofNullable(forecastDate))
                            .build())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Internal-Api-Key", internalApiKey)
                    .retrieve()
                    .body(ForecastResponse.class);
            if (response == null) {
                throw new BusinessException(ErrorCode.SALES_FORECAST_UNAVAILABLE);
            }
            return response;
        } catch (RestClientException exception) {
            throw new BusinessException(ErrorCode.SALES_FORECAST_UNAVAILABLE);
        }
    }

    /** AI가 DB에서 만든 CSV/XLSX를 Spring을 거쳐 브라우저로 전달한다. */
    public ExportFile export(LocalDate startDate, LocalDate endDate, String format, String section) {
        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/api/sales/export")
                            .queryParamIfPresent("start_date", Optional.ofNullable(startDate))
                            .queryParamIfPresent("end_date", Optional.ofNullable(endDate))
                            .queryParamIfPresent("format", Optional.ofNullable(format))
                            .queryParamIfPresent("section", Optional.ofNullable(section))
                            .build())
                    .header("Internal-Api-Key", internalApiKey)
                    .exchange((request, response) -> {
                        if (!response.getStatusCode().is2xxSuccessful()) {
                            throw new BusinessException(ErrorCode.SALES_FORECAST_UNAVAILABLE);
                        }
                        byte[] content = response.getBody() != null
                                ? response.getBody().readAllBytes()
                                : new byte[0];
                        String disposition = response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
                        String contentType = response.getHeaders().getContentType() == null
                                ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                                : response.getHeaders().getContentType().toString();
                        return new ExportFile(content, disposition, contentType);
                    });
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.SALES_FORECAST_UNAVAILABLE);
        }
    }

    public record ForecastResponse(
            Long predicted_sales,
            Double growth_rate,
            LocalDate forecast_date,
            String trained_at,
            Integer training_days,
            String insight) { }

    public record ExportFile(byte[] content, String disposition, String contentType) { }
}
