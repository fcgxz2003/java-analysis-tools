/*Simply counts the number of lines of code the operation or class takes up in the source.
This metric doesn’t discount comments or blank lines.*/

package dlut.edu.metric;

import net.sourceforge.pmd.lang.java.ast.ASTMethodDeclaration;
import net.sourceforge.pmd.lang.java.metrics.JavaMetrics;
import net.sourceforge.pmd.lang.java.metrics.api.JavaOperationMetricKey;
import net.sourceforge.pmd.lang.java.rule.AbstractJavaRule;

import java.io.*;

public class lineOfCodeMetric extends AbstractJavaRule {

    //度量项名
    private final String name = "函数的代码行数";
    //精度
    private final String precision = "1";

    public Object visit(ASTMethodDeclaration MD, Object data) {

        //度量名称
        String calculatorID = "lineOfCodeMetric";
        //得到监控的数值
        int num_monitor = (int) JavaMetrics.get(JavaOperationMetricKey.LOC, MD);
        //起始行数
        int begin_line = (int) MD.getBeginLine();
        //结束行数
        int end_line = (int) MD.getEndLine();
        //得到函数的位置
        String location = MD.getQualifiedName().toString();
        //设置输入到txt的内容
        String content = calculatorID + ";" + name + ";" + location + ";" + num_monitor + ";" + begin_line + ";"
                + end_line + ";" + precision + "\n";

        //写入的文件名
        String fileName = System.getProperty("user.dir")+"\\MetricTempFile.txt";
        //将内容写入
        File file = new File(fileName);
        try {
            FileWriter writer = new FileWriter(file, true); /*覆盖写入文件*/
            BufferedWriter out = new BufferedWriter(writer);
            out.append(content);
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return super.visit(MD, data);
    }
}
