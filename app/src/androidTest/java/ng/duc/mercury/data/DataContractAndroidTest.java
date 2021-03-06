package ng.duc.mercury.data;

import android.net.Uri;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Created by ducnguyen on 6/20/16.
 * Test suite to test data contract.
 */
@RunWith(AndroidJUnit4.class)
public class DataContractAndroidTest {

	@Test
	public void testTagEntryMethods() {

		String baseURL = "content://ng.duc.mercury/tag/";
		String encodedURI = "";

		assertThat(DataContract.tagEntry.buildGeneralTag(),
				is(Uri.parse("content://ng.duc.mercury/tag")));

		try {
			encodedURI = URLEncoder.encode("love Mai$._.fuck Mai", "UTF-8").replace("+", "%20");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		assertThat(DataContract.tagEntry.buildSpecificTags(new String[] {"love Mai", "fuck Mai"}),
				is(Uri.parse(baseURL + encodedURI)));

		try {
			encodedURI = URLEncoder.encode("hello Mai", "UTF-8").replace("+", "%20");
			encodedURI += "/";
			encodedURI += URLEncoder.encode("B123", "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		assertThat(DataContract.tagEntry.buildSingleTag("hello Mai", "B123"),
				is(Uri.parse(baseURL + encodedURI)));

		assertThat(DataContract.tagEntry.buildConditionalQuery(new String[] {"Hello"}),
				is("tag = ?"));

		assertThat(DataContract.tagEntry.buildConditionalQuery(new String[] {"Hello", "from"}),
				is("tag = ? OR tag = ?"));

		assertThat(DataContract.tagEntry.buildConditionalQuery(new String[] {"Hello", "from", "the"}),
				is("tag = ? OR tag = ? OR tag = ?"));

		assertThat(DataContract.tagEntry.getTagNames(
				DataContract.tagEntry.buildSpecificTags(new String[] {"Mai"})),
				is(new String[] {"Mai"}));

		assertThat(DataContract.tagEntry.getTagNames(
				DataContract.tagEntry.buildSpecificTags(new String[] {"I/Duc", "love", "Mai", "a lot"})),
				is(new String[] {"I/Duc", "love", "Mai", "a lot"}));

		assertThat(DataContract.tagEntry.getTagNameAndBusID(
				DataContract.tagEntry.buildSingleTag("hello Mai", "B123")),
				is(new String[] {"hello Mai", "B123"}));
	}

	@Test
	public void testAroundEntryMethods() {

		assertThat(DataContract.aroundEntry.buildGeneralAroundUri().toString(),
					is("content://ng.duc.mercury/around"));

		assertThat(DataContract.aroundEntry.buildDealUri().toString(),
					is("content://ng.duc.mercury/around/type/0"));

		assertThat(DataContract.aroundEntry.buildEventUri().toString(),
					is("content://ng.duc.mercury/around/type/1"));

		assertThat(DataContract.aroundEntry.buildSpecificUri("somebusid123").toString(),
					is("content://ng.duc.mercury/around/somebusid123"));

		Uri testDeal = Uri.parse("content://ng.duc.mercury/around/type/0");
		Uri testEvent = Uri.parse("content://ng.duc.mercury/around/type/1");
		Uri testOther = Uri.parse("content://ng.duc.mercury/around/type/123");
		assertThat(DataContract.aroundEntry.getAroundType(testDeal),
					is(0));
		assertThat(DataContract.aroundEntry.getAroundType(testEvent),
					is(1));
		assertThat(DataContract.aroundEntry.getAroundType(testOther),
					is(-1));

	}

	@Test
	public void testBusInfoEntryMethods() {

		assertThat(DataContract.busInfoEntry.buildGeneralUri().toString(),
					is("content://ng.duc.mercury/busInfo"));

		assertThat(DataContract.busInfoEntry.buildBusUri("abc").toString(),
					is("content://ng.duc.mercury/busInfo/abc"));

		assertThat(DataContract.busInfoEntry.buildSavedUri(1).toString(),
					is("content://ng.duc.mercury/busInfo/saved/1"));

		assertThat(DataContract.busInfoEntry.getBusId(
				DataContract.busInfoEntry.buildBusUri("abc")),
					is("abc"));

		assertThat(DataContract.busInfoEntry.getSaved(
				DataContract.busInfoEntry.buildSavedUri(1)),
					is(1));

	}

}
