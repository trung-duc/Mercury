package ng.duc.mercury.data;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

/**
 * Created by ducprogram on 6/19/16.
 * This class serves as an interface for
 * Android ecosystem to access our database. Basically,
 * if we want to CRUD this database, we contact Android
 * ecosystem (w/ ContentResolver), then Android ecosystem
 * will use this ContentProvider to access the data
 */
public class MercuryDataProvider extends ContentProvider {

	private final String LOG_TAG = MercuryDataProvider.class.getSimpleName();

	// These constants define the interaction between this content
	// provider and the database
	static final int LIST_ALL_TAGS = 210;
	static final int LIST_SOME_TAGS = 211;
	static final int LIST_NO_TAGS = 212;
	static final int SINGLE_TAG = 213;

	// Helper to open and manage database
	private MercuryDatabaseOpener mDatabaseOpener;

	// Matcher helper to match URI to appropriate database operation
	private static final UriMatcher mUriMatcher = buildUriMatcher();

	@Override
	public boolean onCreate() {
		mDatabaseOpener = new MercuryDatabaseOpener(getContext());
		return true;
	}

	@Nullable
	@Override
	public Cursor query(@NonNull Uri uri, String[] projection, String selection,
	                    String[] selectionArgs, String sortOrder) {

		Cursor reCursor;
		SQLiteDatabase db = mDatabaseOpener.getReadableDatabase();

		switch (mUriMatcher.match(uri)) {
			case LIST_ALL_TAGS:
				reCursor = db.query(true, DataContract.TAG_BUS,
						DataContract.tagEntry.PROJECTION,
						null, null, null, null, sortOrder, null);
				break;
			case LIST_SOME_TAGS:
				String[] allTags = DataContract.tagEntry.getTagNames(uri);
				reCursor = db.query(true, DataContract.TAG_BUS,
						DataContract.tagEntry.PROJECTION,
						DataContract.tagEntry.buildConditionalQuery(allTags),
						allTags, null, null, sortOrder, null);
				break;
			case LIST_NO_TAGS:
				reCursor = db.query(DataContract.TAG_BUS,
						new String[]{}, null, null, null, null, null, "0");
				break;
			case SINGLE_TAG:
				throw new UnsupportedOperationException("Cannot query single tag");
			default:
				throw new UnsupportedOperationException("Unknown uri: " + uri);
		}

		if (getContext() != null) {
			reCursor.setNotificationUri(getContext().getContentResolver(), uri);
		} else {
			Log.w(LOG_TAG, "query: getContext() returns null, " +
						"hence setNotificationUri is not called");
		}

		return reCursor;
	}


	@Nullable
	@Override
	public Uri insert(@NonNull Uri uri, ContentValues values) {

		SQLiteDatabase db = mDatabaseOpener.getWritableDatabase();
		Uri returnUri;

		switch (mUriMatcher.match(uri)) {

			case SINGLE_TAG: {
				long _id = db.insert(DataContract.TAG_BUS, null, values);
				if (_id > 0) {
					returnUri = uri;
				} else {
					throw new SQLException("Failed to insert to tag database: " + uri);
				}
				break;
			}

			default:
				throw new UnsupportedOperationException("Invalid uri: " + uri);
		}

		if (getContext() != null) {
			getContext().getContentResolver().notifyChange(uri, null);
		} else {
			Log.w(LOG_TAG, "insert: getContext() returns null, " +
						"cannot get content resolver, " +
						"hence notifyChange() is not called");
		}

		return returnUri;
}


	@Override
	public int bulkInsert(@NonNull Uri uri, @NonNull ContentValues[] values) {

		SQLiteDatabase db = mDatabaseOpener.getWritableDatabase();
		int returnCount = 0;

		switch (mUriMatcher.match(uri)) {

			case LIST_ALL_TAGS: {
				db.beginTransaction();
				try {
					for (ContentValues value: values) {
						long _id = db.insert(DataContract.TAG_BUS, null, value);
						if (_id != -1) {
							returnCount++;
						}
					}
					db.setTransactionSuccessful();
				} finally {
					db.endTransaction();
				}
				break;
			}

			default:
				throw new UnsupportedOperationException("Unknown uri: " + uri);
		}

		if (getContext() != null) {
			getContext().getContentResolver().notifyChange(uri, null);
		} else {
			Log.w(LOG_TAG, "bulkInsert: getContext() returns null, " +
						"cannot retrieve content resolver, " +
						"hence cannot notifyChange()");
		}

		return returnCount;
	}

	@Override
	public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		return 0;
	}

	/**
	 * This method will analyze which table should be affected from the uri, and then delete
	 * rows that pass the selection and selectionArgs. To delete all rows in the table, pass
	 * in null for both selection and selectionArgs.
	 * @param uri               the uri that matches appropriate table
	 * @param selection         the WHERE = ? condition to find the rows
	 * @param selectionArgs     the range of values that match with selection
	 * @return                  the number of rows deleted
	 */
	@Override
	public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {

		SQLiteDatabase db = mDatabaseOpener.getWritableDatabase();
		int rowsDeleted;

		switch (mUriMatcher.match(uri)) {

			case LIST_ALL_TAGS: {
				if (selection == null) selection = "1";
				rowsDeleted = db.delete(DataContract.TAG_BUS, selection, selectionArgs);
				break;
			}

			case LIST_SOME_TAGS: {
				String[] allTags = DataContract.tagEntry.getTagNames(uri);
				rowsDeleted = db.delete(DataContract.TAG_BUS,
						DataContract.tagEntry.buildConditionalQuery(allTags),
						allTags);
				break;
			}

			case SINGLE_TAG: {
				String[] tagAndId = DataContract.tagEntry.getTagNameAndBusID(uri);
				rowsDeleted = db.delete(DataContract.TAG_BUS,
						DataContract.tagEntry.selectTagAndId,
						tagAndId);
				break;
			}

			default:
				throw new UnsupportedOperationException("Unknown uri: " + uri);
		}

		if (rowsDeleted != 0) {
			if (getContext() != null) {
				getContext().getContentResolver().notifyChange(uri, null);
			} else {
				Log.w(LOG_TAG, "delete: getContext() returns null, " +
						"cannot retrieve content resolver, " +
						"hence cannot notifyChange()");
			}
		}

		return rowsDeleted;
	}

	@Nullable
	@Override
	public String getType(@NonNull Uri uri) {

		final int match = mUriMatcher.match(uri);

		switch (match) {
			case LIST_ALL_TAGS:
				return DataContract.tagEntry.CONTENT_TYPE;
			case LIST_SOME_TAGS:
				return DataContract.tagEntry.CONTENT_TYPE;
			case SINGLE_TAG:
				return DataContract.tagEntry.CONTENT_ITEM_TYPE;
			default:
				throw new UnsupportedOperationException(
						"Unknown uri: " + uri.toString());
		}
	}

	/**
	 * This method will be used to create an UriMatcher object, which them be
	 * used to recognize the type of Uri sent by the application
	 * @return  uri matcher object that can categorize uri to correct operations
	 */
	static UriMatcher buildUriMatcher() {

		final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
		final String authority = DataContract.PACKAGE_NAME;

		// We match each possible Uri to a corresponding MIME type
		matcher.addURI(authority, DataContract.TAG_BUS, LIST_ALL_TAGS);
		matcher.addURI(authority, DataContract.TAG_BUS + "/all", LIST_NO_TAGS);
		matcher.addURI(authority, DataContract.TAG_BUS + "/*", LIST_SOME_TAGS);
		matcher.addURI(authority, DataContract.TAG_BUS + "/*/*", SINGLE_TAG);
//		matcher.addURI(authority, DataContract.RECOMMENDATION, RECOMMENDATION);
//		matcher.addURI(authority, DataContract.LOYALTY, ALL_LOYALTY);
//		matcher.addURI(authority, DataContract.EVENTS, ALL_EVENTS);
//		matcher.addURI(authority, DataContract.PRODUCTS + "/*", EACH_BUSINESS_PRODUCTS);
//		matcher.addURI(authority, DataContract.TESTIMONIALS + "/*", EACH_BUSINESS_TESTIMONIALS);
//
//		matcher.addURI(authority, DataContract.DETAILED + "/*", EACH_BUSINESS);
//		matcher.addURI(authority, DataContract.TAG + "/*/*", EACH_BUSINESS);
//		matcher.addURI(authority, DataContract.EVENTS + "/*", EACH_BUSINESS);
//		matcher.addURI(authority, DataContract.LOYALTY_DETAIL + "/*", EACH_LOYALTY);

		return matcher;
	}


	public int deleteTest(Context context, Uri uri, String string, String[] array) {
		mDatabaseOpener = new MercuryDatabaseOpener(context);
		return delete(uri, string, array);
	}

	public Cursor queryTest(Context context, Uri uri, String[] array1,
	                        String string1, String[] array2, String string2) {
		mDatabaseOpener = new MercuryDatabaseOpener(context);
		return query(uri, array1, string1, array2, string2);
	}

	public int bulkInsertTest(Context context, Uri uri, ContentValues[] values) {
		mDatabaseOpener = new MercuryDatabaseOpener(context);
		return bulkInsert(uri, values);
	}
}
