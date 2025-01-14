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

package software.amazon.awssdk.transfer.s3;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.junit.rules.ExpectedException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class UploadFileRequestTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void upload_noRequestParamsProvided_throws() {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage("putObjectRequest");

        UploadFileRequest.builder()
                         .source(Paths.get("."))
                         .build();
    }

    @Test
    public void pathMissing_shouldThrow() {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage("source");
        UploadFileRequest.builder()
                         .putObjectRequest(PutObjectRequest.builder().build())
                         .build();
    }

    @Test
    public void sourceUsingFile() {
        Path path = Paths.get(".");
        UploadFileRequest request = UploadFileRequest.builder()
                                                     .putObjectRequest(b -> b.bucket("bucket").key("key"))
                                                     .source(path.toFile())
                                                     .build();
        assertThat(request.source()).isEqualTo(path);
    }

    @Test
    public void sourceUsingFile_null_shouldThrowException() {
        File file = null;
        thrown.expect(NullPointerException.class);
        thrown.expectMessage("source");
        UploadFileRequest.builder()
                         .putObjectRequest(b -> b.bucket("bucket").key("key"))
                         .source(file)
                         .build();
    }

    @Test
    public void equals_hashcode() {
        EqualsVerifier.forClass(UploadFileRequest.class)
                      .withNonnullFields("source", "putObjectRequest")
                      .verify();
    }

}
