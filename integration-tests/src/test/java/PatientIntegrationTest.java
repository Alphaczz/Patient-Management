import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

public class PatientIntegrationTest {

    @BeforeAll
    static void setUp()
    {
        RestAssured.baseURI = "http://localhost:4004";
    }
    @Test
    public void shouldReturnPatientWithValidToken()
    {
        String loginPayload = """
                {
                 "email" : "testuser@test.com",
                 "password" : "password123"
                }
                """;
        String token =given()
                .contentType("application/json")
                .body(loginPayload)
                .when()
                .post("/auth/login")
                .then()
                .statusCode(200)
                .body("token",notNullValue())
                .extract().jsonPath()
                .get("token");

        Response response = given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/patients")
                .then()
                .statusCode(200)
                .body("patients",notNullValue())
                .extract().response();
       //System.out.println("Patient List: " + response.jsonPath().prettyPrint());


    }
    @Test
    public void shouldReturnUnauthorizedWithoutValidToken()
    {
       String token="RANDOMTOKEN";

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/patients")
                .then()
                .statusCode(401);

        //System.out.println("Patient List: " + response.jsonPath().prettyPrint());


    }
}
