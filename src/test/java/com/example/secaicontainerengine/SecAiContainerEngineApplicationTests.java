package com.example.secaicontainerengine;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SecAiContainerEngineApplicationTests {

	@Test
	void contextLoads() throws JsonProcessingException, JSONException {
		JSONObject obj = new JSONObject(
				" { 'containerType':'docker', 'containers': [ 'nginx', 'tensorflow'], 'adversarialAttackParams':{ 'attackType':'fgsm', 'epsilon':0.1, 'targetModel':'/path/to/model'}}"
		);
		String containerType = obj.getString("containerType");
		JSONObject jsonObject = obj.getJSONObject("adversarialAttackParams");
		System.out.println(containerType);
		System.out.println(jsonObject);

	}


}
