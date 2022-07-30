/*
 * Copyright 2022 Pnoker All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.center.data.config;

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Elasticsearch Configuration
 *
 * @author pnoker
 */
@Configuration
@ConditionalOnProperty(name = "data.point.sava.elasticsearch.enable", havingValue = "true")
public class ElasticsearchConfig {

    @Value("${data.point.sava.elasticsearch.host}")
    private String host;
    @Value("${data.point.sava.elasticsearch.port}")
    private Integer port;

    @Bean
    public ElasticsearchClient elasticsearchClient() {
        ElasticsearchTransport transport = getElasticsearchTransport();

        // And create the API client
        return new ElasticsearchClient(transport);
    }

    @Bean
    public ElasticsearchAsyncClient elasticsearchAsyncClient() {
        // Create the low-level client
        ElasticsearchTransport transport = getElasticsearchTransport();

        // And create the API client
        return new ElasticsearchAsyncClient(transport);
    }

    private ElasticsearchTransport getElasticsearchTransport() {
        // Create the low-level client
        RestClient restClient = RestClient.builder(new HttpHost(host, port)).build();

        // Create the transport with a Jackson mapper
        return new RestClientTransport(restClient, new JacksonJsonpMapper());
    }

    private ElasticsearchTransport getElasticsearchTransportWithAuth() {
        // Create the credentials provider
        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials("elastic", "password"));

        // Create the low-level client
        RestClient restClient = RestClient.builder(new HttpHost(host, port))
                .setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider))
                .build();

        // Create the transport with a Jackson mapper
        return new RestClientTransport(restClient, new JacksonJsonpMapper());
    }
}