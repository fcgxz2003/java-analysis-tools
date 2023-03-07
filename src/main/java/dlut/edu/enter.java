package dlut.edu;

import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.util.*;

public class enter {

    //参数识别
    private static final Set<String> requiresArgumentSet = new HashSet<>();

    //每个参数对应内容
    private static final Map<String, String> optionDescriptionMap = new HashMap<>();

    //度量分析中间产物的路径
    private static final String metricTempFilePath = System.getProperty("user.dir") + File.separator + "MetricTempFile.txt";
    private static final String ruleToLocationPath = System.getProperty("user.dir") + File.separator + "txtToLocation.xml";

    //编译后的文件生成路径
    private static final String compilerFilePath = System.getProperty("user.dir") + File.separator + ".project";

    //findsecbugs以jar包形式导入，其指定路径为
    private static final String findsecbugsFilePath = System.getProperty("user.dir") + File.separator + "findsecbugs-plugin-1.12.0.jar";

    /**
     * enter 构造函数，初始化一定的接口参数
     */
    public enter() {
        requiresArgumentSet.add("-src");
        requiresArgumentSet.add("-checks-file");
        requiresArgumentSet.add("-metrics-file");
        requiresArgumentSet.add("-d");
        requiresArgumentSet.add("-o");
    }

    /**
     * 输入参数解析，运行对应功能
     *
     * @param argv 输入字符串数组
     */
    private void parse(String[] argv) {
        int arg = 0;
        while (arg < argv.length) {
            String option = argv[arg];
            if (requiresArgumentSet.contains(option)) {
                ++arg;
                //判断是否缺少参数
                if (arg >= argv.length) {
                    System.out.println("Option " + option + " requires an argument");
                    break;
                }
                String argument = argv[arg];
                ++arg;
                //判断参数次序是否正确
                if (argument.startsWith("-")) {
                    System.out.println("argument " + argument + " error");
                    break;
                }
                optionDescriptionMap.put(option, argument);
            } else {
                System.out.println("Unknown option: " + option);
                break;
            }
        }
    }

    /**
     * 执行PMD 的度量分析
     *
     * @param analysisMetric 度量项的txt文件格式，
     * @param inputPath      要分析的源文件
     * @param outputPath     分析后的输出路径
     * @throws Exception 文件读写的异常
     */
    public static void runPMDMetricAnalysis(String analysisMetric, String inputPath, String outputPath) throws Exception {
        //首先将规则集合或者度量集合转换为指定输入的XML格式
        Set<String> metricSet = Files.txtToSet(analysisMetric);
        Files.setToXML(metricSet, ruleToLocationPath);
        //执行分析，将PMD中间生成的结果输出至当前路径下
        pmdAnalysis pmdTool = new pmdAnalysis(inputPath, System.getProperty("user.dir"));
        pmdTool.runPMD();
        //因为度量分析只有pmd执行，所以直接传输出参数即可
        pmdTool.metricResultConvert(outputPath);
    }

    /**
     * 执行PMD 的规则分析
     *
     * @param analysisRule    规则项的txt文件格式
     * @param inputPath       要分析的源文件
     * @param ruleCheckResult 存储分析后的结果，以json的格式
     * @throws Exception 文件读写的异常
     */
    public static void runPMDRuleAnalysis(String analysisRule, String inputPath, ArrayList<JSONObject> ruleCheckResult) throws Exception {
        //首先将规则集合或者度量集合转换为指定输入的XML格式
        Set<String> metricSet = Files.txtToSet(analysisRule);
        Files.setToXML(metricSet, ruleToLocationPath);
        //执行分析，将PMD中间生成的结果输出至当前路径下
        pmdAnalysis pmdTool = new pmdAnalysis(inputPath, System.getProperty("user.dir"));
        pmdTool.runPMD();
        //需要对输出的xml文件转换成对应的json格式
        pmdTool.ruleResultConvert(ruleCheckResult, Files.getPmdRuleSet());
    }

    /**
     * 先进行编译，然后执行Spotbugs 的规则分析
     *
     * @param inputPath       要分析的源文件
     * @param dependency       编译源文件所需的依赖文件,存储在某个文件夹下
     * @param ruleCheckResult 存储分析后的结果，以json的格式
     * @throws Exception 文件读写的异常
     */
    public static void runSpotbugsRuleAnalysis(String inputPath, String dependency, ArrayList<JSONObject> ruleCheckResult) throws Exception {

        //Spotbugs 暂且找不到指定特定的代码进行便利的内容，只能全跑一遍然后再筛选
        //依赖文件classpath内容需要详细修改，是用txt还是String？
        //如果编译成功返回flag=1，则ruleCheckResult不更新，如果编译不成功，编译不成功信息会加入到ruleCheckResult中
        boolean flag = Files.compilerJavaFile(inputPath, dependency, System.getProperty("user.dir"), ruleCheckResult);
        System.out.println(flag);
        //如果编译成功,则才执行spotbugs分析，
        if (flag) {
            spotbugsAnalysis spotbugs = new spotbugsAnalysis(compilerFilePath, System.getProperty("user.dir"));
            //导入findsecbugs插件，以jar包的形式
            spotbugs.importPlugin(findsecbugsFilePath);
            //规则检测文件输出
            spotbugs.fileOutput(System.getProperty("user.dir"));
            //需要对xml文件转换成json格式
            spotbugs.ruleResultConvert(ruleCheckResult, Files.getSpotbugsRuleSet(), Files.getSpotbugsAnalysisRule(), inputPath);
        }
    }

    /**
     * 项目入口
     *
     * @param args 输入参数
     * @throws Exception 异常
     */
    public static void main(String[] args) throws Exception {
        //当输入是-checks-file时候，对应的是json文件
        //spec-jsa -src test -checks-file onlyPMDRules.txt -o C:\Users\Remind\Desktop\RuleCheckResult.json
        //当输入是-metrics-file时候，对应的是xml文件
        //spec-jsa -src test -metrics-file Metrics.txt -o C:\Users\Remind\Desktop\spec-jsa\MetricResult.xml

        enter e = new enter();
        e.parse(args);
        //获取对应的条目
        String inputPath = optionDescriptionMap.get("-src");
        String ruleSetPath = optionDescriptionMap.get("-checks-file");
        String metricSetPath = optionDescriptionMap.get("-metrics-file");
        String dependency = optionDescriptionMap.get("-d");
        String outputPath = optionDescriptionMap.get("-o");

        //执行文件加载，包括PMD和Spotbugs的规则和对应关系
        Files files = new Files();
        //度量分析
        if (ruleSetPath == null) {
            //度量分析中间结果，如果没有该文件就创建一个新的,如果存在就覆盖
            Writer fileWriter = new FileWriter(metricTempFilePath, false);
            //PMD 度量分析调用
            runPMDMetricAnalysis(metricSetPath, inputPath, outputPath);
        }
        //规则分析
        else {
            //将ruleSetPath中的规则分成PMD和Spotbugs各自规则两个文件，然后判断是不是两个都要运行，从而减少程序开销
            Files.ruleFileAnalysis(ruleSetPath);
            int toolsLabel = Files.executeTools();

            //存储分析工具输出结果
            ArrayList<JSONObject> ruleCheckResult = new ArrayList<>();

            if (toolsLabel == Files.PMDTool) {
                //PMD 规则分析调用
                runPMDRuleAnalysis(ruleSetPath, inputPath, ruleCheckResult);
            } else if (toolsLabel == Files.SpotbugsTool) {
                //Spotbugs 规则分析调用
                runSpotbugsRuleAnalysis(inputPath, dependency, ruleCheckResult);
            } else {
                //PMD 规则分析调用
                runPMDRuleAnalysis(ruleSetPath, inputPath, ruleCheckResult);
                //Spotbugs 规则分析调用
                runSpotbugsRuleAnalysis(inputPath, dependency, ruleCheckResult);
            }
            //将JSON文件通过Gson转换成带有空格空行的格式，方便查看
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String output = gson.toJson(ruleCheckResult);
            File file = new File(outputPath);
            // 将格式化后的字符串写入文件
            Writer write = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
            write.write(output);
            write.close();
        }
    }
}
