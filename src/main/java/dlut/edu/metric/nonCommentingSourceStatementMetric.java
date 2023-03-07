/* Number of statements in a class or operation.
* That’s roughly equivalent to counting the number of semicolons and opening braces in the program. */

package dlut.edu.metric;

import net.sourceforge.pmd.lang.java.ast.ASTClassOrInterfaceDeclaration;
import net.sourceforge.pmd.lang.java.metrics.JavaMetrics;
import net.sourceforge.pmd.lang.java.metrics.api.JavaClassMetricKey;
import net.sourceforge.pmd.lang.java.rule.AbstractJavaRule;

import java.io.*;

public class nonCommentingSourceStatementMetric extends AbstractJavaRule {

    //度量项名
    private final String name = "类中操作语句数";
    //精度
    private final String precision = "1";

    public Object visit(ASTClassOrInterfaceDeclaration COID, Object data) {

        //度量名称
        String calculatorID = "nonCommentingSourceStatementMetric";
        //得到监控的数值
        int num_monitor = (int) JavaMetrics.get(JavaClassMetricKey.NCSS, COID);
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

        return super.visit(COID, data);
    }
}
