/* This counts the number of other classes a given class or operation relies on.
Classes from the package java.lang are ignored by default (can be changed via options).
Also primitives are not included into the count.*/

package dlut.edu.metric;

import net.sourceforge.pmd.lang.java.ast.ASTClassOrInterfaceDeclaration;
import net.sourceforge.pmd.lang.java.metrics.JavaMetrics;
import net.sourceforge.pmd.lang.java.metrics.api.JavaClassMetricKey;
import net.sourceforge.pmd.lang.java.metrics.api.JavaOperationMetricKey;
import net.sourceforge.pmd.lang.java.rule.AbstractJavaRule;
import net.sourceforge.pmd.lang.metrics.ResultOption;

import java.io.*;

public class classFanOutMetric extends AbstractJavaRule {

    //度量项名
    private final String name = "外部类数";
    //精度
    private final String precision = "1";

    public Object visit(ASTClassOrInterfaceDeclaration COID, Object data) {

        //度量名称
        String calculatorID = "classFanOutMetric";
        //记录类和接口中依赖外部类的数量
        int num_type_CLASS_FAN_OUT =
                (int) JavaMetrics.get(JavaClassMetricKey.CLASS_FAN_OUT, COID);
        //记录函数中依赖外部类的数量
        int num_opeation_CLASS_FAN_OUT =
                (int) JavaMetrics.get(JavaOperationMetricKey.CLASS_FAN_OUT, COID, ResultOption.HIGHEST);
        //得到监控的数值
        int num_monitor = (int) num_type_CLASS_FAN_OUT + num_opeation_CLASS_FAN_OUT;
        //起始行数
        int begin_line = (int) COID.getBeginLine();
        //结束行数
        int end_line = (int) COID.getEndLine();
        //得到函数的位置
        String location = COID.getQualifiedName().toString();

        //设置输入到txt的内容
        String content = calculatorID + ";" + name + ";" + location + ";" + num_monitor + ";" + begin_line + ";"
                + end_line + ";" + precision + "\n";

        //写入的文件名
        String fileName = System.getProperty("user.dir") + "\\MetricTempFile.txt";
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
        return super.visit(COID, data);
    }
}
