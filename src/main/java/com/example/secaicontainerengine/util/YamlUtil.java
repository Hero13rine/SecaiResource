package com.example.secaicontainerengine.util;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.Map;


@Slf4j
public class YamlUtil {

    //填充yml模板方法
    public static String renderTemplate(String templatePath, Map<String, String> values)
            throws IOException, TemplateException {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_32);
        cfg.setClassLoaderForTemplateLoading(Thread.currentThread().getContextClassLoader(), "");
        Template template = cfg.getTemplate(templatePath);
        StringWriter writer = new StringWriter();
        template.process(values, writer);
//        log.info("YamlUtil工具类：" + writer.toString());
        return writer.toString();
    }

    //获取填充好的Pod模板文件中metadata中的name
    public static String getName(ByteArrayInputStream stream) throws IOException {
        Yaml yaml = new Yaml();
        Map<String, Object> yamlMap = yaml.load(new InputStreamReader(stream));
        stream.reset();
        // 获取 metadata 下的 name
        if (yamlMap != null && yamlMap.containsKey("metadata")) {
            Map<String, Object> metadata = (Map<String, Object>) yamlMap.get("metadata");
            if (metadata != null && metadata.containsKey("name")) {
                String podName = (String) metadata.get("name");
                return podName;
            } else {
                return null;
            }
        } else {
            System.out.println("Key 'metadata' not found.");
            return null;
        }
    }

}
