
/**
* Copyright 2015 Google Inc.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.laykart;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.api.client.http.InputStreamContent;
import com.google.api.client.util.Data;
import com.google.api.services.storage.Storage;
import com.google.api.services.storage.model.ObjectAccessControl;
import com.google.api.services.storage.model.Objects;
import com.google.api.services.storage.model.StorageObject;
import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.images.Image;
import com.google.appengine.api.images.ImagesService;
import com.google.appengine.api.images.ImagesServiceFactory;
import com.google.appengine.api.images.Transform;
import com.google.appengine.repackaged.org.joda.time.Instant;
import com.google.appengine.repackaged.org.joda.time.Interval;
import com.google.appengine.tools.cloudstorage.GcsFileOptions;
import com.google.appengine.tools.cloudstorage.GcsFilename;
import com.google.appengine.tools.cloudstorage.GcsService;
import com.google.appengine.tools.cloudstorage.GcsServiceFactory;
import com.google.appengine.tools.cloudstorage.RetryParams;

//@MultipartConfig
@SuppressWarnings("serial")
public class ImagesServlet extends HttpServlet {

	String bucket = null;
	String sourceImageFolder = null;
	String  movedFolder = null;
	String thumbnailDestinationFolder[] = null;
	String productDetailDestinationFolder[] = null;
	String productSmallDestinationFolder[] = null;
	String bannerDestinationFolder[] = null;
	String movedFolderBanner = null;
	String sourceImageBannerFolder = null;
	byte[] imageBytes1 = null;
	String imgPath = "https://storage.cloud.google.com/leykart-resized-images/";

	// [START gcs]

	// Allows creating and accessing files in Google Cloud Storage.
	private final GcsService gcsService = GcsServiceFactory.createGcsService(new RetryParams.Builder()
			.initialRetryDelayMillis(50).retryMaxAttempts(50).totalRetryPeriodMillis(150000).build());
	// [END gcs]

	/*
	 * @Override public void doPost(HttpServletRequest req, HttpServletResponse
	 * resp) throws ServletException,IOException {
	 * 
	 * 
	 * Part filePart = req.getPart("fileName"); // Retrieves <input type="file"
	 * name="file"> //String fileName =
	 * Paths.get(filePart.getSubmittedFileName()).getFileName().toString(); //
	 * MSIE fix. //InputStream fileContent = filePart.getInputStream();
	 * 
	 * String newImageUrl = null; CloudStorageHelper cludhelp=new
	 * CloudStorageHelper(); newImageUrl = cludhelp.uploadFile( filePart ,
	 * bucket);
	 * 
	 * //doGet(req, resp); System.out.println(newImageUrl);
	 * 
	 * }
	 */

	public static List<StorageObject> listBucket(String bucketName) throws IOException, GeneralSecurityException {
		Storage client = StorageFactory.getService();
		Storage.Objects.List listRequest = client.objects().list(bucketName);

		List<StorageObject> results = new ArrayList<StorageObject>();
		Objects objects;

		// Iterate through each page of results, and add them to our results
		// list.
		do {
			objects = listRequest.execute();
			// Add the items in this page of results to the list we'll return.
			results.addAll(objects.getItems());

			// Get the next page, in the next iteration of this loop.
			listRequest.setPageToken(objects.getNextPageToken());
		} while (null != objects.getNextPageToken());

		return results;
	}

	@SuppressWarnings("resource")
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		
		
		

		String[] thumbnail = null;
		String[] productDetail = null;
		String[] productSmall = null;
		String[] banner = null;
		String[] sourceFolder = null;
		String imageFormat = null;
		String imageFormat2 = null;
		
		Date startDate = new Date();

		List<StorageObject> bucketContents = null;
		try {
			File file = new File("WEB-INF/application.properties");
			FileInputStream fileInput = new FileInputStream(file);
			Properties properties = new Properties();
			properties.load(fileInput);

			bucket = properties.getProperty("bucket");
			System.out.println(bucket);
			
			sourceImageFolder = properties.getProperty("sourceImageFolder");
			System.out.println(sourceImageFolder);
			
			sourceImageBannerFolder = properties.getProperty("sourceImageBannerFolder");
			System.out.println(sourceImageBannerFolder);
			
			movedFolder = properties.getProperty("movedFolder");
			System.out.println(movedFolder);
			
			movedFolderBanner = properties.getProperty("movedFolderBanner");
			System.out.println(movedFolderBanner);
			

			String thumbnailDestinationFolderString = properties.getProperty("thumbnailDestinationFolder");
			System.out.println(thumbnailDestinationFolderString);
			thumbnailDestinationFolder = thumbnailDestinationFolderString.split(",");

			String productDetailDestinationFolderString = properties.getProperty("productDetailDestinationFolder");
			productDetailDestinationFolder = productDetailDestinationFolderString.split(",");

			String productSmallDestinationFolderString = properties.getProperty("productSmallDestinationFolder");
			productSmallDestinationFolder = productSmallDestinationFolderString.split(",");

			String bannerDestinationFolderString = properties.getProperty("bannerDestinationFolder");
			bannerDestinationFolder = bannerDestinationFolderString.split(",");

			String thumbnailString = properties.getProperty("thumbnail");
			thumbnail = thumbnailString.split(",");

			String productDetailString = properties.getProperty("productDetail");
			productDetail = productDetailString.split(",");

			String productSmallString = properties.getProperty("productSmall");
			productSmall = productSmallString.split(",");

			String bannerString = properties.getProperty("banner");
			banner = bannerString.split(",");

			fileInput.close();
			bucketContents = listBucket(bucket);
		} catch (GeneralSecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		ImagesService imagesService = ImagesServiceFactory.getImagesService();

		if (null == bucketContents) {
			System.out.println("There were no objects in the given bucket; try adding some and re-running.");
		} else {

			for (StorageObject object : bucketContents) {

				System.out.println(object.getName());

				
				String path = object.getName();
				System.out.println("**********" + path);
				if (path.startsWith(sourceImageFolder)) {
				if (("image/png".equals(object.getContentType())) || ("image/jpeg".equals(object.getContentType()))) {
						
						String objectName = object.getName();
						if (objectName.endsWith(".png") || objectName.endsWith(".jpg")) {
							
							if(objectName.endsWith(".png")){
							imageFormat = ".png";
							}
							if(objectName.endsWith(".jpg")){
							imageFormat = ".jpg";
							}
							
							objectName = objectName.substring(7, (objectName.length() - 4));
							System.out.println(objectName);
						} else if (objectName.endsWith(".jpeg")) {
							
							imageFormat = ".jpeg";
							
							objectName = objectName.substring(7, (objectName.length() - 5));
							System.out.println(objectName);
						}
					
						ServletContext context = getServletContext();
						String imageUrl=imgPath + object.getName();
						URL resource2 = context.getResource(imgPath + object.getName());
						File file2 = null;

						file2 = new File(resource2.toURI());
						FileInputStream fileInputStream2 = new FileInputStream(file2);
						FileChannel fileChannel2 = fileInputStream2.getChannel();
						ByteBuffer byteBuffer2 = ByteBuffer.allocate((int) fileChannel2.size());
						fileChannel2.read(byteBuffer2);

						imageBytes1 = byteBuffer2.array();
						
						Image imagee = ImagesServiceFactory.makeImage(imageBytes1);
						Transform resizee = ImagesServiceFactory.makeResize(100, 50);
						
						Image resizedImagee = imagesService.applyTransform(resizee, imagee);

						// Write the transformed image back to a Cloud Storage
						// object.
						gcsService.createOrReplace(new GcsFilename(bucket, "resizedImage_100X50"+ imageFormat),
								new GcsFileOptions.Builder().mimeType("image/jpeg").build(),
								ByteBuffer.wrap(resizedImagee.getImageData()));

						// Create a temp file to upload
						// Path tempPath = Files.createTempFile("StorageSample",
						// "txt");
						// Files.write(tempPath, "Sample file".getBytes());
						// File tempFile = tempPath.toFile();
						// tempFile.deleteOnExit();

						// uploadFile(TEST_FILENAME, "image/png", tempFile,
						// bucketName);

						// [START rotate]
						// Make an image from a Cloud Storage object, and
						// transform it.

						// BlobstoreService allows you to manage the creation
						// and serving of large, immutable blobs to users.
						System.out.println("Test3");
						BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
						BlobKey blobKey = blobstoreService.createGsBlobKey("/gs/" + bucket + "/" + object.getName()); // Creating
																														// a
						

						Image blobImage = ImagesServiceFactory.makeImageFromBlob(blobKey); // Create

						// For Thumbnail
						
						
						for (int i = 0, j = 0; i < thumbnail.length; i++, j++) {

							int width = Integer.parseInt(thumbnail[i]);
							int height = Integer.parseInt(thumbnail[i + 1]);
							System.out.println(width + "X" + height);

							Transform resize1 = ImagesServiceFactory.makeResize(width, height);
							Image resizeImage1 = imagesService.applyTransform(resize1, blobImage);

							// Write the transformed image back to a Cloud
							// Storage object.
							gcsService.createOrReplace(
									new GcsFilename(thumbnailDestinationFolder[j],
										objectName  + imageFormat),
									new GcsFileOptions.Builder().mimeType("image/jpeg").build(),
									ByteBuffer.wrap(resizeImage1.getImageData()));

							i++;
						}

						// For productDetail

						for (int i = 0, j = 0; i < productDetail.length; i++, j++) {

							int width = Integer.parseInt(productDetail[i]);
							int height = Integer.parseInt(productDetail[i + 1]);
							System.out.println(width + "X" + height);

							Transform resize_1_5 = ImagesServiceFactory.makeResize(width, height);
							Image resizeImage1_5 = imagesService.applyTransform(resize_1_5, blobImage);

							// Write the transformed image back to a Cloud
							// Storage object.
							gcsService.createOrReplace(
									new GcsFilename(productDetailDestinationFolder[j],
										objectName  + imageFormat ),
									new GcsFileOptions.Builder().mimeType("image/jpeg").build(),
									ByteBuffer.wrap(resizeImage1_5.getImageData()));

							i++;
						}

						// For productSmall

						for (int i = 0, j = 0; i < productSmall.length; i++, j++) {

							int width = Integer.parseInt(productSmall[i]);
							int height = Integer.parseInt(productSmall[i + 1]);
							System.out.println(width + "X" + height);

							Transform resize2x = ImagesServiceFactory.makeResize(width, height);
							Image resizeImage2 = imagesService.applyTransform(resize2x, blobImage);

							// Write the transformed image back to a Cloud
							// Storage object.
							gcsService.createOrReplace(
									new GcsFilename(productSmallDestinationFolder[j],
										objectName + imageFormat ),
									new GcsFileOptions.Builder().mimeType("image/jpeg").build(),
									ByteBuffer.wrap(resizeImage2.getImageData()));

							i++;
						}

						
						
						String imageName = object.getName();
						if (imageName.endsWith(".png") || imageName.endsWith(".jpg")) {
							imageName = imageName.substring(7, (imageName.length()));
							System.out.println(imageName);
						} else if (imageName.endsWith(".jpeg")) {
							imageName = imageName.substring(7, (imageName.length()));
							System.out.println(imageName);
						}
						
					GcsFilename source = new GcsFilename(bucket, object.getName());
						GcsFilename source2 = new GcsFilename(bucket + "/"+ sourceImageFolder , imageName);
					    System.out.println("SOURCE::::" + source);
						System.out.println("SOURCE2::::" + source2);
					    GcsFilename dest = new GcsFilename(movedFolder, imageName);
					    gcsService.copy(source, dest);
					    System.out.println("DESTINATION::::" + dest);
					    gcsService.delete(source2);
						
						

					}
				}
				
				//Start Banner Folder
				//System.out.println("Type::::::::::::::::::: " + object.getContentType());
				if (path.startsWith(sourceImageBannerFolder)) {
					if (("image/png".equals(object.getContentType())) || ("image/jpeg".equals(object.getContentType()))) {
						System.out.println("BannerImage Type::::::::::::::::::: " + object.getContentType());
						
						
						String objectName2 = object.getName();
						if (objectName2.endsWith(".png") || objectName2.endsWith(".jpg")) {
							
							if(objectName2.endsWith(".png")){
							imageFormat2 = ".png";
							}
							if(objectName2.endsWith(".jpg")){
							imageFormat2 = ".jpg";
							}
							objectName2 = objectName2.substring(13, (objectName2.length() - 4));
							System.out.println(objectName2);
						} else if (objectName2.endsWith(".jpeg")) {
							
							if(objectName2.endsWith(".jpeg")){
							imageFormat2 = ".jpeg";
							}
							objectName2 = objectName2.substring(13, (objectName2.length() - 5));
							System.out.println(objectName2);
						}

						
						BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
						BlobKey blobKeyBanner = blobstoreService.createGsBlobKey("/gs/" + bucket + "/" + object.getName()); 
																														
						

						Image blobImageBanner = ImagesServiceFactory.makeImageFromBlob(blobKeyBanner); 	
																										

						// For banner

						for (int i = 0, j = 0; i < banner.length; i++, j++) {

							int width = Integer.parseInt(banner[i]);
							int height = Integer.parseInt(banner[i + 1]);
							System.out.println(width + "X" + height);

							Transform resize3xx = ImagesServiceFactory.makeResize(width, height);
							Image resizeImage33 = imagesService.applyTransform(resize3xx, blobImageBanner);

							// Write the transformed image back to a Cloud
							// Storage object.
							gcsService.createOrReplace(
									new GcsFilename(bannerDestinationFolder[j],
											objectName2  + imageFormat2 ),
									new GcsFileOptions.Builder().mimeType("image/jpeg").build(),
									ByteBuffer.wrap(resizeImage33.getImageData()));

							i++;
						}
						
						
						//gcsService.copy("/" + bucket +"/"+ object.getName(), '/'+ movedFolder+ '/' + object);
						String imageName = object.getName();
						if (imageName.endsWith(".png") || imageName.endsWith(".jpg")) {
							imageName = imageName.substring(13, (imageName.length()));
							System.out.println(imageName);
						} else if (imageName.endsWith(".jpeg")) {
							imageName = imageName.substring(13, (imageName.length()));
							System.out.println(imageName);
						}
						
					GcsFilename sourceBanner = new GcsFilename(bucket, object.getName());
						GcsFilename sourceBanner2 = new GcsFilename(bucket + "/"+ sourceImageBannerFolder , imageName);
					    System.out.println("SOURCE::::" + sourceBanner);
						System.out.println("SOURCE2::::" + sourceBanner2);
					    GcsFilename destBanner = new GcsFilename(movedFolderBanner, imageName);
					    gcsService.copy(sourceBanner, destBanner);
					    System.out.println("DESTINATION::::" + destBanner);
					    gcsService.delete(sourceBanner2);


					}
				} //End banner folder
			}

		} // else end
		Date endDate = new Date();
		Interval interval = new Interval(startDate.getTime(), endDate.getTime());
		com.google.appengine.repackaged.org.joda.time.Duration period = interval.toDuration();
		long days = period.getStandardDays(); // gives the number of days
												// elapsed between start and end
												// date
		long hours = period.getStandardHours();
		long mini = period.getStandardMinutes();
		long sec = period.getStandardSeconds();

		// close for loop

		// [END rotate]
		System.out.println("Test4");
		// Output some simple HTML to display the images we wrote to Cloud
		// Storage
		// in the browser.
		PrintWriter out = resp.getWriter();
		out.println("<html><body style='background: blue;'>");
		out.println("<div style='margin: 10% 3%;color:#fff;font-size: 2em;Background: rgba(47, 130, 212, 0.75);width: 83%;padding: 50px;height: auto;box-shadow: 0px 0px 11px 11px #ccc;'>");
		out.println("Converted Successfully !! Please check in cloud storage \n <br>");
		out.println("\n");
		out.println("\n");
		out.println("\nConversion Time: \n");
		out.println("\n");

		out.println("<table style='border: 12px;width: 55%;color: white;font-size: 30px;margin: 10px;'><tr><th>Days</th><th>Hours</th><th>Minutes</th><th>Seconds</th></tr><tr><td style='text-align: center;'>" + days
				+ "</td><td style='text-align: center;'>" + hours + "</td><td style='text-align: center;'>" + mini + "</td><td style='text-align: center;'>" + sec + "</td></tr></table>");
		out.println("</div>");
		
	}

}
