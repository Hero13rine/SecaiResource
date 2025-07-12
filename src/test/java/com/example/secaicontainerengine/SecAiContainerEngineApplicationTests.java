package com.example.secaicontainerengine;

import com.example.secaicontainerengine.pojo.entity.ModelMessage;
import com.example.secaicontainerengine.service.modelEvaluation.EvaluationResultService;
import com.example.secaicontainerengine.service.modelEvaluation.ModelMessageService;
import com.example.secaicontainerengine.util.FileUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.json.JSONException;
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
	private FileUtils fileUtils;

	@Autowired
	private ModelMessageService modelMessageService;

	@Test
	void testScore() throws JsonProcessingException, JSONException {
		evaluationResultService.calculateAndUpdateScores(8784390L);
	}

	@Test
	void testReport() throws Exception {
		String url = "http://localhost:8081/api/evaluation/result/8784390";
		ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
	}

}
