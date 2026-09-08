package com.achyut.operation.search;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "yardflow-search-v1", createIndex = false)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SearchDocument {
    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String type;

    @Field(type = FieldType.Long)
    private Long entityId;

    @Field(type = FieldType.Keyword)
    private String reference;

    @Field(type = FieldType.Text)
    private String title;

    @Field(type = FieldType.Text)
    private String subtitle;

    @Field(type = FieldType.Keyword)
    private String status;

    @Field(type = FieldType.Text)
    private String searchText;
}
