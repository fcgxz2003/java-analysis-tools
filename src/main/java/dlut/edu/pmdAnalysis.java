package dlut.edu;

import com.alibaba.fastjson.JSONObject;
import net.sourceforge.pmd.PMD;
import net.sourceforge.pmd.PMDConfiguration;
import net.sourceforge.pmd.lang.LanguageRegistry;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import se.bjurr.violations.lib.model.Violation;
import se.bjurr.violations.lib.parsers.PMDParser;

import java.io.*;
import java.util.*;

public class pmdAnalysis {

    public String inputPath;
    public String outputPath;

    /**
     * pmdAnalysis 构造函数，负责参数的传递
     *
     * @param inputPath  输入文件路径
     * @param outputPath 输出中间产物路径
     * @throws IOException 初始化规则集文档错误异常
     */
    public pmdAnalysis(String inputPath, String outputPath) throws IOException {
        this.inputPath = inputPath;
        this.outputPath = outputPath;
    }

    /**
     * 调用PMD接口进行代码规范分析并生成报告
     */
    public void runPMD() {

        /*输出结果格式*/
        String outputType = "xml";

        try {
            //规则配置类声明
            PMDConfiguration configuration = new PMDConfiguration();
            //配置输入路径
            configuration.setInputPaths(inputPath);
            //将所有的规则全部加载到系统中
            configuration.addRuleSet(System.getProperty("user.dir") + "\\txtToLocation.xml");
            //配置输出结果格式
            configuration.setReportFormat(outputType);
            //配置输出结果路径
            configuration.setReportFile(outputPath + "\\pmd_result.xml");
            //配置待检测项目的java代码版本
            configuration.setDefaultLanguageVersion(LanguageRegistry.findLanguageByTerseName("java").getVersion("1.8"));
            //运行pmd分析插件
            PMD.runPmd(configuration);
        }
        //分析出现异常
        catch (Exception exception) {
            System.out.println(exception.getMessage());
        }
    }

    /**
     * 将度量分析输出的报告转化为规定格式
     * @param targetPath 输出目标路径
     * @throws Exception 异常
     */
    public void metricResultConvert(String targetPath) throws Exception {

        //获取txt分析文件中的信息
        List<String> metricResultList = readFiles(outputPath + "\\MetricTempFile.txt");
        //声明文件名称
        String xmlFileName = targetPath;
        //创建dom对象
        Document document = DocumentHelper.createDocument();

        //添加最外层节点，根据Rule.xml模板添加
        Element javaMetric = document.addElement("javaMetric");
        //项目级别
        Element project = javaMetric.addElement("project");
        //工程级别的条目，在13条度量集中，是没有这个级别的
        Element metricsProject = project.addElement("metrics");
        //文件级别的条目
        Element children = project.addElement("children");

        //设置类的集合，用于存储所有的被检索工程内的类名,以及对应的检测内容,即（类名：有着该类名，以及该类名下函数的度量条目）
        Map<String, Set<String>> classContentMap = new HashMap<>();
        //设置类中的映射集合，用于存储每个类对应的函数的集合，即（类名：有着该类名下函数的度量条目）
        Map<String, Set<String>> classFunctionMap = new HashMap<>();

        //先将结果进行分类，再生成对应的XML
        for (String element : metricResultList) {
            String[] contentList = element.split(";");
            //如果包含"#"，说明是函数级别的度量
            if (contentList[2].contains("#")) {
                String[] classFunctionName = contentList[2].split("#");
                //存入classFunctionMap中类对应的函数的集合
                if (classFunctionMap.containsKey(classFunctionName[0])) {
                    classFunctionMap.get(classFunctionName[0]).add(element);
                } else {
                    Set<String> fun = new HashSet<>();
                    fun.add(element);
                    classFunctionMap.put(classFunctionName[0], fun);
                }
            } else {//如果不包含"#"，说明是文件级别的度量
                if (classContentMap.containsKey(contentList[2])) {
                    classContentMap.get(contentList[2]).add(element);
                } else {
                    Set<String> fun = new HashSet<>();
                    fun.add(element);
                    classContentMap.put(contentList[2], fun);
                }
            }
        }
        for (Map.Entry<String, Set<String>> entry : classContentMap.entrySet()) {
            //添加该类文件的路径属性
            Element file = children.addElement("file");
            file.addAttribute("filepath", entry.getKey());
            Element metrics = file.addElement("metrics");
            //对应每个类文件下的度量内容
            for (String s : entry.getValue()) {
                Element metric = metrics.addElement("metric");
                String[] classContent = s.split(";");
                metric.addAttribute("key", classContent[0]);
                metric.addAttribute("name", classContent[1]);
                metric.addAttribute("value", classContent[3]);
                metric.addAttribute("precision", classContent[6]);
            }

            Set<String> function = classFunctionMap.get(entry.getKey());
            //先对该应该类下的函数进型分类，每个函数各自的度量项是什么
            //设置函数中的映射集合，用于存储每个函数对应的度量的集合，即（函数名：有着该函数名下度量条目）
            Map<String, Set<String>> functionContentMap = new HashMap<>();
            //存储每个函数对应的位置
            Map<String, String> functionLoc = new HashMap<>();
            for (String s : function) {
                String[] functionContent = s.split(";");
                String[] functionName = functionContent[2].split("#");
                //存入classFunctionMap中类对应的函数的集合
                if (functionContentMap.containsKey(functionName[1])) {
                    functionContentMap.get(functionName[1]).add(s);
                } else {
                    Set<String> fun = new HashSet<>();
                    fun.add(s);
                    functionContentMap.put(functionName[1], fun);
                }
                //存入functionLoc中类对应的函数的位置
                if (!functionLoc.containsKey(functionName[1])) {
                    functionLoc.put(functionName[1], functionContent[4]);
                }
            }

            //添加该类文件下对应的函数度量
            Element classChildren = file.addElement("children");
            for (Map.Entry<String, Set<String>> entryFunction : functionContentMap.entrySet()) {
                Element method = classChildren.addElement("method");
                method.addAttribute("mLine", functionLoc.get(entryFunction.getKey()));
                method.addAttribute("mName", entryFunction.getKey());

                Set<String> functionContent = entryFunction.getValue();
                Element functionMetrics = method.addElement("metrics");
                for (String content : functionContent) {
                    Element functionMetric = functionMetrics.addElement("metric");
                    String[] contentList = content.split(";");
                    functionMetric.addAttribute("key", contentList[0]);
                    functionMetric.addAttribute("name", contentList[1]);
                    functionMetric.addAttribute("value", contentList[3]);
                    functionMetric.addAttribute("precision", contentList[6]);
                }
            }
        }
        OutputFormat format = OutputFormat.createPrettyPrint();
        format.setEncoding("UTF-8");
        File xmlFile = new File(xmlFileName);
        XMLWriter writer = new XMLWriter(new FileOutputStream(xmlFile), format);
        writer.setEscapeText(false);
        writer.write(document);
        writer.close();
    }

    /**
     * 将规则检查输出的报告转化为规定格式
     *
     * @param ruleCheckResult 负责规则的转换工作，由生成的xml文件转换成json文件，通过ruleCheckResult输出
     * @param pmdRuleSet      运行初期加载的规则集内容
     * @throws Exception violations分析异常
     */
    public void ruleResultConvert(ArrayList<JSONObject> ruleCheckResult, Map<String, String> pmdRuleSet) throws Exception {

        ///将xml文件转为字符串类型
        SAXReader saxReader = new SAXReader();
        Document document = saxReader.read(outputPath + "\\pmd_result.xml");
        String xmlString = document.asXML();

        //使用Xml解析器PMDParser，将xml文件解析成Violation对象的集合
        PMDParser pmdParser = new PMDParser();
        Set<Violation> violations = pmdParser.parseReportOutput(xmlString, null);

        //将生成的violation集合按检查器，即violation对象中的rule字段分类
        Iterator<Violation> iterator = violations.iterator();

        //先将每一个结果按照规则进行分类，因为格式要求是每个规则下把各个地方违反的规则加入，而输出的内容是每个文件下的错误。
        Map<String, Set<Violation>> ruleClassify = new HashMap<>();
        for (Violation v : violations) {
            //如果分析的结果的规则在规则集中，那么记录下来
            if (pmdRuleSet.containsKey(v.getRule())) {
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
                location.put("fileName", v.getFile());
                location.put("lineNum", v.getStartLine());
                location.put("columnNum", v.getColumn());
                location.put("message", pmdRuleSet.get(v.getRule()));
                locations.add(location);
            }
            rule.put("locations", locations);
            ruleCheckResult.add(rule);
        }
    }

    /**
     * 获取某个txt里的内容
     *
     * @param fileName 文件名的内容
     * @return 返回按行读取的条目
     */
    public List<String> readFiles(String fileName) {
        String pathname = fileName; //文件名
        List<String> ContentList = new ArrayList<String>();
        try {
            FileReader reader = new FileReader(pathname);
            BufferedReader br = new BufferedReader(reader);
            String line;
            while ((line = br.readLine()) != null) { //循环读取
                ContentList.add(line);
            }
            /*关闭文件读取对象*/
            br.close();
            reader.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return ContentList;
    }
}