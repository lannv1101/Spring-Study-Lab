package accounts.client;

import common.money.Percentage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import rewards.internal.account.Account;
import rewards.internal.account.Beneficiary;

import java.net.URI;
import java.util.Random;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

// TODO-00: In this lab, you are going to exercise the following:
// - Using @SpringBootTest and webEnvironment for end-to-end testing
//   (You are going to refactor the test code of previous lab of "38-rest-ws"
//    to use Spring Boot test framework.)
// - Understanding the different usage model of TestRestTemplate from RestTemplate
//    * Usage of a relative path rather than an absolute path
//    * Handling the 404 response from the service
// - Using MockMvc for Web slice testing
// - Understanding the difference between @MockBean and @Mock

// TODO-01: Make this class a Spring Boot test class
// - Add @SpringBootTest annotation with WebEnvironment.RANDOM_PORT
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AccountClientTests {

	// TODO-02: Autowire TestRestTemplate bean to a field
	// - Name the field as restTemplate
	@Autowired
	private TestRestTemplate testRestTemplate;

	// TODO-03: Update code below to use TestRestTemplate (as opposed to RestTemplate)
	// - Remove RestTemplate from this code
	// - Remove BASE_URL from this code or change the value of it to ""
	// - Run the tests and observe that they pass except
	//   "addAndDeleteBeneficiary" test
	//   (If you are using Gradle, remove test exclude statement
	//    from the build.gradle before running these tests)

	/**
	 * server URL ending with the servlet mapping on which the application can be
	 * reached.
	 */
//	private static final String BASE_URL = "http://localhost:8080";
//
//	private RestTemplate restTemplate = new RestTemplate();
	private Random random = new Random();

	@Test
	public void listAccounts() {
		String url = "/accounts";
		// we have to use Account[] instead of List<Account>, or Jackson won't know what
		// type to unmarshal to
		Account[] accounts = testRestTemplate.getForObject(url, Account[].class);
		assertThat(accounts.length >= 21).isTrue();
		assertThat(accounts[0].getName()).isEqualTo("Keith and Keri Donald");
		assertThat(accounts[0].getBeneficiaries().size()).isEqualTo(2);
		assertThat(accounts[0].getBeneficiary("Annabelle").getAllocationPercentage()).isEqualTo(Percentage.valueOf("50%"));
	}

	@Test
	public void getAccount() {
		String url = "/accounts/{accountId}";
		Account account = testRestTemplate.getForObject(url, Account.class, 0);
		assertThat(account.getName()).isEqualTo("Keith and Keri Donald");
		assertThat(account.getBeneficiaries().size()).isEqualTo(2);
		assertThat(account.getBeneficiary("Annabelle").getAllocationPercentage()).isEqualTo(Percentage.valueOf("50%"));
	}

	@Test
	public void createAccount() {
		String url = "/accounts";
		// use a random account number to avoid conflict
		String number = String.format("12345%4d", random.nextInt(10000));
		Account account = new Account(number, "John Doe");
		account.addBeneficiary("Jane Doe");
		URI newAccountLocation = testRestTemplate.postForLocation(url, account);

		Account retrievedAccount = testRestTemplate.getForObject(newAccountLocation, Account.class);
		assertThat(retrievedAccount.getNumber()).isEqualTo(account.getNumber());

		Beneficiary accountBeneficiary = account.getBeneficiaries().iterator().next();
		Beneficiary retrievedAccountBeneficiary = retrievedAccount.getBeneficiaries().iterator().next();

		assertThat(retrievedAccountBeneficiary.getName()).isEqualTo(accountBeneficiary.getName());
		assertThat(retrievedAccount.getEntityId()).isNotNull();
	}

	// TODO-04: Modify the code below so that it handles 404 HTTP response status
	//          from the server (instead of handling it as an exception as in the
	//          case of RestTemplate)
	// - Remove the "assertThrows" statement (since you are not going to
	//   check if an exception is thrown)
	// - Use "getForEntity" method (instead of "getForObject" method) of
	//   "TestRestTemplate"
	// - Verify that the HTTP response status is 404
	// - Run all tests - they should all pass
	@Test
	public void addAndDeleteBeneficiary() {
		// perform both add and delete to avoid issues with side effects
		String addUrl = "/accounts/{accountId}/beneficiaries";
		URI newBeneficiaryLocation = testRestTemplate.postForLocation(addUrl, "David", 1);
		Beneficiary newBeneficiary = testRestTemplate.getForObject(newBeneficiaryLocation, Beneficiary.class);
		assertThat(newBeneficiary.getName()).isEqualTo("David");

		testRestTemplate.delete(newBeneficiaryLocation);

		ResponseEntity<Beneficiary> response = testRestTemplate.getForEntity(newBeneficiaryLocation, Beneficiary.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	// TODO-05: Observe a log message in the console indicating
	//          Tomcat started as part of testing
	// - Search for "Tomcat started on port(s):"
	// - Note how long it takes for this test to finish - it is
	//   in the range of several seconds

}
