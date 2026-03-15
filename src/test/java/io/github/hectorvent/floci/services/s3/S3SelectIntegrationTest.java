package io.github.hectorvent.floci.services.s3;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

@QuarkusTest
class S3SelectIntegrationTest {

    @Test
    void select_withWhereClause() {
        String bucket = "select-bucket";
        String key = "data.csv";
        String csvData = "name,age\nAlice,30\nBob,25\nCharlie,35";

        // 1. Create bucket
        given()
            .header("Host", bucket + ".localhost")
        .when()
            .put("/")
        .then()
            .statusCode(200);

        // 2. Put object
        given()
            .header("Host", bucket + ".localhost")
            .body(csvData)
        .when()
            .put("/" + key)
        .then()
            .statusCode(200);

        // 3. Select with WHERE clause
        String requestXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <SelectObjectContentRequest>
                <Expression>SELECT * FROM S3Object s WHERE s.age > 30</Expression>
                <ExpressionType>SQL</ExpressionType>
                <InputSerialization>
                    <CSV>
                        <FileHeaderInfo>USE</FileHeaderInfo>
                    </CSV>
                </InputSerialization>
                <OutputSerialization>
                    <CSV/>
                </OutputSerialization>
            </SelectObjectContentRequest>
            """;

        given()
            .header("Host", bucket + ".localhost")
            .queryParam("select", "")
            .queryParam("select-type", "2")
            .body(requestXml)
        .when()
            .post("/" + key)
        .then()
            .statusCode(200)
            .body(containsString("Charlie,35"))
            .body(not(containsString("Alice,30")))
            .body(not(containsString("Bob,25")));
    }

    @Test
    void select_withProjection() {
        String bucket = "select-bucket-proj";
        String key = "data.csv";
        String csvData = "name,age,city\nAlice,30,New York\nBob,25,London";

        given().header("Host", bucket + ".localhost").when().put("/").then().statusCode(200);
        given().header("Host", bucket + ".localhost").body(csvData).when().put("/" + key).then().statusCode(200);

        String requestXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <SelectObjectContentRequest>
                <Expression>SELECT name, city FROM S3Object</Expression>
                <ExpressionType>SQL</ExpressionType>
                <InputSerialization><CSV><FileHeaderInfo>USE</FileHeaderInfo></CSV></InputSerialization>
                <OutputSerialization><CSV/></OutputSerialization>
            </SelectObjectContentRequest>
            """;

        given()
            .header("Host", bucket + ".localhost")
            .queryParam("select", "")
            .queryParam("select-type", "2")
            .body(requestXml)
        .when()
            .post("/" + key)
        .then()
            .statusCode(200)
            .body(containsString("Alice,New York"))
            .body(containsString("Bob,London"))
            .body(not(containsString("30")))
            .body(not(containsString("25")));
    }

    @Test
    void select_withLimit() {
        String bucket = "select-bucket-limit";
        String key = "data.csv";
        String csvData = "name,age\nAlice,30\nBob,25\nCharlie,35";

        given().header("Host", bucket + ".localhost").when().put("/").then().statusCode(200);
        given().header("Host", bucket + ".localhost").body(csvData).when().put("/" + key).then().statusCode(200);

        String requestXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <SelectObjectContentRequest>
                <Expression>SELECT * FROM S3Object LIMIT 2</Expression>
                <ExpressionType>SQL</ExpressionType>
                <InputSerialization><CSV><FileHeaderInfo>USE</FileHeaderInfo></CSV></InputSerialization>
                <OutputSerialization><CSV/></OutputSerialization>
            </SelectObjectContentRequest>
            """;

        given()
            .header("Host", bucket + ".localhost")
            .queryParam("select", "")
            .queryParam("select-type", "2")
            .body(requestXml)
        .when()
            .post("/" + key)
        .then()
            .statusCode(200)
            .body(containsString("Alice,30"))
            .body(containsString("Bob,25"))
            .body(not(containsString("Charlie,35")));
    }
}
