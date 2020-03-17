package Application.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.google.api.gax.longrunning.OperationFuture;
import com.google.cloud.speech.v1p1beta1.LongRunningRecognizeMetadata;
import com.google.cloud.speech.v1p1beta1.LongRunningRecognizeRequest;
import com.google.cloud.speech.v1p1beta1.LongRunningRecognizeResponse;
import com.google.cloud.speech.v1p1beta1.RecognitionAudio;
import com.google.cloud.speech.v1p1beta1.RecognitionConfig;
import com.google.cloud.speech.v1p1beta1.SpeechClient;
import com.google.cloud.speech.v1p1beta1.SpeechRecognitionAlternative;
import com.google.cloud.speech.v1p1beta1.SpeechRecognitionResult;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
 
 
@RestController
public class ApplicationController {
	
	@PostMapping("/upload/file")
	public  String upload(@RequestBody MultipartFile file) throws IOException{
		 
		// The ID of your GCP project
	    String projectId = "GCP PROJECT ID";

	    // The ID of your GCS bucket
	     String bucketName = "GCS BUCKET NAME";

	    // The ID of your GCS object
	    String objectName =file.getOriginalFilename();

	    Storage storage = StorageOptions.newBuilder().setProjectId(projectId).build().getService();
	    BlobId blobId = BlobId.of(bucketName, objectName);
	    BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
	    storage.create(blobInfo,file.getBytes());

	    return ("gs://"+bucketName+"/"+objectName);
	}
	
	
	@PostMapping("/transcript")
	public String getTranscript(String uri) {
		try (SpeechClient speechClient = SpeechClient.create()) {
			// The language of the supplied audio

			// Sample rate in Hertz of the audio data sent
			int sampleRateHertz = 44100;

			// Encoding of audio data sent. This sample sets this explicitly.
			// This field is optional for FLAC and WAV audio formats.
			RecognitionConfig.AudioEncoding encoding = RecognitionConfig.AudioEncoding.MP3;
			RecognitionConfig config = RecognitionConfig.newBuilder().setLanguageCode("en-IN").setEnableAutomaticPunctuation(true)
					.setSampleRateHertz(sampleRateHertz).setEncoding(encoding).build();

			RecognitionAudio audio = RecognitionAudio.newBuilder().setUri(uri)
					.build();
			LongRunningRecognizeRequest request = LongRunningRecognizeRequest.newBuilder().setConfig(config)
					.setAudio(audio).build();

			OperationFuture<LongRunningRecognizeResponse, LongRunningRecognizeMetadata> response = speechClient
					.longRunningRecognizeAsync(config, audio);
			List<SpeechRecognitionResult> speechResult = response.get().getResultsList();

			StringBuilder transcription = new StringBuilder();
			for (SpeechRecognitionResult result : speechResult) {
				SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
				transcription.append(alternative.getTranscript());
			}
			return transcription.toString();
		} catch (Exception exception) {

			System.err.println("Failed to create the client due to: " + exception);
		}
		return "";
	}
	
}
