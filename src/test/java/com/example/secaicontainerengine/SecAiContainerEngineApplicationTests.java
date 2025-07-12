package com.example.secaicontainerengine;

import com.example.secaicontainerengine.pojo.dto.file.UploadFileRequest;
import com.example.secaicontainerengine.service.modelEvaluation.EvaluationResultService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SecAiContainerEngineApplicationTests {

	@Autowired
	private EvaluationResultService evaluationResultService;

	@Autowired
	private TestRestTemplate restTemplate;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void testScore() throws JsonProcessingException, JSONException {
		evaluationResultService.calculateAndUpdateScores(8784390L);
	}

	@Test
	void testReport() throws Exception {
		String url = "http://localhost:8081/api/evaluation/result/8784390";
		ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
	}

	@Test
	void TestBusinessConfig() throws JsonProcessingException {
		String json = """
            {"biz":"modelData","modelConfig":{"description":"","modelNetName":"ResNet18","weightFileName":"resnet18_cifar10.pt","modelNetFileName":"ResNet.py","framework":"pytorch","task":"classification"},"resourceConfig":{"cpuRequired":1,"memoryRequired":1,"gpuNumRequired":1,"gpuMemoryRequired":"1","gpuCoreRequired":1},"businessConfig":{"evaluateMethods":[{"dimension":"basic","methodMetricMap":[{"method":"performance_testing","metrics":["accuracy","precision","recall","f1score"]}]},{"dimension":"robustness","methodMetricMap":[{"method":"corruption","metrics":["RmCE","mCE"]},{"method":"adversarial","metrics":["acac","actc","advacc","adverr"]}]}]},"showBusinessConfig":{"evaluateMethods":[{"dimension":"basic","metrics":["accuracy","precision","recall","f1score"],"methods":["performance_testing"]},{"dimension":"robustness","metrics":["RmCE","mCE","acac","actc","advacc","adverr"],"methods":["adversarial","corruption"]}]},"modelId":""}
        """;

		UploadFileRequest result = objectMapper.readValue(json, UploadFileRequest.class);
		Assertions.assertNotNull(result);
		System.out.println("✅ 反序列化成功: " + result);
	}

}
