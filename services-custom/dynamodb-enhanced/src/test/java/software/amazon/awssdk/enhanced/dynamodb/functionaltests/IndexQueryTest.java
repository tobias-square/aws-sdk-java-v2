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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues.numberValue;
import static software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues.stringValue;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primaryPartitionKey;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primarySortKey;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.secondaryPartitionKey;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.secondarySortKey;
import static software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional.keyEqualTo;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.CreateTableEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.EnhancedGlobalSecondaryIndex;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;

public class IndexQueryTest extends LocalDynamoDbSyncTestBase {
    private static class Record {
        private String id;
        private Integer sort;
        private Integer value;
        private String gsiId;
        private Integer gsiSort;

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

        private Integer getValue() {
            return value;
        }

        private Record setValue(Integer value) {
            this.value = value;
            return this;
        }

        private String getGsiId() {
            return gsiId;
        }

        private Record setGsiId(String gsiId) {
            this.gsiId = gsiId;
            return this;
        }

        private Integer getGsiSort() {
            return gsiSort;
        }

        private Record setGsiSort(Integer gsiSort) {
            this.gsiSort = gsiSort;
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Record record = (Record) o;
            return Objects.equals(id, record.id) &&
                   Objects.equals(sort, record.sort) &&
                   Objects.equals(value, record.value) &&
                   Objects.equals(gsiId, record.gsiId) &&
                   Objects.equals(gsiSort, record.gsiSort);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, sort, value, gsiId, gsiSort);
        }
    }

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
                         .addAttribute(Integer.class, a -> a.name("value")
                                                            .getter(Record::getValue)
                                                            .setter(Record::setValue))
                         .addAttribute(String.class, a -> a.name("gsi_id")
                                                           .getter(Record::getGsiId)
                                                           .setter(Record::setGsiId)
                                                           .tags(secondaryPartitionKey("gsi_keys_only")))
                         .addAttribute(Integer.class, a -> a.name("gsi_sort")
                                                            .getter(Record::getGsiSort)
                                                            .setter(Record::setGsiSort)
                                                            .tags(secondarySortKey("gsi_keys_only")))
                         .build();

    private static final List<Record> RECORDS =
        IntStream.range(0, 10)
                 .mapToObj(i -> new Record()
                                      .setId("id-value")
                                      .setSort(i)
                                      .setValue(i)
                                      .setGsiId("gsi-id-value")
                                      .setGsiSort(i))
                 .collect(Collectors.toList());

    private static final List<Record> KEYS_ONLY_RECORDS =
        RECORDS.stream()
               .map(record -> new Record()
                                    .setId(record.id)
                                    .setSort(record.sort)
                                    .setGsiId(record.gsiId)
                                    .setGsiSort(record.gsiSort))
               .collect(Collectors.toList());

    private DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                                                                          .dynamoDbClient(getDynamoDbClient())
                                                                          .build();

    private DynamoDbTable<Record> mappedTable = enhancedClient.table(getConcreteTableName("table-name"), TABLE_SCHEMA);
    private DynamoDbIndex<Record> keysOnlyMappedIndex = mappedTable.index("gsi_keys_only");

    private void insertRecords() {
        RECORDS.forEach(record -> mappedTable.putItem(r -> r.item(record)));
    }

    @BeforeEach
    public void createTable() {
        mappedTable.createTable(
                CreateTableEnhancedRequest.builder()
                        .provisionedThroughput(getDefaultProvisionedThroughput())
                        .globalSecondaryIndices(
                                EnhancedGlobalSecondaryIndex.builder()
                                        .indexName("gsi_keys_only")
                                        .projection(p -> p.projectionType(ProjectionType.KEYS_ONLY))
                                        .provisionedThroughput(getDefaultProvisionedThroughput())
                                        .build())
                        .build());
    }

    @AfterEach
    public void deleteTable() {
        getDynamoDbClient().deleteTable(DeleteTableRequest.builder()
                                                          .tableName(getConcreteTableName("table-name"))
                                                          .build());
    }

    @Test
    public void queryAllRecordsDefaultSettings_usingShortcutForm() {
        insertRecords();

        Iterator<Page<Record>> results =
            keysOnlyMappedIndex.query(keyEqualTo(k -> k.partitionValue("gsi-id-value"))).iterator();

        assertThat(results.hasNext(), is(true));
        Page<Record> page = results.next();
        assertThat(results.hasNext(), is(false));

        assertThat(page.items(), is(KEYS_ONLY_RECORDS));
        assertThat(page.lastEvaluatedKey(), is(nullValue()));
    }

    @Test
    public void queryBetween() {
        insertRecords();
        Key fromKey = Key.builder().partitionValue("gsi-id-value").sortValue(3).build();
        Key toKey = Key.builder().partitionValue("gsi-id-value").sortValue(5).build();
        Iterator<Page<Record>> results =
            keysOnlyMappedIndex.query(r -> r.queryConditional(QueryConditional.sortBetween(fromKey, toKey))).iterator();

        assertThat(results.hasNext(), is(true));
        Page<Record> page = results.next();
        assertThat(results.hasNext(), is(false));

        assertThat(page.items(),
                   is(KEYS_ONLY_RECORDS.stream().filter(r -> r.sort >= 3 && r.sort <= 5).collect(Collectors.toList())));
        assertThat(page.lastEvaluatedKey(), is(nullValue()));
    }

    @Test
    public void queryLimit() {
        insertRecords();
        Iterator<Page<Record>> results =
            keysOnlyMappedIndex.query(QueryEnhancedRequest.builder()
                                                          .queryConditional(keyEqualTo(k -> k.partitionValue("gsi-id-value")))
                                                          .limit(5)
                                                          .build())
                               .iterator();

        assertThat(results.hasNext(), is(true));
        Page<Record> page1 = results.next();
        assertThat(results.hasNext(), is(true));
        Page<Record> page2 = results.next();
        assertThat(results.hasNext(), is(true));
        Page<Record> page3 = results.next();
        assertThat(results.hasNext(), is(false));

        Map<String, AttributeValue> expectedLastEvaluatedKey1 = new HashMap<>();
        expectedLastEvaluatedKey1.put("id", stringValue(KEYS_ONLY_RECORDS.get(4).getId()));
        expectedLastEvaluatedKey1.put("sort", numberValue(KEYS_ONLY_RECORDS.get(4).getSort()));
        expectedLastEvaluatedKey1.put("gsi_id", stringValue(KEYS_ONLY_RECORDS.get(4).getGsiId()));
        expectedLastEvaluatedKey1.put("gsi_sort", numberValue(KEYS_ONLY_RECORDS.get(4).getGsiSort()));
        Map<String, AttributeValue> expectedLastEvaluatedKey2 = new HashMap<>();
        expectedLastEvaluatedKey2.put("id", stringValue(KEYS_ONLY_RECORDS.get(9).getId()));
        expectedLastEvaluatedKey2.put("sort", numberValue(KEYS_ONLY_RECORDS.get(9).getSort()));
        expectedLastEvaluatedKey2.put("gsi_id", stringValue(KEYS_ONLY_RECORDS.get(9).getGsiId()));
        expectedLastEvaluatedKey2.put("gsi_sort", numberValue(KEYS_ONLY_RECORDS.get(9).getGsiSort()));

        assertThat(page1.items(), is(KEYS_ONLY_RECORDS.subList(0, 5)));
        assertThat(page1.lastEvaluatedKey(), is(expectedLastEvaluatedKey1));
        assertThat(page2.items(), is(KEYS_ONLY_RECORDS.subList(5, 10)));
        assertThat(page2.lastEvaluatedKey(), is(expectedLastEvaluatedKey2));
        assertThat(page3.items(), is(empty()));
        assertThat(page3.lastEvaluatedKey(), is(nullValue()));
    }

    @Test
    public void queryEmpty() {
        Iterator<Page<Record>> results =
            keysOnlyMappedIndex.query(r -> r.queryConditional(keyEqualTo(k -> k.partitionValue("gsi-id-value")))).iterator();
        assertThat(results.hasNext(), is(true));
        Page<Record> page = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page.items(), is(empty()));
        assertThat(page.lastEvaluatedKey(), is(nullValue()));
    }

    @Test
    public void queryExclusiveStartKey() {
        insertRecords();
        Map<String, AttributeValue> expectedLastEvaluatedKey = new HashMap<>();
        expectedLastEvaluatedKey.put("id", stringValue(KEYS_ONLY_RECORDS.get(7).getId()));
        expectedLastEvaluatedKey.put("sort", numberValue(KEYS_ONLY_RECORDS.get(7).getSort()));
        expectedLastEvaluatedKey.put("gsi_id", stringValue(KEYS_ONLY_RECORDS.get(7).getGsiId()));
        expectedLastEvaluatedKey.put("gsi_sort", numberValue(KEYS_ONLY_RECORDS.get(7).getGsiSort()));
        Iterator<Page<Record>> results =
            keysOnlyMappedIndex.query(QueryEnhancedRequest.builder()
                                                          .queryConditional(keyEqualTo(k -> k.partitionValue("gsi-id-value")))
                                                          .exclusiveStartKey(expectedLastEvaluatedKey).build())
                               .iterator();

        assertThat(results.hasNext(), is(true));
        Page<Record> page = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page.items(), is(KEYS_ONLY_RECORDS.subList(8, 10)));
        assertThat(page.lastEvaluatedKey(), is(nullValue()));
    }
}
