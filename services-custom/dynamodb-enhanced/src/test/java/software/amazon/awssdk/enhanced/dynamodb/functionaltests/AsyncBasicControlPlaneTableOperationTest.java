/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.enhanced.dynamodb.functionaltests;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primaryPartitionKey;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primarySortKey;

import java.util.Objects;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.internal.client.DefaultDynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.CreateTableEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.DescribeTableEnhancedResponse;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;

public class AsyncBasicControlPlaneTableOperationTest extends LocalDynamoDbAsyncTestBase {
    private static final TableSchema<Record> TABLE_SCHEMA =
            StaticTableSchema.builder(Record.class)
                    .newItemSupplier(Record::new)
                    .addAttribute(String.class, a -> a.name("id")
                            .getter(Record::getId)
                            .setter(Record::setId)
                            .tags(primaryPartitionKey()))
                    .addAttribute(Integer.class, a -> a.name("sort")
                            .getter(Record::getSort)
                            .setter(Record::setSort)
                            .tags(primarySortKey()))
                    .build();

    private final String tableName = getConcreteTableName("table-name");
    private final DynamoDbEnhancedAsyncClient enhancedAsyncClient = DefaultDynamoDbEnhancedAsyncClient.builder()
            .dynamoDbClient(getDynamoDbAsyncClient())
            .build();

    private final DynamoDbAsyncTable<AsyncBasicControlPlaneTableOperationTest.Record> asyncMappedTable = enhancedAsyncClient.table(tableName, TABLE_SCHEMA);

    @BeforeEach
    public void createTable() {
        asyncMappedTable.createTable(
            CreateTableEnhancedRequest.builder()
                                      .provisionedThroughput(getDefaultProvisionedThroughput())
                                      .build()).join();
        getDynamoDbAsyncClient().waiter().waitUntilTableExists(b -> b.tableName(tableName)).join();
    }

    @AfterEach
    public void deleteTable() {
        getDynamoDbAsyncClient().deleteTable(DeleteTableRequest.builder()
                                                               .tableName(tableName)
                                                               .build())
                                .join();

        getDynamoDbAsyncClient().waiter().waitUntilTableNotExists(b -> b.tableName(tableName)).join();
    }

    @Test
    public void describeTable() {
        DescribeTableEnhancedResponse describeTableEnhancedResponse = asyncMappedTable.describeTable().join();
        assertThat(describeTableEnhancedResponse.table()).isNotNull();
        assertThat(describeTableEnhancedResponse.table().tableName()).isEqualTo(tableName);

    }

    private static class Record {
        private String id;
        private Integer sort;

        private String getId() {
            return id;
        }

        private Record setId(String id) {
            this.id = id;
            return this;
        }

        private Integer getSort() {
            return sort;
        }

        private Record setSort(Integer sort) {
            this.sort = sort;
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Record record = (Record) o;
            return Objects.equals(id, record.id) &&
                   Objects.equals(sort, record.sort);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, sort);
        }
    }
}
