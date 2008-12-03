package org.andnav.osm.contributor;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;

import org.andnav.osm.adt.GPSGeoLocation;
import org.andnav.osm.contributor.util.RecordedRouteGPXFormatter;
import org.andnav.osm.contributor.util.Util;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.impl.client.DefaultHttpClient;

import android.util.Log;


public class GPXToPHPUploader {

	protected static final String UPLOADSCRIPT_URL = "http://www.PLACEYOURDOMAINHERE.com/anyfolder/gpxuploader/upload.php";

	/**
	 * @param recordedGeoPoints
	 */
	public static void uploadAsync(final ArrayList<GPSGeoLocation> recordedGeoPoints){
		new Thread(new Runnable(){
			@Override
			public void run() {
				try{
					if(!Util.isSufficienDataForUpload(recordedGeoPoints)) return;

					final InputStream gpxInputStream = new ByteArrayInputStream(RecordedRouteGPXFormatter.create(recordedGeoPoints).getBytes());
					doHttpPostUpload(gpxInputStream);
				}catch (Exception e){
					//	Log.e(DEBUGTAG, "OSMUpload Error", e);
				}
			}
		}).start();
	}
	
	/**
	 * @param gpxInputStream backed by valid gpx-data.
	 */
	public static void uploadAsync(final InputStream gpxInputStream){
		new Thread(new Runnable(){
			@Override
			public void run() {
				try{
					doHttpPostUpload(gpxInputStream);
				}catch (Exception e){
					//	Log.e(DEBUGTAG, "OSMUpload Error", e);
				}
			}
		}).start();
	}

	private static void doHttpPostUpload(final InputStream gpxInputStream) throws IOException, ClientProtocolException {
		final HttpClient httpClient = new DefaultHttpClient();

		final HttpPost request = new HttpPost(UPLOADSCRIPT_URL);

		// create the multipart request and add the parts to it
		final MultipartEntity requestEntity = new MultipartEntity();
		requestEntity.addPart("gpxfile", new InputStreamBody(gpxInputStream, "" + System.currentTimeMillis() + ".gpx"));

		httpClient.getParams().setBooleanParameter("http.protocol.expect-continue", false);

		request.setEntity(requestEntity);

		final HttpResponse response = httpClient.execute(request);
		final int status = response.getStatusLine().getStatusCode();

		if (status != HttpStatus.SC_OK) {
			Log.e("GPXUploader", "status != HttpStatus.SC_OK");
		} else {
			final Reader r = new InputStreamReader(new BufferedInputStream(response.getEntity().getContent()));
			// see above
			final char[] buf = new char[8 * 1024];
			int read;
			final StringBuilder sb = new StringBuilder();
			while((read = r.read(buf)) != -1)
				sb.append(buf, 0, read);

			Log.d("GPXUploader", "Response: " + sb.toString());
		}
	}
}
