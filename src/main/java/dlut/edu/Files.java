package dlut.edu;

import com.alibaba.fastjson.JSONObject;
import org.dom4j.*;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import javax.tools.*;
import java.io.*;
import java.util.*;

public class Files {

    //不同工具对应的标号
    static final int PMDTool = 0;
    static final int SpotbugsTool = 1;
    static final int bothTools = 2;

    //PMDXML文件路径
    private static final String pmdXMLPath = "ruleRefAndDes.xml";
    //spotbugsXML文件路径
    private static final String spotbugsXMLPath = "ruleDes.xml";

    //当前存储的能分析的PMD的规则条目,一共127条
    private static final Map<String, String> pmdRuleSet = new HashMap<>();

    //当前存储的能分析的spotbugs和findsecbugs的规则条目,一共373条
    private static final Map<String, String> spotbugsRuleSet = new HashMap<>();

    //存储的PMD的127条规则条目对应的代码位置
    private static final Map<String, String> ruleToLocation = new HashMap<>();

    //从输入规则中解析出要分析的PMD规则条目
    private static final Set<String> pmdAnalysisRule = new HashSet<>();

    //从输入规则中解析出要分析的Spotbugs规则条目
    private static final Set<String> spotbugsAnalysisRule = new HashSet<>();

    /**
     * 构造函数，通过加载initPMDXML和initSpotbugsXML实现对规则的导入
     */
    public Files() {
        initXML(pmdXMLPath, PMDTool);
        initXML(spotbugsXMLPath, SpotbugsTool);
    }

    /**
     * 获取 pmdRuleSet
     *
     * @return pmd规则和对应详细描述
     */
    public static Map<String, String> getPmdRuleSet() {
        return pmdRuleSet;
    }

    /**
     * 获取 spotbugsAnalysisRule
     *
     * @return spotbugs规则和对应详细描述
     */
    public static Set<String> getSpotbugsAnalysisRule() {
        return spotbugsAnalysisRule;
    }

    /**
     * 获取 spotbugsRuleSet
     *
     * @return spotbugs要分析的规则
     */
    public static Map<String, String> getSpotbugsRuleSet() {
        return spotbugsRuleSet;
    }

    /**
     * 加载PMD的XML文件,包含规则以及对应的规则翻译和规则位置，同时加载Spotbugs的XML文件,包含规则以及对应的规则翻译.
     *
     * @param ruleXMLPath  XML文件的路径
     * @param analysisTool 执行内容归属的工具，PMD存储的信息比Spotbugs多了一个代码的位置
     */
    private void initXML(String ruleXMLPath, int analysisTool) {
        File xmlFile = new File(ruleXMLPath);
        SAXReader sr = new SAXReader();
        Document doc = null;
        try {
            doc = sr.read(xmlFile);
        } catch (DocumentException e) {
            e.printStackTrace();
        }

        Element root = doc.getRootElement();
        List<Element> elementList = root.elements();
        if (analysisTool == PMDTool) {
            for (Element ele : elementList) {
                List<Attribute> atts = ele.attributes();
                String rule = atts.get(0).getValue().toString();
                String ruleLocation = atts.get(1).getValue().toString();
                ruleToLocation.put(rule, ruleLocation);

                List<Element> ruleDesEle = ele.elements();
                String ruleDescription = ruleDesEle.get(0).getText().toString();
                pmdRuleSet.put(rule, ruleDescription);
            }
        } else if (analysisTool == SpotbugsTool) {
            for (Element ele : elementList) {
                List<Attribute> atts = ele.attributes();
                String rule = atts.get(0).getValue().toString();

                List<Element> ruleDesEle = ele.elements();
                String ruleDescription = ruleDesEle.get(0).getText().toString();
                spotbugsRuleSet.put(rule, ruleDescription);
            }
        }

    }

    /**
     * 将ruleTxt文件进行拆分，根据spotbugs和PMD两者各自包含的规则进行划分，判断执行哪个规则分析器。
     *
     * @param ruleFilePath 输入的规则分析路径
     * @throws Exception 文件读取异常
     */
    public static void ruleFileAnalysis(String ruleFilePath) throws Exception {
        //按行读取文件进行判断
        FileReader reader = new FileReader(ruleFilePath);
        BufferedReader br = new BufferedReader(reader);
        String line;
        while ((line = br.readLine()) != null) { //循环读取
            if (pmdRuleSet.containsKey(line)) {
                pmdAnalysisRule.add(line);
            } else if (spotbugsRuleSet.containsKey(line)) {
                spotbugsAnalysisRule.add(line);
            }
        }
    }

    /**
     * 从规则中判断要执行的分析工具
     *
     * @return 返回int类型，从而判断要执行什么工具。
     */
    public static int executeTools() {
        if (pmdAnalysisRule.size() != 0 && spotbugsAnalysisRule.size() == 0) {
            return PMDTool;
        } else if (pmdAnalysisRule.size() == 0 && spotbugsAnalysisRule.size() != 0) {
            return SpotbugsTool;
        } else {
            return bothTools;
        }
    }

    /**
     * 用于将度量分析的txt文件转换成set的类型，然后执行转换成XML的操作——setToXML函数
     *
     * @param metricPath 输入度量文件的路径
     * @return 包含度量文件条目的集合
     */
    public static Set<String> txtToSet(String metricPath) throws Exception {
        //按行读取文件进行判断
        FileReader reader = new FileReader(metricPath);
        BufferedReader br = new BufferedReader(reader);

        Set<String> metricSet = new HashSet<>();
        String line;
        while ((line = br.readLine()) != null) { //循环读取
            metricSet.add(line);
        }
        return metricSet;
    }

    /**
     * 将输入的规则txt文件转化成PMD识别的XML文件,PMD中分析的规则条目是需要指定的XML文件格式。
     *
     * @param ruleSet    要分析的规则内容
     * @param targetPath XML文件的输出位置
     * @throws IOException 文件写入异常
     */
    public static void setToXML(Set<String> ruleSet, String targetPath) {

        // 创建dom对象
        Document document = DocumentHelper.createDocument();

        // 添加最外层节点，根据Rule.xml模板添加
        Element ruleset = document.addElement("ruleset");

        for (String r : ruleSet) {
            if (ruleToLocation.containsKey(r)) {
                Element rule = ruleset.addElement("rule");
                rule.addAttribute("deprecated", "true");
                rule.addAttribute("ref", ruleToLocation.get(r));
            }
        }
        OutputFormat format = OutputFormat.createPrettyPrint();
        format.setEncoding("UTF-8");
        // 生成xml文件
        File file = new File(targetPath);
        XMLWriter writer = null;
        try {
            writer = new XMLWriter(new FileOutputStream(file), format);
            writer.setEscapeText(false);
            writer.write(document);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /***
     * 迭代的方式往下递归，获取文件列表。
     *
     * @param file 文件目录
     * @param storeFile 存储特定格式的文件，在本项目中是.java文件
     */
    public static void getFile(File file, Set<String> storeFile) {
        if (file != null && file.exists()) {
            File[] listFiles = file.listFiles();
            if (null == listFiles || listFiles.length == 0) {
                return;
            }
            for (File file2 : listFiles) {
                if (file2.isDirectory()) {
                    getFile(file2, storeFile);
                } else {
                    if (file2.getName().endsWith(".java")) {
                        try {
                            storeFile.add(file2.getCanonicalPath());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    /**
     * 递归删除文件夹
     *
     * @param filePath 文件夹的路径
     */
    public static void deleteFileDir(String filePath) {
        File f = new File(filePath);
        if (f.exists() && f.isDirectory()) {//判断是文件还是目录
            if (f.listFiles().length == 0) {//若目录下没有文件则直接删除
                f.delete();
            } else {//若有则把文件放进数组，并判断是否有下级目录
                File delFile[] = f.listFiles();
                for (int j = 0; j < delFile.length; ++j) {
                    if (delFile[j].isDirectory()) {
                        deleteFileDir(delFile[j].getAbsolutePath());//递归调用删除方法方法并取得子目录路径
                    } else {
                        delFile[j].delete();//删除文件
                    }
                }
            }
        }
    }

    /**
     * 编译源文件
     *
     * @param inputPath       要编译的文件
     * @param dependency      依赖的文件包，存储在某个文件夹下
     * @param targetPath      输出的文件目录
     * @param ruleCheckResult 如果编译出错，则将编译错误以json的格式加入到ruleCheckResult中
     * @return 是否编译成功的boolean值
     * @throws IOException 编译异常
     */
    public static boolean compilerJavaFile(String inputPath, String dependency, String targetPath, ArrayList<JSONObject> ruleCheckResult) throws IOException {

        File[] compilerFile = new File[]{new File(inputPath)};

        //获取系统编译器
        //如果出现Exception in thread "main" java.lang.NullPointerException错误
        //尝试输出System.out.println(System.getProperty("java.home"));系统路径，看lib下是否有tools这个jar包，如果没有，复制个进去即可
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        //获取监听信息，用来得到编译错误
        DiagnosticCollector<JavaFileObject> diagnosticCollector = new DiagnosticCollector<JavaFileObject>();
        // 从编译器中获取基础Java文件管理器
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnosticCollector, null, null);
        Iterable<? extends JavaFileObject> javaFileObjectsFromFiles = fileManager
                .getJavaFileObjectsFromFiles(Arrays.asList(compilerFile));

//        //读取dependency中的jar包，列成一个目录。
//        Set<String> depJars = new TreeSet<>();
//        Files.listFilesWithExtends(dependency, ".jar", depJars);

        Iterable<String> options = Arrays.asList("-classpath", dependency, "-d", targetPath, "-g");
        boolean flag = compiler.getTask(null, fileManager, diagnosticCollector, options, null, javaFileObjectsFromFiles).call();

        if (diagnosticCollector.getDiagnostics().size() != 0) {
            //一个JSONObject实例
            JSONObject rule = new JSONObject();
            //这里规则名字统一为编译错误
            rule.put("checkerName", "COMPILER_ERROR");
            ArrayList<JSONObject> locations = new ArrayList<>();

            //这个部分需要作为规则写进去
            for (Diagnostic<? extends JavaFileObject> diagnostic : diagnosticCollector.getDiagnostics()) {

                JSONObject location = new JSONObject();
                location.put("fileName", diagnostic.getSource().getName());
                location.put("lineNum", diagnostic.getStartPosition());
                //v.getColumn()这个值是-1，无法具体定位到列号，只能赋予0
                location.put("columnNum", diagnostic.getEndPosition());
                location.put("message", diagnostic.getMessage(null));
                locations.add(location);

                rule.put("locations", locations);
                ruleCheckResult.add(rule);

//              System.out.println("Code: " + diagnostic.getCode() + "\n" +
//                        "Kind: " + diagnostic.getKind() + "\n" +
//                        "Position: " + diagnostic.getPosition() + "\n" +
//                        "Start Position: " + diagnostic.getStartPosition() + "\n" +
//                        "End Position: " + diagnostic.getEndPosition() + "\n" +
//                        "Source: " + diagnostic.getSource() + "\n" +
//                        "Message: " + diagnostic.getMessage(null) + "\n");
            }
        }
        fileManager.close();
        return flag;
    }
}
