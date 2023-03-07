package dlut.edu;

import com.alibaba.fastjson.JSONObject;
import edu.umd.cs.findbugs.*;
import edu.umd.cs.findbugs.config.CommandLine;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import se.bjurr.violations.lib.model.Violation;
import se.bjurr.violations.lib.parsers.FindbugsParser;


import java.io.*;
import java.util.*;

public class spotbugsAnalysis {
    public String outputPath;//记录生成的文件位置
    public String inputPath;//要分析的类包和文件路径

    public TextUICommandLine commandLine;
    public FindBugs2 findbugs;

    /**
     * spotbugsAnalysis 构造函数，负责参数的传递
     *
     * @param inputPath  代码编译后的.class文件,可以是单个文件也可以是文件夹
     * @param outputPath 输出路径，主要是由spotbugs分析后中间产物的输出路径
     * @throws IOException 加载工程文件fbp失败的异常
     */
    public spotbugsAnalysis(String inputPath, String outputPath) throws IOException, DocumentException {
        //List的复制
        this.inputPath = inputPath;
        this.outputPath = outputPath;

        //TextUICommandLine 的生成。
        commandLine = new TextUICommandLine();
        //findbugs 的生成。
        findbugs = new FindBugs2();

        //构建以spotbugs命名的pb项目文件。
        // 创建dom对象
        Document document = DocumentHelper.createDocument();
        // 添加最外层节点，根据Rule.xml模板添加
        Element project = document.addElement("Project");
        project.addAttribute("projectName", "spotbugs");

        Element jar = project.addElement("Jar");
        jar.addText(this.inputPath);
        // 格式化模板
        OutputFormat format = OutputFormat.createPrettyPrint();
        format.setEncoding("UTF-8");
        //删除生成xml的头部和正文内容的空行
        format.setNewLineAfterDeclaration(false);
        //删除生成xml的头部内容，即<?xml version="1.0" encoding="UTF-8"?>
        format.setSuppressDeclaration(true);

        // 生成文件
        File file = new File(outputPath + File.separator + "spotbugs.fbp");
        XMLWriter writer = new XMLWriter(new FileOutputStream(file), format);
        writer.setEscapeText(false);
        writer.write(document);
        writer.close();

        //加载project工程项目,其中configuration 在项目生成时自动采用默认配置。
        this.commandLine.loadProject(outputPath + File.separator + "spotbugs.fbp");
    }

    /**
     * 负责导入外部插件，即findsecbugs工具，通过插件的形式导入到spotbugs中。
     *
     * @param path 插件的路径
     */
    public void importPlugin(String path) {
        Map<String, Boolean> customPlugins = this.commandLine.getProject().getConfiguration().getCustomPlugins();
        StringTokenizer tok = new StringTokenizer(path, File.pathSeparator);

        while (tok.hasMoreTokens()) {
            File file = new File(tok.nextToken());
            Boolean enabled = file.isFile();
            customPlugins.put(file.getAbsolutePath(), enabled);
            if (enabled) {
                try {
                    Plugin.loadCustomPlugin(file, this.commandLine.getProject());
                } catch (PluginException var9) {
                    throw new IllegalStateException("Failed to load plugin specified by the '-pluginList', file: " + file, var9);
                }
            }
        }
    }

    /**
     * 调用fingbugs中的执行函数，得到分析文件并输出
     *
     * @param outputPath 输出的文件路径，默认为“SpotBugsFile”
     */
    public void fileOutput(String outputPath) throws IOException, InterruptedException {
        String[] argv = new String[1];
        argv[0] = "-xml:withMessages=" + outputPath + "\\spotbugs_result.xml";
        try {
            commandLine.parse(argv);
        } catch (CommandLine.HelpRequestedException e) {
            e.printStackTrace();
        }
        //完成配置，包括输出类型，输出格式之类。
        commandLine.configureEngine(findbugs);
        commandLine.handleXArgs();
        //开始执行分析操作
        findbugs.execute();
        //获取分析报告
        findbugs.getBugReporter().finish();
    }

    /**
     * 将输出的报告转化为规定格式*
     *
     * @param ruleCheckResult 负责规则的转换工作，由生成的xml文件转换成json文件，通过ruleCheckResult输出
     * @param spotbugsRuleSet 存储规则和对应的描述
     * @param spotbugsRule    存储要指定分析的规则
     * @param sourcePath       传入原Java文件的路径
     * @throws Exception violations分析异常
     */
    public void ruleResultConvert(ArrayList<JSONObject> ruleCheckResult, Map<String, String> spotbugsRuleSet, Set<String> spotbugsRule, String sourcePath) throws Exception {
        /* 将xml文件转为字符串类型*/
        SAXReader saxReader = new SAXReader();
        Document document = saxReader.read(outputPath + "\\spotbugs_result.xml");
        String xmlString = document.asXML();

        //使用Xml解析器FindbugsParser，将xml文件解析成Violation对象的集合
        FindbugsParser findbugsParser = new FindbugsParser();
        Set<Violation> violations = findbugsParser.parseReportOutput(xmlString, null);

        //先将每一个结果按照规则进行分类，因为格式要求是每个规则下把各个地方违反的规则加入，而输出的内容是每个文件下的错误。
        Map<String, Set<Violation>> ruleClassify = new HashMap<>();
        for (Violation v : violations) {
            // 如果分析的结果的规则在规则集中，同时也在输入的规则txt中，那么记录下来
            if (spotbugsRuleSet.containsKey(v.getRule()) && spotbugsRule.contains(v.getRule())) {
                // 如果在规则分类map中存在，则添加，如果不存在，则新增。
                if (ruleClassify.containsKey(v.getRule())) {
                    Set<Violation> set = ruleClassify.get(v.getRule());
                    set.add(v);
                    ruleClassify.put(v.getRule(), set);
                } else {
                    Set<Violation> set = new HashSet<>();
                    set.add(v);
                    ruleClassify.put(v.getRule(), set);
                }
            }
        }
        //根据规则map生成对应的JSON文件
        for (String key : ruleClassify.keySet()) {
            //每一条规则一个JSONObject实例
            JSONObject rule = new JSONObject();
            rule.put("checkerName", key);
            //具体定位，数组
            ArrayList<JSONObject> locations = new ArrayList<>();
            Set<Violation> ruleSet = ruleClassify.get(key);
            for (Violation v : ruleSet) {
                JSONObject location = new JSONObject();
                //这里因为输入路径是\，转换成左斜杠
                //这里后期需要好好改改，如果只是单个文件的话，直接将路径传入就行，但是如果是多个文件的话，就不行。
                sourcePath = sourcePath.replaceAll("\\\\","/");
                location.put("fileName", sourcePath);
                location.put("lineNum", v.getStartLine());
                //v.getColumn()这个值是-1，无法具体定位到列号，只能赋予0
                location.put("columnNum", 0);
                location.put("message", spotbugsRuleSet.get(v.getRule()));
                locations.add(location);
            }
            rule.put("locations", locations);
            ruleCheckResult.add(rule);
        }
    }
}
